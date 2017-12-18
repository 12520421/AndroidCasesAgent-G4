package com.xzfg.app.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.TextureView;
import android.view.WindowManager;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.managers.OrientationManager;
import com.xzfg.app.model.Submission;
import com.xzfg.app.model.UploadPackage;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.ImageUtil;

import java.io.IOException;
import java.util.Date;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 *
 */
@SuppressWarnings("deprecation")
public class PhotoService extends Service
        implements TextureView.SurfaceTextureListener, Camera.PictureCallback, Camera.AutoFocusCallback {

    protected volatile boolean mStreaming = false;
    protected volatile boolean mAutoFocusSupported = false;
    @Inject
    Application application;
    @Inject
    WindowManager windowManager;
    @Inject
    PowerManager powerManager;
    @Inject
    MediaManager mediaManager;
    @Inject
    ConnectivityManager connectivityManager;
    @Inject
    Crypto crypto;
    @Inject
    OrientationManager orientationManager;
    @Inject
    FixManager fixManager;
    private PowerManager.WakeLock wakeLock;
    private Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
    private Camera camera;
    private TextureView textureView;
    private int rotation = 0;
    private int attempts = 1;
    // this set of params makes the textureView the full size of the screen, and make it stay
    // "on" even when the screen is locked.
    private WindowManager.LayoutParams cantTouchThisParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
    );

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
        Timber.e("PhotoService started.");
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, VideoRecordingService.class.getName());
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock.acquire();

        // create the TextureView.
        textureView = new TextureView(this);

        // by default - 0 alpha, can't touch this.
        textureView.setAlpha(0f);
        windowManager.addView(textureView, cantTouchThisParams);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        //Timber.e("Calling onPictureTaken, attempts: " + attempts);
        // Wait until final attempt to send photo
        attempts--;
        if (attempts > 0){
            camera.startPreview();
            if (mAutoFocusSupported) {
                camera.autoFocus(this);
            }
            else {
                camera.takePicture(null, null, null, this);
            }
            return;
        }

        UploadPackage uploadPackage = new UploadPackage();
        uploadPackage.setType(Givens.UPLOAD_TYPE_IMG);
        uploadPackage.setFormat(Givens.UPLOAD_FORMAT_JPEG);
        uploadPackage.setDate(new Date());
        uploadPackage.setOrientation(orientationManager.getOrientation());
        uploadPackage.setOrientationMatrix(orientationManager.getOrientationMatrix());
        uploadPackage.setCaseNumber(application.getAgentSettings().getCaseNumber());
        uploadPackage.setCaseDescription(application.getAgentSettings().getCaseDescription());

        Location location = fixManager.getLastLocation();
        if (location != null) {
            uploadPackage.setLongitude(location.getLongitude());
            uploadPackage.setLatitude(location.getLatitude());
            uploadPackage.setAccuracy(location.getAccuracy());
        }
        Submission submission = new Submission(uploadPackage, data);
        Timber.e("submitting upload package: " + uploadPackage);
        mediaManager.submitUpload(submission);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        //Timber.d("Service started.");
        textureView.setSurfaceTextureListener(this);
        attempts = i.getIntExtra(Givens.EXTRA_ATTEMPTS, 1);

        return START_NOT_STICKY;
    }


    private synchronized void stopCamera() {
        //Timber.e("Calling stopCamera");
        if (camera != null) {
            try {
                camera.stopPreview();
            } catch (Exception e) {
                Timber.w(e, "Couldn't stop preview.");
            }
            try {
                camera.setPreviewTexture(null);
            } catch (Exception e) {
                Timber.w(e, "Couldn't clear preview texture.");
            }
            try {
                camera.release();
            } catch (Exception e) {
                Timber.w(e, "Couldn't release camera.");
            }
            camera = null;
            try {
                if (textureView != null) {
                    textureView.setSurfaceTextureListener(null);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        if (textureView.isAttachedToWindow() || textureView.isAvailable()) {
                            windowManager.removeView(textureView);
                        }
                    } else {
                        if (textureView.isAvailable()) {
                            windowManager.removeView(textureView);
                        }
                    }
                    textureView = null;
                }
            } catch (Exception e) {
                Timber.w(e, "Error removing textureView.");
            }

        }
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        stopCamera();

        if (wakeLock.isHeld())
            wakeLock.release();
        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Timber.e("surface texture available.");

        int cameraId = mediaManager.getCameraId();

        try {
            camera = Camera.open(cameraId);
        } catch (Exception e) {
            EventBus.getDefault().post(new Events.CameraRequested());
            try {
                Timber.w("Camera not available. Waiting.");
                try {
                    Thread.sleep(2000);
                } catch (Exception f) {
                    Timber.w(f, "An error occurred waiting for the camera.");
                }
                camera = Camera.open(cameraId);
            } catch (Exception f) {
                Timber.e(f, "Can't open camera.");
                stopSelf();
                return;
            }
        }

        Camera.getCameraInfo(cameraId, cameraInfo);

        // mute the shutter sound.
        if (cameraInfo.canDisableShutterSound) {
            camera.enableShutterSound(false);
        } else {
            AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        }

        camera.setDisplayOrientation(90);

        Camera.Parameters params = camera.getParameters();
        params.set("cam_mode", 1);
        String defaultMode = params.getFocusMode();

        try {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            camera.setParameters(params);
            mAutoFocusSupported = true;
        } catch (Exception noAutoFocus) {
            try {
                params.setFocusMode(defaultMode);
                camera.setParameters(params);
            } catch (Exception e) {
                Timber.e(e, "Unable to set focus mode - even the default failed.");
            }
        }

        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        params.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
        params.setRecordingHint(true);

        Camera.Size previewSize = ImageUtil.getBestPhotoSize(application.getAgentSettings(), connectivityManager, camera.getParameters().getSupportedPreviewSizes());

        params.setPreviewSize(previewSize.width, previewSize.height);
        Camera.Size photoSize = ImageUtil.getBestPhotoSize(application.getAgentSettings(), connectivityManager, camera.getParameters().getSupportedPictureSizes());
        params.setPictureSize(photoSize.width, photoSize.height);

        // set the camera orientation to match the display orientation.
        switch (orientationManager.getOrientation()) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                //Timber.w("SETTING PHOTO TO PORTRAIT.");
                params.setRotation(90);
                rotation = 90;
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                //Timber.w("SETTING PHOTO TO REVERSE PORTRAIT");
                params.setRotation(270);
                rotation = 270;
                break;
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                //Timber.w("SETTING PHOTO TO LANDSCAPE.");
                params.setRotation(0);
                rotation = 0;
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                //Timber.w("SETTING PHOTO TO REVERSE LANDSCAPE");
                params.setRotation(180);
                rotation = 180;
        }

        Location location = fixManager.getLastLocation();
        if (location != null) {
            params.setGpsLatitude(location.getLatitude());
            params.setGpsLongitude(location.getLongitude());
            params.setGpsAltitude(location.getAltitude());
            params.setGpsTimestamp(location.getTime());
            params.setGpsProcessingMethod(location.getProvider());
        }

        if (params.isVideoStabilizationSupported()) {
            params.setVideoStabilization(true);
        }

        params.setJpegQuality(application.getAgentSettings().getPhotoQuality());

        camera.setParameters(params);

        try {
            mStreaming = true;
            camera.setPreviewTexture(surface);
            camera.startPreview();
            if (mAutoFocusSupported) {
                camera.autoFocus(this);
            }
            else {
                camera.takePicture(null, null, null, this);
            }
        } catch (IOException e) {
            Timber.e(e, "Something bad happened.");
            stopSelf();
        } catch (RuntimeException e) {
            Timber.e(e, "Something bad happened.");
            stopSelf();
        }
    }


    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stopCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //Timber.w("surface texture updated.");
    }


    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if(success) {
            camera.takePicture(null, null, null, this);
        }
        else{
            camera.cancelAutoFocus();
            camera.takePicture(null,null,null,this);
        }
    }
}
