package com.xzfg.app.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.activities.AgentActivity;
import com.xzfg.app.fragments.dialogs.AllowLiveTrackingDialogFragment;
import com.xzfg.app.managers.ChatManager;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.SessionManager;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.User;
import com.xzfg.app.services.ProfileService;
import com.xzfg.app.util.ImageUtil;
import com.xzfg.app.util.MessageUtil;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;


public class WebMapFragment extends Fragment implements View.OnClickListener {
    public final static String MAP_FILE_NAME = "7a58a4e965b5414c800349398b604ca9";

    private boolean mIsWebViewAvailable = false;
    private boolean mLoading = true;
    private boolean mCapturedMapLoaded = false;
    private Handler mMapCaptureHandler;
    AllowLiveTrackingDialogFragment trackingDialog;
    private WebView mWebView;
    private TextView mOfflineLabelView;

    @Inject
    Application application;
    @Inject
    ChatManager chatManager;
    @Inject
    FixManager fixManager;
    @Inject
    SessionManager sessionManager;
    @Inject
    ConnectivityManager connectivityManager;


    private Runnable mMapCaptureRunnable = new Runnable() {
        @Override
        public void run() {
            Timber.d("Capturing map...");

            AgentSettings settings = application.getAgentSettings();
            if (settings.getMapCapture() > 0 && sessionManager.getSessionId() != null) {
                // Capture map - if connected
                if (isConnected()) {
                    // Map loaded and displayed - ready to capture
                    if (!mLoading && getWebView().getWidth() > 0 && getWebView().getHeight() > 0) {
                        Bitmap bitmap = ImageUtil.loadBitmapFromView(getWebView());
                        ProfileService.saveBitmap(application, bitmap, MAP_FILE_NAME);
                    } else {
                        // Try again after delay  - Restart timer
                   //     mMapCaptureHandler.postDelayed(this, 1000);
                        return;
                    }
                } else {
                    // Load captured map if just got disconnected
                    if (!mCapturedMapLoaded) {
                        loadSavedMap();
                    }
                }
                // Restart timer
               // mMapCaptureHandler.postDelayed(this, settings.getMapCapture() * 1000);
            }
        }
    };


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Application) activity.getApplication()).inject(this);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, viewGroup, false);
        FrameLayout frame = (FrameLayout) v.findViewById(R.id.map_frame);
        mWebView = new WebView(getActivity());

        // force software rendeding of the webview for blackberry.
        //if (System.getProperty("os.name").equals("qnx")) {
        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //}

        mWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && !mLoading) {
                    EventBus.getDefault().postSticky(new Events.MapCentered(false));
                }
                return false;
            }
        });
        WebSettings webSettings = mWebView.getSettings();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webSettings.setDomStorageEnabled(false);
        webSettings.setAppCacheEnabled(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setGeolocationEnabled(false);
        webSettings.setDatabaseEnabled(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSaveFormData(false);
        webSettings.setSavePassword(false);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        mWebView.addJavascriptInterface(new JSInterface(), "xzfg");
        mWebView.setWebViewClient(new MapWebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setBackgroundColor(getResources().getColor(R.color.timberwolf));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }

        // Enable local store support
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            mWebView.getSettings().setDatabasePath("/data/data/" + mWebView.getContext().getPackageName() + "/databases/");
        }

        frame.addView(mWebView);
        mIsWebViewAvailable = true;
        v.findViewById(R.id.livetrack_button).setOnClickListener(this);
        v.findViewById(R.id.center_button).setOnClickListener(this);

        loadSavedMap();

        mOfflineLabelView = (TextView) v.findViewById(R.id.offline_label);

        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (trackingDialog != null) {
            trackingDialog.dismissAllowingStateLoss();
            trackingDialog = null;
        }
        mWebView.stopLoading();
        mWebView.onPause();
        EventBus.getDefault().unregister(this);

        // Stop map capturing
        if (mMapCaptureHandler != null) {
            mMapCaptureHandler.removeCallbacks(mMapCaptureRunnable);
            mMapCaptureHandler = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mWebView.onResume();
        EventBus.getDefault().registerSticky(this);

        // Start map capturing
        AgentSettings settings = application.getAgentSettings();
        if (settings.getMapCapture() > 0 && sessionManager.getSessionId() != null) {
            mMapCaptureHandler = new Handler();
         //   mMapCaptureHandler.postDelayed(mMapCaptureRunnable, settings.getMapCapture() * 1000);
        }
    }

    public void onEventMainThread(Events.Session event) {
        if (event.getSessionId() != null) {
            if (isConnected()) {
                load();
            } else {
                loadSavedMap();
            }
        }
    }

    public void onEventMainThread(Events.PrivateFileLoaded event) {
        // Ignore if connected
        if (MAP_FILE_NAME.equals(event.getTag()) && event.getImage() != null && !isConnected()) {
            // Display captured map image in WebView
            String filesPath = getActivity().getFilesDir().getAbsolutePath();
            String filePath = "file://" + filesPath + "/" + MAP_FILE_NAME;
            String html = "<html><head></head><body><img src=\"" + filePath + "\" width=\"100%\"></body></html>";
            mCapturedMapLoaded = true;
            mWebView.loadDataWithBaseURL("", html, "text/html", "utf-8", "");
            mOfflineLabelView.setVisibility(View.VISIBLE);
        }
    }

    public void onEventMainThread(Events.OpenPoiMap event) {
        Location currentLocation = fixManager.getLastLocation();
        if (!mLoading && event.getPoiId() != null && currentLocation != null && isAdded() && getActivity() != null && getView() != null && mWebView != null && mIsWebViewAvailable) {
            EventBus.getDefault().post(new Events.MapCentered(false));
            String call = "javascript:(function() {if (directionVos[\"DirectionsToPOI\"] != null) { directionVos[\"DirectionsToPOI\"].destroy(); delete directionVos[\"DirectionsToPOI\"];} var directionVo = new DirectionsVo();" +
                    "var success = directionVo.update(\"DirectionsToPOI\", " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude() + ", null, null, \"CASESAgent\", uid, null, " + event.getPoiId() + ");" +
                    "if ( success ) {directionVos[\"DirectionsToPOI\"] = directionVo;populateDirections(\"DirectionsToPOI\");document.getElementById(\"directionsImage\").style.display = \"block\";} else {alert(\"Error, Directions could not be retreived\");closeDirections();drawCanvas(false);}})();";
            mWebView.loadUrl(call);
            ((AgentActivity) getActivity()).setSelected(R.id.map);

        }
    }

    public void onEventMainThread(Events.OpenAlertMap event) {
        Location currentLocation = fixManager.getLastLocation();
        if (!mLoading && event.getAlertId() != null && currentLocation != null && isAdded() && getActivity() != null && getView() != null && mWebView != null && mIsWebViewAvailable) {
            EventBus.getDefault().post(new Events.MapCentered(false));
            String call = "javascript:(function() {if (directionVos[\"DirectionsToAlert\"] != null) { directionVos[\"DirectionsToAlert\"].destroy(); delete directionVos[\"DirectionsToAlert\"];} var directionVo = new DirectionsVo();" +
                    "var success = directionVo.update(\"DirectionsToAlert\", " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude() + ", null, null, \"CASESAgent\", uid, " + event.getAlertId() + ", null);" +
                    "if ( success ) {directionVos[\"DirectionsToAlert\"] = directionVo;populateDirections(\"DirectionsToAlert\");document.getElementById(\"directionsImage\").style.display = \"block\";} else {alert(\"Error, Directions could not be retreived\");closeDirections();drawCanvas(false);}})();";
            //Timber.d("Calling JavaScript: " + call);
            mWebView.loadUrl(call);
            ((AgentActivity) getActivity()).setSelected(R.id.map);
        }
    }


    public void onEventMainThread(Events.OpenMediaMap event) {
        if (!mLoading && event.getLatitude() != null || event.getLongitude() != null && isAdded() && getActivity() != null && getView() != null && mWebView != null && mIsWebViewAvailable) {
            EventBus.getDefault().post(new Events.MapCentered(false));
            ((AgentActivity) getActivity()).setSelected(R.id.map);
            application.getAgentSettings().setMapMedia(1);
            MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Set Settings|MapMediaCASESAgent@1"));
            EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
            String call = "javascript:(function() {unLockAll();panToCoordWithZoom(" + event.getLatitude() + "," + event.getLongitude() + ", 17);drawCanvas(false);})();";
            //Timber.d("Calling JavaScript: " + call);
            mWebView.loadUrl(call);
        }
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    private void loadSavedMap() {
        ProfileService.loadBitmap(application, MAP_FILE_NAME, MAP_FILE_NAME);
    }

    public void load() {
        String sessionId = sessionManager.getSessionId();
        mOfflineLabelView.setVisibility(View.GONE);

        if (sessionId != null) {
            Location lastLocation = fixManager.getLastLocation();
            StringBuilder sb = new StringBuilder(192);

            String ipAddress;
/*
            if (application.getScannedSettings().getUseMapPort() < 1) {
                ipAddress = application.getScannedSettings().getMapUrl();
                if (ipAddress == null) {
                    Timber.w(new MapPortWithoutMapUrlException(application.getScannedSettings().getUserName()),"User has a map port, but no map url. They should really re-scan");
                    ipAddress = application.getScannedSettings().getIpAddress();
                }
            }
            else {
                ipAddress = application.getScannedSettings().getIpAddress();
            }
*/
            ipAddress = application.getScannedSettings().getIpAddress();

            if (ipAddress == null) {
                throw new RuntimeException("The ipAddress/mapUrl appears to be unavailable. Please re-scan.");
            }

            if (ipAddress.contains("://")) {
                sb.append(ipAddress);
            } else {
                sb.append("https://").append(application.getScannedSettings().getIpAddress());
            }
/*
            if (application.getScannedSettings().getUseMapPort() > 0) {
                sb.append(":").append(application.getScannedSettings().getMapPort());
            }
*/
            sb.append(":").append(application.getScannedSettings().getTrackingPort());
            sb.append(getString(R.string.map_endpoint));

            sb.append("?sessionId=").append(sessionManager.getSessionId());
            sb.append("&uid=").append(application.getScannedSettings().getOrganizationId()).append(application.getDeviceIdentifier());

            if (lastLocation != null) {
                sb.append("&lat=").append(lastLocation.getLatitude());
                sb.append("&lon=").append(lastLocation.getLongitude());
            }
            sb.append("&downTime=2");
            sb.append("&downDist=").append(String.valueOf((int) (50 * getResources().getDisplayMetrics().density)));
            //Timber.d("Loading map url: " + sb.toString());
            mCapturedMapLoaded = false;
            mWebView.loadUrl(sb.toString());
        }
    }

    @Override
    public void onDestroyView() {
        mIsWebViewAvailable = false;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (mWebView != null) {
            mWebView.stopLoading();
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }

    @SuppressWarnings("unused")
    public WebView getWebView() {
        return mIsWebViewAvailable ? mWebView : null;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.center_button: {
                if (isConnected()) {
                    load();
                }
                break;
            }
            case R.id.livetrack_button: {
                if (!fixManager.isTracking()) {
                    if (application.getAgentSettings().getAllowTracking() == 0) {
                        if (trackingDialog != null) {
                            trackingDialog.dismissAllowingStateLoss();
                            trackingDialog = null;
                        }
                        trackingDialog = AllowLiveTrackingDialogFragment.newInstance(getString(R.string.allow_live_tracking_), getString(R.string.allow_live_tracking_description));
                        trackingDialog.show(getFragmentManager(), "allow-tracking");
                    } else {
                        EventBus.getDefault().post(new Events.StartLiveTracking());
                    }
                } else {
                    EventBus.getDefault().post(new Events.StopLiveTracking());
                }
                break;
            }
        }
    }

    public class MapWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (url != null && !url.startsWith("javascript:")) {
                mLoading = true;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (isAdded() && getActivity() != null && getView() != null) {
                view.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.timberwolf));
            }
            if (url != null && !url.startsWith("javascript:")) {
                EventBus.getDefault().postSticky(new Events.MapCentered(true));
                mLoading = false;
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String url) {
            super.onReceivedError(view, errorCode, description, url);
            if (isAdded() && getActivity() != null && getView() != null) {
                view.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.timberwolf));
            }
            if (url != null && !url.startsWith("javascript:")) {
                mLoading = true;
                EventBus.getDefault().postSticky(new Events.MapCentered(false));
            }
            Timber.w("WEB ERROR: " + errorCode + ", DESCRIPTION: " + description + ", URL: " + url);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Timber.e("An SSL Error has been encountered: " + error.toString());
            super.onReceivedSslError(view, handler, error);
        }
    }

    @SuppressWarnings("unused")
    public class JSInterface {

        public JSInterface() {
        }

        @JavascriptInterface
        public void openChat(String userId) {
            //Timber.d("openChat called: " + userId);
            startChat(userId);
        }

        @JavascriptInterface
        public void startChat(String userId) {
            //Timber.d("startChat called: " + userId);
            User user = chatManager.findUserById(userId);
            if (user == null) {
                user = chatManager.findUserByName(userId);
            }
            if (user != null) {
                EventBus.getDefault().post(new Events.StartChat(user));
            }
        }

        /*
        @JavascriptInterface
        public void createPoi(Double latitude,Double longitude,String address) {
            Timber.d("createPOI called (double): " + latitude + ", " + longitude + ", " + address);
            EventBus.getDefault().post(new Events.CreatePoi(latitude,longitude,address));
        }


        @JavascriptInterface
        public void createPoi(Float latitude, Float longitude, String address) {
            Timber.d("createPOI called (float): " + latitude + ", " + longitude + ", " + address);
            EventBus.getDefault().post(new Events.CreatePoi(latitude.doubleValue(),longitude.doubleValue(),address));
        }
        */

        @JavascriptInterface
        public void createPoi(String latitude, String longitude, String address) {
            //Timber.d("createPOI called (string): " + latitude + ", " + longitude + ", " + address);
            EventBus.getDefault().post(new Events.CreatePoi(Double.parseDouble(latitude), Double.parseDouble(longitude), address));
        }

    }
}
