package com.xzfg.app.managers;

import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import com.crashlytics.android.Crashlytics;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.model.Poi;
import com.xzfg.app.model.PoiCategories;
import com.xzfg.app.model.PoiCategory;
import com.xzfg.app.model.PoiGroup;
import com.xzfg.app.model.PoiGroups;
import com.xzfg.app.model.Pois;
import com.xzfg.app.model.url.HeaderUrl;
import com.xzfg.app.model.url.PoiUrl;
import com.xzfg.app.model.url.SessionUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.DateTransformer;
import com.xzfg.app.util.Network;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.RegistryMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class PoiManager {

    private static final Object threadLock = new Object();
    private static final int SHORT_INTERVAL = 30 * 1000;
    private static final int LONG_INTERVAL = 60 * 1000;
    @Inject
    Crypto crypto;
    @Inject
    Application application;
    @Inject
    ConnectivityManager connectivityManager;
    @Inject
    SessionManager sessionManager;
    @Inject
    OkHttpClient httpClient;
    @Inject
    FixManager fixManager;
    private volatile PoiCategories categories;
    private volatile List<PoiGroup> groups = Collections.synchronizedList(new LinkedList<PoiGroup>());
    private volatile List<PoiGroup> unRestrictedGroups = Collections.synchronizedList(new LinkedList<PoiGroup>());
    private volatile ConcurrentHashMap<Object, List<Poi>> data = new ConcurrentHashMap<>();

    private UpdateHandlerThread updateHandlerThread;

    private volatile boolean started = false;

    private String selectedId = null;
    private volatile boolean selected = false;

    public PoiManager(Application application) {
        application.inject(this);
        EventBus.getDefault().registerSticky(this);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void onResumeFromUI() {
        started = true;
        onResume();
    }

    public void onPauseFromUI() {
        started = false;
        clearSelectedId();
        onPause();
        groups.clear();
        unRestrictedGroups.clear();
        data.clear();
        categories = null;
        System.gc();
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    public void onEventMainThread(Events.NetworkAvailable networkAvailable) {
        onResume();
    }

    public void onEventMainThread(Events.NetworkStatus status) {
        if (!status.isUp()) {
            onPause();
        } else {
            onResume();
        }
    }


    public void onEventMainThread(Events.Session session) {
        if (session.getSessionId() != null) {
            onResume();
        } else {
            onPause();
        }
    }

    public void onResume() {
        synchronized (threadLock) {
            if (started) {
                if (isConnected()) {
                    if (updateHandlerThread == null) {
                        updateHandlerThread = new UpdateHandlerThread();
                    }
                }
            }
        }
    }

    public void onPause() {
        synchronized (threadLock) {
            if (updateHandlerThread != null) {
                UpdateHandlerThread deadThread = updateHandlerThread;
                updateHandlerThread = null;
                deadThread.kill();
            }
        }
    }

    public List<PoiGroup> getUnrestrictedGroups() {
        if (unRestrictedGroups == null || unRestrictedGroups.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableList(unRestrictedGroups);
    }

    public List<PoiGroup> getGroups() {
        if (groups == null || groups.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableList(groups);
    }

    public List<PoiCategory> getCategories() {
        if (categories == null || categories.getPoiCategories() == null || categories.getPoiCategories().isEmpty()) {
            return null;
        }
        return Collections.unmodifiableList(categories.getPoiCategories());
    }

    public List<Poi> getPois(Object key) {
        if (data.containsKey(key)) {
            List<Poi> records = data.get(key);
            if (records != null && !records.isEmpty()) {
                return Collections.unmodifiableList(records);
            }
        }
        return null;
    }


    public int getInterval() {
        if (selected)
            return SHORT_INTERVAL;
        else
            return LONG_INTERVAL;
    }

    public void clearSelectedId() {
        selectedId = null;
    }

    public void setSelectedId(String groupId) {
        if (groupId == null) {
            selectedId = "null";
        } else {
            selectedId = groupId;
        }
    }

    private class UpdateHandlerThread extends HandlerThread {
        private final Handler mHandler;
        private final DownloadRunnable runnable;
        private volatile boolean alive = true;

        public UpdateHandlerThread() {
            super(UpdateHandlerThread.class.getName(), Givens.THREAD_PRIORITY + android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
            start();
            mHandler = new Handler(getLooper());
            runnable = new DownloadRunnable();
          //  mHandler.post(runnable);
        }

        public void kill() {
            alive = false;
          //  mHandler.removeCallbacks(runnable);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                quitSafely();
            } else {
                quit();
            }

        }

        private class DownloadRunnable /*implements Runnable*/ {
//            private int getDistance() {
//                if (BuildConfig.DEBUG)
//                    return Givens.DEBUG_DISTANCE;
//                else
//                    return Givens.DEFAULT_DISTANCE;
//            }
//
//            private void loadUnRestrictedGroups() throws Exception {
//                HeaderUrl headerUrl = new HeaderUrl(application.getScannedSettings(), application.getString(R.string.poigroups_endpoint), sessionManager.getSessionId());
//                //Timber.d("Calling url: " + headerUrl.toString());
//
//                Response response = httpClient.newCall(new Request.Builder().url(headerUrl.toString(crypto)).build()).execute();
//                String responseBody = response.body().string().trim();
//                response.body().close();
//
//                //Timber.d("Response Body (encrypted): " + responseBody);
//
//                // if we don't receive a 200 ok, it's a network error.
//                if (response == null || response.code() != 200 || responseBody == null || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", headerUrl.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("Server did not respond appropriately.");
//                }
//
//                responseBody = crypto.decryptFromHexToString(responseBody).trim();
//                //Timber.d("Response Body (unencrypted): " + responseBody);
//
//                if (responseBody.startsWith("Invalid Session") || responseBody.contains("<error>Invalid SessionId</error>")) {
//                    EventBus.getDefault().post(new Events.InvalidSession());
//                    return;
//                }
//
//                if (!responseBody.startsWith("<?xml version='1.0' encoding='utf-8' ?>")) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", headerUrl.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("Server did not return xml.");
//                }
//
//                //Timber.d("Header Response Body: " + responseBody);
//
//                RegistryMatcher matcher = new RegistryMatcher();
//                matcher.bind(Date.class, new DateTransformer());
//                Serializer serializer = new Persister(matcher);
//
//                boolean validated = serializer.validate(PoiGroups.class, responseBody);
//                if (!validated) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", headerUrl.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("XML Received Could Not Be Validated.");
//                }
//                PoiGroups serverHeader = serializer.read(PoiGroups.class, responseBody);
//
//                if (serverHeader != null) {
//                    com.xzfg.app.model.Error error = serverHeader.getError();
//                    if (error != null) {
//                        if (error.getMessage() != null) {
//                            if (error.getMessage().equals("Invalid Session Id")) {
//                                EventBus.getDefault().post(new Events.InvalidSession());
//                            }
//                            throw new Exception("Server Error: " + error.getMessage());
//                        } else {
//                            throw new Exception("Server Returned An Error.");
//                        }
//                    }
//
//                    //Timber.d(serverHeader.toString());
//
//                    unRestrictedGroups.clear();
//                    unRestrictedGroups.addAll(serverHeader.getPoiGroups());
//                }
//
//            }
//
//            private void loadGroups(Location location) throws Exception {
//                HeaderUrl headerUrl = new HeaderUrl(application.getScannedSettings(), application.getString(R.string.poigroups_endpoint), sessionManager.getSessionId());
//                if (location != null)
//                    headerUrl.setLocation(location);
//                headerUrl.setDistance(getDistance());
//
//                //Timber.d("Calling url: " + headerUrl.toString());
//
//                Response response = httpClient.newCall(new Request.Builder().url(headerUrl.toString(crypto)).build()).execute();
//                String responseBody = response.body().string().trim();
//                response.body().close();
//
//                //Timber.d("Response Body (encrypted): " + responseBody);
//
//                // if we don't receive a 200 ok, it's a network error.
//                if (response.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", headerUrl.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("Server did not respond appropriately.");
//                }
//
//                responseBody = crypto.decryptFromHexToString(responseBody).trim();
//                //Timber.d("Response Body (unencrypted): " + responseBody);
//
//                if (responseBody.startsWith("Invalid Session") || responseBody.contains("<error>Invalid SessionId</error>")) {
//                    EventBus.getDefault().post(new Events.InvalidSession());
//                    return;
//                }
//
//                if (!responseBody.startsWith("<?xml version='1.0' encoding='utf-8' ?>")) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", headerUrl.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("Server did not return xml.");
//                }
//
//                //Timber.d("Header Response Body: " + responseBody);
//
//                RegistryMatcher matcher = new RegistryMatcher();
//                matcher.bind(Date.class, new DateTransformer());
//                Serializer serializer = new Persister(matcher);
//
//                boolean validated = serializer.validate(PoiGroups.class, responseBody);
//                if (!validated) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", headerUrl.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("XML Received Could Not Be Validated.");
//                }
//                PoiGroups serverHeader = serializer.read(PoiGroups.class, responseBody);
//
//                if (serverHeader != null) {
//                    com.xzfg.app.model.Error error = serverHeader.getError();
//                    if (error != null) {
//                        if (error.getMessage() != null) {
//                            if (error.getMessage().equals("Invalid Session Id")) {
//                                EventBus.getDefault().post(new Events.InvalidSession());
//                            }
//                            throw new Exception("Server Error: " + error.getMessage());
//                        } else {
//                            throw new Exception("Server Returned An Error.");
//                        }
//                    }
//
//                    //Timber.d(serverHeader.toString());
//
//                    groups.clear();
//                    groups.addAll(serverHeader.getPoiGroups());
//                    try {
//                        EventBus.getDefault().postSticky(new Events.PoisHeaderUpdated());
//                        List<PoiGroup> records = serverHeader.getPoiGroups();
//                        List<Object> ids = new ArrayList<>();
//                        for (int i = 0; i < records.size(); i++) {
//                            PoiGroup entry = records.get(i);
//                            if (!alive)
//                                return;
//
//                            ids.add(entry.getId());
//                            try {
//                                if (!PoiManager.this.data.containsKey(entry.getId()) ||
//                                        (
//                                                selectedId != null && ((entry.getId() == null && selectedId.equals("null")) || entry.getId().equals(selectedId))
//                                        )
//                                        ) {
//                                    loadContents(entry, location);
//                                }
//                            } catch (Exception e) {
//                                Timber.w(e, "Couldn't get pois.");
//                            }
//                        }
//
//                        if (!alive)
//                            return;
//
//                        // remove unneeded entries.
//                        if (!ids.isEmpty()) {
//                            for (Object key : data.keySet()) {
//                                if (!ids.contains(key)) {
//                                    data.remove(key);
//                                    EventBus.getDefault().postSticky(new Events.PoisDataUpdated());
//                                }
//                            }
//                        }
//                        EventBus.getDefault().postSticky(new Events.PoisHeaderUpdated());
//                    } catch (Exception e) {
//                        Timber.w(e, "Error getting poi data.");
//                    }
//
//                }
//            }
//
//            private void loadCategories() throws Exception {
//                SessionUrl headerUrl = new SessionUrl(application.getScannedSettings(), application.getString(R.string.poicategories_endpoint), sessionManager.getSessionId());
//                /*
//                if (location != null)
//                    headerUrl.setLocation(location);
//                HashMap<String,String> params = new HashMap<>();
//                params.put("distance",String.valueOf(getDistance()));
//                headerUrl.setParamData(params);
//                */
//
//                //Timber.d("Calling url: " + headerUrl.toString());
//
//                Response response = httpClient.newCall(new Request.Builder().url(headerUrl.toString(crypto)).build()).execute();
//                String responseBody = response.body().string().trim();
//                response.body().close();
//
//                //Timber.d("Response Body (encrypted): " + responseBody);
//
//                // if we don't receive a 200 ok, it's a network error.
//                if (response.code() != 200 || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", headerUrl.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("Server did not respond appropriately.");
//                }
//
//                responseBody = crypto.decryptFromHexToString(responseBody).trim();
//                //Timber.d("Response Body (unencrypted): " + responseBody);
//
//                if (responseBody.startsWith("Invalid Session") || responseBody.contains("<error>Invalid SessionId</error>")) {
//                    EventBus.getDefault().post(new Events.InvalidSession());
//                    return;
//                }
//
//                if (!responseBody.startsWith("<?xml version='1.0' encoding='utf-8' ?>")) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", headerUrl.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("Server did not return xml.");
//                }
//
//                //Timber.d("Header Response Body: " + responseBody);
//
//                RegistryMatcher matcher = new RegistryMatcher();
//                matcher.bind(Date.class, new DateTransformer());
//                Serializer serializer = new Persister(matcher);
//
//                boolean validated = serializer.validate(PoiCategories.class, responseBody);
//                if (!validated) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", headerUrl.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("XML Received Could Not Be Validated.");
//                }
//                PoiCategories serverHeader = serializer.read(PoiCategories.class, responseBody);
//
//                if (serverHeader != null) {
//                    com.xzfg.app.model.Error error = serverHeader.getError();
//                    if (error != null) {
//                        if (error.getMessage() != null) {
//                            if (error.getMessage().equals("Invalid Session Id")) {
//                                EventBus.getDefault().post(new Events.InvalidSession());
//                            }
//                            throw new Exception("Server Error: " + error.getMessage());
//                        } else {
//                            throw new Exception("Server Returned An Error.");
//                        }
//                    }
//
//                    categories = serverHeader;
//                }
//
//
//            }
//
//            private void loadContents(PoiGroup record, Location location) throws Exception {
//                loadContents(record, location, true);
//            }
//
//            private void loadContents(PoiGroup record, Location location, boolean retry) throws Exception {
//
//                PoiUrl url = new PoiUrl(application.getScannedSettings(), application.getString(R.string.pois_endpoint), sessionManager.getSessionId());
//                if (record.getId() == null || record.getId().isEmpty()) {
//                    url.setGroupId("-1");
//                } else {
//                    url.setGroupId(record.getId());
//                }
//                if (location != null)
//                    url.setLocation(location);
//                url.setDistance(getDistance());
//
//                if (!alive)
//                    return;
//                //Timber.d("Calling url: " + url.toString());
//                Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
//                String responseBody = response.body().string().trim();
//                response.body().close();
//
//                //Timber.d("Response Body (encrypted): " + responseBody);
//
//                // if we don't receive a 200 ok, it's a network error.
//                if (response.code() != 200 || responseBody.isEmpty() || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", url.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("Server did not respond appropriately.");
//                }
//
//                responseBody = crypto.decryptFromHexToString(responseBody).trim();
//                //Timber.d("Response Body (unencrypted): " + responseBody);
//
//                if (responseBody.startsWith("Invalid Session") || responseBody.contains("<error>Invalid SessionId</error>")) {
//                    EventBus.getDefault().post(new Events.InvalidSession());
//                    return;
//                }
//
//                if (!responseBody.startsWith("<?xml version='1.0' encoding='utf-8' ?>")) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", url.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("Server did not return xml. Body: " + responseBody);
//                }
//
//                //Timber.d("Content Response Body: " + responseBody);
//
//                RegistryMatcher matcher = new RegistryMatcher();
//                matcher.bind(Date.class, new DateTransformer());
//                Serializer serializer = new Persister(matcher);
//
//                boolean validated = serializer.validate(Pois.class, responseBody);
//                if (!validated) {
//                    if (BuildConfig.DEBUG) {
//                        Crashlytics.setString("Url", url.toString());
//                        Crashlytics.setLong("Response Code", response.code());
//                        Crashlytics.setString("Response", responseBody);
//                    }
//                    throw new Exception("XML Received Could Not Be Validated.");
//                }
//                Pois serverContent = serializer.read(Pois.class, responseBody);
//
//                if (serverContent != null) {
//                    com.xzfg.app.model.Error error = serverContent.getError();
//                    if (error != null) {
//                        if (error.getMessage() != null) {
//                            if (error.getMessage().equals("Invalid Session Id")) {
//                                EventBus.getDefault().post(new Events.InvalidSession());
//                            }
//                            throw new Exception("Server Error: " + error.getMessage());
//                        } else {
//                            throw new Exception("Server Returned An Error.");
//                        }
//                    }
//
//                    if (serverContent.getPoi() != null) {
//                        data.put(record.getId(), serverContent.getPoi());
//                        EventBus.getDefault().postSticky(new Events.PoisDataUpdated());
//                    }
//                }
//
//            }
//
//
//            public void run() {
//                if (!alive)
//                    return;
//
//                if (alive && application.isSetupComplete()  && application.getAgentSettings().getAgentRoles().pointsofinterest() && isConnected() && sessionManager.getSessionId() != null) {
//                    try {
//                        Location location = fixManager.getLastLocation();
//                        loadUnRestrictedGroups();
//                        loadCategories();
//                        loadGroups(location);
//                    } catch (Exception e) {
//                        if (!Network.isNetworkException(e)) {
//                            if (alive)
//                                Timber.w(e, "Error attempting to download poi data.");
//                            else
//                                Timber.w(e, "Error caught when dead.");
//                        }
//                    }
//
//                }
//
//                if (alive)
//                    mHandler.postDelayed(this, getInterval());
//            }
        }

    }


}