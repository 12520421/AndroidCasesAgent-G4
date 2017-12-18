package com.xzfg.app.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.apache.commons.lang3.exception.ExceptionUtils;

public class Network {
    private Network() {}

    public static boolean isNetworkException(Throwable e) {
        Throwable[] throwables = ExceptionUtils.getThrowables(e);
        for (Throwable t : throwables) {
            if (t.getClass().getPackage().getName().equalsIgnoreCase("java.net")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isConnected(Context context) {
        NetworkInfo activeNetwork = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

}
