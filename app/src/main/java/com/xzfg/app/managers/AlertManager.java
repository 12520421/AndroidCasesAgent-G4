package com.xzfg.app.managers;

import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.fragments.home.PanicStates;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.AlertContent;
import com.xzfg.app.model.AlertHeader;
import com.xzfg.app.model.url.AlertsUrl;
import com.xzfg.app.model.url.HeaderUrl;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.services.SMSService;
import com.xzfg.app.util.DateTransformer;
import com.xzfg.app.util.MessageUtil;
import com.xzfg.app.util.Network;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.RegistryMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

import com.xzfg.app.fragments.home.HomeFragment;
/**
 */
public class AlertManager {

    private static final Object threadLock = new Object();
    private static final int SHORT_INTERVAL = 30 * 1000;
    private static final int LONG_INTERVAL = 60 * 1000;
    @Inject
    Crypto crypto;
    @Inject
    Application application;
    @Inject
    ConnectivityManager connectivityManager;
    @Inject
    SessionManager sessionManager;
    @Inject
    OkHttpClient httpClient;
    @Inject
    FixManager fixManager;
    @Inject
    MediaManager mediaManager;

    private volatile AlertHeader header;
    private volatile ConcurrentHashMap<Object, List<AlertContent.Record>> data = new ConcurrentHashMap<>();

    private UpdateHandlerThread updateHandlerThread;

    private volatile boolean started = false;

    private String selectedId = null;
    private volatile boolean selected = false;

    public AlertManager(Application application) {
        application.inject(this);
        EventBus.getDefault().registerSticky(this);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void onResumeFromUI() {
        started = true;
        onResume();
    }

    public void onPauseFromUI() {
        started = false;
        clearSelectedId();
        onPause();
        data.clear();
        header = null;
        System.gc();

    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    public void onEventMainThread(Events.NetworkAvailable networkAvailable) {
        onResume();
    }

    public void onEventMainThread(Events.NetworkStatus status) {
        if (!status.isUp()) {
            onPause();
        } else {
            onResume();
        }
    }

    public void onEventMainThread(Events.Session session) {
        if (session.getSessionId() != null) {
            onResume();
        } else {
            onPause();
        }
    }

    public void onEventMainThread(Events.SendPanicMessage event) {
        startPanicMode(event.isCovert());
    }

    public void onEventMainThread(Events.CancelPanicMode event) {
        stopPanicMode(false, event.getReason());
    }

    public void onResume() {
        synchronized (threadLock) {
            if (started) {
                if (isConnected()) {
                    if (updateHandlerThread == null) {
                        updateHandlerThread = new UpdateHandlerThread();
                    }
                }
            }
        }
    }

    public void onPause() {
        synchronized (threadLock) {
            if (updateHandlerThread != null) {
                UpdateHandlerThread deadThread = updateHandlerThread;
                updateHandlerThread = null;
             //   deadThread.kill();
            }
        }
    }

    public List<AlertHeader.Record> getHeaders() {
        if (header == null) {
            return null;
        }
        return Collections.unmodifiableList(header.getRecords());
    }

    public List<AlertContent.Record> getRecords(Object key) {
        if (data.containsKey(key)) {
            List<AlertContent.Record> records = data.get(key);
            if (records != null && !records.isEmpty()) {
                return Collections.unmodifiableList(records);
            }
        }
        return null;
    }

    public int getInterval() {
        if (selected)
            return SHORT_INTERVAL;
        else
            return LONG_INTERVAL;
    }

    public void clearSelectedId() {
        selectedId = null;
    }

    public void setSelectedId(String groupId) {
        if (groupId == null) {
            selectedId = "null";
        } else {
            selectedId = groupId;
        }
    }

    // Start panic mode
    public void startPanicMode(boolean covert) {
        startPanicMode(covert,null);
    }
    public void startPanicDuress(boolean covert){
        startPanicDuress(covert,null);
    }

    public void cancelPanicDuress(boolean covert,String reason){
        final AgentSettings settings = application.getAgentSettings();

        // if we're not connected
        if (!isConnected()) {

            // if we're not covert, allow the use of sms.
            if (!covert) {
                if (application.getAgentSettings().getAgentRoles().SMS_SOS()
                        && application.getAgentSettings().getSMSPhoneNumber() != null) {
                    Intent smsIntent = new Intent(application, SMSService.class);
                    smsIntent.putExtra(SMSService.TYPE_IDENTIFIER, SMSService.TYPE_SOS);
                    smsIntent.putExtra(SMSService.PUBLIC_IDENTIFIER, SMSService.PUBLIC);
                    application.startService(smsIntent);
                }
                else {
                    // if w'ere not configured for SMS, tell the user to use the internet.
                    Toast.makeText(
                            application.getApplicationContext(),
                            application.getString(R.string.sos_connect),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
            else {
                // if we are covert, and we don't have an internet connection, display a generic
                // connect toast.
                Toast.makeText(
                        application.getApplicationContext(),
                        application.getString(R.string.covert_connect),
                        Toast.LENGTH_LONG
                ).show();
            }
        }
        else {
            String message = MessageUrl.MESSAGE_SOS_DURESS;
            if (reason != null) {
                message += " " + reason;
            }
            // if we are connected, attempt to send the message via the network
            MessageUrl messageUrl = MessageUtil.getMessageUrl(application, message);
            MessageUtil.sendMessage(application.getApplicationContext(), messageUrl);
        }

        // request the fix manager to send fixes.
        EventBus.getDefault().post(new Events.SendFixes());

        // enable boss mode if the user has the boss mode role.
        if (covert) {/*
            if (settings.getAgentRoles().bossmode()) {
                settings.setBoss(1);
                MessageUtil.sendMessage(
                        application,
                        MessageUtil.getMessageUrl(application, "Set Settings|BossCASESAgent@1")
                );

                // let the UI know to enable boss mode.
                EventBus.getDefault().post(
                        new Events.BossModeStatus(Givens.ACTION_BOSSMODE_ENABLE)
                );
            }

            // if the user has screen lock permissions, attempt to lock the screen.
            try {
                if (application.isAdminReady()) {
                    ((DevicePolicyManager) application
                            .getSystemService(Context.DEVICE_POLICY_SERVICE)).lockNow();
                }
            }
            catch (Exception e) {
                Timber.e(e,"Couldn't lock the device.");
            }*/
        }

        // turn live tracking on.
        if (settings.getAllowTracking() < 1) {
            settings.setAllowTracking(1);
            MessageUtil.sendMessage(
                    application,
                    MessageUtil.getMessageUrl(application, "Set Settings|AllowLiveTracking@1")
            );
        }

        // Save panic state to settings
        settings.setPanicState(PanicStates.PANIC_DURESS.getValue());


        // let everybody know about the new settings.
       EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));

        // start live tracking
        EventBus.getDefault().post(new Events.StartLiveTracking());

        // send a movement detected event so that live tracking will be unpaused
         EventBus.getDefault().post(new Events.MovementDetected());

        // record audio
      //  mediaManager.startAudioRecording(15000);
    }

    public void startPanicMode(boolean covert, String reason) {
        final AgentSettings settings = application.getAgentSettings();

        // if we're not connected
        if (!isConnected()) {

            // if we're not covert, allow the use of sms.
            if (!covert) {
                if (application.getAgentSettings().getAgentRoles().SMS_SOS()
                    && application.getAgentSettings().getSMSPhoneNumber() != null) {
                    Intent smsIntent = new Intent(application, SMSService.class);
                    smsIntent.putExtra(SMSService.TYPE_IDENTIFIER, SMSService.TYPE_SOS);
                    smsIntent.putExtra(SMSService.PUBLIC_IDENTIFIER, SMSService.PUBLIC);
                    application.startService(smsIntent);
                }
                else {
                    // if w'ere not configured for SMS, tell the user to use the internet.
                    Toast.makeText(
                        application.getApplicationContext(),
                        application.getString(R.string.sos_connect),
                        Toast.LENGTH_LONG
                    ).show();
                }
            }
            else {
                // if we are covert, and we don't have an internet connection, display a generic
                // connect toast.
                Toast.makeText(
                    application.getApplicationContext(),
                    application.getString(R.string.covert_connect),
                    Toast.LENGTH_LONG
                ).show();
            }
        }
        else {
            String message = MessageUrl.MESSAGE_SOS;
            if (reason != null) {
                message += " " + reason;
            }
            // if we are connected, attempt to send the message via the network
            MessageUrl messageUrl = MessageUtil.getMessageUrl(application, message);
            MessageUtil.sendMessage(application.getApplicationContext(), messageUrl);
        }

        // request the fix manager to send fixes.
        EventBus.getDefault().post(new Events.SendFixes());

        // enable boss mode if the user has the boss mode role.
        if (covert) {
            if (settings.getAgentRoles().bossmode()) {
                settings.setBoss(1);
                MessageUtil.sendMessage(
                    application,
                    MessageUtil.getMessageUrl(application, "Set Settings|BossCASESAgent@1")
                );

                // let the UI know to enable boss mode.
                EventBus.getDefault().post(
                    new Events.BossModeStatus(Givens.ACTION_BOSSMODE_ENABLE)
                );
            }

            // if the user has screen lock permissions, attempt to lock the screen.
            try {
                if (application.isAdminReady()) {
                    ((DevicePolicyManager) application
                        .getSystemService(Context.DEVICE_POLICY_SERVICE)).lockNow();
                }
            }
            catch (Exception e) {
                Timber.e(e,"Couldn't lock the device.");
            }
        }

        // turn live tracking on.
        if (settings.getAllowTracking() < 1) {
            settings.setAllowTracking(1);
            MessageUtil.sendMessage(
                application,
                MessageUtil.getMessageUrl(application, "Set Settings|AllowLiveTracking@1")
            );
        }

        // Save panic state to settings
        settings.setPanicState(PanicStates.PANIC_ON.getValue());

        // let everybody know about the new settings.
        EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));

        // start live tracking
        EventBus.getDefault().post(new Events.StartLiveTracking());

        // send a movement detected event so that live tracking will be unpaused
       // EventBus.getDefault().post(new Events.MovementDetected());

        // record audio
        mediaManager.startAudioRecording(15000);

    }
    public void startPanicDuress(boolean covert, String reason) {
        final AgentSettings settings = application.getAgentSettings();

        // if we're not connected
        if (!isConnected()) {

            // if we're not covert, allow the use of sms.
            if (!covert) {
                if (application.getAgentSettings().getAgentRoles().SMS_SOS()
                        && application.getAgentSettings().getSMSPhoneNumber() != null) {
                    Intent smsIntent = new Intent(application, SMSService.class);
                    smsIntent.putExtra(SMSService.TYPE_IDENTIFIER, SMSService.TYPE_SOS);
                    smsIntent.putExtra(SMSService.PUBLIC_IDENTIFIER, SMSService.PUBLIC);
                    application.startService(smsIntent);
                }
                else {
                    // if w'ere not configured for SMS, tell the user to use the internet.
                    Toast.makeText(
                            application.getApplicationContext(),
                            application.getString(R.string.sos_connect),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
            else {
                // if we are covert, and we don't have an internet connection, display a generic
                // connect toast.
                Toast.makeText(
                        application.getApplicationContext(),
                        application.getString(R.string.covert_connect),
                        Toast.LENGTH_LONG
                ).show();
            }
        }
        else {
            String message = MessageUrl.MESSAGE_SOS_DURESS;
            if (reason != null) {
                message += " " + reason;
            }
            // if we are connected, attempt to send the message via the network
            MessageUrl messageUrl = MessageUtil.getMessageUrl(application, message);
            MessageUtil.sendMessage(application.getApplicationContext(), messageUrl);
        }

        // request the fix manager to send fixes.
        EventBus.getDefault().post(new Events.SendFixes());

        // enable boss mode if the user has the boss mode role.
        if (covert) {
            if (settings.getAgentRoles().bossmode()) {
                settings.setBoss(1);
                MessageUtil.sendMessage(
                        application,
                        MessageUtil.getMessageUrl(application, "Set Settings|BossCASESAgent@1")
                );

                // let the UI know to enable boss mode.
                EventBus.getDefault().post(
                        new Events.BossModeStatus(Givens.ACTION_BOSSMODE_ENABLE)
                );
            }

            // if the user has screen lock permissions, attempt to lock the screen.
            try {
                if (application.isAdminReady()) {
                    ((DevicePolicyManager) application
                            .getSystemService(Context.DEVICE_POLICY_SERVICE)).lockNow();
                }
            }
            catch (Exception e) {
                Timber.e(e,"Couldn't lock the device.");
            }
        }

        // turn live tracking on.
        if (settings.getAllowTracking() < 1) {
            settings.setAllowTracking(1);
            MessageUtil.sendMessage(
                    application,
                    MessageUtil.getMessageUrl(application, "Set Settings|AllowLiveTracking@1")
            );
        }

        // Save panic state to settings
        settings.setPanicState(PanicStates.PANIC_ON.getValue());

        // let everybody know about the new settings.
        EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));

        // start live tracking
        EventBus.getDefault().post(new Events.StartLiveTracking());

        // send a movement detected event so that live tracking will be unpaused
        EventBus.getDefault().post(new Events.MovementDetected());

        // record audio
       // mediaManager.startAudioRecording(15000);

    }

    public void stopPanicMode(boolean duress, String reason) {
      final AgentSettings settings = application.getAgentSettings();
      if (duress) {
          settings.setPanicState(PanicStates.PANIC_OFF.getValue());
         /* MessageUtil.sendMessage(
              application,
              MessageUtil.getMessageUrl(
                  application,
                  MessageUrl.MESSAGE_SOS + " " + reason
              )

          );*/
          MessageUtil.sendMessage(
                  application,
                  MessageUtil.getMessageUrl(
                          application,
                          MessageUrl.MESSAGE_SOS_DURESS + " " + reason
                  )

          );

      }
      else {
        settings.setPanicState(PanicStates.PANIC_OFF.getValue());
          MessageUtil.sendMessage(
              application,
              MessageUtil.getMessageUrl(
                  application,
                  MessageUrl.MESSAGE_SOS_CANCEL + " " + reason
              )
          );
      }

      EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
    }

    private class UpdateHandlerThread /*extends HandlerThread */{
//        private final Handler mHandler;
//        private final DownloadRunnable runnable;
//        private volatile boolean alive = true;
//
//        public UpdateHandlerThread() {
//            super(UpdateHandlerThread.class.getName(), Givens.THREAD_PRIORITY + android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
//            start();
//            mHandler = new Handler(getLooper());
//            runnable = new DownloadRunnable();
//            mHandler.post(runnable);
//        }
//
//        public void kill() {
//            alive = false;
//            mHandler.removeCallbacks(runnable);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                quitSafely();
//            } else {
//                quit();
//            }
//
//        }
//
//        private class DownloadRunnable implements Runnable {
//
//            private void getContents(AlertHeader.Record record) throws Exception {
//
//                AlertsUrl url = new AlertsUrl(application.getScannedSettings(), application.getString(R.string.alerts_endpoint), sessionManager.getSessionId());
//                url.setGroupId(record.getGroupId());
//                Location location = fixManager.getLastLocation();
//                if (location != null) {
//                    url.setLocation(location);
//                }
//
//                if (!alive)
//                    return;
//                //Timber.d("Calling url: " + url.toString());
//                Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
//                String responseBody = response.body().string().trim();
//                response.body().close();
//
//                //Timber.d("Response Body (encrypted): " + responseBody);
//
//                // if we don't receive a 200 ok, it's a network error.
//                if (response.code() != 200 || responseBody.isEmpty() || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", url.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("Server did not respond appropriately.");
//                }
//
//                responseBody = crypto.decryptFromHexToString(responseBody).trim();
//                //Timber.d("Response Body (unencrypted): " + responseBody);
//
//                if (responseBody.startsWith("Invalid Session") || responseBody.contains("<error>Invalid SessionId</error>")) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", url.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    EventBus.getDefault().post(new Events.InvalidSession());
//                    return;
//                }
//
//                if (!responseBody.startsWith("<?xml version='1.0' encoding='utf-8' ?>")) {
//                    if (BuildConfig.DEBUG) {
//                        if (BuildConfig.DEBUG) {
//                            Crashlytics.setString("Url", url.toString());
//                            Crashlytics.setLong("Response Code", response.code());
//                            Crashlytics.setString("Response", responseBody);
//                        }
//                    }
//                    throw new Exception("Server did not return xml.");
//                }
//
//                //Timber.d("Content Response Body: " + responseBody);
//
//                RegistryMatcher matcher = new RegistryMatcher();
//                matcher.bind(Date.class, new DateTransformer());
//                Serializer serializer = new Persister(matcher);
//
//                boolean validated = serializer.validate(AlertContent.class, responseBody);
//                if (!validated) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", url.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("XML Received Could Not Be Validated.");
//                }
//                AlertContent serverContent = serializer.read(AlertContent.class, responseBody);
//
//                if (serverContent != null) {
//                    com.xzfg.app.model.Error error = serverContent.getError();
//                    if (error != null) {
//                        if (error.getMessage() != null) {
//                            if (error.getMessage().equals("Invalid Session Id")) {
//                                EventBus.getDefault().post(new Events.InvalidSession());
//                            }
//                            throw new Exception("Server Error: " + error.getMessage());
//                        } else {
//                            throw new Exception("Server Returned An Error.");
//                        }
//                    }
//
//                    if (serverContent.getRecords() != null) {
//                        //Timber.d(serverContent.toString());
//                        data.put(record.getGroupId(), serverContent.getRecords());
//                        EventBus.getDefault().postSticky(new Events.AlertsDataUpdated());
//                    }
//                }
//
//            }
//
//            private void getHeaders() throws Exception {
//                HeaderUrl headerUrl = new HeaderUrl(application.getScannedSettings(), application.getString(R.string.alertheader_endpoint), sessionManager.getSessionId());
//
//                //Timber.d("Calling url: " + headerUrl.toString());
//
//                Response response = httpClient.newCall(new Request.Builder().url(headerUrl.toString(crypto)).build()).execute();
//                String responseBody = response.body().string().trim();
//                response.body().close();
//
//                //Timber.d("Response Body (encrypted): " + responseBody);
//
//                // if we don't receive a 200 ok, it's a network error.
//                if (response.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", headerUrl.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("Server did not respond appropriately.");
//                }
//
//                responseBody = crypto.decryptFromHexToString(responseBody).trim();
//                //Timber.d("Response Body (unencrypted): " + responseBody);
//
//                if (responseBody.startsWith("Invalid Session") || responseBody.contains("<error>Invalid SessionId</error>")) {
//                    EventBus.getDefault().post(new Events.InvalidSession());
//                    return;
//                }
//
//                if (!responseBody.startsWith("<?xml version='1.0' encoding='utf-8' ?>")) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", headerUrl.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("Server did not return xml.");
//                }
//
//                //Timber.d("Header Response Body: " + responseBody);
//
//                RegistryMatcher matcher = new RegistryMatcher();
//                matcher.bind(Date.class, new DateTransformer());
//                Serializer serializer = new Persister(matcher);
//
//                boolean validated = serializer.validate(AlertHeader.class, responseBody);
//                if (!validated) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", headerUrl.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("XML Received Could Not Be Validated.");
//                }
//                AlertHeader serverHeader = serializer.read(AlertHeader.class, responseBody);
//
//                if (serverHeader != null) {
//                    com.xzfg.app.model.Error error = serverHeader.getError();
//                    if (error != null) {
//                        if (error.getMessage() != null) {
//                            if (error.getMessage().equals("Invalid Session Id")) {
//                                EventBus.getDefault().post(new Events.InvalidSession());
//                            }
//                            throw new Exception("Server Error: " + error.getMessage());
//                        } else {
//                            throw new Exception("Server Returned An Error.");
//                        }
//                    }
//
//                    //Timber.d(serverHeader.toString());
//
//                    try {
//                        header = serverHeader;
//                        EventBus.getDefault().postSticky(new Events.AlertsHeaderUpdated());
//                        List<AlertHeader.Record> records = serverHeader.getRecords();
//                        List<Object> ids = new ArrayList<>();
//                        for (AlertHeader.Record entry : records) {
//                            if (!alive)
//                                return;
//
//                            ids.add(entry.getGroupId());
//                            try {
//                                if (!AlertManager.this.data.containsKey(entry.getGroupId()) ||
//                                        (
//                                                selectedId != null && ((entry.getGroupId() == null && selectedId.equals("null")) || entry.getGroupId().equals(selectedId))
//                                        )
//                                        ) {
//                                    getContents(entry);
//                                }
//                            } catch (Exception e) {
//                                Timber.e(e, "Couldn't get alerts.");
//                            }
//                        }
//
//                        if (!alive)
//                            return;
//
//                        // remove unneeded entries.
//                        if (!ids.isEmpty()) {
//                            for (Object key : data.keySet()) {
//                                if (!ids.contains(key)) {
//                                    data.remove(key);
//                                    EventBus.getDefault().postSticky(new Events.AlertsDataUpdated());
//                                }
//                            }
//                        }
//                        EventBus.getDefault().postSticky(new Events.AlertsHeaderUpdated());
//                    } catch (Exception e) {
//                        Timber.e(e, "Error getting alert data.");
//                    }
//
//                }
//
//            }
//
//            public void run() {
//                if (!alive)
//                    return;
//
//                if (alive && application.isSetupComplete() && application.getAgentSettings().getAgentRoles().alert() && isConnected() && sessionManager.getSessionId() != null) {
//                    try {
//                        Location location = fixManager.getLastLocation();
//                        if (location == null) {
//                            throw new Exception("Location Not Available, Cannot Retrieve POIs");
//                        }
//
//                        getHeaders();
//                    } catch (Exception e) {
//                        if (!Network.isNetworkException(e)) {
//                            if (alive)
//                                Timber.w(e, "Error attempting to download alert data.");
//                            else
//                                Timber.w(e, "Error caught when dead.");
//                        }
//                    }
//
//                }
//
//                if (alive)
//                    mHandler.postDelayed(this, getInterval());
//            }
//        }

    }


}