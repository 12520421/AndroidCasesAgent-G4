package com.xzfg.app.services;

import android.content.Intent;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.managers.SessionManager;
import com.xzfg.app.model.url.SessionUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.Network;

import java.util.HashMap;

import javax.inject.Inject;
import javax.net.ssl.SSLHandshakeException;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 *
 */
public class CreatePoiService extends BackWakeIntentService {

    @Inject
    Application application;

    @Inject
    SessionManager sessionManager;

    @Inject
    Crypto crypto;

    @Inject
    OkHttpClient httpClient;

    public CreatePoiService() {
        super(CreatePoiService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
    }

    @Override
    protected void doWork(Intent intent) {

        //Timber.d("Creating poi url");
        Bundle request = intent.getExtras();
        SessionUrl sessionUrl = new SessionUrl(application.getScannedSettings(), getString(R.string.create_poi_endpoint), sessionManager.getSessionId());
        //sessionUrl.setEncode(false);
        HashMap<String, String> params = new HashMap<>();
        params.put("name", request.getString("name"));
        params.put("description", request.getString("description"));
        params.put("address", request.getString("address"));
        params.put("categoryId", request.getString("categoryId"));
        String group = request.getString("group");
        if (group != null && group.equalsIgnoreCase("Default")) {
            params.put("group", null);
        } else {
            params.put("group", group);
        }
        params.put("latitude", request.getString("latitude"));
        params.put("longitude", request.getString("longitude"));
        sessionUrl.setParamData(params);

        if (!isConnected()) {
            EventBus.getDefault().post(new Events.PoiCreated(false, getString(R.string.network_error)));
            return;
        }

        try {
            //Timber.d("Calling " + sessionUrl.toString());

            Response response = httpClient.newCall(new Request.Builder().url(sessionUrl.toString(crypto)).build()).execute();
            String responseBody = response.body().string().trim();
            response.body().close();

            // if we don't receive a 200 ok, it's a network error.
            if (response.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
                if (BuildConfig.DEBUG) {
                    Crashlytics.setString("Url", sessionUrl.toString());
                    Crashlytics.setLong("Response Code", response.code());
                    Crashlytics.setString("Response", responseBody);
                }
                throw new Exception("Server did not respond appropriately.");
            }

            //Timber.d("Response Body (encrypted): " + responseBody);

            // if we don't receive a 200 ok, it's a network error.
            if (response.code() != 200 || responseBody.isEmpty() || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
                if (BuildConfig.DEBUG) {
                    Crashlytics.setString("Url", sessionUrl.toString());
                    Crashlytics.setLong("Response Code", response.code());
                    Crashlytics.setString("Response", responseBody);
                }
                throw new Exception("Server did not respond appropriately.");
            }

            responseBody = crypto.decryptFromHexToString(responseBody).trim();
            //Timber.d("Response Body (unencrypted): " + responseBody);

            if (responseBody.startsWith("Error") || responseBody.startsWith("ok")) {
                if (BuildConfig.DEBUG) {
                    Crashlytics.setString("Url", sessionUrl.toString());
                    Crashlytics.setLong("Response Code", response.code());
                    Crashlytics.setString("Response", responseBody);
                }
                throw new Exception("Error response received from server.");
            }
            EventBus.getDefault().post(new Events.PoiCreated(true));

        } catch (Exception e) {
            String msg = getString(R.string.network_error);
            if (e instanceof SSLHandshakeException) {
                msg = getString(R.string.ssl_error);
            }
            EventBus.getDefault().post(new Events.PoiCreated(false, msg));
            if (!Network.isNetworkException(e)) {
                Timber.w(e, "An error occurred attempting to create a poi.");
            }
        }

    }


}
