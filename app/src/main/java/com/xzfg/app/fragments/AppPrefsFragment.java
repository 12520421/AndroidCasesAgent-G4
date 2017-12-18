package com.xzfg.app.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.codec.StringUtils;
import com.xzfg.app.fragments.RemovablePagerAdapter.AgentTabs;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.fragments.dialogs.ConfirmStorageDialog;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.managers.PingManager;
import com.xzfg.app.managers.SessionManager;
import com.xzfg.app.model.AgentProfile;
import com.xzfg.app.model.AgentRoleComponent;
import com.xzfg.app.model.AgentRoles;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.security.Fuzz;
import com.xzfg.app.services.ProfileService;
import com.xzfg.app.util.MessageUtil;
import com.xzfg.app.util.SetSetupFieldUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

public class AppPrefsFragment extends Fragment implements AgentRoleComponent, CompoundButton.OnCheckedChangeListener, AdapterView.OnItemSelectedListener {

    @Inject
    Application application;

    @Inject
    @SuppressWarnings("unused")
    PingManager pingManager;

    @Inject
    SharedPreferences sharedPreferences;

    @Inject
    MediaManager mediaManager;

    @Inject
    SessionManager sessionManager;

    // About
    TextView aboutSerial;
    TextView aboutVersion;

    // Alerts
    Spinner alertReceiveSpinner;
    Spinner alertFromSpinner;
    Spinner alertToSpinner;
    Spinner alertRadiusSpinner;
    ArrayAdapter<String> alertReceiveAdapter;
    ArrayAdapter<String> alertFromAdapter;
    ArrayAdapter<String> alertToAdapter;
    ArrayAdapter<String> alertRadiusAdapter;

    // Panic
    EditText panicCancelPIN;
    EditText panicDuressPIN;
    EditText bossPIN;

    // Boss
    Switch bossSwitch;
    Spinner bossStyleSpinner;
    ArrayList<Integer> bossStyleValues = new ArrayList<>();
    ArrayList<String> bossStyleNames = new ArrayList<>();
    ArrayAdapter<String> bossStyleAdapter;

    // Map capture
    Spinner mapCaptureFrequencySpinner;
    ArrayList<Integer> mapCaptureFrequencyValues = new ArrayList<>();
    ArrayList<String> mapCaptureFrequencyNames = new ArrayList<>();
    ArrayAdapter<String> mapCaptureFrequencyAdapter;

    Switch liveSwitch;
    Switch vibrateOnFixSwitch;
    Spinner phoneRecordCallsSpinner;
    ArrayList<String> phoneCallsStrings = new ArrayList<>();
    ArrayList<Integer> phoneCallsValues = new ArrayList<>();
    ArrayAdapter<String> phoneCallsAdapter;
    Switch phoneSMSLogs;
    Spinner smsDeliverySpinner;
    ArrayList<String> smsDeliveryStrings = new ArrayList<>();
    ArrayList<Integer> smsDeliveryValues = new ArrayList<>();
    ArrayAdapter<String> smsDeliveryAdapter;
    Switch phonePhoneLogs;
    Spinner phoneLogDeliverySpinner;
    ArrayList<String> phoneLogDeliveryStrings = new ArrayList<>();
    ArrayList<Integer> phoneLogDeliveryValues = new ArrayList<>();
    ArrayAdapter<String> phoneLogDeliveryAdapter;
    Switch includeAudioSwitch;
    Switch externalStorageSwitch;
    Switch securitySwitch;

    // CASE - controls
    Spinner caseMapSpinner;
    Switch caseShowTrackersSwitch;
    Spinner caseTrackerVisibilitySpinner;
    Spinner caseAoiVisibilitySpinner;
    Spinner caseTeamSpinner;
    EditText caseNumber;
    EditText caseDescription;
    // CASE - data
    ArrayAdapter<String> caseMapAdapter;
    ArrayAdapter<String> caseTrackerVisibilityAdapter;
    ArrayAdapter<String> caseAoiVisibilityAdapter;
    ArrayAdapter<String> caseTeamAdapter;

    Spinner screenSpinner;
    ArrayList<String> screens = new ArrayList<>();
    ArrayAdapter<String> screenAdapter;

    Spinner pingSpinner;
    ArrayList<Integer> pingValues = new ArrayList<>();
    ArrayList<String> pingTimes = new ArrayList<>();
    ArrayAdapter<String> pingAdapter;

    Spinner fixDaySpinner;
    Spinner fixNightSpinner;
    Spinner fixPanicSpinner;
    ArrayList<Integer> fixValues = new ArrayList<>();
    ArrayList<String> fixTimes = new ArrayList<>();
    ArrayAdapter<String> fixAdapter;

    Spinner reportingSpinner;
    ArrayList<Integer> reportingValues = new ArrayList<>();
    ArrayList<String> reportingTimes = new ArrayList<>();
    ArrayAdapter<String> reportingAdapter;

    Spinner trackingSpinner;
    ArrayList<Integer> trackingValues = new ArrayList<>();
    ArrayList<String> trackingTimes = new ArrayList<>();
    ArrayAdapter<String> trackingAdapter;

    Spinner photoQualitySpinner;
    Spinner videoQualitySpinner;
    Spinner videoStreamQualitySpinner;
    ArrayList<Integer> qualityValues = new ArrayList<>();
    ArrayList<String> qualityStrings = new ArrayList<>();
    ArrayAdapter<String> photoQualityAdapter;
    ArrayAdapter<String> videoQualityAdapter;
    ArrayAdapter<String> videoStreamQualityAdapter;

    Spinner photoSizeSpinner;
    Spinner videoSizeSpinner;
    Spinner videoStreamSizeSpinner;
    ArrayList<Integer> sizeValues = new ArrayList<>();
    ArrayList<String> sizeStrings = new ArrayList<>();

    ArrayAdapter<String> photoSizeAdapter;
    ArrayAdapter<String> videoSizeAdapter;
    ArrayAdapter<String> videoStreamSizeAdapter;

    Spinner videoFrameRateSpinner;
    Spinner videoStreamFrameRateSpinner;
    ArrayList<Integer> streamFrameRateValues = new ArrayList<>();
    ArrayList<String> streamFrameRateStrings = new ArrayList<>();
    ArrayList<Integer> frameRateValues = new ArrayList<>();
    ArrayList<String> frameRateStrings = new ArrayList<>();
    ArrayAdapter<String> videoFrameRateAdapter;
    ArrayAdapter<String> videoStreamFrameRateAdapter;
    View videoSettings;
    View storageSettings;
    AlertDialogFragment languageDialog = null;
    AlertDialogFragment notChangedDialog = null;
    AlertDialogFragment languageDialog2 = null;
    AlertDialogFragment changedDialog = null;
    AlertDialogFragment slNotChanged = null;
    ConfirmStorageDialog confirmDialog = null;
    private List<String> languageCodes;
    private boolean initialized = false;

    View frameAbout = null;
    View frameAlerts = null;
    View frameLiveTracking = null;
    View frameDefaultScreen = null;
    View framePanic = null;
    View framePhone = null;
    View frameMapCapture = null;
    View framePhoto = null;
    View frameVideo = null;
    View frameStream = null;
    View frameBoss = null;
    View frameCases = null;
    View frameStorage = null;
    View frameLanguage = null;
    View frameSecurity = null;

    View frameNonEmergencyButton = null;
    TextView nonEmergencyNumber = null;
    TextView nonEmergencyLabel = null;

    View frameEmergencyButton = null;
    TextView emergencyNumber = null;
    TextView emergencyLabel = null;

    View framePanicButton = null;
    View frameBluetooth = null;
    //TextView panicLabel = null;
    TextView panicMessage = null;
    //
    private static final String SHARED_PREFERENCE_NAME ="NameDevice";
    EditText name_device;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getActivity().getApplication()).inject(this);
        AgentProfile profile = application.getAgentProfile();

        // Alerts
        alertReceiveAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, profile.getDayOfWeeks().getNames());
        alertReceiveAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        alertFromAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, profile.getStartDaytimes().getNames());
        alertFromAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        alertToAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, profile.getEndDaytimes().getNames());
        alertToAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        alertRadiusAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, profile.getAlertRadiuses().getNames());
        alertRadiusAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        bossStyleValues.clear();
        bossStyleValues.add(0);
        bossStyleValues.add(1);
        bossStyleValues.add(2);

        bossStyleNames.clear();
        bossStyleNames.add("Slot");
        bossStyleNames.add("PIN Pad");
        if (BuildConfig.FLAVOR.equals(Givens.FLAVOR_WEATHER)) {
            bossStyleNames.add("Weather");
        }
        bossStyleAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, bossStyleNames);
        bossStyleAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        mapCaptureFrequencyValues.clear();
        mapCaptureFrequencyValues.add(0);
        mapCaptureFrequencyValues.add(15);
        mapCaptureFrequencyValues.add(20);
        mapCaptureFrequencyValues.add(30);
        mapCaptureFrequencyValues.add(40);
        mapCaptureFrequencyValues.add(50);
        mapCaptureFrequencyValues.add(60);
        mapCaptureFrequencyValues.add(120);
        mapCaptureFrequencyValues.add(300);
        mapCaptureFrequencyValues.add(600);
        mapCaptureFrequencyValues.add(900);
        mapCaptureFrequencyValues.add(1200);
        mapCaptureFrequencyValues.add(1800);

        mapCaptureFrequencyNames.clear();
        mapCaptureFrequencyNames.add(getString(R.string.off));
        mapCaptureFrequencyNames.add(getResources().getQuantityString(R.plurals.every_second, 15, 15));
        mapCaptureFrequencyNames.add(getResources().getQuantityString(R.plurals.every_second, 20, 20));
        mapCaptureFrequencyNames.add(getResources().getQuantityString(R.plurals.every_second, 30, 30));
        mapCaptureFrequencyNames.add(getResources().getQuantityString(R.plurals.every_second, 40, 40));
        mapCaptureFrequencyNames.add(getResources().getQuantityString(R.plurals.every_second, 50, 50));
        mapCaptureFrequencyNames.add(getResources().getQuantityString(R.plurals.every_minute, 1, 1));
        mapCaptureFrequencyNames.add(getResources().getQuantityString(R.plurals.every_minute, 2, 2));
        mapCaptureFrequencyNames.add(getResources().getQuantityString(R.plurals.every_minute, 5, 5));
        mapCaptureFrequencyNames.add(getResources().getQuantityString(R.plurals.every_minute, 10, 10));
        mapCaptureFrequencyNames.add(getResources().getQuantityString(R.plurals.every_minute, 15, 15));
        mapCaptureFrequencyNames.add(getResources().getQuantityString(R.plurals.every_minute, 20, 20));
        mapCaptureFrequencyNames.add(getResources().getQuantityString(R.plurals.every_minute, 30, 30));
        mapCaptureFrequencyAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, mapCaptureFrequencyNames);
        mapCaptureFrequencyAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        phoneCallsStrings.clear();
        phoneCallsStrings.add(getString(R.string.phoneCalls_none));
        phoneCallsStrings.add(getString(R.string.phoneCalls_both));
        phoneCallsStrings.add(getString(R.string.phoneCalls_outgoing));
        phoneCallsStrings.add(getString(R.string.phoneCalls_incoming));
        phoneCallsValues.clear();
        phoneCallsValues.add(0);
        phoneCallsValues.add(1);
        phoneCallsValues.add(2);
        phoneCallsValues.add(3);
        phoneCallsAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, phoneCallsStrings);
        phoneCallsAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        smsDeliveryStrings.clear();
        smsDeliveryStrings.add(getString(R.string.sms_delivery_immediately));
        smsDeliveryStrings.add(getString(R.string.sms_delivery_30seconds));
        smsDeliveryStrings.add(getString(R.string.sms_delivery_1minute));
        smsDeliveryStrings.add(getString(R.string.sms_delivery_5minutes));
        smsDeliveryStrings.add(getString(R.string.sms_delivery_15minutes));
        smsDeliveryStrings.add(getString(R.string.sms_delivery_30minutes));
        smsDeliveryStrings.add(getString(R.string.sms_delivery_60minutes));
        smsDeliveryStrings.add(getString(R.string.sms_delivery_2hours));
        smsDeliveryStrings.add(getString(R.string.sms_delivery_4hours));
        smsDeliveryStrings.add(getString(R.string.sms_delivery_6hours));
        smsDeliveryStrings.add(getString(R.string.sms_delivery_12hours));
        smsDeliveryStrings.add(getString(R.string.sms_delivery_24hours));
        smsDeliveryValues.clear();
        smsDeliveryValues.add(1);
        smsDeliveryValues.add(30);
        smsDeliveryValues.add(60);
        smsDeliveryValues.add(300);
        smsDeliveryValues.add(900);
        smsDeliveryValues.add(1800);
        smsDeliveryValues.add(3600);
        smsDeliveryValues.add(7200);
        smsDeliveryValues.add(14400);
        smsDeliveryValues.add(21600);
        smsDeliveryValues.add(43200);
        smsDeliveryValues.add(86400);
        smsDeliveryAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, smsDeliveryStrings);
        smsDeliveryAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        phoneLogDeliveryStrings.clear();
        phoneLogDeliveryStrings.add(getString(R.string.phoneLog_delivery_immediately));
        phoneLogDeliveryStrings.add(getString(R.string.phoneLog_delivery_30seconds));
        phoneLogDeliveryStrings.add(getString(R.string.phoneLog_delivery_1minute));
        phoneLogDeliveryStrings.add(getString(R.string.phoneLog_delivery_5minutes));
        phoneLogDeliveryStrings.add(getString(R.string.phoneLog_delivery_15minutes));
        phoneLogDeliveryStrings.add(getString(R.string.phoneLog_delivery_30minutes));
        phoneLogDeliveryStrings.add(getString(R.string.phoneLog_delivery_60minutes));
        phoneLogDeliveryStrings.add(getString(R.string.phoneLog_delivery_2hours));
        phoneLogDeliveryStrings.add(getString(R.string.phoneLog_delivery_4hours));
        phoneLogDeliveryStrings.add(getString(R.string.phoneLog_delivery_6hours));
        phoneLogDeliveryStrings.add(getString(R.string.phoneLog_delivery_12hours));
        phoneLogDeliveryStrings.add(getString(R.string.phoneLog_delivery_24hours));
        phoneLogDeliveryValues.clear();
        phoneLogDeliveryValues.add(1);
        phoneLogDeliveryValues.add(30);
        phoneLogDeliveryValues.add(60);
        phoneLogDeliveryValues.add(300);
        phoneLogDeliveryValues.add(900);
        phoneLogDeliveryValues.add(1800);
        phoneLogDeliveryValues.add(3600);
        phoneLogDeliveryValues.add(7200);
        phoneLogDeliveryValues.add(14400);
        phoneLogDeliveryValues.add(21600);
        phoneLogDeliveryValues.add(43200);
        phoneLogDeliveryValues.add(86400);
        phoneLogDeliveryAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, phoneLogDeliveryStrings);
        phoneLogDeliveryAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        addScreens();

        pingValues.clear();
        pingValues.add(15);
        pingValues.add(20);
        pingValues.add(30);
        pingValues.add(40);
        pingValues.add(50);
        pingValues.add(60);
        pingValues.add(120);
        pingValues.add(300);
        pingValues.add(600);
        pingValues.add(900);
        pingValues.add(1200);
        pingValues.add(1800);
        pingValues.add(3600);

        pingTimes.clear();
        pingTimes.add(getResources().getQuantityString(R.plurals.every_second, 15, 15));
        pingTimes.add(getResources().getQuantityString(R.plurals.every_second, 20, 20));
        pingTimes.add(getResources().getQuantityString(R.plurals.every_second, 30, 30));
        pingTimes.add(getResources().getQuantityString(R.plurals.every_second, 40, 40));
        pingTimes.add(getResources().getQuantityString(R.plurals.every_second, 50, 50));
        pingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 1, 1));
        pingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 2, 2));
        pingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 5, 5));
        pingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 10, 10));
        pingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 15, 15));
        pingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 20, 20));
        pingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 30, 30));
        pingTimes.add(getResources().getQuantityString(R.plurals.every_hour, 1, 1));
        pingAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, pingTimes);
        pingAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);


        fixValues.clear();
        fixValues.add(2);
        fixValues.add(5);
        fixValues.add(10);
        fixValues.add(15);
        fixValues.add(20);
        fixValues.add(30);
        fixValues.add(40);
        fixValues.add(50);
        fixValues.add(60);
        fixValues.add(120);
        fixValues.add(300);
        fixValues.add(600);
        fixValues.add(900);
        fixValues.add(1200);
        fixValues.add(1800);
        fixValues.add(3600);

        fixTimes.clear();
        fixTimes.add(getResources().getQuantityString(R.plurals.every_second, 2, 2));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_second, 5, 5));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_second, 10, 10));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_second, 15, 15));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_second, 20, 20));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_second, 30, 30));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_second, 40, 40));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_second, 50, 50));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_minute, 1, 1));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_minute, 2, 2));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_minute, 5, 5));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_minute, 10, 10));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_minute, 15, 15));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_minute, 20, 20));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_minute, 30, 30));
        fixTimes.add(getResources().getQuantityString(R.plurals.every_hour, 1, 1));
        fixAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, fixTimes);
        fixAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        reportingValues.clear();
        reportingValues.add(0);
        reportingValues.add(2);
        reportingValues.add(5);
        reportingValues.add(10);
        reportingValues.add(15);
        reportingValues.add(20);
        reportingValues.add(30);
        reportingValues.add(40);
        reportingValues.add(50);
        reportingValues.add(60);
        reportingValues.add(120);
        reportingValues.add(300);
        reportingValues.add(600);
        reportingValues.add(900);
        reportingValues.add(1200);
        reportingValues.add(1800);
        reportingValues.add(3600);

        reportingTimes.clear();
        reportingTimes.add(getString(R.string.same_as_fix_interval));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_second, 2, 2));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_second, 5, 5));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_second, 10, 10));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_second, 15, 15));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_second, 20, 20));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_second, 30, 30));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_second, 40, 40));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_second, 50, 50));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 1, 1));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 2, 2));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 5, 5));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 10, 10));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 15, 15));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 20, 20));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 30, 30));
        reportingTimes.add(getResources().getQuantityString(R.plurals.every_hour, 1, 1));
        reportingAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, reportingTimes);
        reportingAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);


        trackingValues.clear();
        trackingValues.add(0);
        trackingValues.add(60);
        trackingValues.add(120);
        trackingValues.add(180);
        trackingValues.add(240);
        trackingValues.add(300);
        trackingValues.add(600);
        trackingValues.add(1200);
        trackingValues.add(2400);
        trackingValues.add(3600);
        trackingValues.add(7200);
        trackingValues.add(18000);
        trackingValues.add(36000);
        trackingValues.add(86400);

        trackingTimes.clear();
        trackingTimes.add(getString(R.string.disabled));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 1, 1));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 2, 2));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 3, 3));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 4, 4));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 5, 5));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 10, 10));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 20, 20));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_minute, 40, 40));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_hour, 1, 1));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_hour, 2, 2));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_hour, 5, 5));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_hour, 10, 10));
        trackingTimes.add(getResources().getQuantityString(R.plurals.every_hour, 24, 24));
        trackingAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, trackingTimes);
        trackingAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        qualityValues.clear();
        qualityStrings.clear();
        for (int i = 101; i > 4; i--) {
            if (i % 5 == 0) {
                qualityValues.add(i);
                if (i == 100) {
                    qualityStrings.add(String.format(getString(R.string._best_), i));
                } else {
                    qualityStrings.add(String.valueOf(i));
                }
            }
        }
        photoQualityAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, qualityStrings);
        photoQualityAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        videoQualityAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, qualityStrings);
        videoQualityAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        videoStreamQualityAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, qualityStrings);
        videoStreamQualityAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        sizeValues.clear();
        sizeValues.add(-1);
        sizeValues.add(1);
        sizeValues.add(2);
        sizeValues.add(3);
        sizeValues.add(4);
        sizeValues.add(5);
        sizeValues.add(6);
        sizeValues.add(7);
        sizeValues.add(8);

        sizeStrings.clear();
        sizeStrings.add(getString(R.string.best_for_connection));
        sizeStrings.add(getString(R.string.low_3g));
        sizeStrings.add(getString(R.string.medium_wifi));
        sizeStrings.add(getString(R.string.high_maximum));
        sizeStrings.add(getString(R.string.s352_288));
        sizeStrings.add(getString(R.string.s640_480));
        sizeStrings.add(getString(R.string.s960_540));
        sizeStrings.add(getString(R.string.s1280_720));
        sizeStrings.add(getString(R.string.s1920_1080));

        photoSizeAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, sizeStrings);
        photoSizeAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        videoSizeAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, sizeStrings);
        videoSizeAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        videoStreamSizeAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, sizeStrings);
        videoStreamSizeAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        streamFrameRateStrings.clear();
        streamFrameRateValues.clear();
        for (int i = 1; i <= 30; i++) {
            streamFrameRateValues.add(i);
            streamFrameRateStrings.add(String.valueOf(i));
        }
        videoStreamFrameRateAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, streamFrameRateStrings);
        videoStreamFrameRateAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);


        frameRateStrings.clear();
        frameRateValues.clear();
        frameRateStrings.add("15");
        frameRateStrings.add("24");
        frameRateStrings.add("30");
        frameRateValues.add(15);
        frameRateValues.add(24);
        frameRateValues.add(30);

        for (int i = 1; i <= 30; i++) {
            streamFrameRateValues.add(i);
            streamFrameRateStrings.add(String.valueOf(i));
        }

        videoFrameRateAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, frameRateStrings);
        videoFrameRateAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        // Cases
        caseMapAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, profile.getDefaultMaps().getNames());
        caseMapAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        caseTrackerVisibilityAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, profile.getTrackerVisibleRanges().getNames());
        caseTrackerVisibilityAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        caseAoiVisibilityAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, profile.getAoiVisibleRanges().getNames());
        caseAoiVisibilityAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        caseTeamAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, profile.getTeams().getNames());
        caseTeamAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        //




    }


    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = layoutInflater.inflate(R.layout.fragment_appprefs, viewGroup, false);
        AgentSettings settings = application.getAgentSettings();
        AgentProfile profile = application.getAgentProfile();

        // framelayouts, provides groups of views.
        frameAbout = v.findViewById(R.id.frame_about);
        frameAlerts = v.findViewById(R.id.frame_alerts);
        frameLiveTracking = v.findViewById(R.id.frame_livetracking);
        frameDefaultScreen = v.findViewById(R.id.frame_defaultscreen);
        framePanic = v.findViewById(R.id.frame_panic);
        framePhone = v.findViewById(R.id.frame_phone);
        framePhoto = v.findViewById(R.id.frame_photo);
        frameMapCapture = v.findViewById(R.id.frame_mapcapture);
        frameVideo = v.findViewById(R.id.frame_video);
        frameStream = v.findViewById(R.id.frame_stream);
        frameBoss = v.findViewById(R.id.frame_boss);
        frameCases = v.findViewById(R.id.frame_cases);
        frameStorage = v.findViewById(R.id.frame_storage);
        frameLanguage = v.findViewById(R.id.frame_language);
        frameSecurity = v.findViewById(R.id.frame_security);

        // views
        videoSettings = v.findViewById(R.id.video_settings);
        storageSettings = v.findViewById(R.id.storage_settings);
        liveSwitch = (Switch) v.findViewById(R.id.live_switch);
        vibrateOnFixSwitch = (Switch) v.findViewById(R.id.vibrate_fix_switch);

        // About
        aboutSerial = (TextView) v.findViewById(R.id.about_serial_text);
        aboutVersion = (TextView) v.findViewById(R.id.about_version_text);
        aboutSerial.setText(application.getScannedSettings().getOrganizationId() + application.getDeviceIdentifier());
        aboutVersion.setText(BuildConfig.VERSION_NAME + "-" + BuildConfig.FLAVOR.toUpperCase());

        // Alerts
        alertReceiveSpinner = (Spinner) v.findViewById(R.id.receive_alerts_spinner);
        alertReceiveSpinner.setAdapter(alertReceiveAdapter);
        alertReceiveSpinner.setSelection(profile.getDayOfWeeks().getSelectedPosition(), false);
        alertReceiveSpinner.setOnItemSelectedListener(this);

        alertFromSpinner = (Spinner) v.findViewById(R.id.alert_from_spinner);
        alertFromSpinner.setAdapter(alertFromAdapter);
        alertFromSpinner.setSelection(profile.getStartDaytimes().getSelectedPosition(), false);
        alertFromSpinner.setOnItemSelectedListener(this);

        alertToSpinner = (Spinner) v.findViewById(R.id.alert_to_spinner);
        alertToSpinner.setAdapter(alertToAdapter);
        alertToSpinner.setSelection(profile.getEndDaytimes().getSelectedPosition(), false);
        alertToSpinner.setOnItemSelectedListener(this);

        alertRadiusSpinner = (Spinner) v.findViewById(R.id.alert_radius_spinner);
        alertRadiusSpinner.setAdapter(alertRadiusAdapter);
        alertRadiusSpinner.setSelection(profile.getAlertRadiuses().getSelectedPosition(), false);
        alertRadiusSpinner.setOnItemSelectedListener(this);

        // Map Capture
        mapCaptureFrequencySpinner = (Spinner) v.findViewById(R.id.map_capture_frequency_spinner);
        mapCaptureFrequencySpinner.setAdapter(mapCaptureFrequencyAdapter);
        mapCaptureFrequencySpinner.setSelection(mapCaptureFrequencyValues.indexOf(settings.getMapCapture()), false);
        mapCaptureFrequencySpinner.setOnItemSelectedListener(this);

        // Panic
        panicCancelPIN = (EditText) v.findViewById(R.id.panic_cancel_pin_edit);
        panicDuressPIN = (EditText) v.findViewById(R.id.panic_duress_pin_edit);
        if(String.valueOf(settings.getPanicPin()).length()<=3)
        {
            panicCancelPIN.setText(String.format("%04d",settings.getPanicPin()));
            Log.d("test",String.format("%04d",settings.getPanicPin()) );
        }
        else {
            panicCancelPIN.setText(String.valueOf(settings.getPanicPin()));
        }
        panicCancelPIN.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!initialized) {
                    return;
                }

                try {
                    String strValue = s.toString().trim();
                    final int value = Integer.valueOf(strValue);
                    if (strValue.length() < 4) {
                        Toast.makeText(getActivity(), R.string.enter_valid_pin, Toast.LENGTH_SHORT).show();
                    } else {
                        SetSetupFieldUtil.send(application, SetSetupFieldUtil.getUrl(application, "panicPin", strValue));
                        application.getAgentSettings().setpanicPin(value);
                        EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    }
                } catch (NumberFormatException ex) {
                    Toast.makeText(getActivity(), R.string.enter_valid_pin, Toast.LENGTH_SHORT).show();
                }
            }
        });

        if(String.valueOf(settings.getPanicDuressPin()).length()<=3)
        {
            panicDuressPIN.setText(String.format("%04d",settings.getPanicDuressPin()));
            Log.d("test",String.format("%04d",settings.getPanicPin()) );
        }
        else {
            panicDuressPIN.setText(String.valueOf(settings.getPanicDuressPin()));
        }
        panicDuressPIN.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!initialized) {
                    return;
                }

                try {
                    String strValue = s.toString().trim();
                    int value = Integer.valueOf(strValue);
                    if (strValue.length() < 4) {
                        Toast.makeText(getActivity(), R.string.enter_valid_pin, Toast.LENGTH_SHORT).show();
                    } else {
                        SetSetupFieldUtil.send(application, SetSetupFieldUtil.getUrl(application, "panicDuressPin", strValue));
                        application.getAgentSettings().setpanicDuressPin(value);
                        EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    }
                } catch (NumberFormatException ex) {
                    Toast.makeText(getActivity(), R.string.enter_valid_pin, Toast.LENGTH_SHORT).show();
                }
            }
        });

        phoneRecordCallsSpinner = (Spinner) v.findViewById(R.id.phonecalls_spinner);
        phoneSMSLogs = (Switch) v.findViewById(R.id.sms_switch);
        smsDeliverySpinner = (Spinner) v.findViewById(R.id.sms_delivery_spinner);
        phonePhoneLogs = (Switch) v.findViewById(R.id.phonelogs_switch);
        phoneLogDeliverySpinner = (Spinner) v.findViewById(R.id.phonelog_delivery_spinner);

        // Boss
        bossSwitch = (Switch) v.findViewById(R.id.boss_switch);
        bossPIN = (EditText) v.findViewById(R.id.boss_mode_pin_edit);
        bossStyleSpinner = (Spinner) v.findViewById(R.id.boss_mode_style_spinner);
        bossStyleSpinner.setAdapter(bossStyleAdapter);
        bossStyleSpinner.setSelection(bossStyleValues.indexOf(settings.getBossModeStyle()), false);
        bossStyleSpinner.setOnItemSelectedListener(this);
        if(String.valueOf(settings.getBossModePin()).length()<=3)
        {
            bossPIN.setText(String.format("%04d",settings.getBossModePin()));
           // Log.d("test",String.format("%04d",settings.getPanicPin()) );
        }
        else {
            bossPIN.setText(String.valueOf(settings.getBossModePin()));
        }
        bossPIN.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!initialized) {
                    return;
                }

                try {
                    String strValue = s.toString().trim();
                    int value = Integer.valueOf(strValue);
                    if (strValue.length() < 4) {
                        Toast.makeText(getActivity(), R.string.enter_valid_pin, Toast.LENGTH_SHORT).show();
                    } else {
                        SetSetupFieldUtil.send(application, SetSetupFieldUtil.getUrl(application, "bossModePin", strValue));
                        application.getAgentSettings().setbossModePin(value);
                        EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                    }
                } catch (NumberFormatException ex) {
                    Toast.makeText(getActivity(), R.string.enter_valid_pin, Toast.LENGTH_SHORT).show();
                }
            }
        });

        includeAudioSwitch = (Switch) v.findViewById(R.id.audio_switch);
        externalStorageSwitch = (Switch) v.findViewById(R.id.storage_switch);
        securitySwitch = (Switch) v.findViewById(R.id.security_switch);
        screenSpinner = (Spinner) v.findViewById(R.id.screen_spinner);
        pingSpinner = (Spinner) v.findViewById(R.id.ping_spinner);
        fixDaySpinner = (Spinner) v.findViewById(R.id.fix_day_spinner);
        fixNightSpinner = (Spinner) v.findViewById(R.id.fix_night_spinner);
        fixPanicSpinner = (Spinner) v.findViewById(R.id.fix_panic_spinner);
        trackingSpinner = (Spinner) v.findViewById(R.id.tracking_spinner);
        reportingSpinner = (Spinner) v.findViewById(R.id.reporting_spinner);
        photoQualitySpinner = (Spinner) v.findViewById(R.id.photo_quality_spinner);
        photoSizeSpinner = (Spinner) v.findViewById(R.id.photo_size_spinner);
        videoQualitySpinner = (Spinner) v.findViewById(R.id.video_quality_spinner);
        videoStreamQualitySpinner = (Spinner) v.findViewById(R.id.streamvideo_quality_spinner);
        videoSizeSpinner = (Spinner) v.findViewById(R.id.video_size_spinner);
        videoStreamSizeSpinner = (Spinner) v.findViewById(R.id.streamvideo_size_spinner);
        videoFrameRateSpinner = (Spinner) v.findViewById(R.id.video_framerate_spinner);
        videoStreamFrameRateSpinner = (Spinner) v.findViewById(R.id.streamvideo_framerate_spinner);

        // Case
        caseMapSpinner = (Spinner) v.findViewById(R.id.case_map_spinner);
        caseShowTrackersSwitch = (Switch) v.findViewById(R.id.case_show_trackers_switch);
        caseTrackerVisibilitySpinner = (Spinner) v.findViewById(R.id.case_tracker_visibility_spinner);
        caseAoiVisibilitySpinner = (Spinner) v.findViewById(R.id.case_aoi_visibility_spinner);
        caseTeamSpinner = (Spinner) v.findViewById(R.id.case_team_spinner);
        caseNumber = (EditText) v.findViewById(R.id.case_number_edit);
        caseDescription = (EditText) v.findViewById(R.id.case_description_edit);

        caseMapSpinner.setAdapter(caseMapAdapter);
        caseMapSpinner.setSelection(profile.getDefaultMaps().getSelectedPosition(), false);
        caseMapSpinner.setOnItemSelectedListener(this);
        caseShowTrackersSwitch.setChecked(profile.getGeneralTrackers().getSelected().getId().equals("1"));
        caseShowTrackersSwitch.setOnCheckedChangeListener(this);
        caseTrackerVisibilitySpinner.setAdapter(caseTrackerVisibilityAdapter);
        caseTrackerVisibilitySpinner.setSelection(profile.getTrackerVisibleRanges().getSelectedPosition(), false);
        caseTrackerVisibilitySpinner.setOnItemSelectedListener(this);
        caseAoiVisibilitySpinner.setAdapter(caseAoiVisibilityAdapter);
        caseAoiVisibilitySpinner.setSelection(profile.getAoiVisibleRanges().getSelectedPosition(), false);
        caseAoiVisibilitySpinner.setOnItemSelectedListener(this);
        caseTeamSpinner.setAdapter(caseTeamAdapter);
        caseTeamSpinner.setSelection(profile.getTeams().getSelectedPosition(), false);
        caseTeamSpinner.setOnItemSelectedListener(this);

        // Non-emergency
        frameNonEmergencyButton = v.findViewById(R.id.frame_non_emergency_button);

        nonEmergencyNumber = (EditText)v.findViewById(R.id.non_emergency_number);
        if (!TextUtils.isEmpty(settings.getNonEmergencyNumber())) {
            nonEmergencyNumber.setText(settings.getNonEmergencyNumber());
        }
        else {
            nonEmergencyNumber.setText(null);
        }

        nonEmergencyNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!initialized) {
                    return;
                }
                try {
                    String value = null;
                    if (!TextUtils.isEmpty(s)) {
                        value = s.toString().trim();
                    }
                    SetSetupFieldUtil.send(application, SetSetupFieldUtil.getUrl(application, "NonEmergencyNumber", value == null ? "" : value));
                    application.getAgentSettings().setNonEmergencyNumber(value);
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                }
                catch (Exception e) {
                    Timber.e(e,"Couldn't set non-emergency number.");
                }
            }
        });

        nonEmergencyLabel = (EditText)v.findViewById(R.id.non_emergency_label);
        if (!TextUtils.isEmpty(settings.getNonEmergencyLabel())) {
            nonEmergencyLabel.setText(settings.getNonEmergencyLabel());
        }
        else {
            nonEmergencyLabel.setText(null);
        }

        nonEmergencyLabel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!initialized) {
                    return;
                }
                try {
                    String value = null;
                    if (!TextUtils.isEmpty(s)) {
                        value = s.toString().trim();
                        // the maxLength attribute on the <EditText/> _should_  prevent this.
                        if (value.length() > 50) {
                            Toast.makeText(application.getApplicationContext(), R.string.value_too_long, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    SetSetupFieldUtil.send(application, SetSetupFieldUtil.getUrl(application, "NonEmergencyLabel", value == null ? "" : value));
                    application.getAgentSettings().setNonEmergencyLabel(value);
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                }
                catch (Exception e) {
                    Timber.e(e,"Couldn't set non-emergency label.");
                }
            }
        });


        // Emergency
        frameEmergencyButton = v.findViewById(R.id.frame_emergency_button);

        emergencyNumber = (EditText)v.findViewById(R.id.emergency_number);
        if (!TextUtils.isEmpty(settings.getSOSNumber())) {
            emergencyNumber.setText(settings.getSOSNumber());
        }
        else {
            emergencyNumber.setText(null);
        }

        emergencyNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!initialized) {
                    return;
                }
                try {
                    String value = null;
                    if (!TextUtils.isEmpty(s)) {
                        value = s.toString().trim();
                    }
                    SetSetupFieldUtil.send(application, SetSetupFieldUtil.getUrl(application, "SOSNumber", value == null ? "" : value));
                    application.getAgentSettings().setSOSNumber(value);
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                }
                catch (Exception e) {
                    Timber.e(e,"Couldn't set emergency number.");
                }
            }
        });

        emergencyLabel = (EditText)v.findViewById(R.id.emergency_label);
        if (!TextUtils.isEmpty(settings.getSOSLabel())) {
            emergencyLabel.setText(settings.getSOSLabel());
        }
        else {
            emergencyLabel.setText(null);
        }

        emergencyLabel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!initialized) {
                    return;
                }
                try {
                    String value = null;
                    if (!TextUtils.isEmpty(s)) {
                        value = s.toString().trim();
                        // the maxLength attribute on the <EditText/> _should_  prevent this.
                        if (value.length() > 50) {
                            Toast.makeText(application.getApplicationContext(), R.string.value_too_long, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    SetSetupFieldUtil.send(application, SetSetupFieldUtil.getUrl(application, "SOSLabel", value == null ? "" : value));
                    application.getAgentSettings().setSOSLabel(value);
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                }
                catch (Exception e) {
                    Timber.e(e,"Couldn't set non-emergency label.");
                }
            }
        });


        // Panic
        framePanicButton = v.findViewById(R.id.frame_panic_button);
        panicMessage = (EditText)v.findViewById(R.id.panic_button_message);
        if (!TextUtils.isEmpty(settings.getPanicMessage())) {
            panicMessage.setText(settings.getPanicMessage());
        }
        else {
            panicMessage.setText(null);
        }
        panicMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!initialized) {
                    return;
                }
                try {
                    String value = null;
                    if (!TextUtils.isEmpty(s)) {
                        value = s.toString().trim();
                    }
                    SetSetupFieldUtil.send(application, SetSetupFieldUtil.getUrl(application, "PanicMessage", value == null ? "" : value));
                    application.getAgentSettings().setPanicMessage(value);
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                }
                catch (Exception e) {
                    Timber.e(e,"Couldn't set emergency number.");
                }
            }
        });

        Spinner languageSpinner = (Spinner) v.findViewById(R.id.language_spinner);


        if (videoSettings != null && settings.getSecurity() == 1) {
            videoSettings.setVisibility(View.GONE);
        }

        if (storageSettings != null && settings.getSecurity() == 1) {
            storageSettings.setVisibility(View.GONE);
        }

        if (settings.getAllowTracking() > 0) {
            liveSwitch.setChecked(true);
        } else {
            liveSwitch.setChecked(false);
        }
        liveSwitch.setOnCheckedChangeListener(this);

        if (settings.getVibrateOnFix() > 0) {
            vibrateOnFixSwitch.setChecked(true);
        } else {
            vibrateOnFixSwitch.setChecked(false);
        }
        vibrateOnFixSwitch.setOnCheckedChangeListener(this);


        phoneRecordCallsSpinner.setAdapter(phoneCallsAdapter);
        if (settings != null) {
            //phoneRecordCallsSpinner.setSelection(settings.getRecordCalls(), false);
            phoneRecordCallsSpinner.setSelection(settings.getRecordCalls(), false);
        }
        phoneRecordCallsSpinner.setOnItemSelectedListener(this);

        if (settings.getSMSLog() > 0) {
            phoneSMSLogs.setChecked(true);
        } else {
            phoneSMSLogs.setChecked(false);
        }
        phoneSMSLogs.setOnCheckedChangeListener(this);

        smsDeliverySpinner.setAdapter(smsDeliveryAdapter);
        if (settings != null) {
            //smsDeliverySpinner.setSelection(settings.getSMSLogDelivery(), false);
            smsDeliverySpinner.setSelection(smsDeliveryValues.indexOf(settings.getSMSLogDelivery()), false);
        }
        smsDeliverySpinner.setOnItemSelectedListener(this);

        if (settings.getPhoneLog() > 0) {
            phonePhoneLogs.setChecked(true);
        } else {
            phonePhoneLogs.setChecked(false);
        }
        phonePhoneLogs.setOnCheckedChangeListener(this);

        phoneLogDeliverySpinner.setAdapter(phoneLogDeliveryAdapter);
        if (settings != null) {
            //phoneLogDeliverySpinner.setSelection(settings.getPhoneLogDelivery(), false);
            phoneLogDeliverySpinner.setSelection(phoneLogDeliveryValues.indexOf(settings.getPhoneLogDelivery()), false);
        }
        phoneLogDeliverySpinner.setOnItemSelectedListener(this);

        if (settings.getIncludeAudio() > 0) {
            includeAudioSwitch.setChecked(true);
        } else {
            includeAudioSwitch.setChecked(false);
        }
        includeAudioSwitch.setOnCheckedChangeListener(this);

        externalStorageSwitch.setChecked(settings.getExternalStorage() == 1);
        externalStorageSwitch.setOnCheckedChangeListener(this);

        bossSwitch.setChecked(application.isBossModeEnabled());
        bossSwitch.setOnCheckedChangeListener(this);

        securitySwitch.setChecked(settings.getSecurity() == 1);
        securitySwitch.setOnCheckedChangeListener(this);

        screenSpinner.setAdapter(screenAdapter);
        if (settings != null) {
            screenSpinner.setSelection(AgentTabs.indexOfValue(settings.getScreen()), false);
        }
        screenSpinner.setOnItemSelectedListener(this);

        pingSpinner.setAdapter(pingAdapter);
        if (settings != null) {
            pingSpinner.setSelection(pingValues.indexOf(settings.getPingInterval()), false);
        }
        pingSpinner.setOnItemSelectedListener(this);

        fixDaySpinner.setAdapter(fixAdapter);
        if (settings != null) {
            fixDaySpinner.setSelection(fixValues.indexOf(settings.getFixIntervalDaytime()), false);
        }
        fixDaySpinner.setOnItemSelectedListener(this);

        fixNightSpinner.setAdapter(fixAdapter);
        if (settings != null) {
            fixNightSpinner.setSelection(fixValues.indexOf(settings.getFixIntervalNighttime()), false);
        }
        fixNightSpinner.setOnItemSelectedListener(this);

        fixPanicSpinner.setAdapter(fixAdapter);
        if (settings != null) {
            fixPanicSpinner.setSelection(fixValues.indexOf(settings.getFixIntervalPanic()), false);
        }
        fixPanicSpinner.setOnItemSelectedListener(this);

        reportingSpinner.setAdapter(reportingAdapter);
        if (settings != null) {
            reportingSpinner.setSelection(reportingValues.indexOf(settings.getReportInterval()), false);
        }
        reportingSpinner.setOnItemSelectedListener(this);

        trackingSpinner.setAdapter(trackingAdapter);
        if (settings != null) {
            trackingSpinner.setSelection(trackingValues.indexOf(settings.getMaxTracking()), false);
        }
        trackingSpinner.setOnItemSelectedListener(this);

        photoQualitySpinner.setAdapter(photoQualityAdapter);
        if (settings != null) {
            photoQualitySpinner.setSelection(qualityValues.indexOf(settings.getPhotoQuality()), false);
        }
        photoQualitySpinner.setOnItemSelectedListener(this);

        photoSizeSpinner.setAdapter(photoSizeAdapter);
        if (settings != null) {
            photoSizeSpinner.setSelection(sizeValues.indexOf(settings.getPhotoSize()), false);
        }
        photoSizeSpinner.setOnItemSelectedListener(this);

        videoQualitySpinner.setAdapter(videoQualityAdapter);
        if (settings != null) {
            videoQualitySpinner.setSelection(qualityValues.indexOf(settings.getVideoCasesQuality()), false);
        }
        videoQualitySpinner.setOnItemSelectedListener(this);

        videoStreamQualitySpinner.setAdapter(videoStreamQualityAdapter);
        if (settings != null) {
            videoStreamQualitySpinner.setSelection(qualityValues.indexOf(settings.getVideoStreamQuality()), false);
        }
        videoStreamQualitySpinner.setOnItemSelectedListener(this);

        videoSizeSpinner.setAdapter(videoSizeAdapter);
        if (settings != null) {
            videoSizeSpinner.setSelection(sizeValues.indexOf(settings.getVideoCasesSize()), false);
        }
        videoSizeSpinner.setOnItemSelectedListener(this);

        videoStreamSizeSpinner.setAdapter(videoStreamSizeAdapter);
        if (settings != null) {
            videoStreamSizeSpinner.setSelection(sizeValues.indexOf(settings.getVideoStreamSize()), false);
        }
        videoStreamSizeSpinner.setOnItemSelectedListener(this);

        videoFrameRateSpinner.setAdapter(videoFrameRateAdapter);
        if (settings != null) {
            int index = frameRateValues.indexOf(settings.getVideoCasesFrameRate());
            // that value doesn't exist (iOS framerate?) using 30.
            if (index < 0) {
                //Timber.d("Server sent us an FPS of " + settings.getVideoCasesFrameRate() + ". Defaulting to 30FPS.");
                frameRateValues.indexOf(30);
                settings.setVideoCasesFrameRate(30);
                MessageUrl messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|VideoCasesFrameRateCASESAgent@30");
                MessageUtil.sendMessage(application, messageUrl);
            }
            videoFrameRateSpinner.setSelection(index, false);
        }

        videoFrameRateSpinner.setOnItemSelectedListener(this);

        videoStreamFrameRateSpinner.setAdapter(videoStreamFrameRateAdapter);
        if (settings != null) {
            videoStreamFrameRateSpinner.setSelection(streamFrameRateValues.indexOf(settings.getVideoStreamFrameRate()), false);
        }
        videoStreamFrameRateSpinner.setOnItemSelectedListener(this);

        caseNumber.setText(settings.getCaseNumber());
        caseNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!initialized) {
                    return;
                }

                // the replace shouldn't be necessary here, but just in case the number
                // import doesn't work for some reason....
                String value = s.toString().replace("@", "(at)").replace("|", "(pipe)").replace("!", "(tilde)");
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Set Settings|CaseNumberCASESAgent@" + value));
                application.getAgentSettings().setCaseNumber(value);
                EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
            }
        });
        caseDescription.setText(settings.getCaseDescription());
        caseDescription.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!initialized) {
                    return;
                }
                // can't have |, @ or ~
                String value = s.toString().replace("@", "(at)").replace("|", "(pipe)").replace("!", "(tilde)");
                MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Set Settings|CaseDescriptionCASESAgent@" + value));
                application.getAgentSettings().setCaseDescription(value);
                EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
            }
        });

        languageCodes = Arrays.asList(getResources().getStringArray(R.array.language_code));
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(getActivity(), R.layout.start_spinner_item, Arrays.asList(getResources().getStringArray(R.array.languages)));
        languageSpinner.setAdapter(languageAdapter);

        String selectedLanguage = sharedPreferences.getString(Fuzz.en("language", application.getDeviceIdentifier()), null);
        if (selectedLanguage != null) {
            languageSpinner.setSelection(languageCodes.indexOf(Fuzz.de(selectedLanguage, application.getDeviceIdentifier())));
        } else {
            int current = languageCodes.indexOf(Locale.getDefault().getLanguage());
            if (current >= 0) {
                languageSpinner.setSelection(current);
            } else {
                languageSpinner.setSelection(languageCodes.indexOf(null));
            }
        }

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean initialized = false;

            @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!initialized) {
                    initialized = true;
                    return;
                }


                String code = languageCodes.get(position);
                if (position == 0) {
                    if (sharedPreferences.contains(Fuzz.en("language", application.getDeviceIdentifier()))) {
                        sharedPreferences.edit().remove(Fuzz.en("language", application.getDeviceIdentifier())).commit();
                        languageDialog = AlertDialogFragment.newInstance(
                                getString(R.string.language_changed),
                                getString(R.string.language_restart)
                        );
                        languageDialog.show(getFragmentManager(), "language-changed-dialog");
                    }
                    return;
                }

                String currentCode = null;
                if (sharedPreferences.contains(Fuzz.en("language", application.getDeviceIdentifier()))) {
                    currentCode = Fuzz.de(sharedPreferences.getString(Fuzz.en("language", application.getDeviceIdentifier()), null), application.getDeviceIdentifier());
                }

                if (currentCode != null && code.equals(currentCode)) {
                    return;
                }


                Resources res = application.getResources();
                // Change locale settings in the app.
                DisplayMetrics dm = res.getDisplayMetrics();
                Configuration conf = res.getConfiguration();


                boolean committed;

                // if the code is null, or the code matches the current system language, remove the setting
                if (code == null || code.equals(Locale.getDefault().getLanguage())) {
                    conf.locale = Locale.getDefault();
                    conf.setLayoutDirection(conf.locale);
                    committed = sharedPreferences.edit().remove(Fuzz.en("language", application.getDeviceIdentifier())).commit();
                } else {
                    conf.locale = new Locale(code);
                    conf.setLayoutDirection(conf.locale);
                    committed = sharedPreferences.edit().putString(Fuzz.en("language", application.getDeviceIdentifier()), Fuzz.en(code, application.getDeviceIdentifier())).commit();
                }

                if (committed) {
                    // in order to get the application to display the new language, we have to
                    // restart the activity so that it reloads and re-displays all the locale
                    // specific assets.
                    res.updateConfiguration(conf, dm);

                    languageDialog2 = AlertDialogFragment.newInstance(
                            getString(R.string.language_changed),
                            getString(R.string.language_restart)
                    );
                    languageDialog2.show(getFragmentManager(), "language-changed-dialog2");

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
      //  TextView txtnamedevice= (TextView) v.findViewById(R.id.txt_name_device);
        frameBluetooth =  v.findViewById(R.id.frame_bluetooth_device);
        name_device = (EditText) v.findViewById(R.id.edt_name_device);
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        name_device.setText(prefs.getString("device_name",""));
        name_device.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                prefs.edit().putString("device_name", s.toString()).commit();
            }
        });
        // changes visibility of groups of views.
        updateRoles(settings.getAgentRoles());

        EventBus.getDefault().post(new Events.DisplayChanged("Preferences", R.id.preferences));
        initialized = true;








        return v;

    }


    private void addScreens() {
        screens.clear();
        // Add tabs according to the order of values in the AgentTabs enum
        for (AgentTabs tab : AgentTabs.values()) {
            switch (tab) {
//                case TAB_HOME: {
//                    screens.add(getString(R.string.drawer_home));
//                    break;
//                }
//                case TAB_CHECKIN: {
//                    screens.add(getString(R.string.drawer_checkin));
//                    break;
//                }
//                case TAB_TIMER: {
//                    screens.add(getString(R.string.drawer_timer));
//                    break;
//                }
//                case TAB_CONTACTS: {
//                    screens.add(getString(R.string.drawer_contacts));
//                    break;
//                }
                case TAB_CHAT: {
                    screens.add(getString(R.string.drawer_chat));
                    break;
                }
//                case TAB_COLLECT: {
//                    screens.add(getString(R.string.drawer_collect));
//                    break;
//                }
//                case TAB_MAP: {
//                    screens.add(getString(R.string.drawer_map));
//                    break;
//                }
//                case TAB_POI: {
//                    screens.add(getString(R.string.drawer_poi));
//                    break;
//                }
//                case TAB_ALERTS: {
//                    screens.add(getString(R.string.drawer_alerts));
//                    break;
//                }
//                case TAB_MEDIA: {
//                    screens.add(getString(R.string.drawer_media));
//                    break;
//                }
                default: {
                    // This will never happen?
                }
            }
        }

        screenAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item, screens);
        screenAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        // Force close spinners because they may stay open when user presses Home button
        closeSpinners((ViewGroup) this.getView());

        EventBus.getDefault().unregister(this);
        if (languageDialog != null) {
            languageDialog.dismissAllowingStateLoss();
            languageDialog = null;
        }
        if (languageDialog2 != null) {
            languageDialog2.dismissAllowingStateLoss();
            languageDialog2 = null;
        }
        if (notChangedDialog != null) {
            notChangedDialog.dismissAllowingStateLoss();
            notChangedDialog = null;
        }
        if (changedDialog != null) {
            changedDialog.dismissAllowingStateLoss();
            changedDialog = null;
        }
        if (confirmDialog != null) {
            confirmDialog.dismissAllowingStateLoss();
            confirmDialog = null;
        }
        if (slNotChanged != null) {
            slNotChanged.dismissAllowingStateLoss();
            slNotChanged = null;
        }

        super.onPause();
    }

    private void closeSpinners(ViewGroup vg) {
        try {
            Method method = Spinner.class.getDeclaredMethod("onDetachedFromWindow");
            method.setAccessible(true);

            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = vg.getChildAt(i);
                if (v != null) {
                    if (v instanceof Spinner) {
                        method.invoke((Spinner) v);
                    } else if (v instanceof ViewGroup) {
                        closeSpinners((ViewGroup) v);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.SecurityLevelChanged change) {
        //Timber.d("Processing change.");
        if (securitySwitch != null) {
            securitySwitch.setOnCheckedChangeListener(null);
            securitySwitch.setChecked(application.getAgentSettings().getSecurity() == 1);
            securitySwitch.setOnCheckedChangeListener(this);
        }

        int visibility = View.VISIBLE;
        if (application.getAgentSettings().getSecurity() == 1) {
            mediaManager.setCaptureMode(Givens.COLLECT_MODE_PICTURE);
            visibility = View.GONE;
        }

        if (videoSettings != null) {
            //Timber.d("Changing visiblity of video settings");
            videoSettings.setVisibility(visibility);
        } else {
            //Timber.d("VideoSettings Is Null");
        }
        if (storageSettings != null) {
            //Timber.d("Changing visiblity of storage settings");
            storageSettings.setVisibility(visibility);
        } else {
            //Timber.d("StorageSettings Is Null");
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!initialized) {
            return;
        }

        boolean profileChanged = false;
        int id = buttonView.getId();
        MessageUrl messageUrl = null;
        final AgentSettings settings = application.getAgentSettings();
        final AgentProfile profile = application.getAgentProfile();

        switch (id) {
            case R.id.storage_switch: {
                if (isChecked) {
                    settings.setExternalStorage(1);
                    MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Set Settings|ExternalStorageCASESAgent@1"));
                } else {
                    settings.setExternalStorage(0);
                    MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Set Settings|ExternalStorageCASESAgent@0"));
                }
                break;
            }
            case R.id.security_switch: {
                if (mediaManager.isRecording()) {
                    notChangedDialog = AlertDialogFragment.newInstance(getString(R.string.security_not_changed), getString(R.string.not_change_security_level_while_recording));
                    notChangedDialog.show(getFragmentManager(), "alert");
                    if (isChecked) {
                        securitySwitch.setOnCheckedChangeListener(null);
                        securitySwitch.setChecked(false);
                        securitySwitch.setOnCheckedChangeListener(this);
                    } else {
                        securitySwitch.setOnCheckedChangeListener(null);
                        securitySwitch.setChecked(true);
                        securitySwitch.setOnCheckedChangeListener(this);
                    }
                    return;
                }
                if (isChecked) {
                    if (settings.getSecurity() == 1) {
                        //Timber.d("Already high security.");
                        return;
                    }
                    if (settings.getSecurity() == 0) {
                        //Timber.d("Going from medium to high.");
                        settings.setSecurity(1);
                        MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Set Settings|SecurityCASESAgent@1"));
                        EventBus.getDefault().post(new Events.SecurityLevelChanged(1));
                        EventBus.getDefault().post(new Events.AgentSettingsAcquired(settings));
                        changedDialog = AlertDialogFragment.newInstance(getString(R.string.security_changed), getString(R.string.security_changed_description));
                        changedDialog.show(getFragmentManager(), "alert");
                    }
                } else {
                    if (settings.getSecurity() == 0) {
                        //Timber.d("Already medium security.");
                        return;
                    }
                    if (settings.getSecurity() == 1) {
                        //Timber.d("Going from high to medium.");
                        confirmDialog = ConfirmStorageDialog.newInstance();
                        confirmDialog.setCustomDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                if (settings.getSecurity() == 1) {
                                    securitySwitch.setChecked(true);
                                }
                            }
                        });
                        confirmDialog.setCustomCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                if (settings.getSecurity() == 1) {
                                    securitySwitch.setChecked(true);
                                }
                            }
                        });
                        confirmDialog.show(getFragmentManager(), "confirm");

                        return;
                    }
                }

                break;
            }
            case R.id.audio_switch: {
                if (isChecked) {
                    messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|IncludeAudio@Yes");
                    settings.setIncludeAudio(1);
                } else {
                    messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|IncludeAudio@No");
                    settings.setIncludeAudio(0);
                }
                break;
            }
            case R.id.live_switch: {
                if (isChecked) {
                    messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|AllowTrackingCASESAgent@1");
                    settings.setAllowTracking(1);
                } else {
                    messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|AllowTrackingCASESAgent@0");
                    settings.setAllowTracking(0);
                    EventBus.getDefault().post(new Events.StopLiveTracking());
                }
                break;
            }
            case R.id.vibrate_fix_switch: {
                if (isChecked) {
                    messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|VibrateOnFixCASESAgent@1");
                    settings.setVibrateOnFix(1);
                } else {
                    messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|VibrateOnFixCASESAgent@0");
                    settings.setVibrateOnFix(0);
                }
                break;
            }

            case R.id.sms_switch: {
                application.setSMSLogs(isChecked);
                if (isChecked) {
                    messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|SMSLogCASESAgent@1");
                    settings.setSMSLog(1);
                } else {
                    messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|SMSLogCASESAgent@0");
                    settings.setSMSLog(0);
                }
                break;
            }

            case R.id.phonelogs_switch: {
                application.setPhoneLogs(isChecked);
                if (isChecked) {
                    messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|PhoneLogCASESAgent@1");
                    settings.setPhoneLog(1);
                } else {
                    messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|PhoneLogCASESAgent@0");
                    settings.setPhoneLog(0);
                }
                break;
            }

            case R.id.boss_switch: {
                if (isChecked) {
                    application.enableBossMode();
                    messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|BossCASESAgent@1");
                    if (settings != null) {
                        settings.setBoss(1);
                    }
                } else {
                    application.disableBossMode();
                    messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|BossCASESAgent@0");
                    if (settings != null) {
                        settings.setBoss(0);
                    }
                }
                break;
            }

            // Case
            case R.id.case_show_trackers_switch: {
                profileChanged = true;
                profile.getGeneralTrackers().setSelected(isChecked ? 1 : 0);
                break;
            }
        }
        if (messageUrl != null) {
            // Send new config setting to the server
            MessageUtil.sendMessage(application, messageUrl);
        }
        // Notify UI about config changes
        EventBus.getDefault().post(new Events.AgentSettingsAcquired(settings));

        if (profileChanged) {
            // Send new profile settings to the server
            ProfileService.postAgentProfile(application, profile);
            // Notify UI about profile changes
            EventBus.getDefault().post(new Events.AgentProfileAcquired(profile));
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (!initialized) {
            return;
        }

        int viewId = parent.getId();

        boolean profileChanged = false;
        MessageUrl messageUrl = null;
        AgentSettings settings = application.getAgentSettings();
        AgentProfile profile = application.getAgentProfile();

        switch (viewId) {
            case R.id.photo_quality_spinner: {
                int value = qualityValues.get(position);
                settings.setPhotoQuality(value);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|PhotoQualityCASESAgent@" + String.valueOf(value));
                break;
            }
            case R.id.video_quality_spinner: {
                int value = qualityValues.get(position);
                settings.setVideoCasesQuality(value);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|VideoCasesQualityCASESAgent@" + String.valueOf(value));
                break;
            }
            case R.id.photo_size_spinner: {
                int value = sizeValues.get(position);
                settings.setPhotoSize(value);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|PhotoSizeCASESAgent@" + String.valueOf(value));
                break;
            }
            case R.id.video_size_spinner: {
                int value = sizeValues.get(position);
                settings.setVideoCasesSize(value);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|VideoCasesSizeCASESAgent@" + String.valueOf(value));
                break;
            }
            case R.id.video_framerate_spinner: {
                int value = frameRateValues.get(position);
                settings.setVideoCasesFrameRate(value);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|VideoCasesFrameRateCASESAgent@" + String.valueOf(value));
                break;
            }
            case R.id.streamvideo_quality_spinner: {
                int value = qualityValues.get(position);
                settings.setVideoStreamQuality(value);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|VideoStreamQualityCASESAgent@" + String.valueOf(value));
                break;
            }

            case R.id.streamvideo_size_spinner: {
                int value = sizeValues.get(position);
                settings.setVideoStreamSize(value);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|VideoStreamSizeCASESAgent@" + String.valueOf(value));
                break;
            }
            case R.id.streamvideo_framerate_spinner: {
                int value = streamFrameRateValues.get(position);
                settings.setVideoStreamFrameRate(value);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|VideoStreamFrameRateCASESAgent@" + String.valueOf(value));
                break;
            }
            case R.id.tracking_spinner: {
                int tracking = trackingValues.get(position);
                settings.setMaxTracking(tracking);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|MaxTrackingCASESAgent@" + String.valueOf(tracking));
                break;
            }
            case R.id.reporting_spinner: {
                int report = reportingValues.get(position);
                settings.setReportInterval(report);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|ReportIntervalCASESAgent@" + String.valueOf(report));
                break;
            }
            case R.id.fix_day_spinner: {
                int fix = fixValues.get(position);
                settings.setFixInterval(fix);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|FixIntervalCASESAgent@" + String.valueOf(fix));
                break;
            }
            case R.id.fix_night_spinner: {
                int fix = fixValues.get(position);
                settings.setFixIntervalNighttime(fix);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|FixIntervalNighttimeCASESAgent@" + String.valueOf(fix));
                break;
            }
            case R.id.fix_panic_spinner: {
                int fix = fixValues.get(position);
                settings.setFixIntervalPanic(fix);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|FixIntervalPanicCASESAgent@" + String.valueOf(fix));
                break;
            }
            case R.id.ping_spinner: {
                int ping = pingValues.get(position);
                settings.setPingInterval(ping);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|PingIntervalCASESAgent@" + String.valueOf(ping));
                break;
            }
            case R.id.screen_spinner: {
                settings.setScreen(AgentTabs.values()[position].getValue());
                settings.setCurrentScreen(settings.getScreen());
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|ScreenCASESAgent@" + String.valueOf(position));
                break;
            }
            case R.id.phonecalls_spinner: {
                application.setRecordCalls(position);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|RecordCallsCASESAgent@" + String.valueOf(position));
                break;
            }
            case R.id.sms_delivery_spinner: {
                int delivery = smsDeliveryValues.get(position);
                application.setSMSLogDelivery(delivery);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|SMSLogDeliveryCASESAgent@" + String.valueOf(delivery));
                break;
            }
            case R.id.phonelog_delivery_spinner: {
                int delivery = phoneLogDeliveryValues.get(position);
                application.setPhoneLogDelivery(delivery);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|PhoneLogDeliveryCASESAgent@" + String.valueOf(delivery));
                break;
            }
            // Case
            case R.id.case_map_spinner: {
                profileChanged = true;
                profile.getDefaultMaps().setSelected(position);
                break;
            }
            case R.id.case_tracker_visibility_spinner: {
                profileChanged = true;
                profile.getTrackerVisibleRanges().setSelected(position);
                break;
            }
            case R.id.case_aoi_visibility_spinner: {
                profileChanged = true;
                profile.getAoiVisibleRanges().setSelected(position);
                break;
            }
            case R.id.case_team_spinner: {
                profileChanged = true;
                profile.getTeams().setSelected(position);
                break;
            }
            // Alerts
            case R.id.receive_alerts_spinner: {
                profileChanged = true;
                profile.getDayOfWeeks().setSelected(position);
                break;
            }
            case R.id.alert_from_spinner: {
                profileChanged = true;
                profile.getStartDaytimes().setSelected(position);
                break;
            }
            case R.id.alert_to_spinner: {
                profileChanged = true;
                profile.getEndDaytimes().setSelected(position);
                break;
            }
            case R.id.alert_radius_spinner: {
                profileChanged = true;
                profile.getAlertRadiuses().setSelected(position);
                break;
            }
            // Boss
            case R.id.boss_mode_style_spinner: {
                int bossStyle = bossStyleValues.get(position);
                settings.setBossModeStyle(bossStyle);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|BossModeStyleCASESAgent@" + String.valueOf(bossStyle));
                break;
            }
            // Map Capture
            case R.id.map_capture_frequency_spinner: {
                int freq = mapCaptureFrequencyValues.get(position);
                settings.setMapCapture(freq);
                messageUrl = MessageUtil.getMessageUrl(application, "Set Settings|MapCaptureCASESAgent@" + String.valueOf(freq));
                break;
            }
        }

        if (messageUrl != null) {
            // Send new config setting to the server
            MessageUtil.sendMessage(application, messageUrl);
        }
        // Notify UI about config changes
        EventBus.getDefault().post(new Events.AgentSettingsAcquired(settings));

        if (profileChanged) {
            // Send new profile settings to the server
            ProfileService.postAgentProfile(application, profile);
            // Notify UI about profile changes
            EventBus.getDefault().post(new Events.AgentProfileAcquired(profile));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.AgentRolesUpdated event) {
        updateRoles(event.getAgentRoles());
    }

    public void onEventMainThread(Events.SecurityLevelNotChanged event) {
        slNotChanged = AlertDialogFragment.newInstance(getString(R.string.security_not_changed), getString(R.string.security_not_changed_description));
        slNotChanged.show(getFragmentManager(), "alert");
    }


    @Override
    public void updateRoles(AgentRoles roles) {
        // About
        frameAbout.setVisibility(View.VISIBLE);
        // Alerts
        frameAlerts.setVisibility(View.VISIBLE);
        // Panic
        framePanic.setVisibility(View.VISIBLE);
        // Map Capture
        frameMapCapture.setVisibility(View.VISIBLE);
        // live tracking
        frameLiveTracking.setVisibility((roles.map() && roles.livetracking()) ? View.VISIBLE : View.GONE);
        // default screen
        frameDefaultScreen.setVisibility(roles.defaultscreen() ? View.VISIBLE : View.GONE);

        // collect settings
        if (roles.collect()) {
            // photos
            framePhoto.setVisibility((roles.photocollect() && roles.photos()) ? View.VISIBLE : View.GONE);
            // videos
            frameVideo.setVisibility((roles.videorecord() && roles.videocases()) ? View.VISIBLE : View.GONE);
            // stream
            frameStream.setVisibility((roles.videostream() && roles.videostreaming()) ? View.VISIBLE : View.GONE);
            // collect
            framePhone.setVisibility(getResources().getBoolean(R.bool.enable_phone_logs) ? View.VISIBLE : View.GONE);
        } else {
            framePhoto.setVisibility(View.GONE);
            frameVideo.setVisibility(View.GONE);
            frameStream.setVisibility(View.GONE);
            framePhone.setVisibility(View.GONE);
        }

        // boss mode
        frameBoss.setVisibility((roles.bossmode() && roles.bossmodepref()) ? View.VISIBLE : View.GONE);
        // cases
        frameCases.setVisibility(roles.cases() ? View.VISIBLE : View.GONE);
        // storage
        frameStorage.setVisibility(View.VISIBLE);
        // security
        frameSecurity.setVisibility(roles.security() ? View.VISIBLE : View.GONE);
        // language
        frameLanguage.setVisibility(View.VISIBLE);

        frameNonEmergencyButton.setVisibility(roles.buttonnonemergency() ? View.VISIBLE : View.GONE);
        frameEmergencyButton.setVisibility(roles.buttonemergency() ? View.VISIBLE : View.GONE);
        framePanicButton.setVisibility(roles.buttonpanic() ? View.VISIBLE : View.GONE);

        frameBluetooth.setVisibility(View.VISIBLE);


    }
}
