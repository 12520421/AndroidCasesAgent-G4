package com.xzfg.app.services;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.location.Location;
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
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.exceptions.CameraParameterException;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.managers.OrientationManager;
import com.xzfg.app.managers.VolumeManager;
import com.xzfg.app.model.PreviewSize;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.BatteryUtil;
import com.xzfg.app.util.ImageUtil;
import com.xzfg.app.util.MessageUtil;
import com.xzfg.app.util.Network;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import de.greenrobot.event.EventBus;
import timber.log.Timber;


@SuppressWarnings("deprecation")
public class VideoStreamingService extends Service implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {
    private static final int SOCKET_TIMEOUT = 20000;
    protected volatile boolean mStreaming = false;

    public static volatile CircularFifoQueue<byte[]> queue = new CircularFifoQueue<>(3);
    public static final Object queueLock = new Object();


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

    @Inject
    VolumeManager volumeManager;

    @Inject
    SSLSocketFactory socketFactory;

    Boolean startVisible = null;
    private boolean started = false;
    private PowerManager.WakeLock wakeLock;
    private Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
    private Camera camera;
    private Camera.Size size;
    private int previewFormat;
    private TextureView textureView;
    private int rotation = 0;

    // used for calculating the position when dragging the view around.
    //private int initialTouchX;
    //private int initialTouchY;
    //private int initialX;
    //private int initialY;
    // this set of params makes the textureView the full size of the screen, and make it stay
    // "on" even when the screen is locked.
    private WindowManager.LayoutParams cantTouchThisParams;
    // this set of params maks the texture view small, and touchable, so we can drag it around.
    private WindowManager.LayoutParams touchThisParams;
    private volatile FrameHandlerThread frameHandlerThread;

    private SurfaceTexture surface;
    private Handler uiHandler;
    private Handler mCameraWaitHandler;
    private static final int MAX_WAIT = 375;
    private static volatile int waited = 0;
    private int maxDuration = -1;
    private long maxDurationTime = -1;


    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);

        uiHandler = new Handler();
        HandlerThread handlerThread = new HandlerThread(VideoStreamingService.class.getName() + "_CAMERA_WAIT", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        mCameraWaitHandler = new Handler(handlerThread.getLooper());


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
        EventBus.getDefault().post(new Events.CameraRequested());

        // by default - 0 alpha, can't touch this.
        //textureView.setAlpha(0f);
        //windowManager.addView(textureView, cantTouchThisParams);

    }

    private void runOnUiThread(Runnable r) {
        uiHandler.post(r);
    }


    public void onEventMainThread(Events.NetworkStatus status) {
        /*
        if (!status.isUp() && application.getAgentSettings().getSecurity() == 0) {
            stop();
            mediaManager.setCaptureMode(Givens.COLLECT_MODE_VIDEO);
            mediaManager.startVideoRecording();
        }
        */
    }

    public void onEventMainThread(Events.ActivityResumed event) {
        Timber.e("ACTIVITY RESUMED EVENT");
        EventBus.getDefault().removeStickyEvent(Events.ActivityPaused.class);
        makeTouchable();
        EventBus.getDefault().removeStickyEvent(event);
    }

    public void onEventMainThread(Events.NavigationDrawerClosed event) {
        try {
            makeTouchable();
        }
        catch (Exception ex) {
            // Do nothing
        }
    }

    public void onEventMainThread(Events.ActivityPaused event) {
        Timber.e("ACTIVITY PAUSED EVENT RECEIVED");
        makeUntouchable();
        EventBus.getDefault().removeStickyEvent(event);
    }

    public void makeUntouchable() {
        Timber.e("MAKING UNTOUCHABLE");
        if (textureView != null) {
            textureView.setAlpha(0f);
            textureView.setLayoutParams(cantTouchThisParams);
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
        if (textureView != null) {
            textureView.setAlpha(1f);
            textureView.setLayoutParams(touchThisParams);
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
                Timber.e(e,"Not in layout yet?");
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        //Timber.d("onStartCommand called.");
        if (started) {
            Timber.e("Second start command received.");
            stopSelf();
            return START_NOT_STICKY;
        }
        started = true;


        if (frameHandlerThread != null || mStreaming || startVisible != null) {
            Timber.e("Starting already started service?!?!");
            stopSelf();
            return START_NOT_STICKY;
        }

        frameHandlerThread = new FrameHandlerThread();
        //Timber.d("Service started.");
        startVisible = i.getBooleanExtra("visible", powerManager.isScreenOn());

        // the initial state is touchable and visible.
        if (startVisible) {
            textureView.setAlpha(1f);
            windowManager.addView(textureView, touchThisParams);
        } else {
            textureView.setAlpha(0f);
            windowManager.addView(textureView, cantTouchThisParams);
        }

        textureView.setSurfaceTextureListener(this);

        if (i.getExtras() != null) {
            maxDuration = i.getExtras().getInt(Givens.EXTRA_MAX_DURATION, -1);
        }

        return START_NOT_STICKY;
    }

    private synchronized void stopCamera() {
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
                    makeUntouchable();
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
                Timber.w(e, "Couldn't remove textureView.");
            }
        }
    }

    public synchronized void stopStream() {
        // makes sure the stop runs on the same thread as the service,
        // so that the camera is released properly
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (frameHandlerThread != null) {
                    FrameHandlerThread deadThread = frameHandlerThread;
                    frameHandlerThread = null;
                    deadThread.end();
                }
                stopSelf();
            }
        });

    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        stopStream();
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

    private void onCameraOpened() {
        //Timber.d("Surface Texture Available.");
        try {
            int cameraId = mediaManager.getCameraId();


            Camera.getCameraInfo(cameraId, cameraInfo);

            // mute the shutter sound.
            if (cameraInfo.canDisableShutterSound) {
                camera.enableShutterSound(false);
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
                    String setFocus = null;
                    if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        //Timber.d("Setting continuous focus.");
                        setFocus = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }
                    if (setFocus == null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        //Timber.d("Setting continuous picture focus.");
                        setFocus = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    }
                    if (setFocus == null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        //Timber.d("Setting auto focus");
                        setFocus = Camera.Parameters.FOCUS_MODE_AUTO;
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                    if (setFocus == null && defaultMode != null && supportedFocusModes.contains(defaultMode)) {
                        //Timber.d("Setting default ");
                        setFocus = defaultMode;
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
                    //Timber.d("turning off flash.");
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
                    //Timber.d("Enabling anti-banding mode.");
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
                    cameraSettings.append("Auto Exposure Lock: ").append("ON").append("\n");
                }
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set auto exposure lock.", e);
            }

            try {
                // turn on auto white balance lock, if supported
                if (params.isAutoWhiteBalanceLockSupported()) {
                    params.setAutoWhiteBalanceLock(true);
                    camera.setParameters(params);
                    cameraSettings.append("Auto White Balance Lock: ").append("ON").append("\n");
                }
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set auto white balance lock.", e);
            }
            */

            try {
                List<String> whiteBalances = params.getSupportedWhiteBalance();
                if (whiteBalances != null && !whiteBalances.isEmpty()) {
                    if (whiteBalances.contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
                        //Timber.d("Turning on auto white balance.");
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
                    //Timber.d("Turning on video stabilization.");
                    params.setVideoStabilization(true);
                    camera.setParameters(params);
                }
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set video stabilization", e);
            }

            try {
                //Timber.d("Turning on recording hint.");
                params.setRecordingHint(true);
                camera.setParameters(params);
            } catch (Exception e) {
                throw new RuntimeException("Couldn't set recording hint.", e);
            }


            try {
                List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
                if (previewSizes == null || previewSizes.isEmpty()) {
                    previewSizes = params.getSupportedPictureSizes();
                }
                Camera.Size previewSize = ImageUtil.getBestStreamingSize(application.getAgentSettings(), connectivityManager, previewSizes);
                //Timber.d("Setting preview size to: " + previewSize.width + "x" + previewSize.height);
                params.setPreviewSize(previewSize.width, previewSize.height);
                camera.setParameters(params);
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set preview size.", e);
            }

            try {
                List<Camera.Size> pictureSizes = params.getSupportedPictureSizes();
                Camera.Size pictureSize = ImageUtil.getBestStreamingSize(application.getAgentSettings(), connectivityManager, pictureSizes);
                params.setPictureSize(pictureSize.width, pictureSize.height);
                //Timber.d("Setting picture size to: " + pictureSize.width + "x" + pictureSize.height);
                camera.setParameters(params);
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set picture size.", e);
            }

            try {
                //Timber.d("Setting FPS to " + application.getAgentSettings().getVideoCasesFrameRate());
                params.setPreviewFrameRate(application.getAgentSettings().getVideoStreamFrameRate());
                camera.setParameters(params);
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set preview frame rate.", e);
            }

            try {
                // set the camera orientation to match the display orientation.
                switch (orientationManager.getOrientation()) {
                    case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                        //Timber.d("Setting Orientation: 90");
                        params.setRotation(90);
                        rotation = 90;
                        break;
                    case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                        //Timber.d("Setting Orientation: 180");
                        params.setRotation(180);
                        rotation = 180;
                        break;
                    case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                        //Timber.d("Setting Orientation: 0");
                        params.setRotation(0);
                        rotation = 0;
                        break;
                    case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                        //Timber.d("Setting Orientation: 270");
                        params.setRotation(270);
                        rotation = 270;
                }
                camera.setParameters(params);
            } catch (Exception e) {
                throw new CameraParameterException("Couldn't set orientation.", e);
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
            size = params.getPreviewSize();
            previewFormat = params.getPreviewFormat();
            mStreaming = true;
            camera.setPreviewCallback(this);
            camera.setPreviewTexture(surface);
            camera.startPreview();

            if (frameHandlerThread != null) {
                frameHandlerThread.begin();
            } else {
                stopCamera();
            }

            // Set time to stop streaming
            if (maxDuration > 0) {
                maxDurationTime = DateTime.now().getMillis() + maxDuration;
            }

        } catch (Exception e) {
            Timber.e(e, "Camera Streaming Failed.");
            MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Video Streaming Failed."));
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        //Timber.d("Surface Texture Destroyed.");
        stopSelf();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (frameHandlerThread != null)
            frameHandlerThread.process(data);

        // Stop streaming if max duration reached
        if (maxDurationTime > 0 && DateTime.now().getMillis() > maxDurationTime) {
            stopSelf();
        }
    }

    private class FrameHandlerThread extends HandlerThread {
        private final Handler mHandler;
        private final FrameRunnable runnable;
        public volatile boolean running = false;
        public volatile boolean alive = false;
        public volatile boolean started = false;
        private volatile boolean initialized = false;
        private SSLSocket socket;
        private DataOutputStream dataOutputStream;


        public FrameHandlerThread() {
            super(FrameHandlerThread.class.getName(), Givens.THREAD_PRIORITY + android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
            start();
            mHandler = new Handler(getLooper());
            runnable = new FrameRunnable();
            //Timber.d("Frame Handler Thread Created.");
        }

        public synchronized void end() {
            alive = false;
            if (initialized) {
                initialized = false;
                //Timber.d("Shutting Down Streaming Thread.");
                mHandler.removeCallbacks(runnable);

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.flush();
                        dataOutputStream.close();
                    } catch (Exception e) {
                        Timber.w(e, "Error closing output stream.");
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        Timber.w(e, "Error closing socket.");
                    }
                }

                if (started) {
                    MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_LIVE_VIDEO_STOP));
                } else {
                    MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Live Streaming Failed To Start"));
                }

                stopCamera();
                mediaManager.videoStreamingEnded();


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    quitSafely();
                } else {
                    quit();
                }
            }
        }

        public void begin() {
            if (!started && !initialized && !alive && !isInterrupted() && isAlive()) {
                //Timber.d("Initializing Streaming Thread.");
                initialized = true;
                alive = true;
                mHandler.post(runnable);
                //Timber.d("Frame handler thread begun.");
            }
        }

        public synchronized void process(byte[] data) {
            synchronized (queueLock) {
                queue.add(data);
            }
        }

        private class FrameRunnable implements Runnable {

            private synchronized void connectIfNeeded() throws Exception {
                if (socket == null) {

                    StringBuilder headerBuffer = new StringBuilder(192);
                    headerBuffer.append("uid=").append(application.getScannedSettings().getOrganizationId()).append(application.getDeviceIdentifier());
                    Location lastLocation = fixManager.getLastLocation();
                    if (lastLocation != null) {
                        headerBuffer.append("|latitude=").append(String.valueOf(lastLocation.getLatitude()));
                        headerBuffer.append("|longitude=").append(String.valueOf(lastLocation.getLongitude()));
                        headerBuffer.append("|provider=").append(lastLocation.getProvider());
                        headerBuffer.append("|accuracy=").append(String.valueOf(lastLocation.getAccuracy()));
                        headerBuffer.append("|speed=").append(String.valueOf(lastLocation.getSpeed()));
                        headerBuffer.append("|bearing=").append(lastLocation.getBearing());
                        SimpleDateFormat f = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
                        f.setTimeZone(TimeZone.getTimeZone("UTC"));
                        headerBuffer.append("|dateUTC=").append(f.format(new Date(lastLocation.getTime())));
                    }
                    Integer batteryLevel = BatteryUtil.getBatteryLevel(application);
                    if (batteryLevel != null) {
                        headerBuffer.append("|battery=").append(String.valueOf(batteryLevel));
                    }
                    if (application.getAgentSettings() != null) {
                        if (application.getAgentSettings().getCaseNumber() != null && !application.getAgentSettings().getCaseNumber().trim().isEmpty()) {
                            headerBuffer.append("|caseNumber=").append(application.getAgentSettings().getCaseNumber());
                        }
                        if (application.getAgentSettings().getCaseDescription() != null && !application.getAgentSettings().getCaseDescription().trim().isEmpty()) {
                            headerBuffer.append("|msg=").append(application.getAgentSettings().getCaseDescription());
                        }
                    }

                    String headerString = headerBuffer.toString();
                    byte[] header = crypto.encrypt(headerString);
                    ByteBuffer sizeInt = ByteBuffer.allocate(4);
                    sizeInt.clear();

                    //Timber.d("No socket, connection needed.");
                    socket = (SSLSocket) socketFactory.createSocket();
                    //Timber.d("Connecting Video Streaming Socket");
                    String ipAddress = application.getScannedSettings().getIpAddress();
                    if (ipAddress.contains("://")) {
                        ipAddress = ipAddress.split("://")[1];
                    }

                    socket.connect(new InetSocketAddress(ipAddress, application.getScannedSettings().getVideoPort().intValue()), SOCKET_TIMEOUT);
                    socket.setSoTimeout(SOCKET_TIMEOUT);
                    socket.setTcpNoDelay(true);
                    socket.setKeepAlive(true);

                    //Timber.d("Raw Header: " + headerString);
                    //Timber.d("Encrypted Header Size: " + header.length);
                    sizeInt.putInt(header.length);
                    socket.getOutputStream().write(sizeInt.array());
                    socket.getOutputStream().write(header);
                    socket.getOutputStream().flush();
                    sizeInt.clear();
                    //Timber.d("Header sent");


                    //Timber.v("Getting dataOutputStream");
                    if (dataOutputStream == null) {
                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    }

                    if (socket != null && !socket.isClosed() && socket.isConnected() && dataOutputStream != null) {
                        //Timber.d("Socket available, sending start messages.");
                        MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_LIVE_VIDEO_START));
                        mediaManager.videoStreamingStarted();
                        started = true;
                    }
                }

                checkConnection();
            }

            private void checkConnection() throws Exception {
                //Timber.v("Checking connection.");
                if (!alive || (socket == null || socket.isClosed() || !socket.isConnected()) || dataOutputStream == null) {
                    if (alive) {
                        if (textureView.getAlpha() == 1) {
                            Toast.makeText(application, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                        }
                    }
                    throw new Exception("Socket output is not available.");
                }
            }

            @Override
            public void run() {

                byte[] raw;

                if (alive) {
                    synchronized (queueLock) {
                        //Timber.v("Video Streaming: Alive, checking queue.");
                        if (queue.isEmpty()) {
                            //Timber.v("Video Streaming: Queue is empty.");
                            mHandler.post(this);
                            return;
                        }

                        raw = queue.poll();
                    }

                    if (raw == null) {
                        //Timber.v("Video Streaming: No data in queue. Resetting.");
                        mHandler.post(this);
                        return;
                    }
                } else {
                    Timber.d("Video Streaming: Thread Not Alive. Exiting.");
                    return;
                }


                try {

                    connectIfNeeded();

                    YuvImage image = new YuvImage(raw, previewFormat, size.width, size.height, null);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();

                    if (rotation != 0)
                        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
                    else
                        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), application.getAgentSettings().getVideoStreamQuality(), out);

                    byte[] jpgData = out.toByteArray();

                    if (rotation != 0) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotation);
                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(jpgData, 0, jpgData.length);
                        Bitmap bitmap = Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.getWidth(), decodedBitmap.getHeight(), matrix, true);
                        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, application.getAgentSettings().getVideoStreamQuality(), out2);
                        jpgData = out2.toByteArray();
                    }

                    if (crypto.isEncrypting()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        OutputStream cryptoStream = crypto.getOutputStream(baos);
                        IOUtils.copy(new ByteArrayInputStream(jpgData), cryptoStream);
                        cryptoStream.flush();
                        jpgData = baos.toByteArray();
                    }

                    if (alive) {
                        checkConnection();
                        //Timber.d("Sending " + jpgData.length + " bytes");
                        dataOutputStream.writeInt(jpgData.length);
                        dataOutputStream.write(jpgData, 0, jpgData.length);
                        dataOutputStream.flush();
                    }
                } catch (Exception e) {
                    if (alive) {
                        alive = false;
                        stopSelf();
                        if (e instanceof SSLHandshakeException) {
                            EventBus.getDefault().post(new Events.SSLHandshakeError(getString(R.string.ssl_error)));
                        } else {
                            MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Video Streaming Stopped Unexpectedly."));
                            if (!Network.isNetworkException(e)) {
                                Timber.e(e, "Error streaming video.");
                            }
                        }
                        return;
                    } else {
                        Timber.w(e, "Caught error on thread shutdown.");
                    }
                }

                if (alive) {
                    mHandler.post(this);
                }
            }
        }
    }


}
