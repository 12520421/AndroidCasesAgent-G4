package com.xzfg.app.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.xzfg.app.BuildConfig;
import com.xzfg.app.R;

public class SplashActivity extends Activity  implements View.OnClickListener {
    public final static String EXTRA_IGNORE_SPLASH = "EXTRA_IGNORE_SPLASH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Set app's version
        View versionView = findViewById(R.id.app_version);
        if (versionView != null) {
            ((TextView) versionView).setText(BuildConfig.VERSION_NAME + "-" + BuildConfig.FLAVOR.toUpperCase());
        }

        // This will allow user to close Splash screen by touch immediately
        findViewById(android.R.id.content).setOnClickListener(this);

        // This will close Splash screen after short delay automatically
        Handler handler = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                launchMainActivity();
            }
        };
        handler.postDelayed(r, 1000);
    }

    @Override
    public void onClick(View v) {
        launchMainActivity();
    }

    private void launchMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_IGNORE_SPLASH, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }
}
