package com.xzfg.app.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.model.BatteryInfo;
import com.xzfg.app.model.UploadPackage;
import com.xzfg.app.util.BatteryUtil;
import com.xzfg.app.util.MessageUtil;

import java.io.File;
import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;


public class AudioRecordingService extends Service
        implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    @Inject
    Application application;

    @Inject
    PowerManager powerManager;

    @Inject
    MediaManager mediaManager;

    @Inject
    FixManager fixManager;

    private MediaRecorder mediaRecorder;
    private PowerManager.WakeLock wakeLock;
    private volatile boolean mStreaming = false;
    private boolean started = false;
    // Batch GUID for segmented audio recording
    private String fileBatch;
    // Current segment number for segmented audio recording
    private int fileSegment;
    // Absolute max length of recording - single chunk, no segments
    private int absoluteMaxDuration = -1;

    private Notification notification;

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, AudioRecordingService.class.getName());
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock.acquire();

        if (BuildConfig.AUDIO_NOTIFICATION) {
            final Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);


            final PendingIntent contentIntent = PendingIntent.getActivity(this,
                    0, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_action_mic)
                    .setColor(ContextCompat.getColor(this,R.color.redorange))
                    .setContentTitle(getString(R.string.audio_recording))
                    .setWhen(System.currentTimeMillis())
                    .setOngoing(true)
                    .setContentIntent(contentIntent)
                    .build();
        }

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onEventMainThread(Events.FreeSpaceLowEvent freeSpaceLowEvent) {
        EventBus.getDefault().postSticky(new Events.RecordingStoppedEvent());
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (started && mStreaming) {
            Timber.e("Second start command received.");
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }


        started = true;

        // Initialize batch GUID and segment for segmented audio recording
        fileBatch = UUID.randomUUID().toString();
        fileSegment = 1;

        if (i.getExtras() != null) {
            absoluteMaxDuration = i.getExtras().getInt(Givens.EXTRA_MAX_DURATION, -1);
        }

        start();
        return START_NOT_STICKY;
    }


    private void stopMediaRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception e) {
                Timber.w(e, "Error stopping mediaRecorder.");
            }
            try {
                mediaRecorder.reset();
            } catch (Exception e) {
                Timber.w(e, "Error resetting mediaRecorder.");
            }
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Timber.w(e, "Error releasing mediaRecorder.");
            }
            mediaRecorder = null;
        }
        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        stop(true);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }


    private void start() {
        if (BuildConfig.AUDIO_NOTIFICATION && notification != null) {
            startForeground(Givens.NOTIFICATION_AUDIO_RECORDING_ID, notification);
        }

        try {
            String filePath = getFilePath();

            //noinspection ResultOfMethodCallIgnored
            File folder = new File(filePath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            filePath += getFileName();
            //Timber.d("Writing to file path: " + filePath);

            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                } catch (Exception e) {
                    Timber.e(e, "Couldn't stop.");
                }
                try {
                    mediaRecorder.release();
                } catch (Exception e) {
                    Timber.e(e, "Couldn't release.");
                }
                mediaRecorder = null;
            }


            mediaRecorder = new MediaRecorder();

         //   mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncodingBitRate(128 * 1024);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            if (absoluteMaxDuration > 0) {
                // Absolute max length of recording - single chunk, no segments
                mediaRecorder.setMaxDuration(absoluteMaxDuration);
            }
            else {
                // Max length of recording segment - multiple chunks, no absolute max length
                mediaRecorder.setMaxDuration(Givens.MAX_AUDIO_DURATION_MS);
            }

            try {
                mediaRecorder.setOutputFile(filePath);
                mediaRecorder.setOnErrorListener(this);
                mediaRecorder.setOnInfoListener(this);
                mediaRecorder.prepare();
                // Start recording counter only once at the beginning of recording
                if (fileSegment < 2) {
                    mediaManager.audioRecordingStarted();
                }
                Thread.sleep(1000);
                mediaRecorder.start();
            } catch (Exception e) {
                Timber.e(e, "An error occurred preparing for recording.");
                stop(true);
                throw e;
            }

            mStreaming = true;

        } catch (Exception e) {
            Timber.e(e, "Couldn't start recording.");
            stop(true);
        }
    }

    private void stop(boolean lastSegment) {
        stopMediaRecorder();


        if (mStreaming) {
            if (lastSegment) {
                mediaManager.audioRecordingEnded();
            }

            String filePath = getFilePath() + getFileName();
            if (filePath != null) {
                File f = new File(filePath);
                if (f.exists()) {
                    //Timber.d("File Exists");
                    UploadPackage uploadPackage = new UploadPackage();
                    uploadPackage.setDate(new Date());
                    uploadPackage.setLength(f.length());
                    uploadPackage.setType(Givens.UPLOAD_TYPE_AUDIO);
                    uploadPackage.setFormat(Givens.UPLOAD_FORMAT_MP4);
                    uploadPackage.setBatch(fileBatch);
                    uploadPackage.setSegment(fileSegment * (lastSegment ? -1 : 1));

                    if (application.getAgentSettings() != null) {
                        uploadPackage.setCaseNumber(application.getAgentSettings().getCaseNumber());
                        uploadPackage.setCaseDescription(
                            application.getAgentSettings().getCaseDescription());
                    }
                    Location location = fixManager.getLastLocation();
                    if (location != null) {
                        uploadPackage.setLatitude(location.getLatitude());
                        uploadPackage.setLongitude(location.getLongitude());
                        uploadPackage.setAccuracy(location.getAccuracy());
                    }

                    mediaManager.submitUpload(uploadPackage, f);
                } else {
                    Timber.w("File doesn't exist.");
                }
            }
        }
        mStreaming = false;
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_ERROR_SERVER_DIED: {
                Timber.e("Media Recorder Stopped Unexpectedly. Server Died.");
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Audio Recording Stopped Unexpectedly. Server Died."));
                break;
            }
            case MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN: {
                Timber.e("Media Recorder Stopped Unexpectedly. Cause unknown.");
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Audio Recording Stopped Unexpectedly. Cause Unknown."));
                break;
            }
        }
        stopForeground(true);
        stopSelf();
    }

    private String getFileName() {
        return fileBatch + fileSegment + ".m4a";
    }

    private String getFilePath() {
        return application.getFilesDir().getAbsolutePath() + "/media/";
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED: {
                //Timber.d("Media Recorder Reached Maximum Duration: " + MAX_DURATION_MS);
                // Segmented audio recording - stop recording, create upload package for segment, resume recording
                if (started) {
                    BatteryInfo batteryInfo = BatteryUtil.getBatteryInfo(this);
                    boolean lowBattery = (!batteryInfo.isCharging && batteryInfo.percentCharged < 5);

                    if (absoluteMaxDuration > 0 || lowBattery) {
                        stop(true);
                    }
                    // Continue to record next segment if absoluteMaxDuration was not specified
                    else {
                        stop(false);
                        fileSegment++;
                        start();
                    }
                }
                break;
            }
            case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN: {
                break;
            }
        }
    }

}
