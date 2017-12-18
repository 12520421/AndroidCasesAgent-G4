package com.xzfg.app.fragments.alerts;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.SessionManager;
import com.xzfg.app.model.AlertContent;
import com.xzfg.app.services.ProfileService;
import com.xzfg.app.util.DateUtil;

import java.text.NumberFormat;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 *
 */
public class AlertDetailsFragment extends Fragment implements View.OnClickListener {

    @Inject
    Application application;
    @Inject
    FixManager fixManager;
    @Inject
    SessionManager sessionManager;

    private final Handler handler = new Handler();
    private boolean mLoading = true;
    private AlertContent.Record record;

    private WebView webView;
    private TextView contactName;
    private TextView messageView;
    private TextView timeView;
    private TextView distanceText;
    private ImageView directionArrow;
    private LinearLayout location;
    private TextView addressView;
    private ImageView statusIndicator;

    public static AlertDetailsFragment newInstance(AlertContent.Record record) {
        AlertDetailsFragment f = new AlertDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable("record", record);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        record = getArguments().getParcelable("record");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Application) activity.getApplication()).inject(this);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alert, viewGroup, false);
        v.findViewById(R.id.back_button).setOnClickListener(this);
        v.findViewById(R.id.accept_button).setOnClickListener(this);
        v.findViewById(R.id.reject_button).setOnClickListener(this);

        webView = (WebView) v.findViewById(R.id.map);
        contactName = (TextView) v.findViewById(R.id.contact_name);
        messageView = (TextView) v.findViewById(R.id.latest_message);
        timeView = (TextView) v.findViewById(R.id.time);
        location = (LinearLayout) v.findViewById(R.id.location);
        distanceText = (TextView) v.findViewById(R.id.distance);
        directionArrow = (ImageView) v.findViewById(R.id.arrow);
        location = (LinearLayout) v.findViewById(R.id.location);
        statusIndicator = (ImageView) v.findViewById(R.id.status_indicator);

        addressView = (TextView) v.findViewById(R.id.address);
        addressView.setOnClickListener(this);

        if (record.getTargetName() != null) {
            contactName.setText(record.getTargetName());
        } else {
            contactName.setText("");
        }
        if ((record.getDistance() != null && !record.getDistance().trim().isEmpty())) {
            Double distance = Double.parseDouble(record.getDistance());
            if (distance != 0) {
                location.setVisibility(View.VISIBLE);
                distanceText.setText(NumberFormat.getInstance(getResources().getConfiguration().locale).format(distance));
                if (record.getDirection() != null) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(Float.parseFloat(record.getDirection()), directionArrow.getDrawable().getIntrinsicWidth() / 2, directionArrow.getDrawable().getIntrinsicHeight() / 2);
                    directionArrow.setScaleType(ImageView.ScaleType.MATRIX);
                    directionArrow.setImageMatrix(matrix);
                }
            } else {
                location.setVisibility(View.INVISIBLE);
            }
        } else {
            location.setVisibility(View.INVISIBLE);
        }

        DateUtil.formatDate(timeView, record.getCreated());


        if (record.getAction() != null) {
            messageView.setText(record.getAction().trim());
        } else {
            messageView.setText("");
        }

        // Set address
        if (record.getAddress() != null && !record.getAddress().isEmpty()) {
            SpannableString spanStr = new SpannableString(record.getAddress());
            spanStr.setSpan(new UnderlineSpan(), 0, spanStr.length(), 0);
            addressView.setText(spanStr);
            addressView.setVisibility(View.VISIBLE);
        } else {
            addressView.setVisibility(View.GONE);
        }

        // Set status icon
        updateStatusIcon();

        // Initialize web view
        // force software rendeding of the webview for blackberry.
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && !mLoading) {
                    EventBus.getDefault().postSticky(new Events.MapCentered(false));
                }
                return false;
            }
        });
        WebSettings webSettings = webView.getSettings();

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
        //webView.addJavascriptInterface(new WebMapFragment.JSInterface(), "xzfg");
        webView.setWebViewClient(new AlertDetailsFragment.MapWebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.setBackgroundColor(ContextCompat.getColor(getContext(),R.color.timberwolf));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }

        // Enable local store support
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            webView.getSettings().setDatabasePath("/data/data/" + webView.getContext().getPackageName() + "/databases/");
        }
        load();

        return v;
    }

    // Set status icon
    private void updateStatusIcon() {
        Context ctx = getActivity();
        if (record.getUserAccepted() != 0 && record.getOtherAccepted() != 0) {
            statusIndicator.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.alertacceptedbymeandsomeoneelse));
        } else if (record.getUserAccepted() != 0) {
            statusIndicator.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.alertacceptedbyme));
        } else if (record.getOtherAccepted() != 0) {
            statusIndicator.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.alertacceptedbysomeoneelse));
        } else {
            statusIndicator.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.alertunaccepted));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.back_button:
                getActivity().onBackPressed();
                break;
            case R.id.accept_button:
                //Timber.d("ACCEPT BUTTON CLICKED.");
                ProfileService.acceptAlert(getActivity(), record.getAlertId(), 1);
                record.setUserAccepted(1);
                EventBus.getDefault().postSticky(new Events.AlertRecordUpdated(record));
                Toast.makeText(getActivity(), R.string.alert_accepted, Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
                break;
            case R.id.reject_button:
                //Timber.d("REJECT BUTTON CLICKED.");
                ProfileService.acceptAlert(getActivity(), record.getAlertId(), 0);
                record.setUserAccepted(0);
                EventBus.getDefault().postSticky(new Events.AlertRecordUpdated(record));
                Toast.makeText(getActivity(), R.string.alert_rejected, Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
                break;
            case R.id.address:
                // Open map
                Uri address = Uri.parse("geo:" + record.getLatitude() + "," + record.getLongitude() + "?q=" + record.getAddress());
                Intent intent = new Intent(Intent.ACTION_VIEW, address);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    intent.setData(Uri.parse("http://maps.google.com/maps?q=" + record.getAddress()));
                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
                break;
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        webView.stopLoading();
        webView.onPause();
        //EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        webView.onResume();
        //EventBus.getDefault().registerSticky(this);
    }
    public void load() {
        String summary = "<html><body><br>Loading map...</body></html>";
        webView.loadData(summary, "text/html; charset=utf-8", "utf-8");

        String sessionId = sessionManager.getSessionId();
        if (sessionId != null) {
            Location lastLocation = fixManager.getLastLocation();
            StringBuilder sb = new StringBuilder(192);

            String ipAddress = application.getScannedSettings().getIpAddress();

            if (ipAddress == null) {
                throw new RuntimeException("The ipAddress/mapUrl appears to be unavailable. Please re-scan.");
            }

            if (ipAddress.contains("://")) {
                sb.append(ipAddress);
            } else {
                sb.append("https://").append(application.getScannedSettings().getIpAddress());
            }

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
            webView.loadUrl(sb.toString());

            Location currentLocation = fixManager.getLastLocation();
            if (/*!mLoading &&*/ record.getAlertId() != null && currentLocation != null && isAdded() && getActivity() != null && getView() != null && webView != null) {
                String call = "javascript:(function() {if (directionVos[\"DirectionsToAlert\"] != null) { directionVos[\"DirectionsToAlert\"].destroy(); delete directionVos[\"DirectionsToAlert\"];} var directionVo = new DirectionsVo();" +
                        "var success = directionVo.update(\"DirectionsToAlert\", " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude() + ", null, null, \"CASESAgent\", uid, " + record.getAlertId() + ", null);" +
                        "if ( success ) {directionVos[\"DirectionsToAlert\"] = directionVo;populateDirections(\"DirectionsToAlert\");document.getElementById(\"directionsImage\").style.display = \"block\";} else {alert(\"Error, Directions could not be retreived\");closeDirections();drawCanvas(false);}})();";
                //Timber.d("Calling JavaScript: " + call);
                webView.loadUrl(call);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
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



}