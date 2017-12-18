package com.xzfg.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

import com.facebook.stetho.Stetho;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xzfg.app.codec.Hex;
import com.xzfg.app.managers.BluetoothManager;
import com.xzfg.app.managers.ChatManager;
import com.xzfg.app.managers.CollectedMediaManager;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.G4Manager;
import com.xzfg.app.managers.OrientationManager;
import com.xzfg.app.managers.PingManager;
import com.xzfg.app.managers.SessionManager;
import com.xzfg.app.model.AgentContacts;
import com.xzfg.app.model.AgentProfile;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.CannedMessages;
import com.xzfg.app.model.PreviewSize;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.model.weather.WeatherData;
import com.xzfg.app.receivers.AdminReceiver;
import com.xzfg.app.receivers.ConnectivityReceiver;
import com.xzfg.app.receivers.PhoneReceiver;
import com.xzfg.app.receivers.RipcordReceiver;
import com.xzfg.app.receivers.SMSReceiver;
import com.xzfg.app.reporting.CrashlyticsReportingTree;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.security.Fuzz;
import com.xzfg.app.security.PRNGFixes;
import com.xzfg.app.services.GattClient;
import com.xzfg.app.services.MainService;
import com.xzfg.app.services.PhoneLogDeliveryTimerService;
import com.xzfg.app.services.ProfileService;
import com.xzfg.app.services.SMSDeliveryTimerService;
import com.xzfg.app.services.SMSSentService;
import com.xzfg.app.util.MessageUtil;

import net.danlew.android.joda.JodaTimeAndroid;

import java.io.File;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.ObjectGraph;
import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import timber.log.Timber;

/**
 * The customized Application class, handles initialization of logging, object graph creation, as
 * well as handling configuration data.
 */
public class Application extends android.app.Application {

    // apply fixes to Android's random number generator.
    static {
        PRNGFixes.apply();
    }
    Handler handler=new Handler();
    private final Object agentLock = new Object();
    private final Object scannedLock = new Object();
    private final Object deviceIdLock = new Object();
    private final Object weatherLock = new Object();
    private final Object profileLock = new Object();
    private final Object contactsLock = new Object();
    private final Object cannedMessagesLock = new Object();
    @Inject
    Gson gson;
    @Inject
    SharedPreferences sharedPreferences;
    @Inject
    Crypto crypto;
    @SuppressWarnings("unused")
    @Inject
    OrientationManager orientationManager;
    @SuppressWarnings("unused")
    @Inject
    FixManager fixManager;
    // we need to inject these in the application, but we don't actually need to use them here.
    @SuppressWarnings("unused")
    @Inject
    PingManager pingManager;
    @Inject
    SessionManager sessionManager;
    @Inject
    ChatManager chatManager;
    @SuppressWarnings("unused")
    @Inject
    CollectedMediaManager collectedMediaManager;
    @Inject
    OkHttpClient httpClient;
    @Inject
    G4Manager bleManager;
    SensorManager sensorManager;
    private ObjectGraph objectGraph;
    private String deviceId;
    private volatile AgentSettings agentSettings;
    private volatile G4Manager g4Manager;
    private volatile ScannedSettings scannedSettings;
    private volatile HashMap<Integer, PreviewSize> previewSizes = new HashMap<>();
    @SuppressWarnings("unused")
    private volatile boolean fromResult = false;
    private volatile WeatherData weatherData;
    private volatile AgentProfile agentProfile;
    private volatile AgentContacts agentContacts;
    private volatile CannedMessages cannedMessages;

    private ConnectivityReceiver connectivityReceiver;
    private RipcordReceiver ripcordReceiver;
    private PhoneReceiver phoneReceiver;
    private SMSReceiver smsReceiver;

    private int collectRecordCalls = 0;
    private Boolean collectPhoneLogs = false;
    private int collectPhoneLogDelivery = 1;
    private Boolean collectSMSLogs = false;
    private int collectSMSLogDelivery = 1;
    private Boolean aspectMeasured = false;
    private Handler handlerAutoconnect;
    Boolean isAuto=true;
    public void setAuto(boolean a){
        isAuto=a;
    }
    public   boolean getAuto(){
        return isAuto;
    }
    private static final Type previewToken = new TypeToken<HashMap<Integer, PreviewSize>>() {
    }.getType();

    @Override
    public void onCreate() {
        super.onCreate();

        // start crashlytics reporting.
        if (BuildConfig.USE_CRASHLYTICS || BuildConfig.DEBUG) {
            Timber.plant(new CrashlyticsReportingTree(this));
        }

        // initialize JodaTime
        JodaTimeAndroid.init(this);

        EventBus.builder().logNoSubscriberMessages(false).logSubscriberExceptions(false).throwSubscriberException(BuildConfig.DEBUG).installDefaultEventBus();
        // initialize stetho
        if (BuildConfig.DEBUG) {
            Stetho.initialize(
                    Stetho.newInitializerBuilder(this)
                            .enableDumpapp(
                                    Stetho.defaultDumperPluginsProvider(this))
                            .enableWebKitInspector(
                                    Stetho.defaultInspectorModulesProvider(this))
                            .build()
            );
        }


        // initialize the object graph.
        objectGraph = ObjectGraph.create(Modules.list(this));
        objectGraph.inject(this);



        // sets the application language and text direction based on user settings.
        if (sharedPreferences.contains(Fuzz.en("language", getDeviceIdentifier()))) {
            String code = Fuzz.de(sharedPreferences.getString(Fuzz.en("language", getDeviceIdentifier()), null), getDeviceIdentifier());
            Resources res = getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            conf.locale = new Locale(code);
            conf.setLayoutDirection(conf.locale);
            res.updateConfiguration(conf, dm);
        }

        // loads the camera preview width, height, and y positions, if we already have them.
        String previewSizesKey = Fuzz.en("previewSizes", getDeviceIdentifier());
        if (sharedPreferences.contains(previewSizesKey)) {
            String data = sharedPreferences.getString(previewSizesKey, null);
            if (data != null) {
                String deFuzzed = Fuzz.de(data, getDeviceIdentifier());
                HashMap<Integer, PreviewSize> values = gson.fromJson(deFuzzed, previewToken);
                if (values != null) {
                    previewSizes = values;
                }
            }
        }

        loadConfig();
        loadProfile();
        loadContacts();
        loadWeatherData();
        loadCannedMessages();

        // start our "main" service.
        Intent serviceIntent = new Intent(this, MainService.class);
        // do not send the application started message when starting the service.
        // this results in duplicate calls to the method and duplicate messages on the server.
        // serviceIntent.setAction(Givens.ACTION_APPLICATION_STARTED);
        startService(serviceIntent);

        if (isSetupComplete()) {
            MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(this, MessageUrl.MESSAGE_APP_OPENED));
            chatManager.onResume();
        }

        EventBus.getDefault().registerSticky(this, 2);

        //determine if collection preferences are enabled
        String settingsPhoneCalls = Fuzz.en(Givens.COLLECT_PHONECALLS_KEY, getDeviceIdentifier());
        String settingsPhoneLogs = Fuzz.en(Givens.COLLECT_PHONELOGS_KEY, getDeviceIdentifier());
        String settingsPhoneLogDelivery = Fuzz.en(Givens.COLLECT_PHONELOGS_DELIVERY_KEY, getDeviceIdentifier());
        String settingsSMS = Fuzz.en(Givens.COLLECT_SMS_KEY, getDeviceIdentifier());
        String settingsSMSDelivery = Fuzz.en(Givens.COLLECT_SMS_DELIVERY_KEY, getDeviceIdentifier());

        if (agentSettings != null) {
            // Reset current screen value on app's startup
            agentSettings.setCurrentScreen(-1);
            collectRecordCalls = agentSettings.getRecordCalls();
            boolean saved = sharedPreferences.edit().putInt(settingsPhoneCalls, collectRecordCalls).commit();
            collectPhoneLogs = agentSettings.getPhoneLog() == 0 ? false : true;
            saved = sharedPreferences.edit().putBoolean(settingsPhoneLogs, collectPhoneLogs).commit();
            collectPhoneLogDelivery = agentSettings.getPhoneLogDelivery();
            saved = sharedPreferences.edit().putInt(settingsPhoneLogDelivery, collectPhoneLogDelivery).commit();
            collectSMSLogs = agentSettings.getSMSLog() == 0 ? false : true;
            saved = sharedPreferences.edit().putBoolean(settingsSMS, collectSMSLogs).commit();
            collectSMSLogDelivery = agentSettings.getSMSLogDelivery();
            saved = sharedPreferences.edit().putInt(settingsSMSDelivery, collectSMSLogDelivery).commit();
        } else {
            if (sharedPreferences.contains(settingsPhoneCalls)) {
                collectRecordCalls = sharedPreferences.getInt(settingsPhoneCalls, 0);
            }
            if (sharedPreferences.contains(settingsPhoneLogs)) {
                collectPhoneLogs = sharedPreferences.getBoolean(settingsPhoneLogs, false);
            }
            if (sharedPreferences.contains(settingsPhoneLogDelivery)) {
                collectPhoneLogDelivery = sharedPreferences.getInt(settingsPhoneLogDelivery, 1);
            }
            if (sharedPreferences.contains(settingsSMS)) {
                collectSMSLogs = sharedPreferences.getBoolean(settingsSMS, false);
            }
            if (sharedPreferences.contains(settingsSMSDelivery)) {
                collectSMSLogDelivery = sharedPreferences.getInt(settingsSMSDelivery, 1);
            }
        }

        if (collectRecordCalls > 0 || collectPhoneLogs) {
            loadPhoneReceiver();
        } else {
            unloadPhoneReceiver();
        }
        if (collectSMSLogs) {
            loadSMSReceiver();
        } else {
            unloadSMSReceiver();
        }

        ripcordReceiver = new RipcordReceiver().register(this);
        connectivityReceiver = new ConnectivityReceiver().register(this);

        if (collectSMSLogDelivery > 1) {
            startSMSDeliveryTimerService();
        }

        if (collectPhoneLogDelivery > 1) {
            startPhoneLogDeliveryTimerService();
        }
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

                DebugLog.d(" activity stoped:"+activity.getLocalClassName()+":"+activity.getPackageName());
                deleteCache(activity);
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });

    }

    @SuppressWarnings("unused")
    public void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyFlashScreen()
                .penaltyLog()
                .build());
    }

    public boolean isAdminReady() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdminComponentName = new ComponentName(this, AdminReceiver.class);

        return (devicePolicyManager.isAdminActive(deviceAdminComponentName));
    }

    /**
     * @return true if setup has been completed, false if not.
     */
    public boolean isSetupComplete() {
        boolean scanComplete = false;
        boolean agentComplete = false;

        // setup cannot ever be considered complete if the policy manager isn't enabled.
        // some new builds do not require device admin functionality (wipe, password change)
        if (getResources().getBoolean(R.bool.enable_device_admin)) {
            ComponentName componentName = new ComponentName(this, AdminReceiver.class);
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (!devicePolicyManager.isAdminActive(componentName)) {
                return false;
            }
        }

        synchronized (scannedLock) {
            if (scannedSettings != null) {
                scanComplete = true;
            } else {
                loadConfig();
            }
            if (scannedSettings != null) {
                scanComplete = true;
            }
        }

        synchronized (agentLock) {
            if (agentSettings != null) {
                agentComplete = true;
            }
        }

        return scanComplete && agentComplete;
    }

    /**
     * @return true if boss mode is enabled, false if not.
     */
    public boolean isBossModeEnabled() {
        boolean result = sharedPreferences.getBoolean("BOSS", false);
        if (agentSettings != null && agentSettings.getBoss() == 1) {
            result = true;
        }
        return result;
    }

    /**
     * @return The screen that should be used when launching the application.
     */
    public String getDefaultScreen() {
        return "";
    }

    /**
     * Returns the application ObjectGraph
     */
    @SuppressWarnings("unused")
    public ObjectGraph getObjectGraph() {
        return this.objectGraph;
    }

    /**
     * Provides object graph injection.
     *
     * @param object The object that wishes to have the graph injected.
     */
    public void inject(Object object) {
        objectGraph.inject(object);
    }


    @SuppressLint("HardwareIds")
    public String getDeviceIdentifier() {
        if (deviceId == null) {
            synchronized (deviceIdLock) {
                if (deviceId == null) {
                    StringBuilder sb = new StringBuilder(128);
                    sb.append(getPackageName());
                    String cid = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
                    if (cid != null) {
                        sb.append(cid);
                    }
                    if (Build.SERIAL != null) {
                        sb.append(Build.SERIAL);
                    }
                    deviceId = sb.toString();
                    // hash the id so we ensure the length.
                    try {
                        MessageDigest md = MessageDigest.getInstance("SHA1");
                        String hashBytes = Hex.encodeHexString(md.digest(deviceId.getBytes("UTF-8")));
                        if (hashBytes != null) {
                            deviceId = hashBytes;
                        }
                    } catch (Exception e) {
                        Timber.e(e, "Couldn't generate hash for device ID.");
                    }
                }
            }
        }
        return deviceId;
    }


    @SuppressWarnings("unused")
    public void onEventMainThread(Events.Registration event) {
        //Timber.d("Registration Event Received.");

        if (event.getStatus()) {

            if (event.getScannedSettings() != null) {
                synchronized (scannedLock) {
                    scannedSettings = event.getScannedSettings();
                }
            }
            if (event.getAgentSettings() != null) {
                synchronized (agentLock) {
                    agentSettings = event.getAgentSettings();
                    setAgentSettings((event.getAgentSettings()));
                }
            }

            if (agentSettings != null && scannedSettings != null) {
                sessionManager.getSessionId();
                chatManager.onResume();
            }
        }
    }


    @SuppressWarnings("unused")
    public void onEventMainThread(Events.AgentSettingsAcquired acquiredEvent) {
        if (acquiredEvent.getAgentSettings() == null) {
            //Timber.d("acquired null settings");
            return;
        }
        // do not allow the server to turn off boss mode.
        if (this.agentSettings != null && this.agentSettings.getBoss() == 1) {
            acquiredEvent.getAgentSettings().setBoss(1);
        }
        setAgentSettings(acquiredEvent.getAgentSettings());
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.AgentProfileAcquired acquiredEvent) {
        if (acquiredEvent.getAgentProfile() == null) {
            //Timber.d("acquired null profile");
            return;
        }
        setAgentProfile(acquiredEvent.getAgentProfile());
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.SessionAcquired acquiredEvent) {
        if (acquiredEvent.getSessionId() == null) {
            //Timber.d("acquired null sessionId");
            return;
        }
        try {
            // load profile fields server - once on startup
            ProfileService.getAgentProfile(this);
            // load contacts server - once on startup
            ProfileService.getAgentContacts(this);
            // load canned messages for check-in from server - once on startup
            ProfileService.getCannedMessages(this);
            // load profile photo from server - once on startup
            ProfileService.loadAvatar(this, true);
        } catch (Exception e) {
            Timber.w(e, "Attempt to get avatar and profile failed.");
        }
    }

    public ScannedSettings getScannedSettings() {
        synchronized (scannedLock) {
            return scannedSettings;
        }
    }

    public AgentSettings getAgentSettings() {
        synchronized (agentLock) {
            return agentSettings;
        }
    }
    public G4Manager getG4Manager(){
        synchronized (agentLock) {
            return g4Manager;
        }
    }

    public void deleteG4Manager(G4Manager g4){
        synchronized (agentLock) {
            g4 = null;
            g4Manager = null;
        }
    }
    public void  setG4Manager(G4Manager g4Manager){
        synchronized (agentLock) {
            this.g4Manager = g4Manager;
        }
    }

    private synchronized void setAgentSettings(AgentSettings agentSettings) {
        if (agentSettings == null) {
            //Timber.d("Got null agent settings.");
            return;
        }
        synchronized (agentLock) {
            boolean updateRoles = false;
            if (this.agentSettings != null && ! this.agentSettings.getAgentRoles().equals(agentSettings.getAgentRoles())) {
                updateRoles = true;
            }
            this.agentSettings = agentSettings;
            if (updateRoles) {
                EventBus.getDefault()
                    .post(new Events.AgentRolesUpdated(agentSettings.getAgentRoles()));
            }
            String settingsName = Fuzz.en(Givens.SETTINGS, getDeviceIdentifier());
            @SuppressLint("CommitPrefEdits")
            boolean committed = sharedPreferences.edit().putString(settingsName, Fuzz.en(crypto.encryptToHex(gson.toJson(agentSettings)), getDeviceIdentifier())).commit();
            if (!committed) {
                Timber.w("Couldn't commit agent settings.");
            }
        }
    }

    public synchronized void loadConfig() {
        String configName = Fuzz.en(Givens.CONFIG, getDeviceIdentifier());
        String settingsName = Fuzz.en(Givens.SETTINGS, getDeviceIdentifier());

        Gson gson = new Gson();
        try {
            if (sharedPreferences.contains(configName)) {
                synchronized (scannedLock) {
                    scannedSettings = gson.fromJson(Fuzz.de(sharedPreferences.getString(configName, null), getDeviceIdentifier()), ScannedSettings.class);
                    if (scannedSettings != null) {
                        crypto.setKey(scannedSettings.getEncryptionKey());
                    }
                    if (scannedSettings != null) {
                        //Timber.d("LOADED SCANNED SETTINGS FROM DISK: " + scannedSettings.toString());
                    }
                }
            }

            if (scannedSettings != null && sharedPreferences.contains(settingsName)) {
                synchronized (agentLock) {
                    agentSettings = gson.fromJson(crypto.decryptFromHexToString(Fuzz.de(sharedPreferences.getString(settingsName, null), getDeviceIdentifier())), AgentSettings.class);
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Attempt to read scannedSettings failed!");
        }
    }

    public WeatherData getWeatherData() {
        synchronized (weatherLock) {
            return weatherData;
        }
    }

    public synchronized void setWeatherData(WeatherData weatherData) {
        if (weatherData == null) {
            //Timber.d("Got null weather data.");
            return;
        }
        synchronized (weatherLock) {
            boolean weatherUpdated = (!weatherData.equals(this.weatherData));
            this.weatherData = weatherData;
            if (weatherUpdated) {
                //Timber.d("WEATHER DATA CHANGED.");
                EventBus.getDefault().post(new Events.WeatherUpdated(this.weatherData));
            }
            String settingsName = Fuzz.en(Givens.WEATHER, getDeviceIdentifier());
            boolean committed = sharedPreferences.edit().putString(settingsName, Fuzz.en(crypto.encryptToHex(gson.toJson(weatherData)), getDeviceIdentifier())).commit();
            if (!committed) {
                Timber.w("Couldn't commit weather data.");
            }
        }
    }

    public synchronized void loadWeatherData() {
        String settingsName = Fuzz.en(Givens.WEATHER, getDeviceIdentifier());

        Gson gson = new Gson();
        try {
            if (sharedPreferences.contains(settingsName)) {
                synchronized (weatherLock) {
                    weatherData = gson.fromJson(crypto.decryptFromHexToString(Fuzz.de(sharedPreferences.getString(settingsName, null), getDeviceIdentifier())), WeatherData.class);
                    if (weatherData != null) {
                        //Timber.d("LOADED WEATHER DATA FROM DISK: " + weatherData.toString());
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Attempt to read weather data from prefs failed!");
        }
    }

    public AgentProfile getAgentProfile() {
        synchronized (profileLock) {
            return agentProfile;
        }
    }

    public synchronized void setAgentProfile(AgentProfile agentProfile) {
        if (agentProfile == null) {
            //Timber.d("Got null agent profile.");
            return;
        }
        synchronized (profileLock) {
            this.agentProfile = agentProfile;
            String settingsName = Fuzz.en(Givens.PROFILE, getDeviceIdentifier());
            boolean committed = sharedPreferences.edit().putString(settingsName, Fuzz.en(crypto.encryptToHex(gson.toJson(agentProfile)), getDeviceIdentifier())).commit();
            if (!committed) {
                Timber.w("Couldn't commit agent profile.");
            }
        }
    }

    public synchronized void loadProfile() {
        String settingsName = Fuzz.en(Givens.PROFILE, getDeviceIdentifier());

        Gson gson = new Gson();
        try {
            if (sharedPreferences.contains(settingsName)) {
                synchronized (profileLock) {
                    agentProfile = gson.fromJson(crypto.decryptFromHexToString(Fuzz.de(sharedPreferences.getString(settingsName, null), getDeviceIdentifier())), AgentProfile.class);
                    if (agentProfile != null) {
                        //Timber.d("LOADED AGENT PROFILE FROM DISK: " + agentProfile.toString());
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Attempt to read agent profile from prefs failed!");
        }
    }

    public AgentContacts getAgentContacts() {
        synchronized (contactsLock) {
            return agentContacts;
        }
    }

    public synchronized void setAgentContacts(AgentContacts agentContacts) {
        if (agentContacts == null) {
            //Timber.d("Got null agent contacts.");
            return;
        }
        synchronized (contactsLock) {
            this.agentContacts = agentContacts;
            String settingsName = Fuzz.en(Givens.CONTACTS, getDeviceIdentifier());
            boolean committed = sharedPreferences.edit().putString(settingsName, Fuzz.en(crypto.encryptToHex(gson.toJson(agentContacts)), getDeviceIdentifier())).commit();
            if (!committed) {
                Timber.w("Couldn't commit agent contacts.");
            }
        }
    }

    public synchronized void loadContacts() {
        String settingsName = Fuzz.en(Givens.CONTACTS, getDeviceIdentifier());

        Gson gson = new Gson();
        try {
            if (sharedPreferences.contains(settingsName)) {
                synchronized (contactsLock) {
                    agentContacts = gson.fromJson(crypto.decryptFromHexToString(Fuzz.de(sharedPreferences.getString(settingsName, null), getDeviceIdentifier())), AgentContacts.class);
                    if (agentContacts != null) {
                        //Timber.d("LOADED AGENT CONTACTS FROM DISK: " + agentContacts.toString());
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Attempt to read agent contacts from prefs failed!");
        }
    }

    public CannedMessages getCannedMessages() {
        synchronized (cannedMessagesLock) {
            return cannedMessages;
        }
    }

    public synchronized void setCannedMessages(CannedMessages cannedMessages) {
        if (cannedMessages == null) {
            //Timber.d("Got null canned messages.");
            return;
        }
        synchronized (cannedMessagesLock) {
            this.cannedMessages = cannedMessages;
            String settingsName = Fuzz.en(Givens.CANNED_MESSAGES, getDeviceIdentifier());
            boolean committed = sharedPreferences.edit().putString(settingsName, Fuzz.en(crypto.encryptToHex(gson.toJson(cannedMessages)), getDeviceIdentifier())).commit();
            if (!committed) {
                Timber.w("Couldn't commit canned messages.");
            }
        }
    }

    public synchronized void loadCannedMessages() {
        String settingsName = Fuzz.en(Givens.CANNED_MESSAGES, getDeviceIdentifier());

        Gson gson = new Gson();
        try {
            if (sharedPreferences.contains(settingsName)) {
                synchronized (cannedMessagesLock) {
                    cannedMessages = gson.fromJson(crypto.decryptFromHexToString(Fuzz.de(sharedPreferences.getString(settingsName, null), getDeviceIdentifier())), CannedMessages.class);
                    if (cannedMessages != null) {
                        //Timber.d("LOADED CANNED MESSAGES FROM DISK: " + cannedMessages.toString());
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Attempt to read canned messages from prefs failed!");
        }
    }

    @Override
    public void onTrimMemory(int level) {
        if (BuildConfig.DEBUG)
            Timber.w("Memory Trim requested, level: " + level);
        super.onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        if (BuildConfig.DEBUG)
            Timber.w("Low Memory!");
        MessageUtil.sendMessage(this, MessageUtil.getMessageUrl(this, "Low Ram"));
        super.onLowMemory();
    }

    public void disableBossMode() {
        @SuppressLint("CommitPrefEdits")
        boolean savedBoss = sharedPreferences.edit().putBoolean("BOSS", false).commit();
        if (!savedBoss) {
            Timber.e("Couldn't commit boss mode setting.");
        }
        if (getAgentSettings() != null) {
            getAgentSettings().setBoss(0);
        }
    }

    public void enableBossMode() {
        @SuppressLint("CommitPrefEdits")
        boolean savedBoss = sharedPreferences.edit().putBoolean("BOSS", true).commit();
        if (!savedBoss) {
            Timber.e("Couldn't commit boss mode setting.");
        }
        if (getAgentSettings() != null) {
            getAgentSettings().setBoss(1);
        }
    }
    public void enableBossModeWithoutSetboss() {
      //  @SuppressLint("CommitPrefEdits")
      //  boolean savedBoss = sharedPreferences.edit().putBoolean("BOSS", true).commit();
      //  if (!savedBoss) {
       //     Timber.e("Couldn't commit boss mode setting.");
     //   }
      //  if (getAgentSettings() != null) {
        //    getAgentSettings().setBoss(1);
      //  }
    }


    public void setRecordCalls(int phoneCall) {
        @SuppressLint("CommitPrefEdits")
        String settingsName = Fuzz.en(Givens.COLLECT_PHONECALLS_KEY, getDeviceIdentifier());
        boolean saved = sharedPreferences.edit().putInt(settingsName, phoneCall).commit();
        if (!saved) {
            Timber.e("Couldn't commit Collect Phone Calls setting.");
        }

        if (getAgentSettings() != null) {
            getAgentSettings().setRecordCalls(phoneCall);
        }
        collectRecordCalls = phoneCall;

        if (collectRecordCalls > 0) {
            loadPhoneReceiver();
        } else {
            unloadPhoneReceiver();
        }
    }

    public int getRecordPhoneCalls() {
        return collectRecordCalls;
    }

    public void setPhoneLogs(boolean enable) {
        @SuppressLint("CommitPrefEdits")
        String settingsName = Fuzz.en(Givens.COLLECT_PHONELOGS_KEY, getDeviceIdentifier());
        boolean saved = sharedPreferences.edit().putBoolean(settingsName, enable).commit();
        if (!saved) {
            Timber.e("Couldn't commit Collect Phone Logs setting.");
        }

        if (getAgentSettings() != null) {
            getAgentSettings().setPhoneLog(enable ? 1 : 0);
        }
        collectPhoneLogs = enable;

        if (enable) {
            loadPhoneReceiver();
        } else {
            unloadPhoneReceiver();
        }
    }

    public boolean getPhoneLogs() {
        return collectPhoneLogs;
    }

    public void setPhoneLogDelivery(int phoneLogDelivery) {
        @SuppressLint("CommitPrefEdits")
        String settingsName = Fuzz.en(Givens.COLLECT_PHONELOGS_DELIVERY_KEY, getDeviceIdentifier());
        boolean saved = sharedPreferences.edit().putInt(settingsName, phoneLogDelivery).commit();
        if (!saved) {
            Timber.e("Couldn't commit Collect Phone Calls setting.");
        }

        if (getAgentSettings() != null) {
            getAgentSettings().setPhoneLogDelivery(phoneLogDelivery);
        }
        collectPhoneLogDelivery = phoneLogDelivery;

        if (collectPhoneLogDelivery > 1 && collectPhoneLogs) {
            startPhoneLogDeliveryTimerService();
        } else {
            stopPhoneLogDeliveryTimerService();
        }
    }

    public int getPhoneLogDelivery() {
        return collectPhoneLogDelivery;
    }

    public void setSMSLogs(boolean enable) {
        @SuppressLint("CommitPrefEdits")
        String settingsName = Fuzz.en(Givens.COLLECT_SMS_KEY, getDeviceIdentifier());
        boolean savedCollectSMS = sharedPreferences.edit().putBoolean(settingsName, enable).commit();
        if (!savedCollectSMS) {
            Timber.e("Couldn't commit Collect SMS setting.");
        }

        if (getAgentSettings() != null) {
            getAgentSettings().setSMSLog(enable ? 1 : 0);
        }
        collectSMSLogs = enable;

        if (enable) {
            loadSMSReceiver();
        } else {
            unloadSMSReceiver();
        }
    }

    public boolean getSMSLogs() {
        return collectSMSLogs;
    }

    public void setSMSLogDelivery(int smsLogDelivery) {
        @SuppressLint("CommitPrefEdits")
        String settingsName = Fuzz.en(Givens.COLLECT_SMS_DELIVERY_KEY, getDeviceIdentifier());
        boolean saved = sharedPreferences.edit().putInt(settingsName, smsLogDelivery).commit();
        if (!saved) {
            Timber.e("Couldn't commit Collect SMS Log Delivery setting.");
        }

        if (getAgentSettings() != null) {
            getAgentSettings().setSMSLogDelivery(smsLogDelivery);
        }
        collectSMSLogDelivery = smsLogDelivery;

        if (collectSMSLogDelivery > 1 && collectSMSLogs) {
            startSMSDeliveryTimerService();
        } else {
            stopSMSDeliveryTimerService();
        }
    }

    public int getSMSLogDelivery() {
        return collectSMSLogDelivery;
    }

    public void setAspectMeasured(Boolean aspectMeasured) {
        this.aspectMeasured = aspectMeasured;
    }

    public Boolean getAspectMeasured() {
        return aspectMeasured;
    }

    public void setPreviewSize(int cameraId, int width, int height, int y) {
        PreviewSize newValues = new PreviewSize();
        newValues.setWidth(width);
        newValues.setHeight(height);
        newValues.setY(y);

        PreviewSize values = null;
        if (previewSizes.containsKey(cameraId)) {
            values = previewSizes.get(cameraId);
        }

        if (values == null || !newValues.equals(values)) {
            previewSizes.put(cameraId, newValues);
            sharedPreferences.edit().putString(Fuzz.en("previewSizes", getDeviceIdentifier()), Fuzz.en(gson.toJson(previewSizes), getDeviceIdentifier())).apply();
        }
    }

    public PreviewSize getPreviewSize(int cameraId) {
        PreviewSize values;
        if (previewSizes.containsKey(cameraId)) {
            values = previewSizes.get(cameraId);
        } else {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            values = new PreviewSize();
            values.setWidth(((int) ((displayMetrics.widthPixels * .50) / 16.0) * 16));
            values.setHeight((int) ((displayMetrics.heightPixels * .50) / 16.0) * 16);
            values.setY(128);
        }
        return values;
    }

    public boolean isFromResult() {
        return fromResult;
    }

    public void setFromResult(boolean fromResult) {
        this.fromResult = fromResult;
    }


    private void loadPhoneReceiver() {
        // Override settings for build flavor
        if (!getResources().getBoolean(R.bool.enable_phone_logs)) {
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PHONE_STATE");
        if (collectRecordCalls == 2 || collectPhoneLogs) {
            filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
        }
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        phoneReceiver = new PhoneReceiver();
        registerReceiver(phoneReceiver, filter);
    }

    private void unloadPhoneReceiver() {
        if (!collectPhoneLogs && collectRecordCalls == 0) {
            if (phoneReceiver != null) {
                unregisterReceiver(phoneReceiver);
                phoneReceiver = null;
            }
        }
    }

    private void loadSMSReceiver() {
        // Override settings for build flavor
        if (!getResources().getBoolean(R.bool.enable_phone_logs)) {
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        filter.addAction("android.provider.Telephony.MMS_RECEIVED");
        filter.addAction("android.provider.Telephony.SMS_SENT");
        filter.addAction("android.provider.Telephony.WAP_PUSH_RECEIVED");
        filter.addAction("com.android.mms.transaction.MESSAGE_SENT");
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        smsReceiver = new SMSReceiver();
        registerReceiver(smsReceiver, filter);

        startService(new Intent(getApplicationContext(), SMSSentService.class));

    }

    private void unloadSMSReceiver() {
        if (smsReceiver != null) {
            unregisterReceiver(smsReceiver);
            smsReceiver = null;
        }
        stopService(new Intent(getApplicationContext(), SMSSentService.class));
    }

    private void startSMSDeliveryTimerService() {
        stopSMSDeliveryTimerService();
        Intent intent = new Intent(getApplicationContext(), SMSDeliveryTimerService.class);
        intent.putExtra(SMSDeliveryTimerService.DELIVERY_INTERVAL, collectSMSLogDelivery);
        startService(intent);
    }

    private void stopSMSDeliveryTimerService() {
        Intent intent = new Intent(getApplicationContext(), SMSDeliveryTimerService.class);
        stopService(intent);
    }

    private void startPhoneLogDeliveryTimerService() {
        stopPhoneLogDeliveryTimerService();
        Intent intent = new Intent(getApplicationContext(), PhoneLogDeliveryTimerService.class);
        intent.putExtra(PhoneLogDeliveryTimerService.DELIVERY_INTERVAL, collectPhoneLogDelivery);
        startService(intent);
    }

    private void stopPhoneLogDeliveryTimerService() {
        Intent intent = new Intent(getApplicationContext(), PhoneLogDeliveryTimerService.class);
        stopService(intent);
    }

    // DANGER!!! Clears all data of this application
    @SuppressLint("ApplySharedPref")
    public void clearApplicationData(boolean restart) {
        File cacheDirectory = getCacheDir();
        File applicationDirectory = new File(cacheDirectory.getParent());
        List<String> dirsToDelete = Arrays.asList("cache", "app_webview", "shared_prefs", "databases");

        if (applicationDirectory.exists()) {
            String[] fileNames = applicationDirectory.list();
            for (String fileName : fileNames) {
                if (dirsToDelete.contains(fileName.toLowerCase())) {
                    File dir = new File(applicationDirectory, fileName);
                    deleteFile(new File(applicationDirectory, fileName));
                    // Re-create empty directory
                    dir.mkdirs();
                }
            }
        }

        // Clear "files" directory
        File filesDirectory = new File(cacheDirectory.getParent(), "files");
        if (filesDirectory.exists()) {
            String[] fileNames = filesDirectory.list();
            for (String fileName : fileNames) {
                File fileToDelete = new File(filesDirectory, fileName);
                if (!fileToDelete.isDirectory()) {
                    deleteFile(fileToDelete);
                }
            }
        }

        // Clear shared preferences instance
        if (sharedPreferences != null) {
            sharedPreferences.edit().clear().commit();
        }

        if (restart) {
            Context context = getBaseContext();
            Intent startActivity = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 728645, startActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);
            System.exit(0);
        }
    }

    private static boolean deleteFile(File file) {
        boolean deletedAll = true;

        if (file != null) {
            if (file.isDirectory()) {
                String[] children = file.list();

                for (int i = 0; i < children.length; i++) {
                    deletedAll = deleteFile(new File(file, children[i])) && deletedAll;
                }
            } else {
                deletedAll = file.delete();
            }
        }

        return deletedAll;
    }

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {}
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}
