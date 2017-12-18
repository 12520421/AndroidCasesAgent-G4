package com.xzfg.app.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xzfg.app.Application;
import com.xzfg.app.Givens;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.model.UploadPackage;
import com.xzfg.app.util.MessageUtil;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;

import timber.log.Timber;

public class PhoneService extends Service implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    public static final int NOT_STARTED = 0;
    public static final int STARTING = 1;
    public static final int STARTED = 2;
    public static final int STOPPING = 3;

    public static final String TAG = PhoneService.class.getName();
    public static final String ACTION = TAG + "_CALL";
    public static final String PHONE_NUMBER = TAG + "_PHONE_NUMBER";
    public static final String CALL_STARTED = ACTION + "_STARTED";
    public static final String CALL_STOPPED = ACTION + "_STOPPED";

    @IntDef({NOT_STARTED,STARTING,STARTED,STOPPING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State{}

    private @State int currentState = NOT_STARTED;
    // Batch GUID for segmented audio recording
    private String fileBatch;
    // Current segment number for segmented audio recording
    private int fileSegment;
    private String filePath;
    private File file;
    private boolean started = false;
    private String phoneNumber="";

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

    /**
     * We don't use binding. Yay EventBus!
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        ((Application) getApplication()).inject(this);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PhoneService.class.getName());
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock.acquire();
        super.onCreate();
    }

    /**
     * The broadcast receiver will cause this to be run, either to start or stop a call.
     * Starting an already in progress call not do anything.
     * Stopping na already stopped call will not do anything.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int startMode  = super.onStartCommand(intent, flags, startId);

        if (intent != null && intent.hasExtra(PHONE_NUMBER)) {
            phoneNumber = intent.getStringExtra(PHONE_NUMBER);
        }

        if (intent != null && intent.hasExtra(ACTION)) {
            String action = intent.getStringExtra(ACTION);
            if (action.equals(CALL_STARTED)) {
                startRecording(phoneNumber);
                started = true;
            }
            if (action.equals(CALL_STOPPED)) {
                stopRecording(true);
                started = false;
            }
        }
        return startMode;
    }

    /**
     * If android destroys our service on us, ensure that we stop recording.
     */
    @Override
    public void onDestroy() {
        reallyStop();
        super.onDestroy();
    }


    /**
     * This handles starting recording, but only if the current state is NOT_STARTED
     */
    private void startRecording(String phoneNumber) {
        //Timber.d("LOGG: startRecordingTHREAD-check #"+phoneNumber);
        // we're not in the NOT_STARTED state, sorry, can't start recording
        if (currentState != NOT_STARTED) {
            //Timber.d("LOGG: startRecordingTHREAD-skip #"+phoneNumber);
            return;
        }
        //Timber.d("LOGG: startRecordingTHREAD-start #"+phoneNumber);

        fileBatch = UUID.randomUUID().toString();
        fileSegment = 1;
        filePath = getFilePath();
        filePath += getFileName();
        //file = new File(filePath,String.valueOf(phoneNumber.replaceAll("[^\\w\\s]","") + "_" + fileBatch) + ".m4a"); //.mp4
        file = new File(filePath);
        currentState = STARTING;

        /*
         * Try using the VOICE_CALL source, to get both ends of the conversation. If that throws
         * an exception, fall back to only recording one side of the conversation.
         */
        try {
            recordVoiceCall(file);
        }
        catch (Exception e) {
            Log.e(TAG,"Recording Voice Call Failed, falling back to alternative audio source.",e);
            currentState = STARTING;

            // fall back.
            try {
                recordVoiceRecognition(file);
            }
            catch (Exception f) {
                Log.e(TAG,"Recording Voice Recognition Failed, falling back to alternative audio source.",f);
                currentState = NOT_STARTED;

                // fall back.
                try {
                    recordMIC(file);
                }
                catch (Exception g) {
                    Log.e(TAG,"Recording MIC Failed, no alternatives to fallback to.",g);
                    currentState = NOT_STARTED;
                }
            }
        }

    }

    /**
     * This will fail on most devices - most manufacturers have disabled the ability to record
     * the phone voice.  I get an illegal state exception 0 when attempting this on a nexus 6p
     */
    private void recordVoiceCall(File file) throws Exception {

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(128*1024);
        mediaRecorder.setOutputFile(file.getPath());
        mediaRecorder.setOnErrorListener(this);
        mediaRecorder.setOnInfoListener(this);

        boolean prepared = false;
        try {
            mediaRecorder.prepare();
            prepared = true;
        }
        catch (Exception e) {
            Log.e(TAG,"Couldn't prepare mediaRecorder", e);
            reallyStop();
            try {
                file.delete();
            }
            catch (Exception f) {
                Log.e(TAG,"Couldn't remove file.",f);
            }
            throw e;
        }

        if (prepared) {
            try {
                if (fileSegment < 2) {
                    mediaManager.audioRecordingStarted();
                }
                Thread.sleep(2000);
                mediaRecorder.start();
            }
            catch (Exception e) {
                Log.e(TAG,"Couldn't start mediaRecorder", e);
                reallyStop();
                try {
                    file.delete();
                    mediaManager.audioRecordingEnded();
                }
                catch (Exception f) {
                    Log.e(TAG,"Couldn't remove file.",f);
                }
                throw e;
            }
            currentState = STARTED;
        }
    }


    /**
     * This will use the VOICE_RECOGNITION audio source.
     * This appears to be better than using the "MIC" audio source, as it uses the front facing
     * mic(s) with high gain for good speech recognition, which is really what we want here as well.
     */
    private void recordVoiceRecognition(File file) throws Exception {

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(128*1024);
        mediaRecorder.setOutputFile(file.getPath());
        mediaRecorder.setOnErrorListener(this);
        mediaRecorder.setOnInfoListener(this);

        boolean prepared = false;
        try {
            mediaRecorder.prepare();
            prepared = true;
        }
        catch (Exception e) {
            Log.e(TAG,"Couldn't prepare mediaRecorder", e);
            reallyStop();
            try {
                file.delete();
            }
            catch (Exception f) {
                Log.e(TAG,"Couldn't remove file.",f);
            }
            throw e;
        }

        if (prepared) {
            try {
                if (fileSegment < 2) {
                    mediaManager.audioRecordingStarted();
                }
                Thread.sleep(2000);
                mediaRecorder.start();
            }
            catch (Exception e) {
                Log.e(TAG,"Couldn't start mediaRecorder", e);
                reallyStop();
                try {
                    file.delete();
                    mediaManager.audioRecordingEnded();
                }
                catch (Exception f) {
                    Log.e(TAG,"Couldn't remove file.",f);
                }
                throw e;
            }
            currentState = STARTED;
        }

    }

    /**
     * This will fail on most devices - most manufacturers have disabled the ability to record
     * the phone voice.  I get an illegal state exception 0 when attempting this on a nexus 6p
     */
    private void recordMIC(File file) throws Exception {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(128*1024);
        mediaRecorder.setOutputFile(file.getPath());
        mediaRecorder.setOnErrorListener(this);
        mediaRecorder.setOnInfoListener(this);

        boolean prepared = false;
        try {
            mediaRecorder.prepare();
            prepared = true;
        }
        catch (Exception e) {
            Log.e(TAG,"Couldn't prepare mediaRecorder", e);
            reallyStop();
            try {
                file.delete();
            }
            catch (Exception f) {
                Log.e(TAG,"Couldn't remove file.",f);
            }
            throw e;
        }

        if (prepared) {
            try {
                if (fileSegment < 2) {
                    mediaManager.audioRecordingStarted();
                }
                Thread.sleep(2000);
                mediaRecorder.start();
            }
            catch (Exception e) {
                Log.e(TAG,"Couldn't start mediaRecorder", e);
                reallyStop();
                try {
                    file.delete();
                    mediaManager.audioRecordingEnded();
                }
                catch (Exception f) {
                    Log.e(TAG,"Couldn't remove file.",f);
                }
                throw e;
            }
            currentState = STARTED;
        }
    }

    private void stopRecording(boolean lastSegment) {
        // we're not in STARTING or STARTED, so we can't stop anything.
        if (currentState != STARTING && currentState != STARTED) {
            return;
        }
        if (lastSegment) {
            mediaManager.audioRecordingEnded();
        }
        reallyStop();
        currentState = NOT_STARTED;

        if (file != null) {
            //File f = new File(file);
            if (file.exists()) {
                //Timber.d("File Exists");
                UploadPackage uploadPackage = new UploadPackage();
                uploadPackage.setDate(new Date());
                uploadPackage.setLength(file.length());
                uploadPackage.setType(Givens.UPLOAD_TYPE_AUDIO);
                uploadPackage.setFormat(Givens.UPLOAD_FORMAT_MP4);
                uploadPackage.setBatch(fileBatch);
                uploadPackage.setSegment(fileSegment*(lastSegment ? -1 : 1));

                if (application.getAgentSettings() != null) {
                    uploadPackage.setCaseNumber(application.getAgentSettings().getCaseNumber());
                    uploadPackage.setCaseDescription(application.getAgentSettings().getCaseDescription());
                }
                Location location = fixManager.getLastLocation();
                if (location != null) {
                    uploadPackage.setLatitude(location.getLatitude());
                    uploadPackage.setLongitude(location.getLongitude());
                    uploadPackage.setAccuracy(location.getAccuracy());
                }

                mediaManager.submitUpload(uploadPackage, file);
            } else {
                Timber.w("File doesn't exist.");
            }
        }
    }

    private void reallyStop() {
        if (mediaRecorder != null) {
            currentState = STOPPING;
            try {
                mediaRecorder.stop();
            }
            catch (Exception e) {
                Log.w(TAG,"Error stopping media recorder. (Was it started successfully?)",e);
            }
            try {
                mediaRecorder.release();
            }
            catch (Exception e) {
                Log.e(TAG,"Error releasing media recorder.",e);
            }
            mediaRecorder = null;
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_ERROR_SERVER_DIED: {
                Timber.e("Media Recorder Stopped Unexpectedly. Server Died.");
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Phone Recording Stopped Unexpectedly. Server Died."));
                break;
            }
            case MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN: {
                Timber.e("Media Recorder Stopped Unexpectedly. Cause unknown.");
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Phone Recording Stopped Unexpectedly. Cause Unknown."));
                break;
            }
        }
        Log.e(TAG,"Error: " + what + ", " + extra);
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED: {
                //Timber.d("Media Recorder Reached Maximum Duration: " + MAX_DURATION_MS);
                // Segmented audio recording - stop recording, create upload package for segment, resume recording
                if (started) {
                    stopRecording(false);
                    fileSegment++;
                    startRecording(phoneNumber);
                }
                break;
            }
            case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN: {
                break;
            }
        }
        Log.w(TAG,"Info: " + what + ", " + extra);
    }

    /*
    private String getFilePath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
    }
    */

    private String getFileName() {
        return fileBatch + fileSegment + ".m4a";
    }

    private String getFilePath() {
        if (application.getAgentSettings().getExternalStorage() == 1 && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return application.getExternalFilesDir("media") + "/";
        } else {
            return application.getFilesDir().getAbsolutePath() + "/media/";
        }
    }
}
