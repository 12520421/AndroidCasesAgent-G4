package com.xzfg.app.util.location;


import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

import timber.log.Timber;

public abstract class LocationWatcher {

    private final String provider;
    protected final LocationManager locationManager;
    protected final LocationListener locationListener;
    protected LocationWatcherListener listener = null;
    private boolean watching = false;
    private static final long INTERVAL = 24 * 60 * 60 * 1000;
    private PowerManager.WakeLock wakeLock = null;


    public LocationWatcher(String provider, LocationManager locationManager, PowerManager powerManager) {
        this.provider = provider;
        this.locationManager = locationManager;
        this.locationListener = new WatcherLocationListener();
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LocationWatcher.class.getSimpleName() + "_" + provider);
            wakeLock.setReferenceCounted(false);
        }
        //Timber.d("Created watcher for " + provider);
    }

    public void setListener(LocationWatcherListener listener) {
        this.listener = listener;
    }

    public String getProvider() {
        return this.provider;
    }

    @SuppressWarnings("unused")
    public boolean isWatching() {
        return watching;
    }

    public void start(int minTime, int minDistance) {
        synchronized (LocationWatcher.class) {
            if (!watching) {
                if (wakeLock != null && !wakeLock.isHeld()) { wakeLock.acquire(); }
                //Timber.d("Requesting location update in the " + provider + " watcher with a minTime of " + String.valueOf(minTime) + " and a minDistance of " + String.valueOf(minDistance));
                locationManager.requestLocationUpdates(this.provider, minTime, minDistance, locationListener, Looper.getMainLooper());
                watching = true;

            }
        }
    }

    public void stop() {
        synchronized (LocationWatcher.class) {
            if (watching) {
                //Timber.d("Requesting location update stoppage in the " + provider + " watcher.");
                locationManager.removeUpdates(locationListener);
                watching = false;
                if (wakeLock != null && wakeLock.isHeld()) { wakeLock.release(); }
            }
        }
    }

    protected class WatcherLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            //Timber.d("Location received by " + provider + ". Timestamp: " + String.valueOf(location.getTime() + ", Elapsed Realtime: " + String.valueOf(location.getElapsedRealtimeNanos())));
            if (listener != null) {
                //Timber.d("Processing location.");

                long locationTime = location.getTime();
                long ctime = System.currentTimeMillis();

                // if the time value is within 24hrs of the current time, it's ok.
                if (locationTime >= ctime-INTERVAL && locationTime <= ctime+INTERVAL) {
                    listener.onLocationChanged(location);
                }
                else {
                    //Timber.d("Location timestamp is outside of a day from the current time. Calculating new time.");
                    long bootTime = ctime - SystemClock.elapsedRealtime();
                    long takenDifferential = location.getElapsedRealtimeNanos()/1000000;
                    long captureTime = bootTime + takenDifferential;
                    if (captureTime >= ctime-INTERVAL && captureTime <= ctime+INTERVAL) {
                        //Timber.d("Location getTime() is invalid (" + String.valueOf(location.getTime() + "), substituting with " + String.valueOf(captureTime)));
                        location.setTime(captureTime);
                        listener.onLocationChanged(location);
                    }
                    else {
                        Timber.e("Capture time is outside of 24hr window, and is suspect. Discarding. " + location);
                    }


                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Timber.d("Location Provider " + provider + " Status Changed: " + String.valueOf(status));
        }

        @Override
        public void onProviderEnabled(String provider) {
            //Timber.w("Location Provider " + provider + " Enabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            //Timber.w("Location Provider " + provider + " Disabled: " + provider);
        }
    }
}
