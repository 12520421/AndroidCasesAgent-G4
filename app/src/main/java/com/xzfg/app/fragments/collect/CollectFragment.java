package com.xzfg.app.fragments.collect;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.content.PermissionChecker;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.AgentActivity;
import com.xzfg.app.exceptions.CameraParameterException;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.FreeSpaceManager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.managers.OrientationManager;
import com.xzfg.app.managers.VolumeManager;
import com.xzfg.app.model.AgentRoleComponent;
import com.xzfg.app.model.AgentRoles;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.PreviewSize;
import com.xzfg.app.model.Submission;
import com.xzfg.app.model.UploadPackage;
import com.xzfg.app.util.ImageUtil;

import java.io.IOException;
import java.security.Policy;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

import static android.content.Context.WINDOW_SERVICE;

/*
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
*/

/**
 */
@SuppressWarnings("deprecation")
public class CollectFragment extends Fragment implements AgentRoleComponent, TextureView.SurfaceTextureListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener,SurfaceHolder.Callback {

    private final Object previewLock = new Object();
    @Inject
    Application application;
    @Inject
    MediaManager mediaManager;
    @Inject
    ConnectivityManager connectivityManager;
    @Inject
    FreeSpaceManager freeSpaceManager;
    @Inject
    OrientationManager orientationManager;
    @Inject
    VolumeManager volumeManager;
    @Inject
    FixManager fixManager;
    // the current camera instance.
    Camera camera;
    // This textureview holds our preview.
    TextureView preview;
    // countdown timer.
    Chronometer chronometer;
    // capture and collect buttons.
    ToggleButton captureButton;
    ToggleButton collectionTypeButton;
    // this is our media chooser drawer.
    View mediaChooser;
    Button existingButton;
    ToggleButton audioToggle;
    ToggleButton videoToggle;
    ToggleButton photoToggle;
    ToggleButton liveVideoToggle;
    ToggleButton liveAudioToggle;
    ImageButton cameraSwitch;
    TextView caseNumberView;
    ImageView lastCapture;
    boolean previewInitialized = false;
    private boolean safeToTakePicture = false;
    private boolean alreadyMeasured = false;
    private Integer previewWidth = null;
    private Integer previewHeight = null;
    private Handler mCameraWaitHandler;
    private SurfaceTexture surface;
    private int width;
    private int height;

    private AlertDialogFragment alertDialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getActivity().getApplication()).inject(this);
        createWaitHandler();
    }

    private synchronized void createWaitHandler() {
        if (mCameraWaitHandler == null) {
            HandlerThread handlerThread = new HandlerThread(CollectFragment.class.getName() + "_CAMERA_WAIT", android.os.Process.THREAD_PRIORITY_BACKGROUND);
            handlerThread.start();
            mCameraWaitHandler = new Handler(handlerThread.getLooper());
        }
    }

    public void setDisplayOrientation(Camera camera) {
        WindowManager windowManager =  (WindowManager)getContext().getSystemService(WINDOW_SERVICE);

        /*
        Configuration config = getResources().getConfiguration();
        switch (config.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                Timber.e("Portrait");
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                Timber.e("Landscape");
                break;
            case Configuration.ORIENTATION_UNDEFINED:
                Timber.e("Undefined");
                break;
        }
        */

        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mediaManager.getCameraId(), cameraInfo);
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int displayprotation=0;
        if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            displayprotation = (cameraInfo.orientation + degrees) % 360;
            displayprotation = (360 - displayprotation) % 360;  // compensate the mirror
        }
        if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
            displayprotation = (cameraInfo.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(displayprotation);
      /*  Camera.Parameters parameters = camera.getParameters();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mediaManager.getCameraId(), cameraInfo);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            parameters.set("orientation", "portrait");
            // default camera orientation on android is landscape
            // So we need to rotate the preview
            if(cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                parameters.setRotation(90);
                camera.setDisplayOrientation(90);
            }
           if(cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT){
               parameters.setRotation(270);
               camera.setDisplayOrientation(90);
            }

        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            parameters.set("orientation", "landscape");
            parameters.setRotation(0);
            camera.setDisplayOrientation(0);
        }
        camera.setParameters(parameters);*/

    }

    public void measureFrame() {
        alreadyMeasured = true;
        //application.cameraWidth=imageView.getMeasuredWidth();
        //application.cameraHeight=imageView.getMeasuredHeight();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_collect, viewGroup, false);
        preview = (TextureView) v.findViewById(R.id.camera_preview);
        previewInitialized = false;

        // chronometer.
        chronometer = (Chronometer) v.findViewById(R.id.clock);
        chronometer.setText("--:--");

        // collection type button.
        collectionTypeButton = (ToggleButton) v.findViewById(R.id.collection_type_button);
        collectionTypeButton.setOnCheckedChangeListener(this);

        // capture button.
        captureButton = (ToggleButton) v.findViewById(R.id.capture_button);
        captureButton.setOnCheckedChangeListener(this);

        // get a reference to the mediaChooser so we can toggle it later.
        mediaChooser = v.findViewById(R.id.media_chooser);

        audioToggle = (ToggleButton) v.findViewById(R.id.audio_toggle);
        audioToggle.setOnCheckedChangeListener(this);

        videoToggle = (ToggleButton) v.findViewById(R.id.video_toggle);
        videoToggle.setOnCheckedChangeListener(this);

        photoToggle = (ToggleButton) v.findViewById(R.id.photo_toggle);
        photoToggle.setOnCheckedChangeListener(this);

        liveVideoToggle = (ToggleButton) v.findViewById(R.id.video_live_toggle);
        liveVideoToggle.setOnCheckedChangeListener(this);

        liveAudioToggle = (ToggleButton) v.findViewById(R.id.audio_live_toggle);
        liveAudioToggle.setOnCheckedChangeListener(this);

        cameraSwitch = (ImageButton) v.findViewById(R.id.cameraswitch);
        cameraSwitch.setOnClickListener(this);

        // User could've revoked camera permission for our app in Android 6.0
        if (!isCameraPermissionGranted()) {
            photoToggle.setEnabled(false);
            videoToggle.setEnabled(false);
            liveVideoToggle.setEnabled(false);
            cameraSwitch.setEnabled(false);
            mediaManager.setCaptureMode(Givens.COLLECT_MODE_AUDIO);
        }
        existingButton = (Button) v.findViewById(R.id.existing_button);
        existingButton.setOnClickListener(this);

        lastCapture = (ImageView) v.findViewById(R.id.preview);
        if (mediaManager.getLastCapturedImage() != null) {
            lastCapture.setImageBitmap(mediaManager.getLastCapturedImage());
        } else {
            lastCapture.setImageDrawable(getResources().getDrawable(R.drawable.imagepreviewsquare));
        }

        caseNumberView = (TextView) v.findViewById(R.id.case_number);
        String caseNumber = application.getAgentSettings().getCaseNumber();
        if (caseNumber != null && !caseNumber.isEmpty()) {
            caseNumberView.setText(caseNumber);
            caseNumberView.setVisibility(View.VISIBLE);
        } else {
            caseNumberView.setVisibility(View.GONE);
        }

        preview.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                if (alreadyMeasured)
                    preview.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                else
                    measureFrame();
            }
        });

        updateRoles(application.getAgentSettings().getAgentRoles());
        return v;
    }

    private boolean isCameraPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PermissionChecker.checkSelfPermission(application, Manifest.permission.CAMERA) == PermissionChecker.PERMISSION_GRANTED;
        }
        return true;
    }

    private void checkAccess() {
        if (application.getAgentSettings().getSecurity() == 0) {
            audioToggle.setOnCheckedChangeListener(this);
            videoToggle.setOnCheckedChangeListener(this);
        } else {
            if (mediaManager.getCaptureMode() == Givens.COLLECT_MODE_AUDIO || mediaManager.getCaptureMode() == Givens.COLLECT_MODE_VIDEO) {
                if (application.getAgentSettings().getAgentRoles().photocollect()) {
                    setCaptureMode(Givens.COLLECT_MODE_PICTURE);
                } else {
                    setCaptureMode(Givens.COLLECT_MODE_UNKNOWN);
                }
            }
            audioToggle.setOnCheckedChangeListener(null);
            audioToggle.setEnabled(false);
            videoToggle.setOnCheckedChangeListener(null);
            videoToggle.setEnabled(false);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onResume() {
        super.onResume();
        setCaptureMode(mediaManager.getCaptureMode());
        EventBus.getDefault().registerSticky(this);
        createWaitHandler();
        if (mediaManager.isRecording()) {
            chronometer.setBase(mediaManager.getBaseTime());
            chronometer.start();
            captureButton.setOnCheckedChangeListener(null);
            captureButton.setChecked(true);
            captureButton.setOnCheckedChangeListener(this);
            collectionTypeButton.setEnabled(false);
            collectionTypeButton.setOnCheckedChangeListener(null);
            cameraSwitch.setVisibility(View.GONE);
            cameraSwitch.setEnabled(false);
        }

        PreviewSize savedSize = application.getPreviewSize(0);
        if (preview != null & savedSize != null) {
            FrameLayout.LayoutParams previewParams = new FrameLayout.LayoutParams(savedSize.getWidth(), savedSize.getHeight(), Gravity.CENTER);
            preview.setLayoutParams(previewParams);
        }
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        try {
            if (alertDialogFragment != null) {
                alertDialogFragment.dismissAllowingStateLoss();
                alertDialogFragment = null;
            }
        } catch (NullPointerException npe) {
            Timber.w("A dialog dismissal resulting in an NPE. weird.");
        }
        //
        try {
            mCameraWaitHandler.getLooper().quit();
            stopPreview();
        } catch (Exception ex) {
            Timber.e(ex, "Something bad happened.");
        }

        super.onPause();
    }

    public void onEventMainThread(Events.ThumbnailEvent event) {
        if (mediaManager.getLastCapturedImage() != null) {
            lastCapture.setImageBitmap(mediaManager.getLastCapturedImage());
        } else {
            lastCapture.setImageDrawable(getResources().getDrawable(R.drawable.imagepreviewsquare));
        }
    }

    public void onEventMainThread(Events.AudioRecording event) {
        if (mediaManager.getCaptureMode() == Givens.COLLECT_MODE_AUDIO || mediaManager.getCaptureMode() == Givens.COLLECT_MODE_AUDIO_LIVE) {
            if (event.isRecording()) {
                chronometer.setBase(mediaManager.getBaseTime());
                chronometer.start();
                captureButton.setOnCheckedChangeListener(null);
                captureButton.setChecked(true);
                captureButton.setOnCheckedChangeListener(this);
                captureButton.setEnabled(true);
                collectionTypeButton.setEnabled(false);
                collectionTypeButton.setOnCheckedChangeListener(null);
            } else {
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.stop();
                captureButton.setOnCheckedChangeListener(null);
                captureButton.setChecked(false);
                captureButton.setOnCheckedChangeListener(this);
                captureButton.setEnabled(true);
                collectionTypeButton.setEnabled(true);
                collectionTypeButton.setOnCheckedChangeListener(this);
                if (mediaManager.previewModes.contains(mediaManager.getCaptureMode())) {
                    //Timber.d("Starting Preview from AudioRecording Event.");
                    startPreview();
                }
            }
        }
    }

    public void onEventMainThread(Events.VideoRecording event) {
        if (mediaManager.getCaptureMode() == Givens.COLLECT_MODE_VIDEO || mediaManager.getCaptureMode() == Givens.COLLECT_MODE_VIDEO_LIVE) {
            if (event.isRecording()) {
                chronometer.setBase(mediaManager.getBaseTime());
                chronometer.start();
                captureButton.setOnCheckedChangeListener(null);
                captureButton.setChecked(true);
                captureButton.setOnCheckedChangeListener(this);
                captureButton.setEnabled(true);
                collectionTypeButton.setEnabled(false);
                collectionTypeButton.setOnCheckedChangeListener(null);
                cameraSwitch.setVisibility(View.GONE);
                cameraSwitch.setEnabled(false);
                if (((AgentActivity) getActivity()).getSelected() == 1) {
                    EventBus.getDefault().postSticky(new Events.ActivityResumed());
                }
            } else {
                //Timber.d("Recording stopped, can re-open preview.");
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.stop();
                captureButton.setOnCheckedChangeListener(null);
                captureButton.setChecked(false);
                captureButton.setOnCheckedChangeListener(this);
                captureButton.setEnabled(true);
                collectionTypeButton.setEnabled(true);
                collectionTypeButton.setOnCheckedChangeListener(this);
                if (mediaManager.previewModes.contains(mediaManager.getCaptureMode())) {
                    if (Camera.getNumberOfCameras() > 1) {
                        cameraSwitch.setVisibility(View.VISIBLE);
                        cameraSwitch.setEnabled(true);
                    }
                    //Timber.d("Starting preview from video recording event.");
                    startPreview();
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.CameraChanged cameraChanged) {
        if (!mediaManager.isRecording() && mediaManager.previewModes.contains(mediaManager.getCaptureMode())) {
            stopPreview();
            startPreview();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.CameraRequested cameraRequested) {
        if (!mediaManager.isRecording() && mediaManager.previewModes.contains(mediaManager.getCaptureMode())) {
            //Timber.d("stopping preview, camera requested.");
            stopPreview();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.SSLHandshakeError sslError) {
        if (alertDialogFragment != null) {
            alertDialogFragment.dismissAllowingStateLoss();
            alertDialogFragment = null;
        }
        alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.warning), sslError.getMessage());
        alertDialogFragment.show(getActivity().getSupportFragmentManager(), "error-dialog");
    }

    private void openCamera() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Camera openedCamera = Camera.open(mediaManager.getCameraId());
                    camera = openedCamera;
                    if (camera != null) {
                        onCameraOpened();
                    }
                } catch (Exception e) {
                    Timber.w(e, "Couldn't open camera, will try again in 16ms.");
                    mCameraWaitHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            openCamera();
                        }
                    }, 16);
                }
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.surface = surface;
        this.width = width;
        this.height = height;
        openCamera();
    }


    private synchronized void onCameraOpened() {
        synchronized (previewLock) {
            Timber.w("Camera opened.");
            setDisplayOrientation(camera);

            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mediaManager.getCameraId(), cameraInfo);

            if (cameraInfo.canDisableShutterSound) {
                camera.enableShutterSound(false);
            }



            Camera.Parameters params = camera.getParameters();

            try {
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
                            //Timber.d("Setting continuous focus.");
                            setFocus = true;
                            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        }
                        if (!setFocus && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            setFocus = true;
                            //Timber.d("Setting continuous focus picture.");
                            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        }
                        if (!setFocus && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                            setFocus = true;
                            //Timber.d("Setting auto-focus");
                            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        }
                        if (!setFocus && defaultMode != null && supportedFocusModes.contains(defaultMode)) {
                            //Timber.d("Setting default focus: " + defaultMode);
                            setFocus = true;
                            params.setFocusMode(defaultMode);
                        }
                        if (setFocus) {
                            camera.setParameters(params);
                        }
                    }
                } catch (Exception e) {
                    throw new CameraParameterException("Couldn't set camera focus.", e);
                }

                // turn off the flash, if FLASH_MODE_OFF is supported.
                try {
                    List<String> supportedFlashModes = params.getSupportedFlashModes();
                    if (supportedFlashModes != null && !supportedFlashModes.isEmpty() && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                        //Timber.d("Turning off the flash.");
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
                        //Timber.d("Turning on anti-banding.");
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
                        params.setVideoStabilization(true);
                        camera.setParameters(params);
                        //Timber.d("Turning on video stabilization.");
                    }
                } catch (Exception e) {
                    throw new CameraParameterException("Couldn't set video stabilization", e);
                }


            } catch (Exception e) {
                Timber.e(e, "Error setting camera parameters.");
            }

            Camera.Size previewSize = null;

            List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
            if (previewSizes == null || previewSizes.isEmpty()) {
                //Timber.d("No supported preview sizes, falling back on supported video sizes.");
                previewSizes = params.getSupportedVideoSizes();
            }
            if (previewSizes == null || previewSizes.isEmpty()) {
                //Timber.d("No supported preview or video sizes, falling back on supported picture sizes.");
                previewSizes = params.getSupportedPictureSizes();
            }


            previewSize = previewSizes.get(0);
            for (Camera.Size size : previewSizes) {
                if (size.width > previewSize.width && size.height > previewSize.height) {
                    previewSize = size;
                }
            }
            try {
                //Timber.d("Setting preview size: " + previewSize.width + "x" + previewSize.height);
                params.setPreviewSize(previewSize.width, previewSize.height);
                camera.setParameters(params);
            } catch (Exception e) {
                Timber.e("Error setting preview size: " + previewSize.width + "x" + previewSize.height);
            }


            if (!application.getAspectMeasured()) {
                adjustAspect(previewSize.width, previewSize.height);
            }


            //FrameLayout.LayoutParams previewParams = new FrameLayout.LayoutParams(previewSize.height, previewSize.width, Gravity.CENTER);
            //preview.setLayoutParams(previewParams);

            try {
                camera.setPreviewTexture(surface);
                camera.startPreview();
                previewInitialized = true;
                safeToTakePicture = true;
            } catch (IOException ioe) {
                safeToTakePicture = false;
                previewInitialized = false;
                Timber.e(ioe, "Something bad happened.");
            } catch (RuntimeException rte) {
                safeToTakePicture = false;
                previewInitialized = false;
                Timber.e(rte, "Something bad happened.");
            }
        }

    }


    public synchronized void adjustAspect(int videoWidth, int videoHeight) {
        if (previewWidth == null) {
            previewWidth = preview.getWidth();
        }
        if (previewHeight == null) {
            previewHeight = preview.getHeight();
        }

        int viewWidth = previewWidth;
        int viewHeight = previewHeight;

        // find the next closest divisible by 16 size.
        viewWidth = ((int) (Math.floor(viewWidth / 16d)) * 16);
        viewHeight = ((int) (Math.floor(viewHeight / 16d)) * 16);

        double aspectRatio = (double) videoWidth / videoHeight;
        //Timber.d("Aspect Ratio: " + aspectRatio);

        int newWidth, newHeight;

        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            //Timber.d("limited by narrow width; restrict height");
            newHeight = (int) (viewWidth * aspectRatio);
            newWidth = (int) (newHeight / aspectRatio);
        } else {
            //Timber.d("limited by short height; restrict width");
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = (int) (newWidth * aspectRatio);
        }

        //Timber.d("video width: " + videoWidth);
        //Timber.d("video height: " + videoHeight);
        //Timber.d("view width: " + viewWidth);
        //Timber.d("view height: " + viewHeight);
        //Timber.d("new width: " + newWidth);
        //Timber.d("new height: " + newHeight);

        int[] location = new int[2];
        int offset = 0;
        if (previewHeight > newHeight + 2) {
            offset = ((previewHeight - newHeight) / 2);
        }

        preview.getLocationInWindow(location);
        //Timber.d("INITIAL X: " + location[0]);
        //Timber.d("INITIAL Y:" + location[1]);
        //Timber.d("OFFSET: " + offset);
        //Timber.d("CALCULATED Y: " + (location[1] + offset));
        int y = location[1] + offset;

        FrameLayout.LayoutParams previewParams = new FrameLayout.LayoutParams(newWidth, newHeight, Gravity.CENTER);
        preview.setLayoutParams(previewParams);

        //preview.forceLayout();

        preview.getLocationInWindow(location);

        // different cameras on the device may have different sizes/aspect ratios.
        application.setPreviewSize(mediaManager.getCameraId(), newWidth, newHeight, y);
        application.setAspectMeasured(true);

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stopPreview();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void onClick(View v) {
        //Timber.d("Button Clicked");
        int id = v.getId();
        switch (id) {
            case R.id.existing_button:
                application.setFromResult(true);
                if (Build.VERSION.SDK_INT < 19) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*,video/*");
                    getActivity().startActivityForResult(Intent.createChooser(intent, getString(R.string.select_media)), Givens.GALLERY_INTENT);
                } else {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    String[] mimetypes = {"image/*", "video/*"};
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                    intent.setType("image/*,video/*");
                    getActivity().startActivityForResult(Intent.createChooser(intent, getString(R.string.select_media)), Givens.GALLERY_INTENT);
                }
                break;
            case R.id.collection_type_button:
                chronometer.stop();
                chronometer.setBase(SystemClock.elapsedRealtime());
                break;
            case R.id.cameraswitch:
                if (Camera.getNumberOfCameras() > 0 && mediaManager.getCameraId() == 0) {
                    mediaManager.setCameraId(1);
                } else {
                    mediaManager.setCameraId(0);
                }
                stopPreview();
                startPreview();
                break;
        }
    }


    protected void start() {
        AgentSettings settings = application.getAgentSettings();
        AgentRoles roles = settings.getAgentRoles();

        if (mediaManager.getCaptureMode() == Givens.COLLECT_MODE_UNKNOWN) {
            return;
        }

        if (mediaManager.isRecording()) {
            //Timber.d("Can't start, already recording.");
            return;
        }

        // if we're not in a live capture mode, check for free space. If it's not sufficient, throw a dialog, and don't continue.
        if (freeSpaceManager.getFreeMb() <= Givens.FREE_SPACE_LIMIT && mediaManager.getCaptureMode() != Givens.COLLECT_MODE_AUDIO_LIVE && mediaManager.getCaptureMode() != Givens.COLLECT_MODE_VIDEO_LIVE) {
            if (alertDialogFragment != null) {
                alertDialogFragment.dismissAllowingStateLoss();
                alertDialogFragment = null;
            }
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.warning), getString(R.string.insufficient_space));
            alertDialogFragment.show(getActivity().getSupportFragmentManager(), "error-dialog");
            captureButton.setOnCheckedChangeListener(null);
            captureButton.setChecked(false);
            captureButton.setOnCheckedChangeListener(this);
            captureButton.setEnabled(true);
            return;
        }

        // if we're not connected, and the capture mode is a live capture mode, display a not connected dialog.
        if (!isConnected() && (mediaManager.getCaptureMode() == Givens.COLLECT_MODE_AUDIO_LIVE || mediaManager.getCaptureMode() == Givens.COLLECT_MODE_VIDEO_LIVE)) {
            if (alertDialogFragment != null) {
                alertDialogFragment.dismissAllowingStateLoss();
                alertDialogFragment = null;
            }
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.warning), getString(R.string.network_error));
            alertDialogFragment.show(getActivity().getSupportFragmentManager(), "error-dialog");
            captureButton.setOnCheckedChangeListener(null);
            captureButton.setChecked(false);
            captureButton.setOnCheckedChangeListener(this);
            captureButton.setEnabled(true);
            return;
        }

        collectionTypeButton.setChecked(false);
        collectionTypeButton.setEnabled(false);
        collectionTypeButton.setOnCheckedChangeListener(null);

        switch (mediaManager.getCaptureMode()) {

            case Givens.COLLECT_MODE_AUDIO: {
                if (settings.getSecurity() == 0 && roles.audiorecord()) {
                    mediaManager.startAudioRecording(-1);
                }
                break;
            }
            case Givens.COLLECT_MODE_AUDIO_LIVE: {
                if (roles.audiostream()) {
                    mediaManager.startAudioStreaming(-1);
                }
                break;
            }
            case Givens.COLLECT_MODE_VIDEO_LIVE: {
                if (roles.videostream()) {
                    stopPreview();
                    mediaManager.startStreamingVideo();
                }
                break;
            }
            case Givens.COLLECT_MODE_VIDEO: {
                if (settings.getSecurity() == 0 && roles.videorecord()) {
                    stopPreview();
                    mediaManager.startVideoRecording();
                }
                break;
            }
            case Givens.COLLECT_MODE_PICTURE: {
                if (roles.photocollect()) {
                    startPreview();
                    if (safeToTakePicture) {

                        setDisplayOrientation(camera);

                        Camera.Parameters params = camera.getParameters();

                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        Camera.getCameraInfo(mediaManager.getCameraId(), cameraInfo);


                        int rotation = -1;
                        // set the camera orientation to match the display orientation.
                        switch (orientationManager.getOrientation()) {
                            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                                rotation = 90;

                                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                                    // most devices, 270
                                    // reversed sensor devices, 90
                                    // camera.setDisplayOrientation(360-(displayRotation+90));
                                    //camera.setDisplayOrientation(270);
                                    rotation = 270;
                                }
                               // rotation = 270;
                                break;
                            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                                rotation = 270;
                                //Timber.d("REVERSE PORTRAIT PICTURE");
                                break;
                            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                                //Timber.d("LANDSCAPE PICTURE");
                                rotation = 0;
                                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                                    // most devices, 270
                                    // reversed sensor devices, 90
                                    // camera.setDisplayOrientation(360-(displayRotation+90));
                                    //camera.setDisplayOrientation(270);
                                    rotation = 90;
                                }
                                break;
                            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                                //Timber.d("REVERSE LANDSCAPE PICTURE");
                                rotation = 180;
                        }

                        if (rotation != -1) {
                            params.setRotation(rotation);

                        }


                        params.setJpegQuality(application.getAgentSettings().getPhotoQuality());


                        Camera.Size bestSize = ImageUtil.getBestPhotoSize(application.getAgentSettings(), connectivityManager, camera.getParameters().getSupportedPictureSizes());
                        //Timber.d("Best Size: " + bestSize.width + "x" + bestSize.height);

                        params.setPictureSize(bestSize.width, bestSize.height);

                        camera.setParameters(params);

                        //volumeManager.mute();
                        camera.takePicture(null, null, new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                /*
                                try {
                                    Metadata metaData = JpegMetadataReader.readMetadata(new BufferedInputStream(new ByteArrayInputStream(data)));
                                    ExifIFD0Directory exifDirectory = metaData.getFirstDirectoryOfType(ExifIFD0Directory.class);
                                    int exifOrientationTag = exifDirectory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                                    Timber.e("Exif Orientation Tag: " + exifOrientationTag);
                                } catch (Exception e) {
                                    Timber.e(e, "Couldn't Read JpegMetaData.");
                                }
                                */
                                UploadPackage uploadPackage = new UploadPackage();
                                uploadPackage.setType(Givens.UPLOAD_TYPE_IMG);
                                uploadPackage.setFormat(Givens.UPLOAD_FORMAT_JPEG);
                                uploadPackage.setDate(new Date());
                                uploadPackage.setOrientation(orientationManager.getOrientation());
                                //uploadPackage.setOrientation(270);
                                uploadPackage.setOrientationMatrix(orientationManager.getOrientationMatrix());
                                uploadPackage.setCaseNumber(application.getAgentSettings().getCaseNumber());
                                uploadPackage.setCaseDescription(application.getAgentSettings().getCaseDescription());
                                Location location = fixManager.getLastLocation();
                                if (location != null) {
                                    uploadPackage.setLatitude(location.getLatitude());
                                    uploadPackage.setLongitude(location.getLongitude());
                                    if (location.getAccuracy() != 0) {
                                        uploadPackage.setAccuracy(location.getAccuracy());
                                    }
                                }
                                //Timber.d("submitting upload package: " + uploadPackage);
                                Submission submission = new Submission(uploadPackage, data);
                                mediaManager.submitUpload(submission);
                                camera.startPreview();
                                collectionTypeButton.setEnabled(true);
                                collectionTypeButton.setOnCheckedChangeListener(CollectFragment.this);
                                captureButton.setChecked(false);
                                captureButton.setEnabled(true);
                                captureButton.setOnCheckedChangeListener(CollectFragment.this);
                                volumeManager.restore();
                                safeToTakePicture = true;

                            }
                        });
                        safeToTakePicture = false;

                    }
                }
            }

        }

    }

    protected void stop() {
        switch (mediaManager.getCaptureMode()) {
            case Givens.COLLECT_MODE_AUDIO:
                mediaManager.stopAudioRecording();
                break;
            case Givens.COLLECT_MODE_VIDEO:
                mediaManager.stopVideoRecording();
                break;
            case Givens.COLLECT_MODE_AUDIO_LIVE:
                mediaManager.stopAudioStreaming();
                break;
            case Givens.COLLECT_MODE_VIDEO_LIVE:
                mediaManager.stopStreamingVideo();
                break;
        }

    }


    private void slideUp() {
        if (mediaChooser.getVisibility() == View.GONE) {
            Animation slide = AnimationUtils.loadAnimation(getActivity(), R.anim.up);
            mediaChooser.setVisibility(View.VISIBLE);
            mediaChooser.startAnimation(slide);
        }
    }

    private void slideDown() {
        if (mediaChooser.getVisibility() == View.VISIBLE) {
            Animation slide = AnimationUtils.loadAnimation(getActivity(), R.anim.down);
            slide.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mediaChooser.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mediaChooser.startAnimation(slide);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        switch (id) {
            case R.id.capture_button:
                // disable the capture button.
                captureButton.setEnabled(false);
                captureButton.setOnCheckedChangeListener(null);

                if (isChecked) {
                    if (freeSpaceManager.getFreeMb() <= Givens.FREE_SPACE_LIMIT) {
                        if (alertDialogFragment != null) {
                            alertDialogFragment.dismissAllowingStateLoss();
                            alertDialogFragment = null;
                        }
                        alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.warning), getString(R.string.insufficient_space));
                        alertDialogFragment.show(getActivity().getSupportFragmentManager(), "error-dialog");
                        // re-enable the capture button
                        captureButton.setOnCheckedChangeListener(null);
                        captureButton.setChecked(false);
                        captureButton.setOnCheckedChangeListener(this);
                        captureButton.setEnabled(true);
                    } else {
                        //Timber.d("Launching capture");
                        start();

                    }
                } else {
                    //Timber.d("Stopping capture");
                    stop();
                }
                break;
            case R.id.collection_type_button:
                if (isChecked) {
                    checkAccess();
                    slideUp();
                } else {
                    slideDown();
                }
                break;
            case R.id.video_toggle:
                if (isChecked) {
                    setCaptureMode(Givens.COLLECT_MODE_VIDEO);
                    slideDown();
                }
                break;
            case R.id.photo_toggle:
                if (isChecked) {
                    setCaptureMode(Givens.COLLECT_MODE_PICTURE);
                    slideDown();
                }
                break;
            case R.id.audio_toggle:
                if (isChecked) {
                    setCaptureMode(Givens.COLLECT_MODE_AUDIO);
                    slideDown();
                }
                break;
            case R.id.video_live_toggle:
                if (isChecked) {
                    setCaptureMode(Givens.COLLECT_MODE_VIDEO_LIVE);
                    slideDown();
                }
                break;
            case R.id.audio_live_toggle:
                if (isChecked) {
                    setCaptureMode(Givens.COLLECT_MODE_AUDIO_LIVE);
                    slideDown();
                }
                break;
        }
    }


    public void setCaptureMode(int mode) {
        AgentSettings settings = application.getAgentSettings();
        AgentRoles roles = settings.getAgentRoles();
        // Show/hide chronometer
        //chronometer.setVisibility(mode == Givens.COLLECT_MODE_PICTURE ? View.GONE : View.VISIBLE);

        if (mediaManager.isRecording()) {
            if (mediaManager.getCaptureMode() != mode) {
                Toast.makeText(getActivity(), getString(R.string.not_while_recording), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        switch (mode) {
            // this is what happens if you turn off the currently selected capture mode.
            case Givens.COLLECT_MODE_UNKNOWN:
                audioToggle.setChecked(false);
                photoToggle.setChecked(false);
                liveVideoToggle.setChecked(false);
                liveAudioToggle.setChecked(false);
                videoToggle.setChecked(false);
                collectionTypeButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.collectmodeunknown));
                chronometer.setVisibility(View.GONE);
                break;
            case Givens.COLLECT_MODE_VIDEO:
                if (roles.videorecord()) {
                    collectionTypeButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.collectmodevideo));
                    if (!videoToggle.isChecked()) {
                        videoToggle.setOnCheckedChangeListener(null);
                        videoToggle.setChecked(true);
                        videoToggle.setOnCheckedChangeListener(this);
                    }
                    audioToggle.setChecked(false);
                    photoToggle.setChecked(false);
                    liveVideoToggle.setChecked(false);
                    liveAudioToggle.setChecked(false);
                    chronometer.setVisibility(View.VISIBLE);
                }
                break;
            case Givens.COLLECT_MODE_AUDIO:
                if (roles.audiorecord()) {
                    collectionTypeButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.collectmodeaudio));
                    if (!audioToggle.isChecked()) {
                        audioToggle.setOnCheckedChangeListener(null);
                        audioToggle.setChecked(true);
                        audioToggle.setOnCheckedChangeListener(this);
                    }

                    videoToggle.setChecked(false);
                    photoToggle.setChecked(false);
                    liveVideoToggle.setChecked(false);
                    liveAudioToggle.setChecked(false);
                    chronometer.setVisibility(View.VISIBLE);
                }
                break;
            case Givens.COLLECT_MODE_PICTURE:
                if (roles.photocollect()) {
                    collectionTypeButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.collectmodephoto));
                    if (!photoToggle.isChecked()) {
                        photoToggle.setOnCheckedChangeListener(null);
                        photoToggle.setChecked(true);
                        photoToggle.setOnCheckedChangeListener(this);
                    }
                    audioToggle.setChecked(false);
                    videoToggle.setChecked(false);
                    liveVideoToggle.setChecked(false);
                    liveAudioToggle.setChecked(false);
                    chronometer.setVisibility(View.GONE);
                }
                break;
            case Givens.COLLECT_MODE_VIDEO_LIVE:
                if (roles.videostream()) {
                    collectionTypeButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.collectmodevideolive));
                    if (!liveVideoToggle.isChecked()) {
                        liveVideoToggle.setOnCheckedChangeListener(null);
                        liveVideoToggle.setChecked(true);
                        liveVideoToggle.setOnCheckedChangeListener(this);
                    }
                    audioToggle.setChecked(false);
                    videoToggle.setChecked(false);
                    photoToggle.setChecked(false);
                    liveAudioToggle.setChecked(false);
                    chronometer.setVisibility(View.VISIBLE);
                }
                break;
            case Givens.COLLECT_MODE_AUDIO_LIVE:
                if (roles.audiostream()) {
                    collectionTypeButton.setBackgroundDrawable(
                        getResources().getDrawable(R.drawable.collectmodeaudiolive));
                    if (!liveAudioToggle.isChecked()) {
                        liveAudioToggle.setOnCheckedChangeListener(null);
                        liveAudioToggle.setChecked(true);
                        liveAudioToggle.setOnCheckedChangeListener(this);
                    }
                    audioToggle.setChecked(false);
                    videoToggle.setChecked(false);
                    photoToggle.setChecked(false);
                    liveVideoToggle.setChecked(false);
                    chronometer.setVisibility(View.VISIBLE);
                }
                break;
        }


        if (!mediaManager.isRecording() && mediaManager.previewModes.contains(mode)) {
            // we restart the preview, even if the existing mode is a preview mode, so that we can
            // update the focus type.  This makes for some unavoidable lag when switching between
            // photo and video modes.
            showSwitch();
            //Timber.d("Starting preview in mode switch.");
            startPreview();
        } else {
            hideSwitch();
            //Timber.d("Stopping preview in mode switch.");
            stopPreview();
            //Timber.d("Requested mode does not require video preview.");
        }

        mediaManager.setCaptureMode(mode);
    }

    private void showSwitch() {
        if (Camera.getNumberOfCameras() > 1) {
            cameraSwitch.setVisibility(View.VISIBLE);
        }
    }

    private void hideSwitch() {
        cameraSwitch.setVisibility(View.GONE);
    }

    private void startPreview() {
        AgentSettings settings = application.getAgentSettings();
        AgentRoles roles = settings.getAgentRoles();
        if (roles.photocollect() || roles.videorecord() || roles.videostream()) {
            //Timber.d("Start Preview Called.");
            synchronized (previewLock) {
                if (camera != null) {
                    //Timber.d("Camera already initialized.");
                    if (preview.getSurfaceTextureListener() != null) {
                        Timber.d("Surface Texture Already Set.");
                        return;
                    }
                    //Timber.d("Setting Surface Texture Listener");
                    preview.setSurfaceTextureListener(this);
                    //Timber.d("Enabling Preview Visibility.");
                    preview.setVisibility(View.VISIBLE);
                    return;
                }

                if (previewInitialized) {
                    //Timber.d("Setting Surface Texture Listener");
                    preview.setSurfaceTextureListener(this);
                    //Timber.d("Manually calling onSurfaceTextureAvailable.");
                    onSurfaceTextureAvailable(preview.getSurfaceTexture(), preview.getWidth(), preview.getHeight());
                    //Timber.d("Preview was initialized.");
                } else {
                    //Timber.d("Setting Surface Texture Listener");
                    preview.setSurfaceTextureListener(this);
                }

                //Timber.d("Setting preview visibility.");
                preview.setVisibility(View.VISIBLE);
                //Timber.d("Preview Started.");
            }
        }
    }

    private void stopPreview() {
        synchronized (previewLock) {
            safeToTakePicture = false;
            //Timber.d("Stopping Preview.");
            if (camera != null) {
                //Timber.d("Stopping Camera.");
                try {
                    camera.stopPreview();
                } catch (Exception e) {
                    Timber.w(e, "error stopping preview");
                }
                try {
                    camera.setPreviewCallback(null);
                } catch (Exception e) {
                    Timber.w(e, "error clearing callback");
                }
                try {
                    camera.release();
                } catch (Exception e) {
                    Timber.w(e, "error releasing");
                }
                camera = null;
            }
            preview.setSurfaceTextureListener(null);
            preview.setVisibility(View.GONE);
            //Timber.d("Done Stopping Preview (in fragment)");
        }
    }

    @Override
    public void onDestroy() {
        //Timber.d("Stopping preview in onDestroy.");
        stopPreview();
        super.onDestroy();
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.AgentRolesUpdated agentRolesUpdated) {
        updateRoles(agentRolesUpdated.getAgentRoles());
    }

    @Override
    public void updateRoles(AgentRoles roles) {
        if (roles.collect()) {
            // video stream
            if (!roles.videostream()) {
                if (mediaManager.getCaptureMode() == Givens.COLLECT_MODE_VIDEO_LIVE) {
                    setCaptureMode(Givens.COLLECT_MODE_UNKNOWN);
                }
                liveVideoToggle.setVisibility(View.GONE);
            } else {
                liveVideoToggle.setVisibility(View.VISIBLE);
            }

            // audio stream
            if (!roles.audiostream()) {
                if (mediaManager.getCaptureMode() == Givens.COLLECT_MODE_AUDIO_LIVE) {
                    setCaptureMode(Givens.COLLECT_MODE_UNKNOWN);
                }
                liveAudioToggle.setVisibility(View.GONE);
            } else {
                liveAudioToggle.setVisibility(View.VISIBLE);
            }

            if (!roles.audiorecord()) {
                if (mediaManager.getCaptureMode() == Givens.COLLECT_MODE_AUDIO) {
                    setCaptureMode(Givens.COLLECT_MODE_UNKNOWN);
                }
                audioToggle.setVisibility(View.GONE);
            } else {
                audioToggle.setVisibility(View.VISIBLE);
            }

            if (!roles.videorecord()) {
                if (mediaManager.getCaptureMode() == Givens.COLLECT_MODE_VIDEO) {
                    setCaptureMode(Givens.COLLECT_MODE_UNKNOWN);
                }
                videoToggle.setVisibility(View.GONE);
            } else {
                videoToggle.setVisibility(View.VISIBLE);
            }

            if (!roles.photocollect()) {
                if (mediaManager.getCaptureMode() == Givens.COLLECT_MODE_PICTURE) {
                    setCaptureMode(Givens.COLLECT_MODE_UNKNOWN);
                }
                photoToggle.setVisibility(View.GONE);
            } else {
                photoToggle.setVisibility(View.VISIBLE);
            }

            existingButton.setVisibility(roles.existingmedia() ? View.VISIBLE : View.GONE);


            // don't need the camera switch if no camera is in use.
            cameraSwitch.setVisibility(roles.videorecord() || roles.videostream() || roles.photocollect() ? View.VISIBLE : View.GONE);
            if (!roles.videorecord() && !roles.videostream() && !roles.photocollect()) {
                stopPreview();
            }
        }
    }
}
