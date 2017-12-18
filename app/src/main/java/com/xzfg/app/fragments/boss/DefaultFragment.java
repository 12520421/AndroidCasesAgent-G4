package com.xzfg.app.fragments.boss;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.xzfg.app.BuildConfig;
import com.xzfg.app.Givens;
import com.xzfg.app.R;

import timber.log.Timber;


public class DefaultFragment extends BossModeFragment {

  private FrameLayout webContainer;
  private WebView webView;

  public DefaultFragment() {
  }

  @Override
  public int getType() {
    return Givens.BOSSMODE_TYPE_SLOT;
  }

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    if (BuildConfig.DEBUG) {
      Timber.w("Creating fragment view.");
    }

    View view = inflater.inflate(R.layout.activity_boss, container, false);
    webContainer = (FrameLayout)view.findViewById(R.id.web_container);
    webView = new WebView(getActivity());
    webView.setWebViewClient(new BossWebViewClient());
    webView.setBackgroundColor(0);
    webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
    webView.setScrollbarFadingEnabled(false);
    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setAllowContentAccess(true);
    webSettings.setAllowFileAccess(true);
    webSettings.setAllowFileAccessFromFileURLs(true);
    webSettings.setAllowUniversalAccessFromFileURLs(true);
    webContainer.addView(webView);

    if (BuildConfig.DEBUG) {
      Timber.w("Fragment view created.");
    }

    return view;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view,savedInstanceState);
    if (BuildConfig.DEBUG) {
      Timber.w("Loading html into webview.");
    }
    webView.loadUrl("file:///android_asset/www/index.html");
  }

  @Override
  public void onStart() {
    super.onStart();
    if (webView != null) {
      webView.onResume();
    }
  }

  @Override
  public void onStop() {
    if (webView != null) {
      webView.stopLoading();
      webView.onPause();
    }
    super.onStop();
  }

  @Override
  public void onDestroy() {
    if (webContainer != null) {
      webContainer.removeAllViews();
    }
    if (webView != null) {
      webView.destroy();
      webView = null;
    }
    super.onDestroy();
  }


}
