package com.xzfg.app.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

import com.xzfg.app.Givens;
import com.xzfg.app.services.MainService;

/**
 * Handles events related to device administration.
 */
public class AdminReceiver extends DeviceAdminReceiver {
    public static final String TAG = AdminReceiver.class.getName();

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.setAction(Givens.ACTION_DEVICE_ADMIN_ENABLED);
        context.startService(serviceIntent);
    }

    /**
     * Called when this application is no longer the device administrator.
     */
    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.setAction(Givens.ACTION_DEVICE_ADMIN_DISABLED);
        context.startService(serviceIntent);
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        super.onPasswordChanged(context, intent);
        //Timber.d("Device Admin says: Password Changed");
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        super.onPasswordFailed(context, intent);
        //Timber.d("Device Admin says: Password Failed");
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        super.onPasswordSucceeded(context, intent);
        //Timber.d("Device Admin says: Password Succeeded");
    }
}
