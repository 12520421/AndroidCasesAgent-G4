package com.xzfg.app.services;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.managers.SessionManager;
import com.xzfg.app.model.url.SetSetupFieldUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.Network;

import java.util.HashMap;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * This service sends any setup field requests to the server.
 */
public class SetSetupFieldService extends BackWakeIntentService {

  private static final String TAG = SetSetupFieldService.class.getName();
  @Inject
  Application application;
  @Inject
  Crypto crypto;
  @Inject
  OkHttpClient httpClient;
  @Inject
  SessionManager sessionManager;

  public SetSetupFieldService() {
    super(TAG);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    ((Application) getApplication()).inject(this);
  }

  @Override
  protected void doWork(Intent intent) {
    try {
      final SetSetupFieldUrl setSetupFieldUrl = intent.getParcelableExtra(Givens.MESSAGE);

      if (setSetupFieldUrl == null) {
        Timber.w("Received null SetSetupField url.");
        return;
      }

      boolean sent = sendUrl(setSetupFieldUrl);
      if (!sent) {
        sent = sendUrl(setSetupFieldUrl);
      }

    }
    catch (Exception f) {
      Timber.w(f, "A serious problem occurred in the MessageService");
    }

  }


  private boolean sendUrl(SetSetupFieldUrl setSetupFieldUrl) throws Exception {

    PackageManager packageManager = getPackageManager();
    if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
      HashMap<String,String> pairs = new HashMap<>();
      try {
        TelephonyManager telephonyManager =
            (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

        String imei = telephonyManager.getDeviceId();
        String phoneNumber = telephonyManager.getLine1Number();
        //int phoneType = telephonyManager.getPhoneType();

        if (imei != null) {
          pairs.put("IMEI", imei);
        }
        if (phoneNumber != null && phoneNumber.trim().length() > 0) {
          pairs.put("phoneNumber", phoneNumber);
        }
      }
      catch (Exception e) {
        Timber.e(e, "Couldn't get telephony information");
      }
      if (setSetupFieldUrl.getPairs() != null && !setSetupFieldUrl.getPairs().isEmpty()) {
        pairs.putAll(setSetupFieldUrl.getPairs());
      }
      setSetupFieldUrl.setPairs(pairs);
    }

    if (setSetupFieldUrl.getPairs() == null || setSetupFieldUrl.getPairs().isEmpty()) {
      Timber.w("SetSetupFieldUrl has no pairs.");
      return true;
    }

    if (setSetupFieldUrl.getSessionId() == null) {

      setSetupFieldUrl.setSessionId(sessionManager.getSessionId());

      if (setSetupFieldUrl.getSessionId() == null) {
        int wait = 100;
        // use exponential backoff for up to a max of 30 seconds to wait for
        // a session id.
        while (setSetupFieldUrl.getSessionId() == null && wait < 30000) {
          Thread.sleep(wait);
          setSetupFieldUrl.setSessionId(sessionManager.getSessionId());
          wait = wait*2;
        }
      }
    }

    if (setSetupFieldUrl.getSessionId() == null) {
      Timber.w("SetSetupField has no sessionId, and one was not acquired in a reasonable amount of time.");
    }

    try {
      Response setupResponse = httpClient
          .newCall(new Request.Builder().url(setSetupFieldUrl.toString(crypto)).build())
          .execute();
      String setupBody = setupResponse.body().string().trim();

      // if we don't receive a 200 ok, it's a network error.
      if (setupResponse.code() != 200 || setupBody.contains("HTTP/1.0 404 File not found")) {
        throw new Exception("Server did not respond appropriately.");
      }

      setupBody = crypto.decryptFromHexToString(setupBody).trim();

      // we got back something other than an ok. This should be an error.
      if (!setupBody.startsWith("<?xml version='1.0' encoding='utf-8' ?>OK")) {
        if (setupBody.toLowerCase().contains("invalid sessionid")) {
          // invalid sessionid!
          EventBus.getDefault().post(new Events.InvalidSession());
          Thread.sleep(2000);
          return false;
        }
        else {
          throw new Exception("Server returned an error.");
        }
      }

    } catch (Exception e) {
      if (!Network.isNetworkException(e)) {
        Timber.w(e,
            "A problem occurred attempting to send a setupField: " + setSetupFieldUrl.toString());
      }

    }

    return true;
  }
}
