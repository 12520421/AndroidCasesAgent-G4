package com.xzfg.app.services;

import android.content.Intent;

import com.xzfg.app.Application;
import com.xzfg.app.Givens;
import com.xzfg.app.model.url.BaseUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.Network;

import javax.inject.Inject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * This handles logging to the server.
 */
public class UrlService extends BackWakeIntentService {
    private static final String TAG = MessageService.class.getName();
    @Inject
    Application application;
    @Inject
    Crypto crypto;
    @Inject
    OkHttpClient httpClient;

    public UrlService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
    }

    /**
     * Be careful about logging here. An exception in this class could cause
     * an infinte loop --- SO DON'T USE Timber.e() HERE!
     */
    @Override
    protected void doWork(Intent intent) {
        try {
            BaseUrl messageUrl = intent.getParcelableExtra(Givens.MESSAGE);

            try {
                //Timber.d("Calling URL: " + messageUrl);
                Response msgResponse = httpClient.newCall(new Request.Builder().url(messageUrl.toString(crypto)).build()).execute();
                msgResponse.body().close();
            } catch (Exception e) {
                if (!Network.isNetworkException(e)) {
                    Timber.w(e, "A problem occurred attempting to send a message: " + messageUrl.toString());
                }
            }
        } catch (Exception f) {
            if (!Network.isNetworkException(f)) {
                Timber.w(f, "A serious problem occurred in the MessageService");
            }
        }
    }

}
