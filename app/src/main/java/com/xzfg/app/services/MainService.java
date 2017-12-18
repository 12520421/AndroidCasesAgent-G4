package com.xzfg.app.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.managers.AlertManager;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.managers.OrientationManager;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.receivers.VolumeChangeObserver;
import com.xzfg.app.util.MessageUtil;
import com.xzfg.app.util.MessageMetaDataUtil;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * This is the main service which responds to system broadcast events.
 */
public class MainService extends Service {


    @Inject
    Application application;

    @Inject
    SharedPreferences sharedPreferences;

    @Inject
    ConnectivityManager connectivityManager;

    @Inject
    MediaManager mediaManager;
    @Inject
    AlertManager alertManager;
    @Inject
    FixManager fixManager;

    @Inject
    OrientationManager orientationManager;

    private final static Object sosLock = new Object();

    private boolean headsetConnected = false;
    private VolumeChangeObserver volumeChangeObserver;
    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);

        if (BuildConfig.USE_NOTIFICATION) {
          /*  Notification.Builder builder = new Notification.Builder(getApplicationContext());
            builder.setContentTitle("App is running");
            builder.setContentText("This app is running.");
            builder.setSmallIcon(android.R.drawable.stat_notify_more);
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, i, 0);
            builder.setContentIntent(pi);
            builder.setOngoing(true);
            builder.setPriority(Notification.PRIORITY_HIGH);
            startForeground(R.id.main_service, builder.build());*/
        }
        volumeChangeObserver = new VolumeChangeObserver(application, new Handler());
        application.getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, volumeChangeObserver);
        fixManager.createVolumeObserver();
    }

    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        MessageUrl messageUrl = MessageUtil.getMessageUrl(this);
        MessageUrl messageMetaData = MessageMetaDataUtil.getMessageUrl(this);

        // send an SOS if needed.
        if (isConnected() && application.isSetupComplete() && sharedPreferences.getBoolean("ssos", false)) {
            synchronized (sosLock) {
                // remove the ssos value
                sharedPreferences.edit().remove("ssos").commit();
                // send the sos.
                MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(this, MessageUrl.MESSAGE_SOS));
            }
        }

        if (intent != null && intent.getAction() != null) {

            switch (intent.getAction()) {
                case Intent.ACTION_BOOT_COMPLETED: {
                    if (application.isSetupComplete()) {
                        messageUrl = MessageUtil.getMessageUrl(this, "Device Boot Completed.");
                    }
                    break;
                }
                case Givens.ACTION_APPLICATION_STARTED:
                    if (messageUrl != null) {
                        messageUrl.setMessage(MessageUrl.MESSAGE_APP_OPENED);
                        messageMetaData.setMessage("Set MetaData");

                    }
                    break;
                case Givens.ACTION_DEVICE_ADMIN_DISABLED:
                    if (messageUrl != null) {
                        messageUrl.setMessage("Device Administrator Disabled.");
                    }
                    break;

                case Givens.ACTION_DEVICE_ADMIN_ENABLED:
                    if (messageUrl != null) {
                        messageUrl.setMessage("Device Administrator Enabled.");
                    }
                    break;

                case ConnectivityManager.CONNECTIVITY_ACTION: {
                    if (isConnected()) {
                        EventBus.getDefault().post(new Events.NetworkStatus(true));
                    } else {
                        EventBus.getDefault().post(new Events.NetworkStatus(false));
                    }
                    break;
                }

                case Intent.ACTION_SHUTDOWN:
                    if (messageUrl != null) {
                        messageUrl.setMessage("Device Shutdown Initiated.");
                    }
                    // Stop audio & video recording on shutdown
                    if (mediaManager.isRecording()) {
                        mediaManager.stopAudioRecording();
                        mediaManager.stopVideoRecording();
                    }
                    break;

                case Intent.ACTION_BATTERY_LOW:
                    if (messageUrl != null) {
                        messageUrl.setMessage(MessageUrl.MESSAGE_LOW_BATTERY);
                    }
                    break;

                case Intent.ACTION_BATTERY_OKAY:
                    if (messageUrl != null) {
                        messageUrl.setMessage("Battery Ok");
                    }
                    break;

                case Intent.ACTION_MEDIA_MOUNTED: {
                    EventBus.getDefault().postSticky(new Events.MediaMounted());
                    //Timber.d("External media has been mounted.");
                    break;
                }

                case Intent.ACTION_MEDIA_REMOVED: {
                    EventBus.getDefault().postSticky(new Events.MediaUnavailable());
                    //Timber.d("External media has been removed.");
                    break;
                }

                case Intent.ACTION_MEDIA_UNMOUNTED: {
                    EventBus.getDefault().postSticky(new Events.MediaUnavailable());
                    //Timber.d("External media has been unmounted.");
                    break;
                }

                case Intent.ACTION_HEADSET_PLUG: {
                    Bundle extras = intent.getExtras();
                    int state = extras.getInt("state", 0);
                    //String name = extras.getString("name", "Headset");
                    //int microphone = extras.getInt("microphone", 0);
                    //Timber.d("RIPCORD detected.  Port: " + name + ", has microphone: " + microphone);
                    if (headsetConnected && state == 0 &&
                        application.isSetupComplete() &&
                        application.getAgentSettings().getAgentRoles().paniccovertheadphones()) {
                            headsetConnected =state== 0;
                          //  messageUrl = MessageUtil.getMessageUrl(getApplication(), MessageUrl.MESSAGE_SOS);
                            alertManager.startPanicMode(false);
                    }

                    headsetConnected = state == 1;

                    break;
                }
            }

        } else {
            //Timber.d("Action Is Null");
        }

        if (messageUrl != null && messageUrl.getMessage() != null)
            MessageUtil.sendMessage(this, messageUrl);
            MessageMetaDataUtil.sendMessage(this,messageMetaData);
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Timber.e(new Exception("MAIN SERVICE DESTROYED"), "MAIN SERVICE DESTROYED");
        }
        application.getContentResolver().unregisterContentObserver(volumeChangeObserver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }
}
