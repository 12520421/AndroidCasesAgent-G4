package com.xzfg.app.services;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.gson.Gson;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.SessionManager;
import com.xzfg.app.model.AgentProfile;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.model.url.SetSetupFieldUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.BatteryUtil;
import com.xzfg.app.util.Network;

import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * This handles logging to the server.
 */
public class MessageService extends BackWakeIntentService {
    private static final String TAG = MessageService.class.getName();
    @Inject
    Application application;
    @Inject
    Crypto crypto;
    @Inject
    OkHttpClient httpClient;
    @Inject
    SharedPreferences sharedPreferences;
    @Inject
    FixManager fixManager;
    @Inject
    SessionManager sessionManager;

    private Gson gson;

    public MessageService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
        gson = new Gson();
    }

    /**
     * Be careful about logging here. An exception in this class could cause
     * and infinte loop --- SO DON'T USE Timber.e() HERE!
     */
    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    @Override
    protected void doWork(Intent intent) {
        try {
            MessageUrl messageUrl = intent.getParcelableExtra(Givens.MESSAGE);
            SetSetupFieldUrl setSetupFieldUrl = null;
            if (messageUrl == null) {
                Timber.w("Received null message url.");
                return;
            }

            if (messageUrl.getMessage().equals(MessageUrl.MESSAGE_APP_OPENED)) {
                ConcurrentHashMap<String, String> details =  new ConcurrentHashMap<>();
                // os/device specific values
                details.put("SYS_BRAND", Build.BRAND != null ? Build.BRAND : "");
                details.put("SYS_MANUFACTURER", Build.MANUFACTURER != null ? Build.MANUFACTURER : "");
                details.put("SYS_BOARD", Build.BOARD != null ? Build.BOARD : "");
                details.put("SYS_DEVICE", Build.DEVICE != null ? Build.DEVICE : "");
                details.put("SYS_PRODUCT", Build.PRODUCT != null ? Build.PRODUCT : "");
                details.put("SYS_OS", System.getProperty("os.name", ""));
                details.put("SYS_ANDROID_VERSION", Build.VERSION.RELEASE != null ? Build.VERSION.RELEASE : "");
                details.put("SYS_SDK_INT", String.valueOf(Build.VERSION.SDK_INT));
                details.put("SYS_TAGS", Build.TAGS != null ? Build.TAGS : "");
                details.put("SYS_TYPE", Build.TYPE != null ? Build.TYPE : "");
                
                // application and build specific values.
                details.put("APP_ID", BuildConfig.APPLICATION_ID);
                details.put("APP_FLAVOR", BuildConfig.FLAVOR);
                details.put("APP_HEADLESS", String.valueOf(BuildConfig.HEADLESS));
                details.put("APP_AUDIO_NOTIFICATION", String.valueOf(BuildConfig.AUDIO_NOTIFICATION));
                details.put("APP_SSL_PINNING", String.valueOf(BuildConfig.SSL_PINNING));
                details.put("APP_VERSION_NAME", BuildConfig.VERSION_NAME);
                details.put("APP_VERSION_CODE", String.valueOf(BuildConfig.VERSION_CODE));
                details.put("APP_DEBUG", String.valueOf(BuildConfig.DEBUG));
                messageUrl.setMetaData(details);

                setSetupFieldUrl = new SetSetupFieldUrl(application);
            }

            // we're not connected and it's an SOS. save it for later.
            if (!isConnected() && messageUrl.getMessage().equals(MessageUrl.MESSAGE_SOS)) {
                sharedPreferences.edit().putBoolean("ssos", true).commit();
                return;
            }

            if (messageUrl.getLocation() == null) {
                messageUrl.setLocation(fixManager.getLastLocation());
            }
            if (messageUrl.getBattery() == null) {
                messageUrl.setBattery(BatteryUtil.getBatteryLevel(application));
            }

            //Timber.d(messageUrl.toString());

            try {
                Response msgResponse = httpClient.newCall(new Request.Builder().url(messageUrl.toString(crypto)).build()).execute();
                String msgResponseBody = msgResponse.body().string().trim();

                // if we don't receive a 200 ok, it's a network error.
                if (msgResponse.code() != 200 || msgResponseBody.contains("HTTP/1.0 404 File not found")) {
                    throw new Exception("Server did not respond appropriately.");
                }

                msgResponseBody = crypto.decryptFromHexToString(msgResponseBody).trim();

                if (msgResponseBody.startsWith("Error")) {
                    throw new Exception("Error response received from server.");
                }

                if (messageUrl.getAckMessage() != null) {
                    EventBus.getDefault().post(new Events.AckMessage(messageUrl.getAckMessage()));
                }

                // in case of a EULA request, send an event with the contents.
                if (messageUrl.getMessage().equals(MessageUrl.MESSAGE_GET_EULA)) {
                    EventBus.getDefault().post(new Events.EulaReceived(msgResponseBody));
                    return;
                }
                if (messageUrl.getMessage().equals(MessageUrl.MESSAGE_GET_HELP)) {
                    EventBus.getDefault().post(new Events.HelpReceived(msgResponseBody));
                }

                if (messageUrl.getMessage().equals(MessageUrl.MESSAGE_SOS) && msgResponseBody.startsWith("OK")) {
                    EventBus.getDefault().post(new Events.SosMessageSent());
                }

                // we got back something other than an ok. This should be our agentSettings.
                if (!msgResponseBody.startsWith("OK") && messageUrl.getMessage().equals(MessageUrl.MESSAGE_APP_OPENED)) {
                    //Timber.d("Got agent settings!");
                    try {
                        AgentSettings agentSettings = AgentSettings.parse(application, msgResponseBody);
                        if (agentSettings != null) {
                            //Timber.d("DDD: Received default screen: " + agentSettings.getScreen());
                            boolean committed = sharedPreferences.edit().putString(Givens.SETTINGS, Crypto.encode(crypto.encrypt(gson.toJson(agentSettings, AgentSettings.class)))).commit();
                            if (committed) {
                                EventBus.getDefault().postSticky(new Events.AgentSettingsAcquired(agentSettings));
                            }
                            // Update profile fields
                            AgentProfile profile = application.getAgentProfile();
                            if (profile != null) {
                                agentSettings.setProfileFields(profile);
                                EventBus.getDefault().postSticky(new Events.AgentProfileAcquired(profile));
                            }
                        }
                    } catch (Exception e) {
                        Timber.w(e, "Attempt to parse response failed.");
                    }
                }

            } catch (Exception e) {
                if (!Network.isNetworkException(e)) {
                    Timber.w(e, "A problem occurred attempting to send a message: " + messageUrl.toString());
                }

            }

            if (setSetupFieldUrl != null) {
                Intent setupFieldIntent = new Intent(this, SetSetupFieldService.class);
                setupFieldIntent.putExtra(Givens.MESSAGE, setSetupFieldUrl);
                startService(setupFieldIntent);
            }

        } catch (Exception f) {
            Timber.w(f, "A serious problem occurred in the MessageService");
        }

    }


}
