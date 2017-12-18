package com.xzfg.app.util.location;

import android.location.LocationManager;
import android.os.PowerManager;

import com.xzfg.app.BuildConfig;

import timber.log.Timber;

public class GpsWatcher extends LocationWatcher {
    private static GpsWatcher _instance;


    private GpsWatcher(LocationManager locationManager, PowerManager powerManager) {
        super(LocationManager.GPS_PROVIDER, locationManager, powerManager);
    }

    public static GpsWatcher getInstance(LocationManager locationManager,PowerManager powerManager) {
        if (_instance == null) {
            synchronized (GpsWatcher.class) {
                if (_instance == null) {
                    _instance = new GpsWatcher(locationManager, powerManager);
                }
            }
        }
        return _instance;
    }

    @Override
    public void start(int minTime, int minDistance) {
            try {
                super.start(Math.max(minTime, 2), 0);
            }
            catch (Exception e) {
                if (BuildConfig.DEBUG) {
                    Timber.w(e, "GPS Location Provider could not be started");
                }
                else {
                    throw new RuntimeException("GPS Location Provider could not be started",e);
                }
            }
        }
}
