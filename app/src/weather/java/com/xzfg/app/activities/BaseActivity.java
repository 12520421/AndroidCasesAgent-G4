package com.xzfg.app.activities;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.receivers.AdminReceiver;

/**
 * The root from which all other activities spring.  This enforces the check that device
 * administration is enabled (which is what provides the app with the ability to perform a factory
 * reset)
 *
 */
public class BaseActivity extends FragmentActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        overridePendingTransition(0,0);
        if (getResources().getBoolean(R.bool.enable_device_admin)) {
            checkDeviceAdministration();
        }
    }


    private void checkDeviceAdministration() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdminComponentName = new ComponentName(this, AdminReceiver.class);

        if (!devicePolicyManager.isAdminActive(deviceAdminComponentName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponentName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.grant_rights));
            startActivityForResult(intent, Givens.ADMIN_ACTIVATION_REQUEST_CODE);
        }
        else {
            checkEncryption();
        }
    }

    public void checkEncryption() {
        /*
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        int status = devicePolicyManager.getStorageEncryptionStatus();
        switch (status) {
            case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING:
                Timber.d("Activating Encryption");
                break;
            case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                Timber.d("Encryption Active");
                break;
            case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE:
                if (!BuildConfig.DEBUG) {
                    startActivityForResult(new Intent(DevicePolicyManager.ACTION_START_ENCRYPTION), Givens.ADMIN_ENCRYPTION_REQUEST_CODE);
                    Timber.d("Encryption Inactive");
                    finish();
                }
                break;
            case DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED:
                Timber.d("Encryption Not Supported.");
                finish();
                break;
        }
        */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Givens.ADMIN_ACTIVATION_REQUEST_CODE && resultCode != Activity.RESULT_OK) {
            finish();
            return;
        }
        else {
            checkEncryption();
            return;
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0,0);
    }
}
