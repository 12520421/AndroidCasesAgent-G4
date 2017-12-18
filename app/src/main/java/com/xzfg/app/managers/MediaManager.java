package com.xzfg.app.managers;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.model.Checkin;
import com.xzfg.app.model.Submission;
import com.xzfg.app.model.UploadPackage;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.services.AudioRecordingService;
import com.xzfg.app.services.AudioStreamingService;
import com.xzfg.app.services.PhotoService;
import com.xzfg.app.services.ProfileService;
import com.xzfg.app.services.VideoRecordingService;
import com.xzfg.app.services.VideoStreamingService;
import com.xzfg.app.util.ImageUtil;
import com.xzfg.app.util.MessageUtil;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

import static android.content.ContentValues.TAG;

/**
 */
public class MediaManager {

    private static final Object recordingLock = new Object();
    public final List<Integer> previewModes = Collections.unmodifiableList(Arrays.asList(Givens.COLLECT_MODE_PICTURE, Givens.COLLECT_MODE_VIDEO, Givens.COLLECT_MODE_VIDEO_LIVE));
    // start video recording.
    private final Intent videoRecordingIntent;
    // start video recording, in the background (no preview created)
    // this is for c&c to initiate recording.
    private final Intent backgroundVideoRecordingIntent;
    private final Intent backgroundVideoStreamingIntent;
    private final Intent audioRecordingIntent;
    private final Intent audioStreamingIntent;
    // handles generating thumbnails
    private final PackageHandlerThread packageHandlerThread;
    private final HighSecurityHandlerThread highSecurityHandlerThread;
    // handles performing file uploads
    private final UploadHandlerThread uploadHandlerThread;
    private final HighSecurityUploadHandlerThread highSecurityUploadHandlerThread;
    // holds the packages we need to upload
    private final ConcurrentLinkedQueue<String> uploadQueue = new ConcurrentLinkedQueue<>();
    // holds the (secure) packages we need to upload
    private final ConcurrentLinkedQueue<Submission> secureQueue = new ConcurrentLinkedQueue<>();
    // monitors the filesystem for free space, only while recording.
    private final FileSystemMonitorThread fileSystemMonitorThread;
    @Inject
    volatile Crypto crypto;

    @Inject
    volatile Application application;

    @Inject
    volatile ConnectivityManager connectivityManager;

    @Inject
    volatile Gson gson;

    @Inject
    OkHttpClient httpClient;

    @Inject
    SSLSocketFactory socketFactory;

    public static volatile int cameraId = 0;
    private static volatile int inFlight = 0;
    private static volatile boolean recording = false;
    private static volatile boolean streaming = false;
    private static volatile long baseTime;
    private static volatile int captureMode = Givens.COLLECT_MODE_PICTURE;
    private static volatile Bitmap lastCapturedImage;
    private static volatile boolean wasDown = false;

    public MediaManager(Application app) {
        application = app;
        application.inject(this);
        videoRecordingIntent = new Intent(application, VideoRecordingService.class);
        backgroundVideoRecordingIntent = new Intent(application, VideoRecordingService.class);
        backgroundVideoRecordingIntent.putExtra("visible", false);
        backgroundVideoStreamingIntent = new Intent(application, VideoStreamingService.class);
        backgroundVideoStreamingIntent.putExtra("visible", false);
        audioRecordingIntent = new Intent(application, AudioRecordingService.class);
        audioStreamingIntent = new Intent(application, AudioStreamingService.class);
        packageHandlerThread = new PackageHandlerThread();
        fileSystemMonitorThread = new FileSystemMonitorThread();
        uploadHandlerThread = new UploadHandlerThread();
        highSecurityHandlerThread = new HighSecurityHandlerThread();
        highSecurityUploadHandlerThread = new HighSecurityUploadHandlerThread();

        CleanUpHandlerThread cleanUpThread = new CleanUpHandlerThread(Givens.INTERNAL_STORAGE);

        if ((Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) || (Environment.isExternalStorageEmulated() && !Environment.isExternalStorageRemovable())) {
            //Timber.d("External storage");
            CleanUpHandlerThread cleanUpExternalThread = new CleanUpHandlerThread(Givens.EXTERNAL_STORAGE);
        }

        EventBus.getDefault().registerSticky(this);
    }

    public boolean isRecording() {
        return recording;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public long getBaseTime() {
        return baseTime;
    }

    public int getCameraId() {
        return cameraId;
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public int getCaptureMode() {
        return captureMode;
    }

    public void setCaptureMode(int captureMode) {
        this.captureMode = captureMode;
    }

    // holds the last thumbnail
    public Bitmap getLastCapturedImage() {
        return lastCapturedImage;
    }

    private void setLastCapturedImage(Bitmap bitmap) {
        lastCapturedImage = bitmap;
        EventBus.getDefault().post(new Events.ThumbnailEvent());
    }

    public void updateCount() {
        EventBus.getDefault().postSticky(new Events.FileStatus(uploadQueue.size() + secureQueue.size() + inFlight));
        Log.d(TAG,"run:"+ uploadQueue.size() + secureQueue.size() + inFlight);
    }

    public void onEventMainThread(Events.MediaMounted mediaMounted) {
        EventBus.getDefault().removeStickyEvent(mediaMounted);
        if (application.isSetupComplete() && application.getAgentSettings().getExternalStorage() == 1) {
            //Timber.d("External media mounted.");
            CleanUpHandlerThread cleanUpThread = new CleanUpHandlerThread(Givens.EXTERNAL_STORAGE);
        }
    }

    public void onEventMainThread(Events.MediaUnavailable mediaUnavailable) {
        EventBus.getDefault().removeStickyEvent(mediaUnavailable);
        if (application.isSetupComplete() && application.getAgentSettings().getExternalStorage() == 1) {
            //Timber.d("External media unavailable.");
            removeExternal();
        }
    }

    /*
    public void onEventMainThread(Events.NetworkStatus networkStatus) {
        if (networkStatus.isUp() && wasDown) {
            wasDown = false;
            uploadHandlerThread.onResume();
        }
        else {
            wasDown = true;
            uploadHandlerThread.onPause();
        }
    }
    */

    public synchronized void removeExternal() {
        File mediaFolder = application.getExternalFilesDir("media");
        if (mediaFolder != null) {
            String path = mediaFolder.getAbsolutePath();
            ConcurrentLinkedQueue<String> tempQueue = new ConcurrentLinkedQueue<>();
            tempQueue.addAll(uploadQueue);
            uploadQueue.removeAll(tempQueue);

            while (!tempQueue.isEmpty()) {
                String file = tempQueue.poll();
                if (!file.startsWith(path)) {
                    uploadQueue.add(file);
                }
            }
        }
        updateCount();
    }

    public void onEventMainThread(Events.FreeSpaceLowEvent freeSpaceLowEvent) {
        if (isRecording()) {
            stopVideoRecording();
            EventBus.getDefault().postSticky(new Events.RecordingStoppedEvent());
        }
    }

    public synchronized void startStreamingVideo() {
        synchronized (recordingLock) {
            if (!recording) {
                recording = true;
                application.startService(new Intent(application, VideoStreamingService.class));
            }
        }
    }

    public synchronized void stopStreamingVideo() {
        application.stopService(new Intent(application, VideoStreamingService.class));
    }

    public synchronized void startBackgroundStreaming(int maxDuration) {
        synchronized (recordingLock) {
            if (!recording) {
                recording = true;
                //Timber.d("Calling start background streaming");

                Bundle bundle = new Bundle();
                bundle.putInt(Givens.EXTRA_MAX_DURATION, maxDuration);
                backgroundVideoStreamingIntent.putExtras(bundle);

                application.startService(backgroundVideoStreamingIntent);
            }
        }
    }

    public synchronized void stopBackgroundStreaming() {
        application.stopService(backgroundVideoStreamingIntent);
    }

    public synchronized void startBackgroundRecording(int maxDuration) {
        if (!recording) {
            recording = true;

            Bundle bundle = new Bundle();
            bundle.putInt(Givens.EXTRA_MAX_DURATION, maxDuration);
            backgroundVideoRecordingIntent.putExtras(bundle);

            application.startService(backgroundVideoRecordingIntent);
        }
    }

    public synchronized void stopBackgroundRecording() {
        application.stopService(backgroundVideoRecordingIntent);
    }

    public synchronized void startVideoRecording() {
        synchronized (recordingLock) {
            if (!recording) {
                recording = true;
                videoRecordingIntent.putExtra("camera", cameraId);
                application.startService(videoRecordingIntent);
            }
        }
    }

    public void stopVideoRecording() {
        //Timber.d("Stopping service");
        application.stopService(videoRecordingIntent);
    }

    public synchronized void videoRecordingStarted() {
        synchronized (recordingLock) {
            recording = true;
            baseTime = SystemClock.elapsedRealtime();
            EventBus.getDefault().postSticky(new Events.VideoRecording(true));
            if (application.getAgentSettings().getIncludeAudio() == 1) {
              //  EventBus.getDefault().postSticky(new Events.AudioStatus(isRecording(), isStreaming()));
            }
            EventBus.getDefault().postSticky(new Events.VideoStatus(isRecording(), isStreaming()));
        }
    }

    public synchronized void videoStreamingStarted() {
        synchronized (recordingLock) {
            recording = true;
            streaming = true;
            baseTime = SystemClock.elapsedRealtime();
            EventBus.getDefault().postSticky(new Events.VideoRecording(true));
            EventBus.getDefault().postSticky(new Events.VideoStatus(isRecording(), isStreaming()));
        }
    }

    public synchronized void videoRecordingEnded() {
        synchronized (recordingLock) {
            recording = false;
            EventBus.getDefault().postSticky(new Events.VideoRecording(false));
            if (application.getAgentSettings().getIncludeAudio() == 1) {
                EventBus.getDefault().postSticky(new Events.AudioStatus(false, false));
            }
            EventBus.getDefault().postSticky(new Events.VideoStatus(isRecording(), isStreaming()));
        }
    }

    public synchronized void videoStreamingEnded() {
        synchronized (recordingLock) {
            recording = false;
            streaming = false;
            EventBus.getDefault().postSticky(new Events.VideoRecording(false));
            EventBus.getDefault().postSticky(new Events.VideoStatus(isRecording(), isStreaming()));
        }
    }

    public synchronized void startAudioRecording(int maxDuration) {
        synchronized (recordingLock) {
            if (!recording) {
                recording = true;
                Bundle bundle = new Bundle();
                bundle.putInt(Givens.EXTRA_MAX_DURATION, maxDuration);
                audioRecordingIntent.putExtras(bundle);
                application.startService(audioRecordingIntent);
            }
        }
    }

    public synchronized void startAudioStreaming(int maxDuration) {
        synchronized (recordingLock) {
            if (!recording) {
                recording = true;

                Bundle bundle = new Bundle();
                bundle.putInt(Givens.EXTRA_MAX_DURATION, maxDuration);
                audioStreamingIntent.putExtras(bundle);

                application.startService(audioStreamingIntent);
            }
        }
    }

    public void stopAudioStreaming() {
        application.stopService(audioStreamingIntent);
    }

    public void stopAudioRecording() {
        application.stopService(audioRecordingIntent);
    }

    public synchronized void audioRecordingStarted() {
        synchronized (recordingLock) {
            recording = true;
            streaming = true;
            baseTime = SystemClock.elapsedRealtime();
            EventBus.getDefault().postSticky(new Events.AudioRecording(true));
            EventBus.getDefault().postSticky(new Events.AudioStatus(true, false));
        }
    }

    public synchronized void audioRecordingEnded() {
        synchronized (recordingLock) {
            recording = false;
            streaming = false;
            EventBus.getDefault().postSticky(new Events.AudioRecording(false));
            EventBus.getDefault().postSticky(new Events.AudioStatus(false, false));
        }
    }

    public void takePhoto(int attempts) {
        Intent intent = new Intent(application, PhotoService.class);
        intent.putExtra(Givens.EXTRA_ATTEMPTS, attempts);
        application.startService(intent);
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    public void submitUpload(UploadPackage uploadPackage, byte[] data) {
        Submission submission = new Submission(uploadPackage, data);
        if (application.getAgentSettings().getSecurity() == 0) {
            packageHandlerThread.process(submission);
        } else {
            secureQueue.add(submission);
        }
    }

    /**
     * There is no "high security" possible with this method.
     */
    public void submitUpload(UploadPackage uploadPackage, File file) {
        if (application.getAgentSettings().getSecurity() == 0) {
            Submission submission = new Submission(uploadPackage, file);
            packageHandlerThread.process(submission);
        }
    }

    public void submitUpload(UploadPackage uploadPackage, Uri uri) {
        Submission submission = new Submission(uploadPackage, uri);
        if (application.getAgentSettings().getSecurity() == 0) {
            packageHandlerThread.process(submission);
        } else {
            highSecurityHandlerThread.process(submission);
        }
    }

    public void submitUpload(Submission submission) {
        if (application.getAgentSettings().getSecurity() == 0) {
            packageHandlerThread.process(submission);
        } else {
            if (submission.getBytes() != null || submission.getUri() != null) {
                highSecurityHandlerThread.process(submission);
            }
        }
    }


    private void resolveUri(Submission submission) {
        if (submission.getUri() != null) {
            //Timber.d("Resolving URL: " + submission.getUri().toString());
            // try and lookup the mime type.
            String fileType = application.getContentResolver().getType(submission.getUri());
            if (fileType != null) {
                String[] resolvedType = application.getContentResolver().getType(submission.getUri()).toUpperCase().split("/");
                String type = resolvedType[0].replace("IMAGE", "IMG");
                //Timber.d("Resolved Type: " + type);
                String format = resolvedType[1].replace("JPEG", "JPG");
                submission.getUploadPackage().setType(type);
                submission.getUploadPackage().setFormat(format);
                //Timber.d("Resolved Format: " + format);
            } else {
                // fall back to filename parsing.
                if (submission.getUri().toString().toLowerCase().endsWith(".jpg")) {
                    submission.getUploadPackage().setType("IMG");
                    submission.getUploadPackage().setFormat("JPG");
                }
                if (submission.getUri().toString().toLowerCase().endsWith(".png")) {
                    submission.getUploadPackage().setType("IMG");
                    submission.getUploadPackage().setFormat("PNG");
                }
                if (submission.getUri().toString().toLowerCase().endsWith(".mp4")) {
                    submission.getUploadPackage().setType("VIDEO");
                    submission.getUploadPackage().setFormat("MP4");
                }
            }
        }

    }

    private class CleanUpHandlerThread extends HandlerThread {
        private final Handler mHandler;
        private final CleanUpRunnable runnable;

        public CleanUpHandlerThread(String type) {
            super(CleanUpHandlerThread.class.getName() + "_" + type, Givens.THREAD_PRIORITY + android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
            start();
            mHandler = new Handler(getLooper());
            runnable = new CleanUpRunnable(type);
            mHandler.post(runnable);
        }

        private class CleanUpRunnable implements Runnable {
            private String type;

            public CleanUpRunnable(String type) {
                this.type = type;
            }

            public void run() {
                if (type.equals(Givens.INTERNAL_STORAGE)) {
                    //Timber.d("Checking external storage.");
                    try {
                        //Timber.d("Internal path: " + application.getFilesDir().getAbsolutePath() + "/media/");
                        File internal = new File(application.getFilesDir().getAbsolutePath() + "/media/");
                        if (internal != null & internal.exists() && internal.isDirectory()) {
                            //Timber.d("Internal directory exists.");
                            File[] files = internal.listFiles();
                            for (File f : files) {
                                //Timber.d(f.getAbsolutePath());
                                if (f.getAbsolutePath().endsWith(".upload")) {
                                    uploadQueue.add(f.getAbsolutePath());
                                } else {
                                    try {
                                        //noinspection ResultOfMethodCallIgnored
                                        f.delete();
                                    } catch (Exception e) {
                                        Timber.e(e, "Error deleting file.");
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Timber.e(e, "A problem occurred attempting to clean up internal storage.");
                    }
                }

                if (type.equals(Givens.EXTERNAL_STORAGE)) {
                    try {
                        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                            File external = application.getExternalFilesDir("media");
                            //Timber.d(external.getAbsolutePath());
                            if (external != null && external.exists() && external.isDirectory()) {
                                //Timber.d("External folder exists.");
                                File[] files = external.listFiles();
                                for (File f : files) {
                                    //Timber.d(f.getAbsolutePath());
                                    if (f.getAbsolutePath().endsWith(".upload")) {
                                        uploadQueue.add(f.getAbsolutePath());
                                    } else {
                                        try {
                                            f.delete();
                                        } catch (Exception e) {
                                            Timber.e(e, "Error deleting file.");
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Timber.e(e, "Error cleaning up external storage.");
                    }
                }
                updateCount();

            }
        }

    }

    private class PackageHandlerThread extends HandlerThread {
        Handler mHandler;
        ConcurrentLinkedQueue<Submission> queue = new ConcurrentLinkedQueue<>();
        PackageRunnable runnable;

        public PackageHandlerThread() {
            super(PackageHandlerThread.class.getName(), Givens.THREAD_PRIORITY + android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
            start();
            mHandler = new Handler(getLooper());
            runnable = new PackageRunnable();
            mHandler.post(runnable);
        }

        public void process(Submission submission) {
            queue.add(submission);
            //Timber.d("Submission Queued For Processing.");
        }


        private class PackageRunnable implements Runnable {

            public PackageRunnable() {
            }


            private void build(Submission submission) {
                if (!application.isSetupComplete()) {
                    Timber.d("Setup not complete.");
                }
                // at this point, thumnail data is no longer required.
                submission.setThumbnailData(null);

                String outString = null;
                File outFile = null;
                ZipOutputStream zos = null;
                InputStream bis = null;
                OutputStream bos = null;

                try {

                    if (application.getAgentSettings().getExternalStorage() == 1 && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        outString = application.getExternalFilesDir("media") + "/";
                    } else {
                        outString = application.getFilesDir().getAbsolutePath() + "/media/";
                    }
                    outString += (UUID.randomUUID().toString() + ".upload");
                    String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+(UUID.randomUUID().toString() + ".upload");
                    outFile = new File(outString);
                  //  outFile = new File(sdcard);
                    Log.d("liem", "build: "+sdcard);
                    boolean created = outFile.getParentFile().mkdirs();
                    outFile.createNewFile();
                    if (created) {
                        Timber.d("Created " + outFile.getParentFile().toString());
                    }

                    zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
                    zos.setLevel(0);


                    // if we have a file, then we make an encrypted copy of that file on the storage requested.
                    if (submission.getFile() != null) {
                        try {
                            File submittedFile = new File(submission.getFile());

                            // write the upload package.
                            zos.putNextEntry(new ZipEntry("UploadPackage"));
                            String uploadPackage = gson.toJson(submission.getUploadPackage());
                            bis = new ByteArrayInputStream(crypto.encryptToHex(uploadPackage).getBytes());
                            IOUtils.copy(bis, zos);
                            zos.closeEntry();

                            // start the data.
                            zos.putNextEntry(new ZipEntry("DATA"));
                            bis = new BufferedInputStream(new FileInputStream(submittedFile));
                            bos = new BufferedOutputStream(crypto.getOutputStream(zos));
                            IOUtils.copy(bis, bos);
                            bos.flush();
                            zos.closeEntry();
                            //noinspection ResultOfMethodCallIgnored
                            submittedFile.delete();
                        } finally {
                            if (bos != null)
                                IOUtils.closeQuietly(bos);
                            if (bis != null)
                                IOUtils.closeQuietly(bis);
                            if (zos != null)
                                IOUtils.closeQuietly(zos);
                        }
                        submission.setFile(outString);
                        submission.setBytes(null);
                        submission.setUri(null);
                        return;
                    }


                    if (submission.getBytes() != null) {
                        try {
                            //Timber.d("PREPARING BYTES.");
                            //Timber.d("Submission has " + submission.getBytes().length + " bytes.");
                            // write the upload package.
                            //Timber.d("Creating UploadPackage zip file entry.");
                            zos.putNextEntry(new ZipEntry("UploadPackage"));
                            String uploadPackage = gson.toJson(submission.getUploadPackage());
                            //Timber.d("INITIAL UPLOAD PACKAGE: " + uploadPackage);
                            bis = new ByteArrayInputStream(crypto.encryptToHex(uploadPackage).getBytes());
                            IOUtils.copy(bis, zos);
                            zos.closeEntry();
                            //Timber.d("UploadPackage zip entry complete.");

                            // start the data.
                            //Timber.d("Creating DATA zip file entry.");
                            zos.putNextEntry(new ZipEntry("DATA"));
                            //Timber.d("Writing bytes to entry.");
                            bis = new BufferedInputStream(new ByteArrayInputStream(submission.getBytes()));
                            bos = new BufferedOutputStream(crypto.getOutputStream(zos));
                            IOUtils.copy(bis, bos);
                            bos.flush();
                            zos.closeEntry();
                            //Timber.d("DATA zip entry complete.");
                        } finally {
                            if (bos != null)
                                IOUtils.closeQuietly(bos);
                            if (bis != null)
                                IOUtils.closeQuietly(bis);
                            if (zos != null)
                                IOUtils.closeQuietly(zos);
                        }
                        submission.setFile(outString);
                        submission.setBytes(null);
                        submission.setUri(null);
                        zos.close();
                        return;
                    }

                    if (submission.getUri() != null) {
                        //Timber.d("PREPARING URI");
                        Uri uri = submission.getUri();
                        String uploadType = submission.getUploadPackage().getType();

                        if (uploadType !=null && uploadType.equals(Givens.UPLOAD_TYPE_IMG)) {
                            //Timber.d("PROCESSING IMAGE.");
                            boolean wasPng = submission.getUploadPackage().getFormat().equals("PNG");
                            if (wasPng) {
                                submission.getUploadPackage().setFormat("JPG");
                            }
                            // write the upload package.
                            zos.putNextEntry(new ZipEntry("UploadPackage"));
                            String uploadPackage = gson.toJson(submission.getUploadPackage());
                            //Timber.d("INITIAL UPLOAD PACKAGE: " + uploadPackage);
                            Log.d(TAG,"INITIAL UPLOAD PACKAGE: " + uploadPackage);

                            bis = new ByteArrayInputStream(crypto.encryptToHex(uploadPackage).getBytes());
                            IOUtils.copy(bis, zos);
                            zos.closeEntry();
                            //Timber.d("UploadPackage zip entry complete.");
                            Log.d(TAG,"UploadPackage zip entry complete.");
                            // start the data.
                            //Timber.d("Creating DATA zip file entry.");
                            Log.d(TAG,"Creating DATA zip file entry.");
                            zos.putNextEntry(new ZipEntry("DATA"));

                            BufferedInputStream boundsStream = null;
                            try {
                                int[] requested = ImageUtil.getRequestedSize(application.getAgentSettings().getPhotoSize(), connectivityManager);

                                // if we know the requested size, and it isn't max, compute the scale factor.
                                BitmapFactory.Options options = null;

                                bos = new BufferedOutputStream(crypto.getOutputStream(zos));

                                if (requested[0] >= 1 || wasPng) {
                                    //Timber.d("Determining bounds");
                                    boundsStream = new BufferedInputStream(application.getContentResolver().openInputStream(uri));
                                    options = new BitmapFactory.Options();
                                    options.inJustDecodeBounds = true;
                                    BitmapFactory.decodeStream(boundsStream, null, options);

                                    if (options.outWidth > options.outHeight) {
                                        options.inSampleSize = ImageUtil.calculateInSampleSize(options, requested[0], requested[1]);
                                    } else {
                                        options.inSampleSize = ImageUtil.calculateInSampleSize(options, requested[1], requested[0]);
                                    }
                                    if (options.inSampleSize > 1 || wasPng) {
                                        //Timber.d("Scaling is needed, the file is larger than requested.");
                                        options.inJustDecodeBounds = false;
                                        bis = new BufferedInputStream(application.getContentResolver().openInputStream(uri));
                                        BitmapFactory.decodeStream(bis, null, options).compress(Bitmap.CompressFormat.JPEG, application.getAgentSettings().getPhotoQuality(), bos);
                                        submission.setFile(outString);
                                        submission.setUri(null);
                                        submission.setBytes(null);
                                    } else {
                                        //Timber.d("Scaling is not needed.");
                                        bis = new BufferedInputStream(application.getContentResolver().openInputStream(uri));
                                        IOUtils.copy(bis, bos);
                                        bos.flush();
                                        zos.closeEntry();
                                        submission.setFile(outString);
                                        submission.setUri(null);
                                        submission.setBytes(null);
                                    }
                                } else {
                                    bos = new BufferedOutputStream(crypto.getOutputStream(zos));
                                    bis = new BufferedInputStream(application.getContentResolver().openInputStream(uri));
                                    IOUtils.copy(bis, bos);
                                    bos.flush();
                                    zos.closeEntry();
                                    zos.close();
                                    submission.setFile(outString);
                                    submission.setUri(null);
                                    submission.setBytes(null);
                                }
                                // Delete temporary file after adding to the package
                                if (ProfileService.isTempFile(uri)) {
                                    final String filePrefix = "file://";
                                    File tempFile = new File(uri.toString().substring(filePrefix.length()));
                                    if (tempFile.exists()) {
                                        tempFile.delete();
                                    }
                                }
                            } finally {
                                if (bos != null)
                                    IOUtils.closeQuietly(bos);
                                if (bis != null)
                                    IOUtils.closeQuietly(bis);
                                if (zos != null)
                                    IOUtils.closeQuietly(zos);
                                if (boundsStream != null)
                                    IOUtils.closeQuietly(boundsStream);
                            }

                        } else {
                            //Timber.d("PROCESSING NON-IMAGE.");
                            try {
                                zos.putNextEntry(new ZipEntry("UploadPackage"));
                                String uploadPackage = gson.toJson(submission.getUploadPackage());
                                //Timber.d("INITIAL UPLOAD PACKAGE: " + uploadPackage);
                                bis = new ByteArrayInputStream(crypto.encryptToHex(uploadPackage).getBytes());
                                IOUtils.copy(bis, zos);
                                zos.closeEntry();

                                // start the data.
                                if (!uri.toString().equals("checkin://")) {
                                    zos.putNextEntry(new ZipEntry("DATA"));
                                    bos = new BufferedOutputStream(crypto.getOutputStream(zos));
                                    bis = new BufferedInputStream(application.getContentResolver().openInputStream(uri));
                                    IOUtils.copy(bis, bos);
                                    bos.flush();
                                }
                                zos.closeEntry();
                                submission.setFile(outString);
                                submission.setUri(null);
                                submission.setBytes(null);
                            } finally {
                                if (bos != null)
                                    IOUtils.closeQuietly(bos);
                                if (bis != null)
                                    IOUtils.closeQuietly(bis);
                                if (zos != null)
                                    IOUtils.closeQuietly(zos);
                            }
                        }

                        if (zos != null)
                            IOUtils.closeQuietly(zos);

                        return;
                    }

                    throw new Exception("Could not write file.");
                } catch (Exception e) {
                    Timber.e(e, "Could not write file.");
                } finally {
                    if (bos != null)
                        IOUtils.closeQuietly(bos);
                    if (bis != null)
                        IOUtils.closeQuietly(bis);
                    if (zos != null)
                        IOUtils.closeQuietly(zos);
                }
            }

            private void generateThumbnail(Submission submission) throws Exception {
                // not an image, so no thumbnail.
                if (submission == null || submission.getUploadPackage() == null || submission.getUploadPackage().getType() == null || !submission.getUploadPackage().getType().equals(Givens.UPLOAD_TYPE_IMG)) {
                    return;
                }
                //Timber.d("Generating thumbnail.");

                byte[] data = null;
                if (submission.getThumbnailData() != null) {
                    //Timber.d("Using thumbnail data from submission.");
                    data = submission.getThumbnailData();
                }
                if (data == null && submission.getBytes() != null) {
                    //Timber.d("Using thumbnail data from bytes.");
                    data = submission.getBytes();
                }
                if (data != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(data, 0, data.length, options);
                    // now we want to fully decode.
                    options.inJustDecodeBounds = false;
                    int size = ImageUtil.getPixels(42);
                    // sample size.
                    options.inSampleSize = ImageUtil.calculateInSampleSize(options, size, size);
                    setLastCapturedImage(
                            Bitmap.createBitmap(
                                    Bitmap.createScaledBitmap(
                                            BitmapFactory.decodeByteArray(data, 0, data.length, options),
                                            size,
                                            size,
                                            false
                                    ),
                                    0,
                                    0,
                                    size,
                                    size
                            )
                    );
                    return;
                }

                if (submission.getFile() != null) {
                    //Timber.d("Generating thumbnail from file.");
                    new FileInputStream(submission.getFile());
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(submission.getFile(), options);
                    // now we want to fully decode.
                    options.inJustDecodeBounds = false;
                    int size = ImageUtil.getPixels(42);
                    // sample size.
                    options.inSampleSize = ImageUtil.calculateInSampleSize(options, size, size);
                    setLastCapturedImage(
                            Bitmap.createBitmap(
                                    Bitmap.createScaledBitmap(
                                            BitmapFactory.decodeFile(submission.getFile(), options),
                                            size,
                                            size,
                                            false
                                    ),
                                    0,
                                    0,
                                    size,
                                    size
                            )
                    );
                    return;

                }

                if (submission.getUri() != null) {
                    //Timber.d("Generating thumbnail from uri.");
                    BufferedInputStream boundsStream = null;
                    BufferedInputStream fullStream = null;
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        boundsStream = new BufferedInputStream(application.getContentResolver().openInputStream(submission.getUri()));
                        BitmapFactory.decodeStream(boundsStream, null, options);
                        boundsStream.close();

                        // now we want to fully decode.
                        options.inJustDecodeBounds = false;
                        int size = ImageUtil.getPixels(42);
                        // sample size.
                        options.inSampleSize = ImageUtil.calculateInSampleSize(options, size, size);
                        fullStream = new BufferedInputStream(application.getContentResolver().openInputStream(submission.getUri()));
                        setLastCapturedImage(
                                Bitmap.createBitmap(
                                        Bitmap.createScaledBitmap(
                                                BitmapFactory.decodeStream(fullStream, null, options),
                                                size,
                                                size,
                                                false
                                        ),
                                        0,
                                        0,
                                        size,
                                        size
                                )
                        );
                    } finally {
                        if (boundsStream != null)
                            IOUtils.closeQuietly(boundsStream);
                        if (fullStream != null)
                            IOUtils.closeQuietly(fullStream);
                    }
                    return;
                }
            }


            @Override
            public void run() {
                if (!application.isSetupComplete() || queue.isEmpty()) {
                    // queue has been emptied - wait 1.5 seconds before checking again.
                    mHandler.postDelayed(this, 1500);
                    return;
                }

                Submission submission = queue.poll();
                try {
                    //Timber.d("resolving uri (if there is one)");
                    resolveUri(submission);
                    //Timber.d("processing thumbnail");
                    generateThumbnail(submission);
                    //Timber.d("building file.");
                    build(submission);
                    //Timber.d("Submitting file: " + submission.getFile());
                    uploadQueue.add(submission.getFile());
                    updateCount();
                } catch (Exception e) {
                    Timber.e(e, "Could not process submission.");
                }

                mHandler.post(this);

            }
        }
    }

    private class UploadHandlerThread extends HandlerThread {
        Handler mHandler = null;
        UploadRunnable runnable;

        public UploadHandlerThread() {
            super(UploadHandlerThread.class.getName(), Givens.THREAD_PRIORITY);
            start();
            mHandler = new Handler(getLooper());
            runnable = new UploadRunnable();
            mHandler.post(runnable);
        }

        private class UploadRunnable implements Runnable {
            public UploadRunnable() {

            }

            private boolean sendCheckinMessage(final Checkin checkin, final String batch) throws IOException {
                String responseBody = null;
                MessageUrl url = new MessageUrl(application.getScannedSettings(),
                        application.getString(R.string.checkin_endpoint),
                        application.getDeviceIdentifier());
                url.setMessage(checkin.getMessage());
                Map<String, String> param = new HashMap<String, String>() {{
                    put("batch", batch);
                    put("message", checkin.getMessage());
                    put("dateUTC", checkin.getDate());
                    put("lat", checkin.getLatitude());
                    put("lon", checkin.getLongitude());
                }};
                if (checkin.getContacts() != null && !checkin.getContacts().isEmpty()) {
                    param.put("contacts", checkin.getContacts());
                }
                url.setParamData(param);

                //Timber.d("Calling url: " + url.toString());
                Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
                responseBody = response.body().string().trim();
                response.body().close();

                if (responseBody != null && !responseBody.isEmpty()) {
                    return true;
                }

                return false;
            }

            @Override
            public void run() {
                updateCount();
                if (!application.isSetupComplete() || !isConnected() || uploadQueue.isEmpty()) {
                    mHandler.postDelayed(this, 1500);
                    return;
                }

                //Timber.d("Processing a Package");

                String fileString = uploadQueue.poll();
                inFlight++;
                File file = new File(fileString);

                if (!application.isSetupComplete() || !file.exists()) {
                    inFlight--;
                    updateCount();
                    //Timber.d("File has been deleted out from under us.");
                    mHandler.post(this);
                    return;
                }

                try {
                    if (file.length() == 0) {
                        inFlight--;
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                        //Timber.d("File was 0 size, deleted.");
                        updateCount();
                        mHandler.post(this);
                        return;
                    }

                    ZipFile zipFile = new ZipFile(file);

                    byte[] bytes = IOUtils.toByteArray(zipFile.getInputStream(zipFile.getEntry("UploadPackage")));
                    String uploadPackageString = crypto.decryptFromHexToString(new String(bytes));
                    //Timber.d("got upload package: " + uploadPackageString);
                    UploadPackage uploadPackage = gson.fromJson(uploadPackageString, UploadPackage.class);

                    // append header data.
                    StringBuilder headerBuffer = new StringBuilder(192);

                    // the original length of the data (pre-enryption)
                    /*
                    if (uploadPackage.getLength() == null || uploadPackage.getLength() < 0) {
                        headerBuffer.append("uid=").append(application.getDeviceIdentifier());
                    }
                    else {
                        headerBuffer.append("len=").append(uploadPackage.getLength());
                    */

                    // device id.
                    headerBuffer.append("uid=").append(application.getScannedSettings().getOrganizationId()).append(application.getDeviceIdentifier());
                    // ??
                    headerBuffer.append("|cat=AGENT");
                    // type
                    headerBuffer.append("|type=").append(uploadPackage.getType());
                    // format
                    headerBuffer.append("|format=").append(uploadPackage.getFormat());

                    // latitude and longitude, if present. may not be, especially for existing media.
                    if (uploadPackage.getLatitude() != null && uploadPackage.getLongitude() != null) {
                        headerBuffer.append("|lat=").append(uploadPackage.getLatitude());
                        headerBuffer.append("|lon=").append(uploadPackage.getLongitude());
                        if (uploadPackage.getAccuracy() != null) {
                            headerBuffer.append("|acc=").append(uploadPackage.getAccuracy());
                        }
                    }

                    // organization id.
                    headerBuffer.append("|org=").append(application.getScannedSettings().getOrganizationId());
                    // ??
                    headerBuffer.append("|info=anonymous");

                    // orientation matrix stuff, if present.
                    if (uploadPackage.getOrientationMatrix() != null) {
                        headerBuffer.append("|azimuth=").append(uploadPackage.getOrientationMatrix()[0]);
                        headerBuffer.append("|pitch=").append(uploadPackage.getOrientationMatrix()[1]);
                        headerBuffer.append("|roll=").append(uploadPackage.getOrientationMatrix()[2]);
                    }

                    if (uploadPackage.getCaseNumber() != null && !uploadPackage.getCaseNumber().trim().isEmpty()) {
                        headerBuffer.append("|CaseNumber=").append(uploadPackage.getCaseNumber());
                    }
                    if (uploadPackage.getCaseDescription() != null && !uploadPackage.getCaseNumber().trim().isEmpty()) {
                        headerBuffer.append("|msg=").append(uploadPackage.getCaseDescription());
                    } else {
                        headerBuffer.append("|msg=");
                    }

                    if (uploadPackage.getIsAvatar()) {
                        headerBuffer.append("|isAvatar=true");
                    }

                    // Additional parameters for segmented uploads only (local audio & video)
                        if(uploadPackage.getType().equals("IMG"))
                        {
                            uploadPackage.setBatch(null);
                        }
                        if (uploadPackage.getBatch() != null) {
                            // payload length
                            if (uploadPackage.getLength() == null && zipFile.getEntry("DATA") != null) {
                                uploadPackage.setLength(zipFile.getEntry("DATA").getSize());
                            }
                            headerBuffer.append("|len=").append(uploadPackage.getLength());
                            // batch
                            headerBuffer.append("|batch=").append(uploadPackage.getBatch());
                            // segment - last segment is negative number
                            Integer uploadSegment = uploadPackage.getSegment() == null ? -1 : uploadPackage.getSegment();
                            String uploadSegmentValue = String.format("%dof%d", Math.abs(uploadSegment), uploadSegment < 0 ? Math.abs(uploadSegment) : 0);
                            headerBuffer.append("|segment=" + uploadSegmentValue);
                        }


                    // end of header.
                    headerBuffer.append("\r\n");

                    SSLSocket socket = null;
                    OutputStream os = null;
                    InputStream is = null;
                    String responseBody = "";

                    try {
                        boolean uploadMedia = true;
                        //  Send "Check-in" message if needed
                        Checkin checkin = uploadPackage.getCheckin();
                        if (checkin != null) {
                            sendCheckinMessage(checkin, uploadPackage.getBatch());
                            // Skip data upload if nothing attached
                            if (zipFile.getEntry("DATA") == null) {
                                uploadMedia = false;
                            }
                        }

                        // Will come here again if server asked to resend data
                        while (uploadMedia) {
                            //socket = new Socket();
                            socket = (SSLSocket) socketFactory.createSocket();
                            String ipAddress = application.getScannedSettings().getIpAddress();
                            if (ipAddress.contains("://")) {
                                ipAddress = ipAddress.split("://")[1];
                            }
                            socket.connect(new InetSocketAddress(ipAddress, application.getScannedSettings().getUploadPort().intValue()), 20000);
                            Log.d(TAG, "run: "+ipAddress + application.getScannedSettings().getUploadPort().intValue());
                            //Timber.d("Socket connected");
                            os = crypto.getOutputStream(socket.getOutputStream());
                            //Timber.d("Header: " + headerBuffer.toString());
                            Log.d(TAG, "run:" +headerBuffer.toString());
                            //Timber.d("Port: " + application.getScannedSettings().getUploadPort());
                            os.write(headerBuffer.toString().getBytes("UTF-8"));

                            is = crypto.getInputStream(new BufferedInputStream(zipFile.getInputStream(zipFile.getEntry("DATA"))));
                            IOUtils.copy(is, os);
                            os.flush();
                            IOUtils.closeQuietly(os);
                            IOUtils.closeQuietly(is);
                            // Read response to check for "resend"
                            /*is = socket.getInputStream();
                            if (is != null) {
                                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                                String line = null;
                                StringBuilder response = new StringBuilder();
                                while ((line = in.readLine()) != null) {
                                    response.append(line);
                                }
                                responseBody = response.toString().trim();
                                IOUtils.closeQuietly(is);
                                Timber.d("Upload response: " + responseBody);
                            }*/

                            socket.close();
                            if (!responseBody.equals("resend")) {
                                break;
                            }
                        }

                        String uploadType = uploadPackage.getType();
                        if (uploadType != null) {
                            if (uploadType.equals(Givens.UPLOAD_TYPE_AUDIO))
                                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_AUDIO_SUBMITTED));
                            if (uploadType.equals(Givens.UPLOAD_TYPE_VIDEO))
                                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_VIDEO_SUBMITTED));
                            if (uploadType.equals(Givens.UPLOAD_TYPE_IMG))
                                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_PICTURE_SUBMITTED));
                            if (uploadType.equals(Givens.UPLOAD_TYPE_SMSLOG))
                                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_SMS_LOG_SUBMITTED));
                            if (uploadType.equals(Givens.UPLOAD_TYPE_PHONELOG))
                                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_PHONE_LOG_SUBMITTED));
                        }

                        try {
                            //Timber.d("deleting");
                            boolean deleted = file.delete();
                            if (!deleted) {
                                Timber.w("File Delete Failed.");
                            }
                        } catch (Exception e) {
                            Timber.e(e, "File Delete Failed.");
                        }

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

                } catch (ZipException ze) {
                    Timber.w(ze, "Couldn't handle compressed file. " + fileString + " Deleting.");
                    file.delete();
                } catch (JsonSyntaxException jse) {
                    Timber.w(jse, "JSON Syntax Exception - incomplete zip file. Deleting.");
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                } catch (Exception e) {
                    Timber.w(e, "Error uploading.");
                    uploadQueue.add(fileString);
                    inFlight--;
                    updateCount();
                    mHandler.postDelayed(this, 3000);
                    return;
                }

                inFlight--;
                updateCount();
                mHandler.post(this);
            }
        }

    }

    private class FileSystemMonitorThread extends HandlerThread {
        Handler mHandler = null;

        public FileSystemMonitorThread() {
            super(FileSystemMonitorThread.class.getName(), android.os.Process.THREAD_PRIORITY_DEFAULT + (2 * android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE));
            start();
            mHandler = new Handler(getLooper());
        }
    }

    private final class HighSecurityHandlerThread extends HandlerThread {
        private final Handler mHandler;
        private final Runnable runnable;
        private final ConcurrentLinkedQueue<Submission> queue = new ConcurrentLinkedQueue<>();

        public HighSecurityHandlerThread() {
            super(HighSecurityHandlerThread.class.getName(), Givens.THREAD_PRIORITY);
            start();
            mHandler = new Handler(getLooper());
            runnable = new HighSecurityRunnable();
            mHandler.post(runnable);
        }

        public void process(Submission submission) {
            queue.add(submission);
        }

        private final class HighSecurityRunnable implements Runnable {

            private void generateThumbnail(Submission submission) throws Exception {
                // not an image, so no thumbnail.
                if (!Givens.UPLOAD_TYPE_IMG.equals(submission.getUploadPackage().getType())) {
                    return;
                }
                //Timber.d("Generating thumbnail.");

                byte[] data = null;
                if (submission.getThumbnailData() != null) {
                    //Timber.d("Using thumbnail data from submission.");
                    data = submission.getThumbnailData();
                }
                if (data == null && submission.getBytes() != null) {
                    //Timber.d("Using thumbnail data from bytes.");
                    data = submission.getBytes();
                }
                if (data != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(data, 0, data.length, options);
                    // now we want to fully decode.
                    options.inJustDecodeBounds = false;
                    int size = ImageUtil.getPixels(42);
                    // sample size.
                    options.inSampleSize = ImageUtil.calculateInSampleSize(options, size, size);
                    setLastCapturedImage(
                            Bitmap.createBitmap(
                                    Bitmap.createScaledBitmap(
                                            BitmapFactory.decodeByteArray(data, 0, data.length, options),
                                            size,
                                            size,
                                            false
                                    ),
                                    0,
                                    0,
                                    size,
                                    size
                            )
                    );
                    return;
                }
                if (submission.getUri() != null) {
                    //Timber.d("Generating thumbnail from uri.");
                    BufferedInputStream boundsStream = null;
                    BufferedInputStream fullStream = null;
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        boundsStream = new BufferedInputStream(application.getContentResolver().openInputStream(submission.getUri()));
                        BitmapFactory.decodeStream(boundsStream, null, options);
                        boundsStream.close();

                        // now we want to fully decode.
                        options.inJustDecodeBounds = false;
                        int size = ImageUtil.getPixels(42);
                        // sample size.
                        options.inSampleSize = ImageUtil.calculateInSampleSize(options, size, size);
                        fullStream = new BufferedInputStream(application.getContentResolver().openInputStream(submission.getUri()));
                        setLastCapturedImage(
                                Bitmap.createBitmap(
                                        Bitmap.createScaledBitmap(
                                                BitmapFactory.decodeStream(fullStream, null, options),
                                                size,
                                                size,
                                                false
                                        ),
                                        0,
                                        0,
                                        size,
                                        size
                                )
                        );
                    } finally {
                        if (boundsStream != null)
                            IOUtils.closeQuietly(boundsStream);
                        if (fullStream != null)
                            IOUtils.closeQuietly(fullStream);
                    }
                    return;
                }

            }

            public void run() {
                if (queue.isEmpty()) {
                    // queue has been emptied - wait 1.5 seconds before checking again.
                    mHandler.postDelayed(this, 1500);
                    return;
                }

                try {
                    Submission submission = queue.poll();
                    resolveUri(submission);
                    generateThumbnail(submission);
                    secureQueue.add(submission);
                    updateCount();
                } catch (Exception e) {
                    Timber.e(e, "Error processing submission for upload.");
                }

                mHandler.post(this);
            }
        }
    }

    private final class HighSecurityUploadHandlerThread extends HandlerThread {
        private final Handler mHandler;
        private final Runnable runnable;

        public HighSecurityUploadHandlerThread() {
            super(HighSecurityUploadHandlerThread.class.getName(), Givens.THREAD_PRIORITY);
            start();
            mHandler = new Handler(getLooper());
            runnable = new HighUploadRunnable();
            mHandler.post(runnable);
        }

        private final class HighUploadRunnable implements Runnable {
            public void run() {
                if (!application.isSetupComplete() || !isConnected() || secureQueue.isEmpty()) {
                    mHandler.postDelayed(this, 1500);
                    return;
                }

                Submission submission = secureQueue.poll();
                inFlight++;

                if (submission == null) {
                    inFlight--;
                    mHandler.postDelayed(this, 1500);
                    return;
                }

                try {
                    UploadPackage uploadPackage = submission.getUploadPackage();

                    // append header data.
                    StringBuilder headerBuffer = new StringBuilder(192);

                    headerBuffer.append("uid=").append(application.getScannedSettings().getOrganizationId()).append(application.getDeviceIdentifier());

                    // ??
                    headerBuffer.append("|cat=AGENT");
                    // type
                    headerBuffer.append("|type=").append(uploadPackage.getType());
                    // format
                    headerBuffer.append("|format=").append(uploadPackage.getFormat());

                    // latitude and longitude, if present. may not be, especially for existing media.
                    if (uploadPackage.getLatitude() != null && uploadPackage.getLongitude() != null) {
                        headerBuffer.append("|lat=").append(uploadPackage.getLatitude());
                        headerBuffer.append("|lon=").append(uploadPackage.getLongitude());
                        if (uploadPackage.getAccuracy() != null) {
                            headerBuffer.append("|acc=").append(uploadPackage.getAccuracy());
                        }
                    }
                    // organization id.
                    headerBuffer.append("|org=").append(application.getScannedSettings().getOrganizationId());

                    // ??
                    headerBuffer.append("|info=anonymous");

                    // orientation matrix stuff, if present.
                    if (uploadPackage.getOrientationMatrix() != null) {
                        headerBuffer.append("|azimuth=").append(uploadPackage.getOrientationMatrix()[0]);
                        headerBuffer.append("|pitch=").append(uploadPackage.getOrientationMatrix()[1]);
                        headerBuffer.append("|roll=").append(uploadPackage.getOrientationMatrix()[2]);
                    }

                    if (uploadPackage.getCaseNumber() != null && !uploadPackage.getCaseNumber().trim().isEmpty()) {
                        headerBuffer.append("|CaseNumber=").append(uploadPackage.getCaseNumber());
                    }
                    if (uploadPackage.getCaseDescription() != null && !uploadPackage.getCaseNumber().trim().isEmpty()) {
                        headerBuffer.append("|msg=").append(uploadPackage.getCaseDescription());
                    } else {
                        headerBuffer.append("|msg=");
                    }

                    // end of header.
                    headerBuffer.append("\r\n");


                    SSLSocket socket = null;
                    OutputStream os = null;
                    InputStream is = null;


                    try {
                        socket = (SSLSocket) socketFactory.createSocket();
                        String ipAddress = application.getScannedSettings().getIpAddress();
                        if (ipAddress.contains("://")) {
                            ipAddress = ipAddress.split("://")[1];
                        }
                        socket.connect(new InetSocketAddress(ipAddress, application.getScannedSettings().getUploadPort().intValue()), 20000);
                        //Timber.d("Socket connected");
                        os = crypto.getOutputStream(socket.getOutputStream());
                        //Timber.d("Header: " + headerBuffer.toString());
                        //Timber.d("Port: " + application.getScannedSettings().getUploadPort());
                        os.write(headerBuffer.toString().getBytes("UTF-8"));
                        //Timber.d("Header Sent.");

                        if (submission.getBytes() != null) {
                            is = new ByteArrayInputStream(submission.getBytes());
                        }
                        if (submission.getBytes() == null && submission.getUri() != null) {
                            is = new BufferedInputStream(application.getContentResolver().openInputStream(submission.getUri()));
                        }
                        if (is != null) {
                           //Timber.d("Sending bytes.");
                            Log.d(TAG,"Sending bytes");
                            IOUtils.copy(is, os);
                            os.flush();
                            os.close();
                            is.close();
                            //Timber.d("Sending is completed.");
                            Log.d(TAG,"Sending is completed");
                        }
                        socket.close();
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
                } catch (Exception e) {
                    Timber.e(e, "An error occurred uploading file.");
                    if (submission != null)
                        secureQueue.add(submission);
                }
                inFlight--;

                updateCount();
                mHandler.post(this);
            }
        }
    }


}