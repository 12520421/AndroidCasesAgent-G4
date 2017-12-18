package com.xzfg.app.util.location;

import android.location.LocationManager;

import com.xzfg.app.BuildConfig;

import timber.log.Timber;

public class PassiveWatcher extends LocationWatcher {
    private static PassiveWatcher _instance;

    private PassiveWatcher(LocationManager locationManager) {
        super(LocationManager.PASSIVE_PROVIDER, locationManager, null);
    }

    public static PassiveWatcher getInstance(LocationManager locationManager) {
        if (_instance == null) {
            synchronized (PassiveWatcher.class) {
                if (_instance == null) {
                    _instance = new PassiveWatcher(locationManager);
                }
            }
        }
        return _instance;
    }

    public void start() {
        super.start(2,0);
    }

    @Override
    public void start(int minDistance, int minTime) {
        try {
            super.start(2, 0);
        }
        catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Timber.w(e, "Passive Location Provider could not be started.");
            }
            else {
                throw new RuntimeException("Passive Location Provider could not be started.",e);
            }
        }

    }

}
