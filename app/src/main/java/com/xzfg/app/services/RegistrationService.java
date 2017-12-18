package com.xzfg.app.services;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.gson.Gson;
import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.fragments.StatusFragment;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.model.url.BaseUrl;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.model.url.RegistrationUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.security.Fuzz;
import com.xzfg.app.util.Network;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.inject.Inject;
import javax.net.ssl.SSLHandshakeException;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * This service handles device registration.
 */
public class RegistrationService extends BackWakeIntentService {

    @Inject
    public Crypto crypto;

    @Inject
    public OkHttpClient httpClient;

    @Inject
    public Application application;

    @Inject
    public SharedPreferences sharedPreferences;

    @Inject
    public Gson gson;

    public RegistrationService() {
        super(RegistrationService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
    }

    @Override
    protected void doWork(Intent intent) {
        try {
            EventBus.getDefault().postSticky(new Events.handleDisConnect("Connect"));
            ScannedSettings scannedSettings = intent.getParcelableExtra(ScannedSettings.class.getName());
            //Timber.d(scannedSettings.toString());

            try {
                crypto.setKey(scannedSettings.getEncryptionKey());
                checkTimestamp(scannedSettings);
            } catch (Exception e) {
                if (!Network.isNetworkException(e)) {
                    Timber.w(e, "Couldn't get time from server.");
                }
                EventBus.getDefault().postSticky(new Events.Registration(false, getString(R.string.network_error)));
                return;
            }

            try {
                //Timber.d("Performing registration");
                performRegistration(scannedSettings);
            } catch (Exception e) {
                if (!Network.isNetworkException(e)) {
                    Timber.w(e, "An error occurred while attempting to send the registration request.");
                }
                String msg = getString(R.string.network_error);
                if (e instanceof SSLHandshakeException) {
                    msg = getString(R.string.ssl_error);
                }
                EventBus.getDefault().postSticky(new Events.Registration(false, msg));
                return;
            }
        } catch (Exception f) {
            Timber.w(f, "A serious problem occurred in the RegistrationService");
        }
    }


    protected void checkTimestamp(ScannedSettings scannedSettings) throws Exception {
        SimpleDateFormat serverSdf = new SimpleDateFormat("MM-dd-yyyy-HH:mm:ss");

        // if we don't have a timestamp value (it's blank), then we don't check the time.
        if (scannedSettings.getTimestamp() == null || scannedSettings.getTimestamp().trim().length() == 0) {
            return;
        }

        BaseUrl baseUrl = new BaseUrl(scannedSettings, getString(R.string.message_endpoint));
        HashMap<String, String> params = new HashMap<>();
        params.put("message", "GetTime");
        baseUrl.setParamData(params);
        SimpleDateFormat barcodeSdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

        Response timeResponse = httpClient.newCall(new Request.Builder().url(baseUrl.toString(crypto)).build()).execute();
        String timeResponseBody = timeResponse.body().string().trim();
        timeResponseBody = new String(crypto.decryptFromHex(timeResponseBody));

        Date currentDate = serverSdf.parse(timeResponseBody);
        Date barcodeDate = barcodeSdf.parse(scannedSettings.getTimestamp());

        if (barcodeDate.getTime() <= currentDate.getTime()) {
            throw new Exception("Barcode has expired.");
        }

    }

    /**
     * Attempts to perform the registration.
     */
    protected boolean performRegistration(ScannedSettings scannedSettings) throws Exception {
        RegistrationUrl registrationUrl = new RegistrationUrl(scannedSettings, getString(R.string.registration_endpoint), application.getDeviceIdentifier());
        //Timber.d("Registration url: " + registrationUrl.toString());
        Response registrationResponse = httpClient.newCall(new Request.Builder().url(registrationUrl.toString(crypto)).build()).execute();
        String registrationResponseBody = registrationResponse.body().string();

        // if we don't receive a 200 ok, it's a network error.
        if (registrationResponse.code() != 200 || registrationResponseBody.contains("HTTP/1.0 404 File not found")) {
            Timber.d("Server returned non-200 status. Code: " + registrationResponse.code() + ", Message: " + registrationResponse.message() + ", Body: " + registrationResponseBody);
            EventBus.getDefault().post(new Events.Registration(false, getString(R.string.network_error)));
            return false;
        }

        // if we do have a 200 ok, but the response begins with the error prefix, send the user
        // the error message from the server.
        if (registrationResponseBody.startsWith(getString(R.string.server_error_prefix))) {
            String message = registrationResponseBody.substring(getString(R.string.server_error_prefix).length(), registrationResponseBody.length());
            //Timber.d("registration failed: " + message);
            EventBus.getDefault().post(new Events.Registration(false, message));
            return false;
        }

        MessageUrl messageUrl = new MessageUrl(scannedSettings, getString(R.string.message_endpoint), application.getDeviceIdentifier());
        messageUrl.setMessage(MessageUrl.MESSAGE_APP_OPENED);
        Response appOpenedResponse = httpClient.newCall(new Request.Builder().url(messageUrl.toString(crypto)).build()).execute();

        String appOpenedResponseBody = appOpenedResponse.body().string().trim();

        if (appOpenedResponse.code() != 200 || appOpenedResponseBody.contains("HTTP/1.0 404")) {
            throw new Exception("Invalid response from server.");
        }

        appOpenedResponseBody = crypto.decryptFromHexToString(appOpenedResponseBody);

        if (appOpenedResponseBody.startsWith(getString(R.string.server_error_prefix))) {
            String message = appOpenedResponseBody.substring(getString(R.string.server_error_prefix).length(), registrationResponseBody.length());
            //Timber.d("loading settings failed: " + message);
            EventBus.getDefault().post(new Events.Registration(false, message));
            return false;
        }


        AgentSettings agentSettings = AgentSettings.parse(application, appOpenedResponseBody);

        application.setRecordCalls(agentSettings.getRecordCalls());
        application.setPhoneLogs(agentSettings.getPhoneLog()!=0);
        application.setPhoneLogDelivery(agentSettings.getPhoneLogDelivery());
        application.setSMSLogs(agentSettings.getSMSLog()!=0);
        application.setSMSLogDelivery(agentSettings.getSMSLogDelivery());

        // if the registration returns anything else, the registration was successful.
        //Timber.d("registration completed successfully!");
        boolean committed = sharedPreferences.edit().putString(Fuzz.en(Givens.CONFIG, application.getDeviceIdentifier()), Fuzz.en(gson.toJson(scannedSettings), application.getDeviceIdentifier())).commit();
        if (committed) {
            EventBus.getDefault().postSticky(new Events.Registration(true, getString(R.string.registration_success), agentSettings, scannedSettings));
        //    EventBus.getDefault().post(new Events.MenuItemSelected("StatusFragmentCode",R.id.status));
         //   deleteAppData();
        //    EventBus.getDefault().postSticky(new Events.OpenStatus(R.id.status));
         //   EventBus.getDefault().postSticky(new Events.ScanbarCodeConnect());
          //  boolean scanbarcode = true;
         //   exercises = ParseJSON.ChallengeParseJSON(strJson);
            SharedPreferences.Editor editor = getSharedPreferences("ScanbarCode", 0).edit();
            editor.putBoolean("scan",true);
            editor.apply();
            return true;
        } else {
            EventBus.getDefault().postSticky(new Events.Registration(false, getString(R.string.registration_failed)));
            return false;
        }

    }
    private void deleteAppData() {
        try {
            // clearing app data
            String packageName = getApplicationContext().getPackageName();
            Runtime runtime = Runtime.getRuntime();
            runtime.exec("pm clear "+packageName);

        } catch (Exception e) {
            e.printStackTrace();
        } }

}
