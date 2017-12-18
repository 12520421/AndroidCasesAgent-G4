package com.xzfg.app.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import timber.log.Timber;

public abstract class BackWakeIntentService extends IntentService {

    PowerManager powerManager;

    public BackWakeIntentService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);

    }

    @Override
    final protected void onHandleIntent(Intent intent) {
        // get the wake lock before dropping the priority.
        final WakeLock lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, BackWakeIntentService.class.getName());
        // if getting or releasing the wakelock fails, we don't want to crash the application.
        try {
            lock.setReferenceCounted(false);
            if (!lock.isHeld()) {
                lock.acquire();
            }
        }
        catch (Exception e) {
            Timber.w(e,"Failed to acquire wakelock");
        }

        // drop the thread priority
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        // do the work
        try {
            doWork(intent);
        }
        finally {
            // release the lock
            try {
                if (lock.isHeld()) {
                    lock.release();
                }
            }
            catch (Exception e) {
                Timber.w(e,"Failed to release wakelock.");
            }
        }

    }

    abstract protected void doWork(Intent intent);

    final protected boolean isConnected() {
        NetworkInfo activeNetwork = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

}
