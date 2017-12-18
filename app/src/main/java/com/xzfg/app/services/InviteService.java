package com.xzfg.app.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.model.url.InviteUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.Network;

import java.io.IOException;

import javax.inject.Inject;
import javax.net.ssl.SSLHandshakeException;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class InviteService extends BackWakeIntentService {

    private final static int ACTION_GET_INVITE_DATA = 1;
    private final static String PARAM_ACTION = "PARAM_ACTION";
    private final static String PARAM_INVITE = "PARAM_INVITE";

    @Inject
    Application application;
    @Inject
    FixManager fixManager;
    @Inject
    Crypto crypto;
    @Inject
    OkHttpClient httpClient;


    public static void getInviteData(Context context, String invite) {
        Intent i = new Intent(context, InviteService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_GET_INVITE_DATA);
        bundle.putString(PARAM_INVITE, invite);
        i.putExtras(bundle);
        context.startService(i);
    }

    //
    public InviteService() {
        super(InviteService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
    }

    @Override
    protected void doWork(Intent intent) {
        Bundle request = intent.getExtras();
        int action = request.getInt(PARAM_ACTION);

        if (!isConnected()) {
            EventBus.getDefault().post(new Events.InviteDataReceived(false, getString(R.string.network_error)));
            return;
        }

        try {
            //
            if (action == ACTION_GET_INVITE_DATA) {
                Timber.d("Getting invite data...");
                String invite = request.getString(PARAM_INVITE);

                // Get invite data from server
                String data = getInviteData(invite);

                // Send notification to UI
                String errorPrefix = "error:";
                boolean status = (data != null && !data.toLowerCase().contains(errorPrefix));
                if (!status && data != null) {
                    data = data.substring(data.toLowerCase().indexOf(errorPrefix) + errorPrefix.length()).trim();
                }
                EventBus.getDefault().post(new Events.InviteDataReceived(status, data));
            }

        } catch (Exception e) {
            String msg = getString(R.string.network_error);
            if (e instanceof SSLHandshakeException) {
                msg = getString(R.string.ssl_error);
            }
            EventBus.getDefault().post(new Events.ProfilePhotoLoaded(false, msg));
            if (!Network.isNetworkException(e)) {
                Timber.w(e, "An error occurred in Invite Service.");
            }
        }

    }

    // Get invite data from server
    private String getInviteData(String invite) throws IOException {
        if (BuildConfig.DEFAULT_URL.length() > 0) {
            ScannedSettings settings = new ScannedSettings(BuildConfig.DEFAULT_URL, Long.valueOf(BuildConfig.DEFAULT_PORT));
            InviteUrl url = new InviteUrl(settings,
                    application.getString(R.string.get_invite_data_endpoint),
                    invite);

            Timber.d("Calling url: " + url.toString());
            Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
            String responseBody = response.body().string().trim();
            response.body().close();

            return responseBody;
        }

        return null;
    }

}
