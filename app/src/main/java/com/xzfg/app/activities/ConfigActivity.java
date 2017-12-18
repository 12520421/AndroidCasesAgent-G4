package com.xzfg.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.services.RetryRegistrationService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import timber.log.Timber;

public class ConfigActivity extends BaseActivity {
    @Inject
    Application application;

    @Inject
    Crypto crypto;

    @Inject
    SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getApplication()).inject(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (application.getScannedSettings() == null) {
            getSettingsFromIntent();
        }
    }


    @SuppressLint("ApplySharedPref")
    private void getSettingsFromIntent() {
        //Timber.d("No scanned settings yet, attempting to load config from extras.");
        Intent intent = getIntent();
        if (intent != null) {
            //Timber.d("We have an intent.");
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                //Timber.d("We have extras.");

                String config;
                // if the config was passed as a single string, use it.
                if (extras.containsKey("config")) {
                    //Timber.d("Using single config string.");
                    config = extras.getString("config");
                }
                // if the config was passed as individual parameters, use them.
                else {
                    //Timber.d("Using individual config parameters.");
                    StringBuilder sb = new StringBuilder(192);
                    // ipAddress
                    sb.append(extras.getString("ipAddress", "")).append("|");
                    // organizationId
                    sb.append(extras.getLong("organizationId")).append("|");
                    // userName
                    sb.append(extras.getString("userName", "")).append("|");
                    // password
                    sb.append(extras.getString("password", "")).append("|");
                    // timestamp
                    sb.append(extras.getString("timestamp", "")).append("|");
                    // encryption
                    //sb.append(extras.getString("encryptionFormat", "")).append("|");
                    // encryption key
                    //sb.append(extras.getString("encryptionKey", "")).append("|");

                    // ports!
                    StringBuilder portSb = new StringBuilder(48);
                    appendPort(portSb, "T", extras.getLong("trackingPort", -1));
                    appendPort(portSb, "V", extras.getLong("videoPort", -1));
                    appendPort(portSb, "A", extras.getLong("audioPort", -1));
                    appendPort(portSb, "U", extras.getLong("uploadPort", -1));
                    /*appendPort(portSb, "C", extras.getLong("mapPort", -1));
                    sb.append(portSb).append("|");
                    sb.append(extras.getInt("useMapPort", 1)).append("|");
                    sb.append(extras.getString("mapUrl",""));*/
                    config = sb.toString();
                }

                //Timber.d("Config String: " + config);
                Result result = new Result();
                result.setBarcodeFormat(BarcodeFormat.QRCODE);
                result.setContents(config);

                try {
                    ScannedSettings fromExtras = ScannedSettings.parse(this, result);
                    fromExtras.validateCrypto(application, crypto);
                    EventBus.getDefault().postSticky(new Events.ScanSuccess(fromExtras));
                    Intent registrationIntent = new Intent(application, RetryRegistrationService.class);
                    registrationIntent.putExtra(ScannedSettings.class.getName(), fromExtras);
                    sharedPreferences.edit().putBoolean(Givens.MDM_CONFIG,true).commit();
                    application.startService(registrationIntent);
                } catch (Exception e) {
                    Timber.e(e, "Couldn't build scanned settings from extras.");
                }
            }

        }
        finish();
    }


    private void appendPort(StringBuilder sb, String key, long port) {
        if (port != -1) {
            if (sb.length() > 0) {
                sb.append("~");
            }
            sb.append(key).append("@").append(port);
        }
    }

}
