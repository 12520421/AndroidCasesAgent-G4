package com.xzfg.app.managers;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;

import com.squareup.seismic.ShakeDetector;
import com.xzfg.app.Application;
import com.xzfg.app.Events;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * This actually does a bit more than just watch for orientation changes. It also does shake
 * detection.
 */
public class OrientationManager implements SensorEventListener {

    private static volatile int orientation;
    @SuppressWarnings("FieldCanBeLocal")
    private static SensorManager sensorManager;
    private static Sensor accelerometer;
    private static Sensor magnetometer;
    private static final float[] lastAccelerometer = new float[3];
    private static final float[] lastMagnetometer = new float[3];
    private static volatile boolean lastAccelerometerSet = false;
    private static volatile boolean lastMagnetometerSet = false;
    private static final float[] rotationMatrix = new float[9];
    private static final float[] orientationMatrix = new float[3];
    private static volatile PowerManager powerManager;
    private static volatile PowerManager.WakeLock wakeLock;
    private static final Object hardLock = new Object();

    private static final int SHAKE_THRESHOLD = 50;
    private static final int SHAKE_INTERVAL = 100;
    private static final float[] lastShakeAccelerometer = new float[3];
    private static volatile long lastShakeUpdateTime = 0;

    private final Application application;
    @SuppressWarnings("WeakerAccess")
    @Inject
    AlertManager alertManager;
    @Inject
    AlertManager fixManager;
    @Inject
    public InputMethodManager inputMethodManager;
    private View decorView;
    private View containerView;
    @SuppressWarnings("FieldCanBeLocal")
    private final ShakeDetector.Listener hardShakeListener = new ShakeDetector.Listener() {
        @Override
        public void hearShake() {
            synchronized (hardLock) {
                if (application.isSetupComplete() && application.getAgentSettings().getAgentRoles().paniccovert()) {
                        alertManager.startPanicMode(false);


                }
            }
        }
    };


    @SuppressWarnings("unused")
    public OrientationManager(Application application) {
        this(application, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public OrientationManager(Application application, int rate) {
        //Timber.d("Orientation Manager Created.");
       this.application = application;
        this.application.inject(this);

        // get a wake lock.
        powerManager = (PowerManager) application.getSystemService(Context.POWER_SERVICE);

        getWakeLock();
        //EventBus.getDefault().registerSticky(this);

        // use a background thread handler.
        HandlerThread sensorHandlerThread = new SensorHandlerThread();
        Handler sensorHandler = new Handler(sensorHandlerThread.getLooper());

        OrientationEventListener orientationEventListener = new OrientationEventListener(application, rate) {
            @Override
            public void onOrientationChanged(int eventOrientation) {
                if (eventOrientation >= 315 || eventOrientation <= 45) {
                    //Timber.w("PORTRAIT MODE.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    return;
                }
                if (eventOrientation <= 225 && eventOrientation >= 135) {
                    //Timber.w("REVERSE PORTAIT MODE.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    return;
                }
                if (eventOrientation < 315 && eventOrientation > 225) {
                    //Timber.w("LANDSCAPE");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    return;
                }
                if (eventOrientation > 45 && eventOrientation < 135) {
                    //Timber.w("REVERSE LANDSCAPE");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            /*   if(eventOrientation < 90)
               {
                   //Timber.w("PORTRAIT MODE.");
                   orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                   return;
               }
               if(eventOrientation >= 90 && eventOrientation < 180)
               {
                   orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                   return;
               }
               if(eventOrientation >=180 && eventOrientation <270)
               {
                   orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                   return;
               }
               if(eventOrientation >= 270)
               {
                   orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                   return;
               }*/
            }
        };

        sensorManager = (SensorManager) application.getSystemService(Activity.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, rate, sensorHandler);
        }
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, rate, sensorHandler);
        }


       ShakeDetector hardShakeDetector = new ShakeDetector(hardShakeListener);
        hardShakeDetector.setSensitivity(ShakeDetector.SENSITIVITY_HARD);
        hardShakeDetector.start(sensorManager);

        // Replaced LightShake with SignificantMovement sensor for detecting
        // movements because old one was constantly on
        /*motionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

        ShakeDetector lightShakeDetector = new ShakeDetector(lightShakeListener);
        lightShakeDetector.setSensitivity(ShakeDetector.SENSITIVITY_LIGHT);
        lightShakeDetector.start(sensorManager);*/

        orientationEventListener.enable();
       // orientationEventListener.disable();
    }

    public int getOrientation() {
        getWakeLock();
        return orientation;
    }

    public float[] getOrientationMatrix() {
        getWakeLock();
        return orientationMatrix;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        getWakeLock();

        // Shake detection to keep live tracking going
        if (event.sensor == accelerometer) {
            long curTime = System.currentTimeMillis();
            long diffTime = (curTime - lastShakeUpdateTime);

            // only allow one update every 100ms.
            if (diffTime > SHAKE_INTERVAL) {
                lastShakeUpdateTime = curTime;

                double x = event.values[0];
                double y = event.values[1];
                double z = event.values[2];
                double last_x = lastShakeAccelerometer[0];
                double last_y = lastShakeAccelerometer[1];
                double last_z = lastShakeAccelerometer[2];

                double speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;
                if (speed > SHAKE_THRESHOLD) {
                    //Timber.w("sensor shake detected w/ speed: " + speed);
                    // Notify app about movement
                    EventBus.getDefault().post(new Events.MovementDetected());
                }
                System.arraycopy(event.values, 0, lastShakeAccelerometer, 0, event.values.length);
            }
        }

        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            lastAccelerometerSet = true;
        }
        if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            lastMagnetometerSet = true;
        }
        if (lastAccelerometerSet && lastMagnetometerSet) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer);
            SensorManager.getOrientation(rotationMatrix, orientationMatrix);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        getWakeLock();
        // noop.
    }

    private synchronized void getWakeLock() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, OrientationManager.class.getName());
            wakeLock.setReferenceCounted(false);
        }
        try {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }
        catch (Exception e) {
            Timber.e(e,"Failed to acquire wake lock.");
        }
    }

    private class SensorHandlerThread extends HandlerThread {
        SensorHandlerThread() {
            super(SensorHandlerThread.class.getName(), android.os.Process.THREAD_PRIORITY_BACKGROUND);
            start();
        }
    }

}
