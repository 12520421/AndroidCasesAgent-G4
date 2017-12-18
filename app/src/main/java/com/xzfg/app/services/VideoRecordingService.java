package com.xzfg.app.services;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.TextureView;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.exceptions.CameraParameterException;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.managers.OrientationManager;
import com.xzfg.app.managers.VolumeManager;
import com.xzfg.app.model.BatteryInfo;
import com.xzfg.app.model.PreviewSize;
import com.xzfg.app.model.UploadPackage;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.util.BatteryUtil;
import com.xzfg.app.util.ImageUtil;
import com.xzfg.app.util.MessageUtil;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 *
 */
@SuppressWarnings("deprecation")
public class VideoRecordingService extends Service
        implements TextureView.SurfaceTextureListener, MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    protected boolean mStreaming = false;
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
    OrientationManager orientationManager;
    @Inject
    FixManager fixManager;
    @Inject
    VolumeManager volumeManager;
    //String fileName;
    //String filePath;
    UploadPackage uploadPackage;
    private MediaRecorder mediaRecorder;
    private PowerManager.WakeLock wakeLock;
    //private int base;
    private int cameraId;
    private SurfaceTexture surface;
    private Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
    private Camera camera;

    // used for calculating the position when dragging the view around.
    //private int initialTouchX;
    //private int initialTouchY;
    //private int initialX;
    //private int initialY;
    private TextureView textureView;
    private boolean started = false;
    // this set of params makes the textureView the full size of the screen, and make it stay
    // "on" even when the screen is locked.
    private WindowManager.LayoutParams cantTouchThisParams;
    // this set of params maks the texture view small, and touchable, so we can drag it around.
    private WindowManager.LayoutParams touchThisParams;
    private boolean stopped = false;
    private int x;
    private int y;
    private Handler uiHandler;
    private Handler mCameraWaitHandler;
    private static final int MAX_WAIT = 375;
    private static volatile int waited = 0;
    // Batch GUID for segmented video recording
    private String fileBatch;
    // Current segment number for segmented video recording
    private int fileSegment;
    private int maxDuration = -1;
    private long startTime = 0;


    @Override
    public void onCreate() {
        super.onCreate();
        //Timber.d("CREATING VIDEO RECORDING SERVICE.");
        ((Application) getApplication()).inject(this);

        uiHandler = new Handler();
        HandlerThread handlerThread = new HandlerThread(VideoRecordingService.class.getName() + "_CAMERA_WAIT", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        mCameraWaitHandler = new Handler(handlerThread.getLooper());


        int frameRate = application.getAgentSettings().getVideoCasesFrameRate();
        if (frameRate != 15 && frameRate != 24 && frameRate != 30) {
            //Timber.d("Server has provided an invalid framerate. Forcing to 30.");
            application.getAgentSettings().setVideoCasesFrameRate(30);
            MessageUrl messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|VideoCasesFrameRateCASESAgent@30");
            MessageUtil.sendMessage(application, messageUrl);
        }

        // the handler is used to ensure that shutdown and other
        // requests all go through the main thread, required when accessing
        // camera and/or mediarecorder
        PreviewSize size = application.getPreviewSize(0);

        int touchThisFlags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        int cantTouchThisFlags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        if (!BuildConfig.DEBUG) {
            touchThisFlags = touchThisFlags | LayoutParams.FLAG_SECURE;
            cantTouchThisFlags = cantTouchThisFlags | LayoutParams.FLAG_SECURE;
        }

        // use relative sizes.
        touchThisParams = new WindowManager.LayoutParams(
                size.getWidth(),
                size.getHeight(),
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                touchThisFlags,
                PixelFormat.TRANSLUCENT
        );
        touchThisParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        touchThisParams.y = size.getY();

        // this adds the "SHOW_WHEN_LOCKED" attribute, which
        // allows the camera to stay on when the device is locked.
        cantTouchThisParams = new WindowManager.LayoutParams(
                size.getWidth(),
                size.getHeight(),
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                cantTouchThisFlags,
                PixelFormat.TRANSLUCENT
        );
        cantTouchThisParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        cantTouchThisParams.y = size.getY();

        /*
        if (size[2] > -1 && size[3] > -1) {
            cantTouchThisParams.x = size[2];
            cantTouchThisParams.y = size[3];
        }
        */

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, VideoRecordingService.class.getName());
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock.acquire();


        // create the TextureView.
        textureView = new TextureView(this);

        // touch listener allows for dragging the view around.
        // only applies if the layout permits it to be touched.
        //textureView.setOnTouchListener(this);

        // register for events.
        EventBus.getDefault().registerSticky(this);
        //Timber.d("VIDEO RECORDING SERVICE CREATED.");
    }

    private void runOnUiThread(Runnable r) {
        uiHandler.post(r);
    }

    public void onEventMainThread(Events.NavigationDrawerClosed event) {
        try {
            if (!stopped) {
                makeTouchable();
            }
        } catch (Exception ex) {
            // Do nothing
        }
    }

    public void onEventMainThread(Events.FreeSpaceLowEvent freeSpaceLowEvent) {
        EventBus.getDefault().postSticky(new Events.RecordingStoppedEvent());
        stopSelf();
    }

    public void onEventMainThread(Events.ActivityResumed event) {
        //Timber.e("Activity resumed, making visible.");
        Timber.e("ACTIVITY RESUMED");
        EventBus.getDefault().removeStickyEvent(Events.ActivityPaused.class);
        makeTouchable();
        EventBus.getDefault().removeStickyEvent(event);
    }

    public void onEventMainThread(Events.ActivityPaused event) {
        Timber.e("ACTIVITY PAUSED");
        //Timber.e("Activity paused, hiding display.");
        makeUntouchable();
        EventBus.getDefault().removeStickyEvent(event);
    }

    public void makeUntouchable() {
        Timber.e("MAKING UNTOUCHABLE");
        //Timber.d("Removing preview from view.");
        if (textureView != null) {
            textureView.setAlpha(0f);
            textureView.setLayoutParams(cantTouchThisParams);
            //textureView.setOnTouchListener(null);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (textureView.isAttachedToWindow() || textureView.isAvailable()) {
                        windowManager.updateViewLayout(textureView, cantTouchThisParams);
                    }
                } else {
                    if (textureView.isAvailable()) {
                        windowManager.updateViewLayout(textureView, cantTouchThisParams);
                    }
                }
            } catch (Exception e) {
                Timber.e(e,"Not in layout yet?");
            }
        }
    }

    public void makeTouchable() {
        Timber.e("MAKING TOUCHABLE");
        //Timber.d("Adding preview to view.");
        if (textureView != null) {
            textureView.setAlpha(1f);
            textureView.setLayoutParams(touchThisParams);
            //textureView.setOnTouchListener(this);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (textureView.isAttachedToWindow() || textureView.isAvailable()) {
                        windowManager.updateViewLayout(textureView, touchThisParams);
                    }
                } else {
                    if (textureView.isAvailable()) {
                        windowManager.updateViewLayout(textureView, touchThisParams);
                    }
                }
            } catch (Exception e) {
                Timber.e(e,"Not in layout yet.");
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (started) {
            Timber.e("Second start command received.");
            stopSelf();
            return START_NOT_STICKY;
        }
        started = true;

        if (mStreaming || stopped) {
            stopSelf();
            return START_NOT_STICKY;
        }
        EventBus.getDefault().post(new Events.CameraRequested());

        mediaManager.videoRecordingStarted();
        cameraId = i.getIntExtra("camera", 0);

        boolean startVisible = i.getBooleanExtra("visible", powerManager.isScreenOn());

        // the initial state is touchable and visible.
        if (startVisible) {
            textureView.setAlpha(1f);
            windowManager.addView(textureView, touchThisParams);
        } else {
            textureView.setAlpha(0f);
            windowManager.addView(textureView, cantTouchThisParams);
        }

        //fileName = UUID.randomUUID().toString() + ".mp4";
        // Initialize batch GUID and segment for segmented audio recording
        fileBatch = UUID.randomUUID().toString();
        fileSegment = 1;

        textureView.setSurfaceTextureListener(this);

        if (i.getExtras() != null) {
            maxDuration = i.getExtras().getInt(Givens.EXTRA_MAX_DURATION, -1);
        }

        return START_NOT_STICKY;
    }

    private synchronized void stopMediaRecorder() {
        if (mediaRecorder != null) {
            MediaRecorder deadMediaRecorder = mediaRecorder;
            mediaRecorder = null;
            try {

                deadMediaRecorder.stop();
            } catch (Exception e) {
                Timber.w(e, "Error stopping mediaRecorder.");
            }
            try {
                deadMediaRecorder.reset();
            } catch (Exception e) {
                Timber.w(e, "Error resetting mediaRecorder.");
            }
            try {
                deadMediaRecorder.release();
            } catch (Exception e) {
                Timber.w(e, "Error releasing mediaRecorder.");
            }
            deadMediaRecorder = null;
        }

        // attempt to lock the camera.
        if (camera != null) {
            try {
                camera.lock();
            } catch (Exception e) {
                Timber.e(e, "Couldn't lock camera.");
            }
        }
    }

    private synchronized void stopCamera() {
        //Timber.d("Stop camera called.");
        if (camera != null) {
            Camera deadCamera = camera;
            camera = null;

            try {
                if (deadCamera != null)
                    deadCamera.setPreviewCallback(null);
            } catch (Exception e) {
                Timber.w(e, "Error removing preview callback.");
            }
            try {
                if (deadCamera != null)
                    deadCamera.stopPreview();
            } catch (Exception e) {
                Timber.w(e, "Couldn't stop preview.");
            }
            try {
                if (deadCamera != null)
                    deadCamera.setPreviewTexture(null);
            } catch (Exception e) {
                Timber.w(e, "Couldn't clear preview texture.");
            }
            try {
                if (deadCamera != null) {
                    deadCamera.lock();
                }
            } catch (Exception e) {
                Timber.w(e, "Couldn't release camera.");
            }
            try {
                if (deadCamera != null) {
                    deadCamera.release();
                }
            } catch (Exception e) {
                Timber.w(e, "Couldn't release camera.");
            }
            deadCamera = null;
            try {
                if (textureView != null) {
                    textureView.setOnTouchListener(null);
                    textureView.setSurfaceTextureListener(null);
                    makeUntouchable();
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
                Timber.w(e, "Couldn't remove textureView.");
            }
        }
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        stopRecording(true);
        stopCamera();
        mCameraWaitHandler.getLooper().quit();
        if (wakeLock.isHeld())
            wakeLock.release();
        volumeManager.restore();
        super.onDestroy();
    }


    private void openCamera() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    EventBus.getDefault().post(new Events.CameraRequested());
                    Camera openedCamera = Camera.open(mediaManager.getCameraId());
                    camera = openedCamera;
                    if (camera != null) {
                        onCameraOpened();
                    }
                } catch (Exception e) {
                    waited = waited + 1;
                    if (waited < MAX_WAIT) {
                        Timber.w(e, "Couldn't open camera, will try again in 16ms.");
                        mCameraWaitHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                openCamera();
                            }
                        }, 16);
                    } else {
                        Timber.w("Max wait exceeded, giving up on camera.");
                        waited = 0;
                        stopSelf();
                    }
                }
            }
        });
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.surface = surface;
        openCamera();
    }

    private synchronized void onCameraOpened() {
        try {
            Timber.w("Camera Opened.");
            Camera.getCameraInfo(cameraId, cameraInfo);

            // mute the shutter sound.
            if (cameraInfo.canDisableShutterSound) {
                camera.enableShutterSound(false);
                //Timber.d("Shutter sound disabled.");
            }

            volumeManager.mute();
            camera.setDisplayOrientation(90);

            Camera.Parameters params = camera.getParameters();

            try {
                params.set("cam_mode", 1);
                camera.setParameters(params);
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set cam_mode (samsung specific setting to enable continuous focus).", e);
            }

            try {
                String defaultMode = params.getFocusMode();
                List<String> supportedFocusModes = params.getSupportedFocusModes();

                // set the focus mode, preferring continuous video, picture, and auto,
                // finally falling back to the default focus mode.
                if (supportedFocusModes != null && !supportedFocusModes.isEmpty()) {
                    boolean setFocus = false;
                    if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        setFocus = true;
                        //Timber.d("Using continuous video focus.");
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }
                    if (setFocus == false && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        setFocus = true;
                        //Timber.d("Using continuous picture focus.");
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    }
                    if (setFocus == false && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        setFocus = true;
                        //Timber.d("Using auto focus");
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                    if (setFocus == false && defaultMode != null && supportedFocusModes.contains(defaultMode)) {
                        setFocus = true;
                        //Timber.d("Using default focus mode: " + defaultMode);
                        params.setFocusMode(defaultMode);
                    }
                    camera.setParameters(params);
                }
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set camera focus.", e);
            }

            // turn off the flash, if FLASH_MODE_OFF is supported.
            try {
                List<String> supportedFlashModes = params.getSupportedFlashModes();
                if (supportedFlashModes != null && !supportedFlashModes.isEmpty() && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                    //Timber.d("Turning off flash mode.");
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(params);
                }
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set flash mode.", e);
            }

            try {
                // turn on auto anti banding support, if available.
                List<String> supportedAntiBanding = params.getSupportedAntibanding();
                if (supportedAntiBanding != null && !supportedAntiBanding.isEmpty() && supportedAntiBanding.contains(Camera.Parameters.ANTIBANDING_AUTO)) {
                    //Timber.d("Turning on auto anti-banding.");
                    params.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
                    camera.setParameters(params);
                }
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set anti-banding mode.", e);
            }

            /*
            try {
                // turn on auto exposure lock, if supported
                if (params.isAutoExposureLockSupported()) {
                    params.setAutoExposureLock(true);
                    camera.setParameters(params);
                }
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set auto exposure lock.", e);
            }

            try {
                // turn on auto white balance lock, if supported
                if (params.isAutoWhiteBalanceLockSupported()) {
                    params.setAutoWhiteBalanceLock(true);
                    camera.setParameters(params);
                }
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set auto white balance lock.", e);
            }
            */

            try {
                List<String> whiteBalances = params.getSupportedWhiteBalance();
                if (whiteBalances != null && !whiteBalances.isEmpty()) {
                    if (whiteBalances.contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
                        //Timber.d("Enabling auto-white balance.");
                        params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
                        camera.setParameters(params);
                    }
                }
            } catch (Exception e) {
                throw new CameraParameterException(("Couldn't set auto-white balance."));
            }

            try {
                // turn on video stabilization, if supported.
                if (params.isVideoStabilizationSupported()) {
                    //Timber.d("Enabling video stabilization.");
                    params.setVideoStabilization(true);
                    camera.setParameters(params);
                }
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set video stabilization", e);
            }


            /*
            try {
                List<Camera.Size> videoSizes = params.getSupportedVideoSizes();
                if (videoSizes == null || videoSizes.isEmpty()) {
                    Timber.d("No supported video sizes, falling back to supported picture sizes.");
                    videoSizes = params.getSupportedPictureSizes();
                }
                Camera.Size videoSize = ImageUtil.getBestVideoSize(application.getAgentSettings(), connectivityManager, videoSizes);
                Timber.d("Best video size: " + videoSize.width + "x" + videoSize.height);
                params.setPictureSize(videoSize.width, videoSize.height);
                camera.setParameters(params);
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set picture size", e);
            }
            */

            try {
                List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
                if (previewSizes == null || previewSizes.isEmpty()) {
                    //Timber.d("No supported preview sizes, falling back to supported video sizes.");
                    previewSizes = params.getSupportedVideoSizes();
                }
                if (previewSizes == null || previewSizes.isEmpty()) {
                    //Timber.d("No supported preview or video sizes, falling back to supported picture sizes.");
                    previewSizes = params.getSupportedPictureSizes();
                }
                Camera.Size previewSize = ImageUtil.getBestVideoSize(application.getAgentSettings(), connectivityManager, previewSizes);
                //Timber.d("Best preview size: " + previewSize.width + "x" + previewSize.height);
                params.setPreviewSize(previewSize.width, previewSize.height);
                camera.setParameters(params);
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set preview size.", e);
            }

            try {
                int requestedRate = application.getAgentSettings().getVideoCasesFrameRate();
                List<int[]> previewRanges = params.getSupportedPreviewFpsRange();
                int[] bestRange = null;
                if (previewRanges != null && !previewRanges.isEmpty()) {
                    for (int[] range : previewRanges) {
                        int min = range[0];
                        int max = range[1];
                        if (min >= 10000) {
                            min = min / 10000;
                        }
                        if (max >= 10000) {
                            max = max / 10000;
                        }
                        //Timber.d("Range: " + "min: " + min + ", max: " + max);

                        if (requestedRate > min && requestedRate <= max) {
                            //Timber.d("USING Range: " + "min: " + min + ", max: " + max);
                            bestRange = range;
                            params.setPreviewFpsRange(range[0], range[1]);
                        }
                    }
                }

            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set preview frame rate.", e);
            }

            try {
                // set the camera orientation to match the display orientation.
                switch (orientationManager.getOrientation()) {
                    case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                        //Timber.d("Orientation: " + 90);
                        params.setRotation(90);
                        break;
                    case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                        //Timber.d("Orientation: " + 180);
                        params.setRotation(180);
                        break;
                    case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                        //Timber.d("Orientation: " + 0);
                        params.setRotation(0);
                        break;
                    case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                        //Timber.d("Orientation: " + 270);
                        params.setRotation(270);
                }
                camera.setParameters(params);
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set rotation.", e);
            }

            try {
                Location location = fixManager.getLastLocation();
                if (location != null) {
                    params.setGpsLatitude(location.getLatitude());
                    params.setGpsLongitude(location.getLongitude());
                    params.setGpsAltitude(location.getAltitude());
                    params.setGpsTimestamp(location.getTime());
                    params.setGpsProcessingMethod(location.getProvider());
                }
                camera.setParameters(params);
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set GPS metaData.");
            }


            camera.setParameters(params);
            camera.setPreviewTexture(surface);
            camera.startPreview();

            Timber.w("Preview started.");

            startTime = System.currentTimeMillis();
            startRecording();
        } catch (Exception e) {
            Timber.e(e, "Video Recording Start Failed.");
            MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Video Recording Start Failed."));
            stopSelf();
        }


    }


    private void startRecording() throws Exception {
        volumeManager.mute();

        uploadPackage = new UploadPackage();
        uploadPackage.setType(Givens.UPLOAD_TYPE_VIDEO);
        uploadPackage.setFormat(Givens.UPLOAD_FORMAT_MP4);
        if (application.getAgentSettings() != null) {
            uploadPackage.setCaseNumber(application.getAgentSettings().getCaseNumber());
            uploadPackage.setCaseDescription(application.getAgentSettings().getCaseDescription());
        }

        String filePath = getFilePath();

        File folder = new File(filePath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        filePath += getFileName();
        //(new File(application.getFilesDir().getAbsolutePath() + "/media/")).mkdirs();
        //filePath = application.getFilesDir().getAbsolutePath() + "/media/" + fileName;
        //Timber.d("Writing to file path: " + filePath);

        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            } catch (Exception e) {
                Timber.e(e, "Failed to stop existing media recorder.");
            }
        }

        mediaRecorder = new MediaRecorder();

        List<Camera.Size> videoSizes = camera.getParameters().getSupportedVideoSizes();
        if (videoSizes == null || videoSizes.isEmpty()) {
            //Timber.d("No supported video sizes, falling back to supported picture sizes.");
            videoSizes = camera.getParameters().getSupportedPictureSizes();
        }
        Camera.Size videoSize = ImageUtil.getBestVideoSize(application.getAgentSettings(), connectivityManager, videoSizes);
        //Timber.d("Best video size: " + videoSize.width + "x" + videoSize.height);

        CamcorderProfile profile = null;

        if (videoSize.height >= 1080 && CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
            //Timber.d("Using 1080p profile as base.");
            profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P);
        }
        if (profile == null && videoSize.height >= 720 && CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
            //Timber.d("Using 720p profile as base.");
            profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
        }
        if (profile == null && videoSize.height >= 480 && CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
            //Timber.d("Using 480p profile as base.");
            profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
        }
        if (profile == null && CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_CIF)) {
            //Timber.d("Using CIF profile as base.");
            profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_CIF);
        }
        if (profile == null) {
            Timber.d("No near-match profile.");
        }

        camera.unlock();

        mediaRecorder.setCamera(camera);
        if (application.getAgentSettings().getIncludeAudio() == 1) {
            //Timber.d("Including Audio");
        //    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        mediaRecorder.setVideoFrameRate(application.getAgentSettings().getVideoCasesFrameRate());
        mediaRecorder.setVideoSize(videoSize.width, videoSize.height);
        if (profile != null) {
            //Timber.d("Setting bitrate to " + profile.videoBitRate);
            mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        }
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        if (application.getAgentSettings().getIncludeAudio() == 1) {
            //Timber.d("Using 128bit audio");
            mediaRecorder.setAudioEncodingBitRate(128 * 1024);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        }

        int orientation = orientationManager.getOrientation();
        // set the camera orientation to match the display orientation.
        switch (orientation) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                mediaRecorder.setOrientationHint(90);
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                mediaRecorder.setOrientationHint(180);
                break;
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                mediaRecorder.setOrientationHint(0);
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                mediaRecorder.setOrientationHint(270);
        }
        uploadPackage.setOrientation(orientation);
        uploadPackage.setOrientationMatrix(orientationManager.getOrientationMatrix());


        // get the location at the start.
        Location location = fixManager.getLastLocation();
        if (location != null) {
            uploadPackage.setLatitude(location.getLatitude());
            uploadPackage.setLongitude(location.getLongitude());
            if (location.getAccuracy() != 0) {
                uploadPackage.setAccuracy(location.getAccuracy());
            }
            mediaRecorder.setLocation((float) location.getLatitude(), (float) location.getLongitude());
        }

        //Timber.d("Setting outputfile");
        mediaRecorder.setOutputFile(filePath);
        //Timber.d("Setting error listener");
        mediaRecorder.setOnErrorListener(this);
        mediaRecorder.setMaxDuration(maxDuration > 0 ? maxDuration : Givens.MAX_VIDEO_DURATION_MS);
        mediaRecorder.setOnInfoListener(this);
        //Timber.d("Preparing");
        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            Timber.e(e, "Could not prepare recording.");
            throw e;
        }
        //Timber.d("Sleeping");
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            Timber.w(e, "Couldn't sleep between prepare and start.");
        }
        uploadPackage.setDate(new Date());

        try {
            mediaRecorder.start();
        } catch (Exception e) {
            try {
                if (camera != null) {
                    camera.lock();
                }
            } catch (Exception f) {
                Timber.w(f, "Problem locking camera.");
            }
            throw e;
        }
        // Start recording counter only once at the beginning of recording
        if (fileSegment < 2) {
            mediaManager.videoRecordingStarted();
        }
        mStreaming = true;

        volumeManager.restore();
    }

    private synchronized void stopRecording(boolean lastSegment) {
        if (!stopped) {
            volumeManager.mute();

            stopMediaRecorder();
            if (lastSegment) {
                stopped = true;
                stopCamera();
            }

            String filePath = getFilePath() + getFileName();
            if (filePath != null) {
                File f = new File(filePath);
                if (f.exists()) {
                    uploadPackage.setLength(f.length());
                    uploadPackage.setBatch(fileBatch);
                    uploadPackage.setSegment(fileSegment * (lastSegment ? -1 : 1));
                    mediaManager.submitUpload(uploadPackage, f);
                } else {
                    Timber.w("File doesn't exist.");
                }
                filePath = null;
            }

            mStreaming = false;
            if (lastSegment) {
                mediaManager.videoRecordingEnded();
            }

            volumeManager.restore();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        //Timber.d("Video Recording Surface Texture Destroyed");
        stopSelf();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    /*
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v == null) {
            return false;
        }
        final int X = (int) event.getRawX();
        final int Y = (int) event.getRawY();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                initialX = touchThisParams.x;
                initialY = touchThisParams.y;
                initialTouchX = X;
                initialTouchY = Y;
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                touchThisParams.x = (initialX + (X - initialTouchX));
                touchThisParams.y = (initialY + (Y - initialTouchY));
                if (textureView != null) {
                    windowManager.updateViewLayout(textureView, touchThisParams);
                    return true;
                }
            }
        }

        return false;
    }*/


    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_ERROR_SERVER_DIED: {
                Timber.e("Media Recorder Stopped Unexpectedly. Server Died.");
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Video Recording Stopped Unexpectedly. Server Died."));
                break;
            }
            case MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN: {
                Timber.e("Media Recorder Stopped Unexpectedly. Cause unknown.");
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Video Recording Stopped Unexpectedly. Cause Unknown."));
                break;
            }
            default: {
                Timber.e("Media Recorder Error.  What: " + what + ", Extra: " + extra);
            }
        }
        stopSelf();
    }

    private String getFileName() {
        return fileBatch + fileSegment + ".mp4";
    }

    private String getFilePath() {
        return application.getFilesDir().getAbsolutePath() + "/media/";
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED: {
                //Timber.d("Media Recorder Reached Maximum Duration: " + MAX_DURATION_MS);
                // If maxDuration > 0 then stop recording and quit
                // Segmented video recording - stop recording, create upload package for segment, resume recording
                try {
                    BatteryInfo batteryInfo = BatteryUtil.getBatteryInfo(this);
                    boolean lowBattery = (!batteryInfo.isCharging && batteryInfo.percentCharged < 5);
                    //long recordingTime = System.currentTimeMillis() - startTime;

                    if (maxDuration > 0 || lowBattery) {
                        stopRecording(true);
                    } else {
                        stopRecording(false);
                        fileSegment++;
                        startRecording();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN: {
                break;
            }
        }
    }
}
