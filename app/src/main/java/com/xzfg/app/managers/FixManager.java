package com.xzfg.app.managers;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.FileObjectQueue.Converter;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.Fix;
import com.xzfg.app.model.Fixes;
import com.xzfg.app.model.PingResults;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.receivers.VolumeChangeObserver;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.services.ProfileService;
import com.xzfg.app.services.SMSService;
import com.xzfg.app.util.BatteryUtil;
import com.xzfg.app.util.MessageMetaDataUtil;
import com.xzfg.app.util.MessageUtil;
import com.xzfg.app.util.location.GpsWatcher;
import com.xzfg.app.util.location.LocationWatcherListener;
import com.xzfg.app.util.location.NetworkWatcher;
import com.xzfg.app.util.location.PassiveWatcher;

import org.apache.commons.io.IOUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import timber.log.Timber;


public class FixManager implements LocationWatcherListener {


    private final ReportingHandlerThread reportingHandlerThread;
    private final MovementHandlerThread movementHandlerThread;
    private final FixHandlerThread fixHandlerThread;
    private final GpsWatcher gpsWatcher;
    private final NetworkWatcher networkWatcher;
    private final PassiveWatcher passiveWatcher;
    private final PowerManager.WakeLock wakeLock;

    @Inject
    Application application;
    @Inject
    ConnectivityManager connectivityManager;
    @Inject
    LocationManager locationManager;
    @Inject
    PowerManager powerManager;
    @Inject
    Crypto crypto;
    @Inject
    OkHttpClient httpClient;
    @Inject
    SSLSocketFactory socketFactory;
    @Inject
    MediaManager mediaManager;

    private final TelephonyManager telephonyManager;

    private static volatile Location lastLocation;
    private static volatile MaxHandlerThread maxHandlerThread;
    private static volatile boolean tracking = false;
    private static volatile boolean paused = false;
    private static volatile long lastReceived = -1;
    private static volatile long trackingStart = 0;
    private static volatile long lastMovement = System.currentTimeMillis();
    private static volatile boolean watching = false;

    private static final Object watchLock = new Object();

    private MediaSessionCompat mediaSession;
    private VolumeChangeObserver volumeObserver;

    // SOS sequence
    final private String mSosSequence = "udud";
    final private long mSosSequenceMaxTime = 5000;
    private long mSosSequenceStart = 0;
    private String mSosSequenceQueue = "";

    private static final ConcurrentLinkedQueue<Fix> fixQueue = new ConcurrentLinkedQueue<>();
    private static final int MAX_FIXES = 86400;
    private final FileObjectQueue<Fix> fileQueue;

    public FixManager(Application application) {
        application.inject(this);
        telephonyManager = (TelephonyManager)application.getSystemService(Context.TELEPHONY_SERVICE);

        File queueFile;
        Gson gson = new Gson();
        Converter<Fix> converter = new GsonConverter<>(gson, Fix.class);
        FileObjectQueue<Fix> tmpQueue = null;

        try {
            queueFile = new File(application.getFilesDir(), "fixes.json");
            tmpQueue = new FileObjectQueue<>(queueFile, converter);
        }
        catch (Exception e) {
            Timber.e(e, "Couldn't setup fix file queue.");
        }

        if (tmpQueue == null) {
            fileQueue = null;
        }
        else {
            fileQueue = tmpQueue;
        }

        passiveWatcher = PassiveWatcher.getInstance(locationManager);
        passiveWatcher.setListener(this);

        gpsWatcher = GpsWatcher.getInstance(locationManager, powerManager);
        gpsWatcher.setListener(this);

        networkWatcher = NetworkWatcher.getInstance(locationManager, powerManager);
        networkWatcher.setListener(this);

        reportingHandlerThread = new ReportingHandlerThread();
        movementHandlerThread = new MovementHandlerThread();
        fixHandlerThread = new FixHandlerThread();

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, FixManager.class.getSimpleName());
        EventBus.getDefault().registerSticky(this);


        if (lastLocation == null) {
            Location lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastGps != null) {
                lastLocation = lastGps;
            }
            Location lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if ((lastLocation == null && lastNetwork != null) || lastLocation != null && lastNetwork != null && lastNetwork.getTime() > lastLocation.getTime()) {
                lastLocation = lastNetwork;
            }
            Location lastPassive = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if ((lastLocation == null && lastPassive != null) || lastLocation != null && lastPassive != null && lastPassive.getTime() > lastLocation.getTime()) {
                lastLocation = lastPassive;
            }
        }
        if (lastLocation != null) {
            lastReceived = lastLocation.getElapsedRealtimeNanos();
        }

        passiveWatcher.start();
    }

    public void createVolumeObserver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession = new MediaSessionCompat(application, "KdcVolumeObserver");
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 1, 1.0f)
                    .build());
            mediaSession.setPlaybackToRemote(createVolumeProvider(mediaSession));
            mediaSession.setActive(true);
        } else {
            volumeObserver = new VolumeChangeObserver(application, new Handler());
            application.getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, volumeObserver);
        }
    }

    public void destroyVolumeObserver() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        if (volumeObserver != null) {
            application.getApplicationContext().getContentResolver().unregisterContentObserver(volumeObserver);
            volumeObserver = null;
        }
    }

    private VolumeProviderCompat createVolumeProvider(MediaSessionCompat session) {
        // I don't use this callback directly, but i need to set it or my VolumeProvider will not work.
        // (sounds strange but i tried it several times)
        session.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(final Intent mediaButtonEvent) {
                //Timber.d("ddd: onMediaButtonEvent() called with: " + "mediaButtonEvent = [" + mediaButtonEvent + "]");
                return super.onMediaButtonEvent(mediaButtonEvent);
            }
        });

        return new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, 100, 100) {
            @Override
            public void onAdjustVolume(int direction) {
                super.onAdjustVolume(direction);

                int volume = getCurrentVolume();
                if (direction > 0) {
                    //Timber.d("ddd: onAdjustVolume(UP) called with: " + "direction = [" + direction + "]");
                    setCurrentVolume(volume + 1);
                }
                if (direction < 0) {
                    //Timber.d("ddd: onAdjustVolume(DOWN) called with: " + "direction = [" + direction + "]");
                    setCurrentVolume(volume - 1);
                }

                // Reset timed-out sequence
                long now = Calendar.getInstance().getTimeInMillis();
                long period = now - mSosSequenceStart;
                if (period > mSosSequenceMaxTime || mSosSequenceQueue.length() > mSosSequence.length()) {
                    mSosSequenceQueue = "";
                    mSosSequenceStart = now;
                }

                // UP
                if (direction > 0) {
                    mSosSequenceQueue += 'u';
                }
                // DOWN
                if (direction < 0) {
                    mSosSequenceQueue += 'd';
                }

                // Sequence completed?
                if (mSosSequence.equals(mSosSequenceQueue) && (period < mSosSequenceMaxTime)) {
                    mSosSequenceQueue = "";
                    // Send covert SOS
                    // We need EventBus to send this event, otherwise we have a circular
                    // dependency between the FixManager and the AlertManager
                    if (application.getAgentSettings().getAgentRoles().panicvolumebuttons()) {
                        EventBus.getDefault().post(new Events.SendPanicMessage(true));
                    }
                }
            }
        };
    }


    public boolean isTracking() {
        return tracking;
    }

    public void onEventMainThread(Events.MovementDetected event) {
        lastMovement = System.currentTimeMillis();
        //Timber.d("Movement Detected: " + lastMovement);
    }

    public void removeUpdates() {
        synchronized (watchLock) {
            if (watching) {
                gpsWatcher.stop();
                networkWatcher.stop();
                passiveWatcher.start();
                watching = false;
                //Timber.d("Location updates stopped.");
            }
        }
    }

    public void startUpdates() {
        synchronized (watchLock) {
            if (application.getAgentSettings() != null && !watching) {
                passiveWatcher.stop();
                int fixInterval = (application.getAgentSettings().getFixInterval() * 1000)/2;
                gpsWatcher.start(fixInterval, 0);
                networkWatcher.start(fixInterval, 0);
                watching = true;
                //Timber.d("Location updates activated.");
            }
        }
    }

    public void onEventMainThread(Events.StartLiveTracking event) {
        //Timber.d("Starting Live Tracking.");
        if (application.getAgentSettings() != null /*&& application.getAgentSettings().getAllowTracking() == 1*/) {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
            tracking = true;
            trackingStart = System.currentTimeMillis();
            startUpdates();

            maxHandlerThread = new MaxHandlerThread();
            EventBus.getDefault().postSticky(new Events.TrackingStatus(true));
            lastMovement = System.currentTimeMillis();
            MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_STARTED_TRACKING));
        }

    }

    public void onEventMainThread(Events.StopLiveTracking event) {
        //Timber.d("Stopping Live Tracking");
        removeUpdates();

        // stop looking fo max tracking time.
        if (maxHandlerThread != null) {
            MaxHandlerThread deadThread = maxHandlerThread;
            maxHandlerThread = null;
            deadThread.kill();
        }

        tracking = false;
        paused = false;
        trackingStart = 0;
        EventBus.getDefault().postSticky(new Events.TrackingStatus(false));
        MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_STOPPED_TRACKING));
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void onEventMainThread(Events.PauseLiveTracking event) {
        if (tracking) {
            if (event.isPauseRequired()) {
                //Timber.d("Pausing live tracking.");
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Device Stationary"));
                paused = true;
                removeUpdates();
            } else {
                paused = false;
                startUpdates();
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Device In Motion"));
            }
        }
    }

    public void onLocationChanged(Location location) {
        MessageUrl messageMetaData = MessageMetaDataUtil.getMessageUrl(application);
        MessageMetaDataUtil.sendMessage(application,messageMetaData);
        updateLocation(location);
        lastReceived = SystemClock.elapsedRealtimeNanos();

        if (!tracking || paused) {
            //Timber.e("Update received, but we're not tracking. Attempting to remove updates.");
            removeUpdates();
        }
    }

    /**
     * Updates the lastLocation if appropriate using a set of simple checks utilizing the accuracy
     * and distance from previous locations.  The more accurate location wins. This means that
     * sometimes a network location will trump a GPS location, especially in situations where
     * access to multiple GPS satellites is limited (indoors, in cities with lots of skyscrapers,
     * or outdoors with lots of tree cover)
     *
     * This could potentially be improved by looking at percentage change in accuracy, rather than
     * a simple check we have in place. Finding what values work best however would be a matter of
     * trial and error.
     *
     * @param location the new location
     */
    ArrayList<Location> arrayLocation =new ArrayList<>();
    int count = 0;
    boolean trying = false;
    private void updateLocation(Location location) {
        if (lastLocation == null) {
            EventBus.getDefault().post(new Events.MovementDetected());
            lastLocation = location;
            return;
        }

        // this new location is from the past. or is the old location from the future?
        // anyway, ignore it, because it's out of time.
        if (location.getElapsedRealtimeNanos() < lastLocation.getElapsedRealtimeNanos()) {
            return;
        }

        // GPS accuracy gets a "boost" (actually a decrease) of 33% so that in close situations
        // it will be preferred over a network location.
        float oldAccuracy = lastLocation.getAccuracy();
        float newAccuracy = location.getAccuracy();

        if (lastLocation.getProvider().equals("gps")) {
            oldAccuracy = oldAccuracy - (oldAccuracy*.33F);
        }

        if (location.getProvider().equals("gps")) {
            newAccuracy = newAccuracy - (newAccuracy*.33F);
            if (location.hasSpeed() && location.getSpeed() >= 0.22352) {
                EventBus.getDefault().post(new Events.MovementDetected());
            }
        }

        // this new location is more accurate use it instead.
        if (newAccuracy < oldAccuracy) {
            lastLocation = location;
            return;
        }

        // new location has the same accuracy. only take the new location if we've travelled beyond
        // it's accuracy range.
        if (newAccuracy == oldAccuracy) {
            if (location.distanceTo(lastLocation) > lastLocation.getAccuracy()) {
                // the distance travelled is beyond the accuracy range. count this as movement,
                // so that even if the device isn't being banged around, e.g., in a smooth ride,
                // we keep the GPS on.
                EventBus.getDefault().post(new Events.MovementDetected());
                lastLocation = location;
                return;
            }
        }

        // new location is less accurate. only take the new location if it's beyond the sum of the
        // accuracies.
        if (newAccuracy > oldAccuracy) {

            if(lastLocation.getAccuracy() * 1.5 <= location.getAccuracy())
            {
                trying = true;
                count++;

                if(count <= 3)
                {
                    arrayLocation.add(location);
                    startUpdates();

                    return;
                }
                else {

                    int index = arrayLocation.indexOf(Collections.min(arrayLocation, new Comparator<Location>() {
                        @Override
                        public int compare(Location o1, Location o2) {
                            return Float.compare(o1.getAccuracy(), o2.getAccuracy());
                        }
                    }));

                    lastLocation = arrayLocation.get(index);
                    count = 0;
                    trying = false;
                    arrayLocation.clear();

                    return;
                }
            }
            if(trying)
            {
                arrayLocation.add(location);

                int index = arrayLocation.indexOf(Collections.min(arrayLocation, new Comparator<Location>() {
                    @Override
                    public int compare(Location o1, Location o2) {
                        return Float.compare(o1.getAccuracy(), o2.getAccuracy());
                    }
                }));
                lastLocation = arrayLocation.get(index);

                count = 0;
                trying = false;
                arrayLocation.clear();

                return;
            }
            // distance between locations uses the centroids, and doesn't include the accuracy.
            // if the old location had an accuracy of 20m, and the new location has an accuracy of 40m,
            // we only care if the new location is at least 60m away.
            if (location.distanceTo(lastLocation) > location.getAccuracy() + lastLocation.getAccuracy()) {
                // again, we've detected travel beyond the combined accuracy range of the current
                // and previous updates. this is significant enough movement to warrant keeping the GPS
                // on, or turning it back on.
                EventBus.getDefault().post(new Events.MovementDetected());
                lastLocation = location;
            }
            else {
                // the amount of time since the last location update and this one.
                long delta = location.getElapsedRealtimeNanos() - lastLocation.getElapsedRealtimeNanos();

                long stopWait = 60L * 1000L * 1000L;


                if (application.isSetupComplete() && application.getAgentSettings() != null && application.getAgentSettings().getOnStop() != 0) {
                  stopWait = application.getAgentSettings().getOnStop() * 1000L * 1000L;
                }

                // if it's been more than the stop time since the last more accurate/moved location,
                // fire a movement detected event, which should ensure resume if we've been paused
                if (delta > stopWait) {
                    EventBus.getDefault().post(new Events.MovementDetected());
                }

                // at this point, we're not getting any new locations that are more accurate, and
                // locations we're getting aren't significantly farther away than the old locations.
                // nothing else to do here unless Chuck says otherwise.
            }

        }

    }


    public synchronized Location getLastLocation() {
        // if the last location is > 1 minute old, or we don't have a lastLocation, request an update.
        // this is done asynchronously, and we don't want to block on it, so it may not be updated
        // for awhile.

        if (lastLocation != null) {
            if (trying) {
                startUpdates();
            }
            if (lastReceived == -1) {
                //Timber.e("Setting lastReceived time to lastLocation time.");
                lastReceived = lastLocation.getElapsedRealtimeNanos();
            }
            if (!watching) {
                long lastLocationDiff = (SystemClock.elapsedRealtimeNanos() - lastReceived)/1000000L;
                // Do it only if location older than 1 minute and moving
                if (lastLocationDiff >= (60*1000L) && !paused) {
                    //Timber.e("Location requested, and last location was too old - " + String.valueOf(diff/1000000) + "ms.");
                    startUpdates();
                }
            }
        } else {
            startUpdates();
            MessageUrl messageMetaData = MessageMetaDataUtil.getMessageUrl(application);
            MessageMetaDataUtil.sendMessage(application,messageMetaData);
        }

        return lastLocation;
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }


    public class FixHandlerThread extends HandlerThread {
        private final Handler handler;
  //      private final Runnable runnable;

        public FixHandlerThread() {
            super(FixHandlerThread.class.getName(), Givens.THREAD_PRIORITY);
            start();
            handler = new Handler(getLooper());
           // runnable = new FixRunnable();
            //Timber.d("Fix Collector Initiated.");
          //  handler.post(runnable);
        }

        public int getInterval() {
            // uses the fix interval, otherwise runs every second - but it won't do anything unless it's tracking.
            if (application.isSetupComplete() && application.getAgentSettings().getFixInterval() > 0) {
                return application.getAgentSettings().getFixInterval() * 1000;
            } else {
                return 1000;
            }
        }

        private final class FixRunnable /* implements Runnable*/ {
//            public void run() {
//                //Timber.d("Running fix collector.");
//                if (tracking && !paused) {
//                    //Timber.d("Tracking is on.");
//                    Location location = getLastLocation();
//                    if (location != null) {
//                        final com.xzfg.app.model.Location fixLocation = new com.xzfg.app.model.Location(location);
//                        fixLocation.setBattery(BatteryUtil.getBatteryLevel(application));
//
//                        HashMap<String,String> metaData = new HashMap<>();
//                        if (location.hasBearing()) {
//                            metaData.put("Bearing", String.valueOf(location.getBearing()));
//                        }
//                        if (telephonyManager != null) {
//                            CellLocation cellLocation = telephonyManager.getCellLocation();
//                            if (cellLocation != null) {
//                                if (cellLocation instanceof GsmCellLocation) {
//                                    try {
//                                        GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
//                                        metaData.put(
//                                            "CID",
//                                            String.valueOf(gsmCellLocation.getCid())
//                                        );
//                                        metaData.put(
//                                            "LAC",
//                                            String.valueOf(gsmCellLocation.getLac())
//                                        );
//                                    } catch (Exception e) {
//                                        Timber.e(e, "Couldn't get GSM Cell Location");
//                                    }
//                                }
//                                if (cellLocation instanceof CdmaCellLocation) {
//                                    try {
//                                        CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
//                                        metaData.put(
//                                            "BaseStationId",
//                                            String.valueOf(cdmaCellLocation.getBaseStationId())
//                                        );
//                                        metaData.put(
//                                            "BaseStationLatitude",
//                                            String.valueOf(cdmaCellLocation.getBaseStationLatitude())
//                                        );
//                                        metaData.put(
//                                            "BaseStationLongitude",
//                                            String.valueOf(cdmaCellLocation.getBaseStationLongitude())
//                                        );
//                                        metaData.put(
//                                            "NetworkId",
//                                            String.valueOf(cdmaCellLocation.getNetworkId())
//                                        );
//                                        metaData.put(
//                                            "SystemId",
//                                            String.valueOf(cdmaCellLocation.getSystemId())
//                                        );
//                                    }
//                                    catch (Exception e) {
//                                        Timber.e(e,"Couldn'get CMDA Cell Location");
//                                    }
//                                }
//                            }
//                            try {
//                                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
//                                String networkType = activeNetwork.getTypeName();
//                                if (networkType != null) {
//                                    metaData.put("NetworkType", networkType);
//                                }
//                                else {
//                                    metaData.put("NetworkType","");
//                                }
//                                String networkSubType = activeNetwork.getSubtypeName();
//                                if (networkSubType != null) {
//                                    metaData.put("NetworkSubType", activeNetwork.getSubtypeName());
//                                }
//                                else {
//                                    metaData.put("NetworkSubType","");
//                                }
//                            }
//                            catch (Exception e) {
//                                Timber.e(e,"Network Info");
//                            }
//                            try {
//                                String networkOperatorName = telephonyManager.getNetworkOperatorName();
//                                if (networkOperatorName != null) {
//                                    metaData.put("NetworkOperator", networkOperatorName);
//                                }
//                                else {
//                                    metaData.put("NetworkOperator", "");
//                                }
//                            }
//                            catch (Exception e) {
//                                Timber.e(e,"Couldn't get network operator");
//                            }
//                        }
//
//                        if (!metaData.isEmpty()) {
//                            StringBuilder sb = new StringBuilder();
//                            for (Entry<String, String> entry : metaData.entrySet()) {
//                                if (sb.length() > 0) {
//                                    sb.append("|");
//                                }
//                                sb.append(entry.getKey() + "~" + entry.getValue());
//                            }
//                            if (sb.length() > 0) {
//                                fixLocation.setMetaData(sb.toString());
//                            }
//
//                        }
//                        // prefer file storage of fixes to memory storage.
//                        final Fix fix = new Fix(fixLocation);
//
//                        if (BuildConfig.DEBUG) {
//                            Timber.w("Captured Fix: " + new Gson().toJson(fix));
//                        }
//                        if (fileQueue != null) {
//                            fileQueue.add(fix);
//                        }
//                        else {
//                            if (fixQueue.size() == MAX_FIXES) {
//                                //Timber.d("Popping a fix to make room for more. Max Size: " + MAX_FIXES);
//                                fixQueue.poll();
//                            }
//                            fixQueue.add(fix);
//                        }
//
//                        // vibrate
//                        if (application.getAgentSettings().getVibrateOnFix() == 1) {
//                            Vibrator v = (Vibrator) application
//                                .getSystemService(Context.VIBRATOR_SERVICE);
//                            v.vibrate(500);
//                        }
//                    }
//                }
//                else {
//                    //Timber.d("Not tracking, nothing to do.");
//                }
//
//                //Timber.d("Fix collector run complete, trying again in " + getInterval() + " ms.");
//                handler.postDelayed(this, getInterval());
//            }
        }
    }

    private class MaxHandlerThread extends HandlerThread {
        private final Handler handler;
    //    private final Runnable runnable;

        public MaxHandlerThread() {
            super(MaxHandlerThread.class.getName(), Givens.THREAD_PRIORITY + android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
            start();
            handler = new Handler(getLooper());
            //runnable = new TimeCatcher();
            if (application.getAgentSettings().getMaxTracking() > 0) {
              //  handler.postDelayed(runnable, application.getAgentSettings().getMaxTracking() * 1000);
            }
        }

        public void kill() {
          //  handler.removeCallbacks(runnable);
            quit();
        }

        private final class TimeCatcher implements Runnable {
            @Override
            public void run() {
                if (trackingStart != 0)
                    EventBus.getDefault().post(new Events.StopLiveTracking());
            }
        }

    }

    private class MovementHandlerThread extends HandlerThread {
        private final Handler handler;
        private final Runnable runnable;

        public MovementHandlerThread() {
            super(MovementHandlerThread.class.getName(), Givens.THREAD_PRIORITY + android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
            start();
            handler = new Handler(getLooper());
            runnable = new MovementCatcher();
       //     handler.postDelayed(runnable, 1000);
        }

        private final class MovementCatcher implements Runnable {
            @Override
            public void run() {
                AgentSettings settings = application.getAgentSettings();
                if (settings == null) {
                    settings = new AgentSettings();
                }
                int stopDelay = settings.getOnStop();
                int sensitivity = settings.getSensitivity();

                if (tracking && sensitivity > 0 && stopDelay > 0) {
                    if (!paused) {
                        if ((System.currentTimeMillis() - lastMovement) > (stopDelay * 1000)) {
                            EventBus.getDefault().post(new Events.PauseLiveTracking(true));
                        }
                    } else {
                        if ((System.currentTimeMillis() - lastMovement) <= (stopDelay * 1000)) {
                            EventBus.getDefault().post(new Events.PauseLiveTracking(false));
                        }
                    }
                }
              //  handler.postDelayed(this, 1000);
            }
        }

    }

    private class ReportingHandlerThread extends HandlerThread {
        private final Handler handler;
        private final Runnable runnable;

        public ReportingHandlerThread() {
            super(ReportingHandlerThread.class.getName(), Givens.THREAD_PRIORITY);
            start();
            handler = new Handler(getLooper());
            runnable = new Reporter(false);
          //  handler.postDelayed(runnable, getInterval());
            EventBus.getDefault().registerSticky(this);
        }

        public int getInterval() {
            int interval = 1;
            if (application.isSetupComplete() && application.getAgentSettings() != null) {
                interval = application.getAgentSettings().getReportInterval();
                if (interval == 0)
                    interval = application.getAgentSettings().getFixInterval();
            }
            return interval * 1000;
        }

        public void onEventMainThread(Events.SendFixes event) {
            //Timber.d("Force send location fixes");

            Location location = getLastLocation();
            if (location != null) {
                final com.xzfg.app.model.Location fixLocation = new com.xzfg.app.model.Location(
                    location);
                fixLocation.setBattery(BatteryUtil.getBatteryLevel(application));

                HashMap<String, String> metaData = new HashMap<>();
                if (location.hasBearing()) {
                    metaData.put("Bearing", String.valueOf(location.getBearing()));
                }
                if (telephonyManager != null) {
                    CellLocation cellLocation = telephonyManager.getCellLocation();
                    if (cellLocation != null) {
                        if (cellLocation instanceof GsmCellLocation) {
                            try {
                                GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
                                metaData.put(
                                    "CID",
                                    String.valueOf(gsmCellLocation.getCid())
                                );
                                metaData.put(
                                    "LAC",
                                    String.valueOf(gsmCellLocation.getLac())
                                );
                            } catch (Exception e) {
                                Timber.e(e, "Couldn't get GSM Cell Location");
                            }
                        }
                        if (cellLocation instanceof CdmaCellLocation) {
                            try {
                                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
                                metaData.put(
                                    "BaseStationId",
                                    String.valueOf(cdmaCellLocation.getBaseStationId())
                                );
                                metaData.put(
                                    "BaseStationLatitude",
                                    String.valueOf(cdmaCellLocation.getBaseStationLatitude())
                                );
                                metaData.put(
                                    "BaseStationLongitude",
                                    String.valueOf(cdmaCellLocation.getBaseStationLongitude())
                                );
                                metaData.put(
                                    "NetworkId",
                                    String.valueOf(cdmaCellLocation.getNetworkId())
                                );
                                metaData.put(
                                    "SystemId",
                                    String.valueOf(cdmaCellLocation.getSystemId())
                                );
                            }
                            catch (Exception e) {
                                Timber.e(e,"Couldn'get CMDA Cell Location");
                            }
                        }
                    }
                    try {
                        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                        String networkType = activeNetwork.getTypeName();
                        if (networkType != null) {
                            metaData.put("NetworkType", networkType);
                        }
                        else {
                            metaData.put("NetworkType","");
                        }
                        String networkSubType = activeNetwork.getSubtypeName();
                        if (networkSubType != null) {
                            metaData.put("NetworkSubType", activeNetwork.getSubtypeName());
                        }
                        else {
                            metaData.put("NetworkSubType","");
                        }
                    }
                    catch (Exception e) {
                        Timber.e(e,"Network Info");
                    }
                    try {
                        String networkOperatorName = telephonyManager.getNetworkOperatorName();
                        if (networkOperatorName != null) {
                            metaData.put("NetworkOperator", networkOperatorName);
                        }
                        else {
                            metaData.put("NetworkOperator", "");
                        }
                    }
                    catch (Exception e) {
                        Timber.e(e,"Couldn't get network operator");
                    }
                }

                if (!metaData.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Entry<String, String> entry : metaData.entrySet()) {
                        if (sb.length() > 0) {
                            sb.append("|");
                        }
                        sb.append(entry.getKey() + "~" + entry.getValue());
                    }
                    if (sb.length() > 0) {
                        fixLocation.setMetaData(sb.toString());
                    }

                }
                // prefer file storage of fixes to memory storage.
                final Fix fix = new Fix(fixLocation);
                if (fileQueue != null) {
                    if (fileQueue.size() < 1) {
                        fileQueue.add(fix);
                    }
                }
                else {
                    if (fixQueue.isEmpty()) {
                        fixQueue.add(fix);
                    }
                }
            }

            // This handler will call sendFixes()
            Handler handler = new Handler(getLooper());
            Runnable runnable = new Reporter(true);
          //  handler.postDelayed(runnable, 1);
        }

        public class Reporter implements Runnable {
            private boolean singleRunInstance;

            public Reporter(boolean singleRunInstance) {
                this.singleRunInstance = singleRunInstance;
            }

            private void stopMedia() throws Exception {
                mediaManager.stopStreamingVideo();
                mediaManager.stopBackgroundRecording();
                mediaManager.stopVideoRecording();
                mediaManager.stopAudioRecording();
                mediaManager.stopAudioStreaming();
                int count = 0;
                while (mediaManager.isRecording()) {
                    // increment counter.
                    count++;
                    // sleep for 10ms.
                    Thread.sleep(10);
                    // if we've been here for 2 seconds, give up.
                    if (count >= 200) {
                        break;
                    }
                }
            }

            private void sendFixes(Fixes fixes) throws Exception {
                if (!isConnected() && application.getAgentSettings().getAgentRoles().SMS_Fixes() && application.getAgentSettings().getSMSPhoneNumber() != null) {
                    Intent smsIntent = new Intent(application, SMSService.class);
                    smsIntent.putExtra(SMSService.TYPE_IDENTIFIER, SMSService.TYPE_POSITION);
                    smsIntent.putExtra(SMSService.PUBLIC_IDENTIFIER, SMSService.PUBLIC);
                    application.startService(smsIntent);
                    return;
                }

                if (isConnected() && fixes != null && !fixes.getFixes().isEmpty()) {

                    //Timber.d("Serializing fixes.");
                    final Format format = new Format(0);
                    Serializer serializer = new Persister(format);
                    StringWriter sw = new StringWriter();
                    serializer.write(fixes, sw);
                    //Timber.d("Fixes Serialized.");

                    String responseBody = "";
                    String xml = "SendXml=xml=" + sw.toString() + "\n";
                    if (BuildConfig.DEBUG) {
                        Timber.w("Sending XML: " + xml);

                    }
                    String content = crypto.encryptToHex(xml);
                    String ipAddress = application.getScannedSettings().getIpAddress();
                    if (ipAddress.contains("://")) {
                        ipAddress = ipAddress.split("://")[1];
                    }

                    SSLSocket socket = null;
                    OutputStream os = null;
                    InputStream is = null;

                    try {
                        socket = (SSLSocket) socketFactory.createSocket();
                        socket.connect(new InetSocketAddress(ipAddress, application.getScannedSettings().getTrackingPort().intValue()), 20000);
                        os = socket.getOutputStream();
                        os.write(content.getBytes("UTF-8"));
                        os.flush();

                        is = socket.getInputStream();
                        if (is != null) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(is));
                            String line = null;
                            StringBuilder response = new StringBuilder();
                            while ((line = in.readLine()) != null) {
                                response.append(line);
                            }
                            responseBody = response.toString().trim();
                        }

                        // Request last address from server
                        ProfileService.getLastAddress(application);
                    } catch (Exception ex) {
                        // TODO: Handle SSL errors here...
                        String msg = ex.getMessage();
                    } finally {
                        if (socket != null && !socket.isClosed()) {
                            socket.close();
                        }
                        if (os != null) {
                            IOUtils.closeQuietly(os);
                        }
                        if (is != null) {
                            IOUtils.closeQuietly(is);
                        }
                    }


                    //Timber.d("Calling " + url);
                    //Timber.d("Payload (Unencrypted): " + "xml=" + xml);
                    //Timber.d("Payload (Encrypted): " + "xml=" + content);
                    //Timber.d("Response (undecrypted): " + responseBody);


                    // if we don't receive a 200 ok, it's a network error.
                    if (responseBody.contains("Error") || responseBody.contains("ERROR") || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
                        if (BuildConfig.DEBUG) {
                            Crashlytics.setString("Url", ipAddress);
                            Crashlytics.setString("Content", xml);
                            Crashlytics.setString("Response", responseBody);
                        }
                        throw new Exception("Server did not respond appropriately.");
                    }

                    responseBody = crypto.decryptFromHexToString(responseBody).trim();
                    //Timber.d("Fix Response Body decrypted: " + responseBody);

                    if (responseBody.contains("ERROR") || responseBody.contains("Error") || responseBody.contains("Invalid SessionId") || responseBody.contains("Invalid Session Id") || responseBody.startsWith("ok")) {
                        if (BuildConfig.DEBUG) {
                            Crashlytics.setString("Url", ipAddress);
                            Crashlytics.setString("Content", xml);
                            Crashlytics.setString("Response", responseBody);
                        }
                        throw new Exception("Server returned us an error.");
                    }

                    if (!responseBody.startsWith("OK")) {
                        //Timber.d("Attempting to parse response as if it was a ping response.");
                        try {
                            PingResults pingResults = PingResults.parse(application, responseBody);

                            if (pingResults.getCommands() != null && !pingResults.getCommands().isEmpty()) {
                                List<String> commands = pingResults.getCommands();
                                for (int i = 0; i < commands.size(); i++) {
                                    String command = commands.get(i).trim();
                                    // Check if parameter follows command
                                    int commandParamPos = command.indexOf(" ");
                                    String commandParam = commandParamPos > 0 ? command.substring(commandParamPos).trim() : "";
                                    if (commandParamPos > 0) {
                                        command = command.substring(0, commandParamPos).trim();
                                    }
                                    //Timber.d("Command:" + command + ".");

                                    switch (command) {
                                        // live audio
                                        case "startAudio": {
                                            if (mediaManager.isRecording()) {
                                                stopMedia();
                                            }
                                            if (!mediaManager.isRecording()) {
                                                mediaManager.setCaptureMode(Givens.COLLECT_MODE_AUDIO_LIVE);
                                                int maxDuration = commandParam.isEmpty() ? -1 : Integer.valueOf(commandParam) * 1000;
                                                mediaManager.startAudioStreaming(maxDuration);
                                            }
                                            break;
                                        }
                                        case "stopAudio":
                                            mediaManager.stopAudioStreaming();
                                            break;
                                        // live video
                                        case "startVideo":
                                            if (mediaManager.isRecording()) {
                                                stopMedia();
                                            }
                                            if (!mediaManager.isRecording()) {
                                                if (mediaManager.previewModes.contains(mediaManager.getCaptureMode())) {
                                                    EventBus.getDefault().post(new Events.CameraRequested());
                                                    Thread.sleep(1500);
                                                }
                                                mediaManager.setCaptureMode(Givens.COLLECT_MODE_VIDEO_LIVE);
                                                int maxDuration = commandParam.isEmpty() ? -1 : Integer.valueOf(commandParam) * 1000;
                                                mediaManager.startBackgroundStreaming(maxDuration);
                                            }
                                            break;
                                        case "stopVideo":
                                            mediaManager.stopBackgroundStreaming();
                                            break;
                                        // live tracking
                                        case "startLiveTrack":
                                            EventBus.getDefault().post(new Events.StartLiveTracking());
                                            break;
                                        case "stopLiveTrack":
                                            EventBus.getDefault().post(new Events.StopLiveTracking());
                                            break;
                                        // switch camera
                                        case "toggleCameraView":
                                            if (mediaManager.isRecording()) {
                                                stopMedia();
                                            }
                                            //noinspection deprecation
                                            if (Camera.getNumberOfCameras() > 1) {
                                                if (mediaManager.getCameraId() == 1) {
                                                    mediaManager.setCameraId(0);
                                                } else {
                                                    mediaManager.setCameraId(1);
                                                }
                                                EventBus.getDefault().post(new Events.CameraChanged());
                                            }
                                            break;
                                        // boss mode
                                        case "bossMode":
                                            MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Set Settings|BossCASESAgent@1"));
                                            if (pingResults.getAgentSettings() != null) {
                                                pingResults.getAgentSettings().setBoss(1);
                                            } else {
                                                if (application.getAgentSettings() != null) {
                                                    application.getAgentSettings().setBoss(1);
                                                }
                                            }
                                            EventBus.getDefault().post(new Events.BossModeStatus(Givens.ACTION_BOSSMODE_ENABLE));

                                            if (pingResults.getAgentSettings() != null) {
                                                EventBus.getDefault().post(new Events.AgentSettingsAcquired(pingResults.getAgentSettings()));
                                            } else {
                                                EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                                            }
                                            break;
                                        // take photo
                                        case "takePhoto":
                                            if (mediaManager.isRecording()) {
                                                stopMedia();
                                            }
                                            if (!mediaManager.isRecording()) {
                                                if (mediaManager.previewModes.contains(mediaManager.getCaptureMode())) {
                                                    EventBus.getDefault().post(new Events.CameraRequested());
                                                    Thread.sleep(1500);
                                                }
                                                mediaManager.setCaptureMode(Givens.COLLECT_MODE_PICTURE);
                                                // Take photo and send it to the server
                                                mediaManager.takePhoto(1);
                                            }
                                            break;
                                        // lock screen
                                        case "lockScreen":
                                            if (application.isAdminReady()) {
                                                ((DevicePolicyManager) application.getSystemService(Context.DEVICE_POLICY_SERVICE)).lockNow();
                                            }
                                            break;
                                        // wipe data.
                                        case "wipeData":
                                            Toast.makeText(application, application.getString(R.string.wipe_notice), Toast.LENGTH_SHORT).show();
                                            if (!BuildConfig.DEBUG) {
                                                if (application.isAdminReady()) {
                                                    ((DevicePolicyManager) application.getSystemService(Context.DEVICE_POLICY_SERVICE)).wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
                                                }
                                            }
                                            break;
                                        // record video
                                        case "startVideoCase":
                                            if (mediaManager.isRecording()) {
                                                stopMedia();
                                            }
                                            if (!mediaManager.isRecording() && application.isSetupComplete() && application.getAgentSettings().getSecurity() == 0) {
                                                if (mediaManager.previewModes.contains(mediaManager.getCaptureMode())) {
                                                    EventBus.getDefault().post(new Events.CameraRequested());
                                                    Thread.sleep(1500);
                                                }
                                                mediaManager.setCaptureMode(Givens.COLLECT_MODE_VIDEO);
                                                int maxDuration = commandParam.isEmpty() ? -1 : Integer.valueOf(commandParam) * 1000;
                                                mediaManager.startBackgroundRecording(maxDuration);
                                            }
                                            break;

                                        case "stopVideoCase":
                                            mediaManager.stopBackgroundRecording();
                                            break;

                                        // record audio
                                        case "startAudioCase":
                                            if (mediaManager.isRecording()) {
                                                stopMedia();
                                            }
                                            if (!mediaManager.isRecording() && application.isSetupComplete() && application.getAgentSettings().getSecurity() == 0) {
                                                if (!mediaManager.isRecording()) {
                                                    mediaManager.setCaptureMode(Givens.COLLECT_MODE_AUDIO);
                                                    int maxDuration = commandParam.isEmpty() ? -1 : Integer.valueOf(commandParam) * 1000;
                                                    mediaManager.startAudioRecording(maxDuration);
                                                }
                                            }
                                            break;

                                        case "stopAudioCase":
                                            mediaManager.stopAudioRecording();
                                            break;

                                        case "getLastKnownPosition":
                                            //Timber.d("Force send location fixes");
                                            EventBus.getDefault().post(new Events.SendFixes());
                                            break;

                                        case "unregister":
                                            //Timber.d("Unregister app - FixManager");
                                            application.clearApplicationData(true);
                                            break;

                                        case "cancelSOS":
                                            // we use the event bus to cancel the panic mode,
                                            // since we have a recursive relationship between
                                            // fix manager and alert manager.
                                            EventBus.getDefault().post(new Events.CancelPanicMode("C2 " + command));
                                            break;

                                        default:
                                            Timber.w("Caught unknown C2 command: " + command);
                                            break;
                                    }
                                    if (i < commands.size()) {
                                        //Timber.d("Sleeping so command can finish.");
                                        Thread.sleep(2000);
                                    }
                                }
                            }
                            if (pingResults.getAgentSettings() != null) {
                                //Timber.d("Got Agent Settings On Ping.");
                                EventBus.getDefault().post(new Events.AgentSettingsAcquired(pingResults.getAgentSettings()));
                            }
                        } catch (Exception e) {
                            Timber.w(e, "Failed to parse ping response");
                        }
                    }
                }
            }


            @Override
            public void run() {

                //Timber.d("Fix queue contains: " + fixQueue.size());
                if (isConnected() &&
                    (
                        (fileQueue != null && fileQueue.size() > 0) || !fixQueue.isEmpty()
                    )
                    && application.getAgentSettings() != null
                    && application.getScannedSettings() != null
                    ) {
                    //Timber.d("We are connected, have settings, and have fixes, begin send attempt.");
                    Fixes fixes = new Fixes(application.getScannedSettings().getOrganizationId() + application.getDeviceIdentifier(), application.getScannedSettings().getPassword());
                    //Timber.d("New fix object instatiated. Removing fixes from global queue and adding to local queue.");

                    ConcurrentLinkedQueue<Fix> queue = new ConcurrentLinkedQueue<>();
                    if (fileQueue != null) {
                        while (fileQueue.size() > 0) {
                            Fix fix = fileQueue.peek();
                            if (fix != null) {
                                queue.add(fix);
                            }
                            fileQueue.remove();
                        }
                    }
                    else {
                        queue.addAll(fixQueue);
                        fixQueue.clear();
                    }

                    fixes.setFixes(queue);

                    try {
                        sendFixes(fixes);
                    }
                    catch (Exception e) {
                        if (fileQueue != null) {
                            for (Fix fix : queue) {
                                fileQueue.add(fix);
                            }
                        }
                        else {
                            fixQueue.addAll(queue);
                        }
                    }
                }

                //Timber.d("Upload attempt completed. Fix queue contains " + fixQueue.size());
                if (!singleRunInstance) {
              //      handler.postDelayed(runnable, getInterval());
                }
            }
        }
    }


    public class GsonConverter<T> implements FileObjectQueue.Converter<T> {
        private final Gson gson;
        private final Class<T> type;

        public GsonConverter(Gson gson, Class<T> type) {
            this.gson = gson;
            this.type = type;
        }

        @Override public T from(byte[] bytes) {
            Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
            return gson.fromJson(reader, type);
        }

        @Override public void toStream(T object, OutputStream bytes) throws IOException {
            Writer writer = new OutputStreamWriter(bytes);
            gson.toJson(object, writer);
            writer.close();
        }
    }
}

