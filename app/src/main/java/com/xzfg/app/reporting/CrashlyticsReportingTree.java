package com.xzfg.app.reporting;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.util.Network;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

/**
 * This timber tree sends exceptions to crashlytics.
 * Standard logging (info, debug, etc. is ignored).
 */
public class CrashlyticsReportingTree extends Timber.DebugTree {
    private static final int MAX_LOG_LENGTH = 1024;

    Application application;

    public CrashlyticsReportingTree(Application application) {
        Fabric.with(application, new Crashlytics());
        this.application = application;
    }

    private void logMessage(int priority, String tag, String message) {
        if (message != null) {
            if (message.length() < MAX_LOG_LENGTH) {
                Crashlytics.log(priority, tag, message);
            } else {
                // Split by line, then ensure each line can fit into Log's maximum length.
                for (int i = 0, length = message.length(); i < length; i++) {
                    int newline = message.indexOf('\n', i);
                    newline = newline != -1 ? newline : length;
                    do {
                        int end = Math.min(newline, i + MAX_LOG_LENGTH);
                        String part = message.substring(i, end);
                        Crashlytics.log(priority, tag, part);
                        i = end;
                    } while (i < newline);
                }

            }
        }
    }

    private void logException(Throwable t) {
        if (t != null && !Network.isNetworkException(t)) {
            Crashlytics.logException(t);
        }
    }

    @Override
    public void log(int priority, String tag, String message, Throwable t) {

        // debug builds, send it all.
        if (BuildConfig.DEBUG) {
            if (application.getScannedSettings() != null) {
                Crashlytics.setString("scannedSettings",application.getScannedSettings().toString());
            }
            if (application.getAgentSettings() != null) {
                Crashlytics.setString("agentSettings",application.getAgentSettings().toString());
            }
            logMessage(priority, tag, message);
            logException(t);
            return;
        }

        logMessage(priority, tag, message);
        if (priority > Log.WARN) {
            logException(t);
        }
    }

}
