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
import com.xzfg.app.model.Media;
import com.xzfg.app.model.MediaHeader;
import com.xzfg.app.model.url.MediaUrl;
import com.xzfg.app.model.url.SessionUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.DateTransformer;
import com.xzfg.app.util.Network;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.RegistryMatcher;
import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;


public class CollectedMediaManager {

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
    // handles generating thumbnails
    @SuppressWarnings("unused")
    private volatile MediaHeader mediaHeader;
    private volatile ConcurrentHashMap<Object, List<Media.Record>> caseData = new ConcurrentHashMap<>();

    private MediaHeaderHandlerThread mediaHeaderHandlerThread;

    private volatile boolean started = false;

    private String selectedCase = null;
    private volatile boolean selected = false;

    public CollectedMediaManager(Application application) {
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
        clearSelectedCase();
        onPause();
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
                    if (mediaHeaderHandlerThread == null) {
                        mediaHeaderHandlerThread = new MediaHeaderHandlerThread();
                    }
                }
            }
        }
    }

    public void onPause() {
        synchronized (threadLock) {
            if (mediaHeaderHandlerThread != null) {
                MediaHeaderHandlerThread deadThread = mediaHeaderHandlerThread;
                mediaHeaderHandlerThread = null;
                deadThread.kill();
            }
        }
    }

    public List<MediaHeader.Media> getMediaHeaders() {
        if (mediaHeader == null) {
            return null;
        }
        return Collections.unmodifiableList(mediaHeader.getMedia());
    }

    public List<Media.Record> getCaseRecords(Object key) {
        if (caseData.containsKey(key)) {
            List<Media.Record> records = caseData.get(key);
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

    public void clearSelectedCase() {
        selectedCase = null;
    }

    public void setSelectedCase(String caseNumber) {
        if (caseNumber == null) {
            selectedCase = "null";
        } else {
            selectedCase = caseNumber;
        }
    }

    private class MediaHeaderHandlerThread extends HandlerThread {
        private final Handler mHandler;
        private final MediaHeaderDownloadRunnable runnable;
        private volatile boolean alive = true;

        public MediaHeaderHandlerThread() {
            super(MediaHeaderHandlerThread.class.getName(), Givens.THREAD_PRIORITY + android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
            start();
            mHandler = new Handler(getLooper());
            runnable = new MediaHeaderDownloadRunnable();
            mHandler.post(runnable);
        }

        public void kill() {
            alive = false;
            mHandler.removeCallbacks(runnable);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                quitSafely();
            } else {
                quit();
            }

        }

        private int getDistance() {
            if (BuildConfig.DEBUG)
                return Givens.DEBUG_DISTANCE;
            else
                return Givens.DEFAULT_DISTANCE;
        }

        private class MediaHeaderDownloadRunnable implements Runnable {

            private void getCaseMedia(MediaHeader.Media mediaEntry, Location location) throws Exception {

                MediaUrl url = new MediaUrl(application.getScannedSettings(), application.getString(R.string.casesmedia_endpoint), sessionManager.getSessionId());
                url.setCaseNumber(mediaEntry.getCaseNumber());
                if (location != null) {
                    url.setLocation(location);
                }
                url.setDistance(getDistance());

                if (!alive)
                    return;
                //Timber.d("Calling url: " + url.toString());
                Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
                String responseBody = response.body().string().trim();
                response.body().close();

                //Timber.d("Response Body (encrypted): " + responseBody);

                // if we don't receive a 200 ok, it's a network error.
                if (response.code() != 200 || responseBody.isEmpty() || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
                    if (BuildConfig.DEBUG) {
                        Crashlytics.setString("Url", url.toString());
                        Crashlytics.setLong("Response Code", response.code());
                        Crashlytics.setString("Response", responseBody);
                    }
                    throw new Exception("Server did not respond appropriately.");
                }

                responseBody = crypto.decryptFromHexToString(responseBody).trim();
                //Timber.d("Response Body (unencrypted): " + responseBody);

                if (responseBody.startsWith("Invalid Session") || responseBody.contains("<error>Invalid SessionId</error>")) {
                    EventBus.getDefault().post(new Events.InvalidSession());
                    return;
                }

                if (!responseBody.startsWith("<?xml version='1.0' encoding='utf-8' ?>")) {
                    if (BuildConfig.DEBUG) {
                        if (BuildConfig.DEBUG) {
                            Crashlytics.setString("Url", url.toString());
                            Crashlytics.setLong("Response Code", response.code());
                            Crashlytics.setString("Response", responseBody);
                        }
                    }
                    throw new Exception("Server did not return xml.");
                }

                //Timber.d("Response Body: " + responseBody);
                responseBody = responseBody.replace("</results>", "</media>").replace("<record index='1'>Input string was not in a correct format.</record>", "");
                // Escape ampersand in XML response to satisfy parser
                responseBody = responseBody.replace("&sessionId=", "&amp;sessionId=");

                RegistryMatcher matcher = new RegistryMatcher();
                matcher.bind(Date.class, new DateTransformer());
                Serializer serializer = new Persister(matcher);

                boolean validated = false;
                try {
                    validated = serializer.validate(Media.class, responseBody);
                } catch (XmlPullParserException e) {
                    // Broken XML detected
                    Timber.e("", e);
                }
                if (!validated) {
                    if (BuildConfig.DEBUG) {
                        Crashlytics.setString("Url", url.toString());
                        Crashlytics.setLong("Response Code", response.code());
                        Crashlytics.setString("Response", responseBody);
                    }
                    throw new Exception("XML Received Could Not Be Validated.");
                }
                Media serverMedia = serializer.read(Media.class, responseBody);

                if (serverMedia != null) {
                    com.xzfg.app.model.Error error = serverMedia.getError();
                    if (error != null) {
                        if (error.getMessage() != null) {
                            if (error.getMessage().equals("Invalid Session Id")) {
                                EventBus.getDefault().post(new Events.InvalidSession());
                            }
                            throw new Exception("Server Error: " + error.getMessage());
                        } else {
                            throw new Exception("Server Returned An Error.");
                        }
                    }

                    if (serverMedia.getRecords() != null) {
                        //Timber.d(serverMedia.toString());
                        caseData.put(mediaEntry.getCaseNumber(), serverMedia.getRecords());
                        EventBus.getDefault().postSticky(new Events.CollectedMediaDataUpdated());
                    }
                }

            }

            private void getMediaHeaders(Location location) throws Exception {
                SessionUrl headerUrl = new SessionUrl(application.getScannedSettings(), application.getString(R.string.mediaheader_endpoint), sessionManager.getSessionId());
                if (location != null) {
                    headerUrl.setLocation(location);
                }
                HashMap<String, String> params = new HashMap<>();

                params.put("distance", String.valueOf(getDistance()));
                headerUrl.setParamData(params);

                //Timber.d("Calling url: " + headerUrl.toString());


                Response response = httpClient.newCall(new Request.Builder().url(headerUrl.toString(crypto)).build()).execute();
                String responseBody = response.body().string().trim();
                response.body().close();

                //Timber.d("Response Body (encrypted): " + responseBody);

                // if we don't receive a 200 ok, it's a network error.
                if (response == null || response.code() != 200 || responseBody == null || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
                    if (BuildConfig.DEBUG) {
                        Crashlytics.setString("Url", headerUrl.toString());
                        Crashlytics.setLong("Response Code", response.code());
                        Crashlytics.setString("Response", responseBody);
                    }
                    throw new Exception("Server did not respond appropriately.");
                }

                responseBody = crypto.decryptFromHexToString(responseBody).trim();
                //Timber.d("Response Body (unencrypted): " + responseBody);

                if (responseBody.startsWith("Invalid Session") || responseBody.contains("<error>Invalid SessionId</error>")) {
                    EventBus.getDefault().post(new Events.InvalidSession());
                    return;
                }

                if (!responseBody.startsWith("<?xml version='1.0' encoding='utf-8' ?>")) {
                    if (BuildConfig.DEBUG) {
                        Crashlytics.setString("Url", headerUrl.toString());
                        Crashlytics.setLong("Response Code", response.code());
                        Crashlytics.setString("Response", responseBody);
                    }
                    throw new Exception("Server did not return xml.");
                }

                //Timber.d("MediaHeader Response Body: " + responseBody);

                RegistryMatcher matcher = new RegistryMatcher();
                matcher.bind(Date.class, new DateTransformer());
                Serializer serializer = new Persister(matcher);

                boolean validated = serializer.validate(MediaHeader.class, responseBody);
                if (!validated) {
                    if (BuildConfig.DEBUG) {
                        Crashlytics.setString("Url", headerUrl.toString());
                        Crashlytics.setLong("Response Code", response.code());
                        Crashlytics.setString("Response", responseBody);
                    }
                    throw new Exception("XML Received Could Not Be Validated.");
                }
                MediaHeader serverMediaHeader = serializer.read(MediaHeader.class, responseBody);

                if (serverMediaHeader != null) {
                    com.xzfg.app.model.Error error = serverMediaHeader.getError();
                    if (error != null) {
                        if (error.getMessage() != null) {
                            if (error.getMessage().equals("Invalid Session Id")) {
                                EventBus.getDefault().post(new Events.InvalidSession());
                            }
                            throw new Exception("Server Error: " + error.getMessage());
                        } else {
                            throw new Exception("Server Returned An Error.");
                        }
                    }

                    //Timber.d(serverMediaHeader.toString());

                    try {
                        mediaHeader = serverMediaHeader;
                        EventBus.getDefault().postSticky(new Events.CollectedMediaHeaderUpdated());
                        List<MediaHeader.Media> media = serverMediaHeader.getMedia();
                        List<Object> cases = new ArrayList<>();
                        for (MediaHeader.Media mediaEntry : media) {
                            if (!alive)
                                return;

                            cases.add(mediaEntry.getCaseNumber());
                            try {
                                if (!CollectedMediaManager.this.caseData.containsKey(mediaEntry.getCaseNumber()) ||
                                        (
                                                selectedCase != null && ((mediaEntry.getCaseNumber() == null && selectedCase.equals("null")) || mediaEntry.getCaseNumber().equals(selectedCase))
                                        )
                                        ) {
                                    getCaseMedia(mediaEntry, location);
                                }
                            } catch (Exception e) {
                                Timber.w(e, "Couldn't get case media.");
                            }
                        }

                        if (!alive)
                            return;

                        // remove unneeded entries.
                        if (!cases.isEmpty()) {
                            for (Object key : caseData.keySet()) {
                                if (!cases.contains(key)) {
                                    caseData.remove(key);
                                    EventBus.getDefault().postSticky(new Events.CollectedMediaDataUpdated());
                                }
                            }
                        }
                        EventBus.getDefault().postSticky(new Events.CollectedMediaHeaderUpdated());

                    } catch (Exception e) {
                        Timber.w(e, "Error getting case data.");
                    }
                    // ok. now we've got the mediaHeaders, get the media data by case

                }

            }

            public void run() {
                if (!alive)
                    return;

                if (alive && application.isSetupComplete() && application.getAgentSettings().getAgentRoles().collectedmedia() && isConnected() && sessionManager.getSessionId() != null) {
                    try {
                        Location location = fixManager.getLastLocation();
                        getMediaHeaders(location);
                    } catch (Exception e) {
                        if (!Network.isNetworkException(e)) {
                            if (alive)
                                Timber.w(e, "Error attempting to download collected media data.");
                            else
                                Timber.w(e, "Error caught when dead.");
                        }
                    }

                }

                if (alive)
                    mHandler.postDelayed(this, getInterval());
            }
        }

    }

}