package com.xzfg.app.services;


import android.content.Intent;
import android.os.SystemClock;

import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.util.Network;

import timber.log.Timber;

public class RetryRegistrationService extends RegistrationService {

    // 90 second retry interval
    private static final long RETRY_INTERVAL = 90*1000;
    // max retries.
    private static final int MAX_TRIES = 3;

    private int tries = 0;

    @Override
    protected void doWork(Intent intent) {
        if (intent.getExtras() != null && intent.getExtras().containsKey(ScannedSettings.class.getName())) {
            try {
                final ScannedSettings scannedSettings = intent.getParcelableExtra(ScannedSettings.class.getName());
                //Timber.d("Registration service got scanned settings: " + scannedSettings.toString());
                while (!register(scannedSettings)) {
                    tries++;
                    if (tries >= MAX_TRIES) {
                        break;
                    }
                    //Timber.d("Trying again in " + RETRY_INTERVAL/1000 + " seconds. " + tries);
                    SystemClock.sleep(RETRY_INTERVAL);
                }
                //Timber.d("All done attempting to register.");
            }
            catch (Exception e) {
                if (!Network.isNetworkException(e)) {
                    Timber.e(e, "A severe error has occurred in the retry registration service.");
                }
            }
        }
    }

    public boolean register(final ScannedSettings scannedSettings) {
        boolean success;
        try {
            crypto.setKey(scannedSettings.getEncryptionKey());
            checkTimestamp(scannedSettings);
            success = performRegistration(scannedSettings);
        } catch (Exception e) {
            //Timber.d(e, "An error occurred attempting to register.");
            success = false;
        }
        return success;
    }


}
