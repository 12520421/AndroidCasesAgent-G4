package com.xzfg.app.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.xzfg.app.Application;
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
public class MetaDataService extends BackWakeIntentService {
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

    // DOC Screen - Location State
    public static int locationState = 0<<0;

    final int NETWORK_PROVIDER_ENABLED = 1<<5;
    final int GPS_PROVIDER_ENABLED = 1<<4;
    final int ACCESS_FINE_LOCATION = 1<<3;
    final int ACCESS_COARSE_LOCATION = 1<<2;
    final int LOCATION_MODE_HIGH_ACCURACY = 1<<1;
    final int LOCATION_SERVICE_ON = 1<<0;

    // DOC Screen - Permissions
    public static int permissions = 0<<0;
    final int CALL_PHONE = 1<<9;
    final int READ_CONTACTS =1<<8;
    final int SMS_RECEIVE_MMS=1<<7;
    final int SMS_RECEIVE = 1<<6;
    final int SMS_SEND = 1<<5;
    final int RECORD_AUDIO = 1<<4;
    final int CAMERA = 1<<3;
    final int STORAGE = 1<<2;
    final int ALERT_WINDOW = 1<<1;
    final int READ_PHONE_STATE =1<<0;


    //
    public MetaDataService() {
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
    public static boolean checkHighAccuracyLocationMode(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            //Equal or higher than API 19/KitKat
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
                if (locationMode == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY){
                    return true;
                }
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
        }else{
            //Lower than API 19
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

            if (locationProviders.contains(LocationManager.GPS_PROVIDER) && locationProviders.contains(LocationManager.NETWORK_PROVIDER)){
                return true;
            }
        }
        return false;
    }
    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }
    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    @Override
    protected void doWork(Intent intent) {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            // LOCATION STATE
            int local_LocationState = 0<<0;
            if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                local_LocationState = local_LocationState|GPS_PROVIDER_ENABLED;
            }
            if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            {
                local_LocationState = local_LocationState|NETWORK_PROVIDER_ENABLED;
            }
            if(isLocationEnabled(this))
            {
                local_LocationState = local_LocationState|LOCATION_SERVICE_ON;
            }
            if(checkHighAccuracyLocationMode(this)){
                local_LocationState = local_LocationState|LOCATION_MODE_HIGH_ACCURACY;
            }


            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                local_LocationState = local_LocationState|ACCESS_COARSE_LOCATION;
            }
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                local_LocationState = local_LocationState|ACCESS_FINE_LOCATION;
            }

            // PERMISSIONS
            int local_permissions = 0<<0;
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
            {
                local_permissions = local_permissions|READ_PHONE_STATE;
            }
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.SYSTEM_ALERT_WINDOW)==PackageManager.PERMISSION_GRANTED)
            {
                local_permissions = local_permissions|ALERT_WINDOW;
            }
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)
            {
                local_permissions = local_permissions|STORAGE;
            }
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)
            {
                local_permissions = local_permissions|CAMERA;
            }
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.SEND_SMS)==PackageManager.PERMISSION_GRANTED)
            {
                local_permissions = local_permissions|SMS_SEND;
            }
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.RECEIVE_SMS)==PackageManager.PERMISSION_GRANTED)
            {
                local_permissions = local_permissions|SMS_RECEIVE;
            }
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.RECEIVE_MMS)==PackageManager.PERMISSION_GRANTED)
            {
                local_permissions = local_permissions|SMS_RECEIVE_MMS;
            }
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED)
            {
                local_permissions = local_permissions|READ_CONTACTS;
            }
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.CALL_PHONE)==PackageManager.PERMISSION_GRANTED)
            {
                local_permissions = local_permissions|CALL_PHONE;
            }

            //
            boolean docNeedSend = false;
            if(local_LocationState != locationState) {
                docNeedSend = true;
                locationState = local_LocationState;
            }
            if(local_permissions == permissions)
            {
                docNeedSend = true;
                permissions = local_permissions;
            }
            if(!docNeedSend) {
                return;
            }

            //
            String location_state = Integer.toHexString(local_LocationState);
            String permission_hex = Integer.toHexString(local_permissions);
            MessageUrl messageUrl = intent.getParcelableExtra(Givens.MESSAGE);
            messageUrl.setMessage("Set MetaData");

            ConcurrentHashMap<String, String> details =  new ConcurrentHashMap<>();
            details.put("PERMISSION",permission_hex);
            details.put("LOCATION_STATE",location_state);
            SetSetupFieldUrl setSetupFieldUrl = null;
            // MessageUrl messageUrl1 = intent.getParcelableExtra("SendMetaData");
            // MessageUrl messageUrl1 = intent.getParcelableExtra("SetMetaData");

            if (messageUrl == null) {
                Timber.w("Received null message url.");
                return;
            }
            messageUrl.setMetaData(details);
            /*if (messageUrl.getMessage().equals("Set MetaData")) {

                ConcurrentHashMap<String, String> details =  new ConcurrentHashMap<>();

                details.put("PERMISSION","3F");
                details.put("LOCATION_STATE",location_state);


                messageUrl.setMetaData(details);
                //  messageUrl1.setMetaData(details1);
                setSetupFieldUrl = new SetSetupFieldUrl(application);

               // MessageUrl messageUrl1 = intent.getParcelableExtra("Send");
               // ConcurrentHashMap<String,String> details1 = new ConcurrentHashMap<String, String>();
               // details1.put("LOCATION_STATE","3FF");
               // details1.put("PERMISSION","3F");
               // messageUrl1.setMetaData(details1);
            }
            else {
                messageUrl.setMessage("Set MetaData");
                ConcurrentHashMap<String, String> details =  new ConcurrentHashMap<>();

                details.put("PERMISSION","3F");
                details.put("LOCATION_STATE",location_state);


                messageUrl.setMetaData(details);

            }*/

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
