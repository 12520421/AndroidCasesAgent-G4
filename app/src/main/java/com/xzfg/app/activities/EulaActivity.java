package com.xzfg.app.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.util.MessageUtil;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class EulaActivity extends Activity implements View.OnClickListener {

    @Inject
    Application application;

    WebView mWebView = null;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getApplication()).inject(this);
        setContentView(R.layout.activity_eula);
        findViewById(R.id.btn_decline).setOnClickListener(this);
        findViewById(R.id.btn_accept).setOnClickListener(this);
        FrameLayout container = (FrameLayout)findViewById(R.id.view_container);
        mWebView = new WebView(this);
        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        WebSettings settings = mWebView.getSettings();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        settings.setDomStorageEnabled(false);
        settings.setAppCacheEnabled(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setGeolocationEnabled(false);
        settings.setDatabaseEnabled(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptEnabled(false);

        settings.setSaveFormData(false);
        settings.setSavePassword(false);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setBackgroundColor(getResources().getColor(R.color.timberwolf));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }

        container.addView(mWebView);
    }

    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, MessageUrl.MESSAGE_GET_EULA));
    }

    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(Events.EulaReceived eulaReceived) {
        if (mWebView != null) {
            mWebView.loadDataWithBaseURL(null,eulaReceived.getEula(), "text/html", "utf-8", null);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_accept) {
            application.getAgentSettings().setEULA(0);
            MessageUtil.sendMessage(application,MessageUtil.getMessageUrl(application,MessageUrl.MESSAGE_SET_EULA));
            Intent intent = new Intent(this,MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        }
        finish();
    }
}
