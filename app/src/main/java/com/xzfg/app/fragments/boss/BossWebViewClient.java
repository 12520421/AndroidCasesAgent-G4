package com.xzfg.app.fragments.boss;

import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;

import de.greenrobot.event.EventBus;
import timber.log.Timber;


public class BossWebViewClient extends WebViewClient {
  /**
   * Override URL handling for any url that contains Agent.html, allowing exit from boss mode.
   */
  @SuppressWarnings("deprecation")
  @Override
  public boolean shouldOverrideUrlLoading(WebView view, String url) {
    if (BuildConfig.DEBUG) {
      Timber.w("Checking for overridden url handling for: " + String.valueOf(url));
    }
    if (url.contains("Agent.html")) {
      if (BuildConfig.DEBUG) {
        Timber.w("Exiting Boss Mode");
      }
      // send the event to exit boss mode.
      EventBus.getDefault().post(new Events.BossModeStatus(Givens.ACTION_BOSSMODE_EXIT));
      return true;
    }
    return false;
  }

  @Override
  public void onPageStarted(WebView view, String url, Bitmap favicon) {
    super.onPageStarted(view,url,favicon);
    if (BuildConfig.DEBUG) {
      Timber.w("PAGE STARTED: " + String.valueOf(url));
    }
  }

  @Override
  public void onPageFinished(WebView view, String url) {
    super.onPageFinished(view,url);
    if (BuildConfig.DEBUG) {
      Timber.w("PAGE FINISHED: " + String.valueOf(url));
    }
  }
}
