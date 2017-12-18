package com.xzfg.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.services.RegistrationService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import timber.log.Timber;


public class UrlActivity extends BaseActivity {

    @Inject
    Application application;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application)getApplication()).inject(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);

        try {
            Intent intent = getIntent();

            Uri data = intent.getData();
            String register = new String(Base64.decode(data.getQueryParameter("register"), Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP));
            Result result = new Result();
            result.setBarcodeFormat(BarcodeFormat.QRCODE);
            result.setContents(register);
            final ScannedSettings settings = ScannedSettings.parse(application, result);
            Intent registrationIntent = new Intent(application, RegistrationService.class);
            registrationIntent.putExtra(ScannedSettings.class.getName(), settings);
            application.startService(registrationIntent);


        }
        catch (Exception e) {
            Timber.e(e,"Couldn't handle the url.");
            finish();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(Events.Registration registration) {
        if (registration.getStatus()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(Givens.EXTRA_AGENT_SETTINGS, registration.getAgentSettings());
            startActivity(intent);
            finish();
        }
        else {
            Toast.makeText(this,getString(R.string.registration_failed),Toast.LENGTH_LONG).show();
            finish();
        }
    }

}
