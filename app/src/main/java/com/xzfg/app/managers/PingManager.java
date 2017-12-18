package com.xzfg.app.managers;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.model.PingResults;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.model.url.PingUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.BatteryUtil;
import com.xzfg.app.util.MessageMetaDataUtil;
import com.xzfg.app.util.MessageUtil;
import com.xzfg.app.util.Network;

import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * This manager handles ping operations.
 */
public class PingManager {

    @Inject
    Application application;

    @Inject
    Crypto crypto;

    @Inject
    PowerManager powerManager;

    @Inject
    ConnectivityManager connectivityManager;

    @Inject
    OkHttpClient httpClient;

    @Inject
    MediaManager mediaManager;

    @Inject
    AlertManager alertManager;


    private volatile boolean wasDown = false;

    private static volatile int interval = 0;

    private PingHandlerThread pingHandlerThread;

    public PingManager(Application application) {
        application.inject(this);
        EventBus.getDefault().registerSticky(this);
        if (application.isSetupComplete() && application.getAgentSettings() != null) {
            interval = application.getAgentSettings().getPingInterval();
            if (isConnected()) {
                pingHandlerThread = new PingHandlerThread();
            }
        }
    }

    public int getInterval() {
        return interval * 1000;
    }

    public void onEventMainThread(Events.NetworkStatus statusEvent) {
        if (statusEvent.isUp()) {
            if (pingHandlerThread == null && application.isSetupComplete()) {
                //Timber.d("Network up, firing up ping thread.");
                pingHandlerThread = new PingHandlerThread();
            }
        } else {
            //Timber.d("Network down. Disabling ping thread.");
            if (pingHandlerThread != null) {
                PingHandlerThread deadThread = pingHandlerThread;
                deadThread.kill();
                pingHandlerThread = null;
            }
        }
    }

    public void onEventMainThread(Events.AgentSettingsAcquired settingsAcquired) {
        if (application.isSetupComplete() && application.getAgentSettings() != null) {
            if (application.getAgentSettings().getPingInterval() != interval) {
                interval = application.getAgentSettings().getPingInterval();
                //Timber.d("Settings acquired, interval changed.");
                if (pingHandlerThread != null) {
                    //Timber.d("Killing old ping handler thread.");
                    PingHandlerThread deadThread = pingHandlerThread;
                    deadThread.kill();
                    pingHandlerThread = null;
                }
                if (isConnected()) {
                    //Timber.d("Starting a new ping handler thread.");
                    pingHandlerThread = new PingHandlerThread();
                }
            }
        }
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    private final class PingHandlerThread extends HandlerThread {
        private final Handler mHandler;
        private final PingRunnable runnable;
        private volatile boolean alive = true;

        public PingHandlerThread() {
            super(PingHandlerThread.class.getName(), Givens.THREAD_PRIORITY + android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
            start();
            mHandler = new Handler(getLooper());
            runnable = new PingRunnable();
            if (getInterval() > 0) {
                mHandler.post(runnable);
            }
        }

        public void kill() {
            alive = false;
            mHandler.removeCallbacks(runnable);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                quitSafely();
            } else {
                quit();
            }
        }

        private final class PingRunnable implements Runnable {

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

            private void sendPing() throws Exception {
                MessageUrl messageMetaData = MessageMetaDataUtil.getMessageUrl(application);
                MessageMetaDataUtil.sendMessage(application,messageMetaData);
                if (!isConnected()) {
                    //Timber.d("Not connected, skipping ping.");
                    return;
                }
                if (!application.isSetupComplete()) {
                    //Timber.d("Setup is incomplete, skipping ping.");
                    return;
                }


                PingUrl url = new PingUrl(application.getScannedSettings(), application.getString(R.string.ping_endpoint), application.getDeviceIdentifier());
                url.setMessage("-Android");


                url.setBattery(BatteryUtil.getBatteryLevel(application));
                //Timber.d("Calling Ping URL: " + url.toString());

                Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
                String responseBody = response.body().string().trim();
                response.body().close();
                //Timber.d("Ping Response Body (encrypted): " + responseBody);

                // if we don't receive a 200 ok, it's a network error.
                if (response.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
                    if (BuildConfig.DEBUG) {
                        Crashlytics.setString("Url", url.toString());
                        Crashlytics.setLong("Response Code", response.code());
                        Crashlytics.setString("Response", responseBody);
                    }
                    throw new Exception("Server did not respond appropriately.");
                }

                responseBody = crypto.decryptFromHexToString(responseBody).trim();
                //Timber.d("Ping Response Body Unencrypted: " + responseBody);

                if (responseBody.contains("Error") || responseBody.contains("Invalid SessionId") || responseBody.startsWith("ok")) {
                    if (BuildConfig.DEBUG) {
                        Crashlytics.setString("Url", url.toString());
                        Crashlytics.setString("Response", responseBody);
                    }
                    throw new Exception("Server returned us an error.");
                }

                if (!responseBody.startsWith("OK")) {
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
                                            // Take 3 photos and send only the last one to the server
                                            mediaManager.takePhoto(1/*3*/);
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
                                        //Timber.d("Unregister app - PingManager");
                                        application.clearApplicationData(true);
                                        break;

                                    case "cancelSOS":
                                        // ping manager can access alert manager, so we don't
                                        // need to use the eventBus.
                                        alertManager.stopPanicMode(false, "C2 " + command);
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

            @Override
            public void run() {
                //Timber.d("Ping initiated.");
                // ensure that the CPU and network stays queued up during our processing.
                PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PingHandlerThread.class.getName());
                wakeLock.acquire();

                try {
                    sendPing();
                } catch (Exception e) {
                    if (!Network.isNetworkException(e)) {
                        Timber.w(e, "An error occurred attempting to send a ping.");
                    }
                }

                wakeLock.release();

                // only go again if we have a positive ping interval.
                if (getInterval() > 0 && alive) {
                    //Timber.d("Ping complete, next ping in " + getInterval() + "ms.");
                    mHandler.postDelayed(this, getInterval());
                } else {
                    //Timber.d("Ping complete.");
                }
            }


        }
    }
}
