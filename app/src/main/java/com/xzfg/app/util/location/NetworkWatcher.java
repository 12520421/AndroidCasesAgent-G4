package com.xzfg.app.util.location;

import android.location.LocationManager;
import android.os.PowerManager;

import timber.log.Timber;

public class NetworkWatcher extends LocationWatcher {
    private static NetworkWatcher _instance;

    private NetworkWatcher(LocationManager locationManager, PowerManager powerManager) {
        super(LocationManager.NETWORK_PROVIDER, locationManager, powerManager);
    }

    public static NetworkWatcher getInstance(LocationManager locationManager, PowerManager powerManager) {
        if (_instance == null) {
            synchronized (NetworkWatcher.class) {
                if (_instance == null) {
                    _instance = new NetworkWatcher(locationManager, powerManager);
                }
            }
        }
        return _instance;
    }

    @Override
    public void start(int minTime, int minDistance) {
        try {
            super.start(Math.max(2 * 1000, minTime), 0);
        }
        catch (Exception e) {
            Timber.w(e,"Network Location Provider could not be started.");
        }
    }
}
