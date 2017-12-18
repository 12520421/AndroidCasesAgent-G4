package com.xzfg.app.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.BaseFragment;
import com.xzfg.app.DebugLog;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.managers.G4Manager;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.services.GattClient;
import com.xzfg.app.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import static android.content.Context.BLUETOOTH_SERVICE;
import static com.xzfg.app.fragments.StatusFragment.SERVICE_UUID;

public class ConfigurationFragment extends BaseFragment implements AdapterView.OnItemSelectedListener {
    Spinner spinnerNormal;
    Spinner spinerAlert;
    Spinner spinnerLed;
    Spinner spinnerAlertMark;
    SharedPreferences sharedPref ;
    SharedPreferences.Editor editor;
    Spinner spinnerMsgTimeout;
    ArrayAdapter<String> adapterNormal;
    ArrayAdapter<String> adapterAlert;
    ArrayAdapter<String> adapterLed;
    ArrayAdapter<String> adapterAlertMark;
    ArrayAdapter<String> adapterTimeout;
    ArrayList<String> arrNormal;
    ArrayList<String> arrAlert;
    ArrayList<String> arrLed;
    ArrayList<String> arrAlertMark;
    ArrayList<String> arrMsgTimeout;
    ReconnectHandler reconnectHandler;
    private GattClient mGattClient;
Handler handlerMessage;
    int count = 0;
    boolean isScreen=false;
    String address = null;
    @Inject
    Application application;
    @Inject
    G4Manager g4Manager;
    StringBuilder databuilder = new StringBuilder(200);
    Handler handler=new Handler();

    GattClient.OnCounterReadListener listenerGat = new GattClient.OnCounterReadListener() {
        @Override
        public void onCounterRead(final String value) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (databuilder.indexOf(value) < 0) {
                        databuilder.append(value);
                    }
                    //find OK to get data
                    if (databuilder.indexOf("OK") > 0) {
                        g4Manager.processingData(databuilder);
                        g4Manager.executeCommand(mGattClient);
                        g4Manager.detectNewMessage(databuilder.toString());
                        databuilder.delete(0, databuilder.length());
                    }
                    UpdateUI();
                }
            });
        }

        @Override
        public void onConnected(boolean success) {
            if (!success) {
                //reconnect1();
                //  EventBus.getDefault().postSticky(new Events.handleDisConnect("Connect"));
                EventBus.getDefault().postSticky(new Events.sendEvent());
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
                        ///
                        /*if (!Utils.isAppIsInBackground(getActivity())){
                            if (isScreen) {
                                Toast toast = Toast.makeText(getActivity(), "Disconnected", Toast.LENGTH_SHORT);
                                if (toast.getView().isShown()) {
                                    toast.cancel();
                                } else {
                                    toast.show();
                                }
                            }
                        }*/

                    }
                });


                g4Manager.setConnectionState(false);
                try {
                    mGattClient.onDestroy();
                } catch (Exception e) {
                }

            } else {
               // EventBus.getDefault().post(new Events.UpdateAlertState());
                if (g4Manager==null)
                    return;

                if (g4Manager!=null) {
			/*if (isConnect) {
				CheckAlertState();
			}*/
                    if (g4Manager.isPastAlertState()) {
                        mGattClient.writeInteractor("AT$FFALERT=1",false);
                        DebugLog.d("check mark: "+g4Manager.isPastAlertState());
				/*if (g4Manager != null) {
					g4Manager = application.getG4Manager();
				}
				if (!g4Manager.isConnectionState()) {
					return;
				}
					g4Manager.setlastMarkedDate(new Date());
					Log.d("StateAlert", "Click Alert On " + String.valueOf(pastAlertState));
					g4Manager.resetCommandList();
					g4Manager.setAlertOn();
					g4Manager.getGPSFix();
					pastAlertState = g4Manager.isAlertState();
					g4Manager.setAlertState(true);

					g4Manager.setAlertClicked(true);
					//  g4Manager = application.getG4Manager();
					g4Manager.executeCommand(mGattClient);
					startRepeatingTask();
				CheckAlertState();*/
                    }
                }
                try {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            EventBus.getDefault().post(new Events.BluetoothStatus(true));
                            ///
                            /*if (!Utils.isAppIsInBackground(getActivity())){
                                Toast toast = Toast.makeText(getActivity(), "Connected Successfully", Toast.LENGTH_SHORT);
                                if(toast.getView().isShown()){
                                    toast.cancel();
                                }else {
                                    toast.show();
                                }
                            }*/

                            InitSetting();
                            //wait for G4 so it can receive command
                            SystemClock
                                    .sleep(500);
                            if(spinnerLed.getSelectedItem().toString().equals("High"))
                            {
                                g4Manager.resetCommandList();
                                g4Manager.setLedBrightness(254);
                                g4Manager.executeCommand(mGattClient);
                            }
                            else if (spinnerLed.getSelectedItem().toString().equals("Medium"))
                            {
                                g4Manager.resetCommandList();
                                g4Manager.setLedBrightness(128);
                                g4Manager.executeCommand(mGattClient);
                            }
                            else if (spinnerLed.getSelectedItem().toString().equals("Low"))
                            {
                                g4Manager.resetCommandList();
                                g4Manager.setLedBrightness(50);
                                g4Manager.executeCommand(mGattClient);
                            }
                            else if (spinnerLed.getSelectedItem().toString().equals("Off"))
                            {
                                g4Manager.resetCommandList();
                                g4Manager.setLedBrightness(0);
                                g4Manager.executeCommand(mGattClient);
                            }
                            g4Manager.setConnectionState(true);

                            if (reconnectHandler != null) {
                                reconnectHandler.stopReconnect();
                            }
                        }
                    });
                }catch (Exception e){
                    e.toString();
                }
            }
        }
        @Override
        public void onRSSIChange(int rssi) {
        }
    };

    @Override
    public void binData(String result) {
        if (result.contains("OK")) {
            if (result.contains("GPMS=0")) {
                DebugLog.d("new Message: "+result);
                g4Manager.detectNewMessage(result);
            }
            //g4Manager.setStringBuider(null);
        }
    }

    @Override
    public void binConnectionState(boolean isConnect) {

    }

    @Override
    public void binRssi(int rssi) {

    }
Runnable runCheckNewMessage=new Runnable() {
    @Override
    public void run() {
        g4Manager=application.getG4Manager();
        if (g4Manager!=null){
            DebugLog.d("send command :");
            g4Manager.getClient().writeInteractor("AT$GPMS=0", false);
        }
        handlerMessage.postDelayed(runCheckNewMessage,10000);

    }
};
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        arrNormal = new ArrayList<>();
        arrNormal.add("1 Minute");
        arrNormal.add("2 Minutes");
        arrNormal.add("4 Minutes");
        arrNormal.add("6 Minutes");
        arrNormal.add("10 Minutes");
        arrNormal.add("15 Minutes");
        adapterNormal = new ArrayAdapter<String>(getActivity(),R.layout.simple_spinner_item,arrNormal);
        adapterNormal.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        arrAlert = new ArrayList<>();
        arrAlert.add("1 Minute");
        arrAlert.add("2 Minutes");
        arrAlert.add("4 Minutes");
        arrAlert.add("6 Minutes");
        arrAlert.add("10 Minutes");
        arrAlert.add("15 Minutes");
        adapterAlert = new ArrayAdapter<String>(getActivity(),R.layout.simple_spinner_item,arrAlert);
        adapterAlert.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        arrLed = new ArrayList<>();
        arrLed.add("High");
        arrLed.add("Medium");
        arrLed.add("Low");
        arrLed.add("Off");

        adapterLed = new ArrayAdapter<String>(getActivity(),R.layout.simple_spinner_item,arrLed);
        adapterLed.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        arrAlertMark = new ArrayList<>();
        arrAlertMark.add("Combined");
        arrAlertMark.add("Alert Only");
        arrAlertMark.add("Mark Only");
        adapterAlertMark = new ArrayAdapter<String>(getActivity(),R.layout.simple_spinner_item,arrAlertMark);
        adapterAlertMark.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        arrMsgTimeout = new ArrayList<>();
        arrMsgTimeout.add("Off");
        for(int i=1;i<11;i++){
            arrMsgTimeout.add(+i+" "+"min");
        }
        adapterTimeout = new ArrayAdapter<String>(getActivity(),R.layout.simple_spinner_item,arrMsgTimeout);
        adapterTimeout.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
    }


    public void InitView()
    {
        spinnerNormal = (Spinner) v.findViewById(R.id.spinner_normal);
        spinnerNormal.setAdapter(adapterNormal);
        spinerAlert = (Spinner) v.findViewById(R.id.spinner_alerting);
        spinerAlert.setAdapter(adapterAlert);
        spinnerLed = (Spinner) v.findViewById(R.id.spinner_led);
        spinnerLed.setAdapter(adapterLed);

        spinnerAlertMark = (Spinner) v.findViewById(R.id.spinner_alertmark);
        spinnerAlertMark.setAdapter(adapterAlertMark);

        spinnerMsgTimeout = (Spinner) v.findViewById(R.id.spinner_timeout);
        spinnerMsgTimeout.setAdapter(adapterTimeout);

        UpdateUI();
        SystemClock.sleep(1000);
        spinerAlert.setOnItemSelectedListener(this);
        spinnerLed.setOnItemSelectedListener(this);
        spinnerNormal.setOnItemSelectedListener(this);
        spinnerMsgTimeout.setOnItemSelectedListener(this);
        spinnerAlertMark.setOnItemSelectedListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        try{
            super.onConfigurationChanged(newConfig);
            frameLayout. removeAllViews();
            LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
                v = inflater.inflate(R.layout.fragment_configuration, null);
            }
            else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                v = inflater.inflate(R.layout.fragment_configuration, null);
            }
            InitView();
            frameLayout .addView(v);
            //UpdateUI();
        }
        catch (Exception e)
        {

        }


    }
    FrameLayout frameLayout;
    View v;


    @Nullable
    @Override
    public View onCreateView(@Nullable LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        frameLayout = new FrameLayout(getActivity());
        v = inflater.inflate(R.layout.fragment_configuration, container, false);
        InitView();
        frameLayout.addView(v);
        isScreen=true;
        return frameLayout;
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Application) getActivity().getApplication()).inject(this);
        EventBus.getDefault().post(new Events.DisplayChanged("Configuration",R.id.configuration));
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            g4Manager.setConfigurationScreen(false);
        }catch (Exception e){}
        if(reconnectHandler!=null){
            reconnectHandler.stopReconnect();
        }
    }
    void InitSetting()
    {
        try {
            if (g4Manager != null) {
                AgentSettings settings = application.getAgentSettings();
                g4Manager = application.getG4Manager();
                //    if(g4Manager!= null)
                //   {
                if (g4Manager.isConnectionState()) {
                    if (g4Manager.getClient().getListCommandAll() != null) {
                        mGattClient = g4Manager.getClient();
                        mGattClient.setContext(getActivity());
                        mGattClient.setListener(listenerGat);
                    }
                }
            }
        }catch (Exception e){}
    }

    @Override
    public void onResume() {
        if (handlerMessage==null){
            handlerMessage=new Handler();
            handlerMessage.postDelayed(runCheckNewMessage,10000);
        }
        try {
            super.onResume();
            EventBus.getDefault().register(this);
            InitSetting();
            g4Manager.setConfigurationScreen(true);
            g4Manager.setFromConfigScreen(true);
            g4Manager.setFromATScreen(false);
            UpdateUI();
        }catch (Exception e){}
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        try{

        }
        catch (Exception e)
        {

        }

    }
    void UpdateUI(){
        if (g4Manager!=null) {
            if (g4Manager.getLed() != null) {
                try {
                    switch (Integer.parseInt(g4Manager.getLed())) {
                        case 0:
                            spinnerLed.setSelection(3);
                            break;
                        case 50:
                            spinnerLed.setSelection(2);
                            break;
                        case 128:
                            spinnerLed.setSelection(1);
                            break;
                        case 254:
                            spinnerLed.setSelection(0);
                            break;
                    }
                } catch (Exception e) {
                    try {
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        int mode = sharedPref.getInt("spinner_led", 0);
                        switch (mode) {
                            case 0:
                                spinnerLed.setSelection(3);
                                break;
                            case 50:
                                spinnerLed.setSelection(2);
                                break;
                            case 128:
                                spinnerLed.setSelection(1);
                                break;
                            case 254:
                                spinnerLed.setSelection(0);
                                break;
                        }
                    } catch (Exception ex) {
                        spinnerLed.setSelection(0);
                    }
                }
            }
        }
        else {
            try {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                int mode = sharedPref.getInt("spinner_led",0);
                switch (mode){
                    case 0:
                        spinnerLed.setSelection(3);
                        break;
                    case 50:
                        spinnerLed.setSelection(2);
                        break;
                    case 128:
                        spinnerLed.setSelection(1);
                        break;
                    case 254:
                        spinnerLed.setSelection(0);
                        break;
                }
            }catch (Exception e){
                spinnerLed.setSelection(0);
            }
        }
        if (g4Manager != null) {
            if (g4Manager.getAlertReportingTime() != null) {
                try {
                    switch (Integer.parseInt(g4Manager.getAlertReportingTime())) {
                        case 60:
                            spinerAlert.setSelection(0);
                            break;
                        case 120:
                            spinerAlert.setSelection(1);
                            break;
                        case 240:
                            spinerAlert.setSelection(2);
                            break;
                        case 360:
                            spinerAlert.setSelection(3);
                            break;
                        case 600:
                            spinerAlert.setSelection(4);
                            break;
                        case 900:
                            spinerAlert.setSelection(5);
                            break;
                    }
                } catch (Exception e) {
                    try {
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        int mode = sharedPref.getInt("spinner_alerting", 0);
                        switch (mode) {
                            case 60:
                                spinerAlert.setSelection(0);
                                break;
                            case 120:
                                spinerAlert.setSelection(1);
                                break;
                            case 240:
                                spinerAlert.setSelection(2);
                                break;
                            case 360:
                                spinerAlert.setSelection(3);
                                break;
                            case 600:
                                spinerAlert.setSelection(4);
                                break;
                            case 900:
                                spinerAlert.setSelection(5);
                                break;
                        }
                    } catch (Exception ex) {
                        spinerAlert.setSelection(0);
                    }
                }
            }
        }
        else {
            try {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                int mode = sharedPref.getInt("spinner_alerting",0);
                switch (mode){
                    case 60:
                        spinerAlert.setSelection(0);
                        break;
                    case 120:
                        spinerAlert.setSelection(1);
                        break;
                    case 240:
                        spinerAlert.setSelection(2);
                        break;
                    case 360:
                        spinerAlert.setSelection(3);
                        break;
                    case 600:
                        spinerAlert.setSelection(4);
                        break;
                    case 900:
                        spinerAlert.setSelection(5);
                        break;
                }
            }catch (Exception e){
                spinerAlert.setSelection(0);
            }
        }
        if (g4Manager!=null) {
            if (g4Manager.getNormalReportingTime() != null) {
                try {
                    switch (Integer.parseInt(g4Manager.getNormalReportingTime())) {
                        case 60:
                            spinnerNormal.setSelection(0);
                            break;
                        case 120:
                            spinnerNormal.setSelection(1);
                            break;
                        case 240:
                            spinnerNormal.setSelection(2);
                            break;
                        case 360:
                            spinnerNormal.setSelection(3);
                            break;
                        case 600:
                            spinnerNormal.setSelection(4);
                            break;
                        case 900:
                            spinnerNormal.setSelection(5);
                            break;
                    }
                } catch (Exception e) {
                    try {
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        int mode = sharedPref.getInt("spinner_normal", 0);
                        switch (mode) {
                            case 60:
                                spinnerNormal.setSelection(0);
                                break;
                            case 120:
                                spinnerNormal.setSelection(1);
                                break;
                            case 240:
                                spinnerNormal.setSelection(2);
                                break;
                            case 360:
                                spinnerNormal.setSelection(3);
                                break;
                            case 600:
                                spinnerNormal.setSelection(4);
                                break;
                            case 900:
                                spinnerNormal.setSelection(5);
                                break;
                        }
                    } catch (Exception ex) {
                        spinnerNormal.setSelection(1);
                    }
                }
            }
        }
        else {
            try {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                int mode = sharedPref.getInt("spinner_normal",0);
                switch (mode){
                    case 60:
                        spinnerNormal.setSelection(0);
                        break;
                    case 120:
                        spinnerNormal.setSelection(1);
                        break;
                    case 240:
                        spinnerNormal.setSelection(2);
                        break;
                    case 360:
                        spinnerNormal.setSelection(3);
                        break;
                    case 600:
                        spinnerNormal.setSelection(4);
                        break;
                    case 900:
                        spinnerNormal.setSelection(5);
                        break;
                }
            }catch (Exception e){
                spinnerNormal.setSelection(1);
            }
        }
        try{
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String mode = sharedPref.getString("mark_alert_mode", "");
            switch (mode){
                case "Combined":
                    spinnerAlertMark.setSelection(0);
                    break;
                case "Alert Only":
                    spinnerAlertMark.setSelection(1);
                    break;
                case "Mark Only":
                    spinnerAlertMark.setSelection(2);
                    break;
            }
        }
        catch (Exception e)
        {
        }
        try{
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            int mode = sharedPref.getInt("message_time_out",0);
            switch (mode){
                case 0:
                    spinnerMsgTimeout.setSelection(0);
                    break;
                case 1:
                    spinnerMsgTimeout.setSelection(1);
                    break;
                case 2:
                    spinnerMsgTimeout.setSelection(2);
                    break;
                case 3:
                    spinnerMsgTimeout.setSelection(3);
                    break;
                case 4:
                    spinnerMsgTimeout.setSelection(4);
                    break;
                case 5:
                    spinnerMsgTimeout.setSelection(5);
                    break;
                case 6:
                    spinnerMsgTimeout.setSelection(6);
                    break;
                case 7:
                    spinnerMsgTimeout.setSelection(5);
                    break;
                case 8:
                    spinnerMsgTimeout.setSelection(5);
                    break;
                case 9:
                    spinnerMsgTimeout.setSelection(5);
                    break;
                case 10:
                    spinnerMsgTimeout.setSelection(5);
                    break;
                case 11:
                    spinnerMsgTimeout.setSelection(5);
                    break;
            }
        }
        catch (Exception e)
        {
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int selectedID = parent.getId();

        switch (selectedID){
            case R.id.spinner_normal:
                sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                editor = sharedPref.edit();
                String value = arrNormal.get(position);
                if(g4Manager!=null) {
                    if (value.equals("1 Minute")) {
                        g4Manager.resetCommandList();
                        g4Manager.setReportNormal(60);
                        g4Manager.setNormalReportingTime("60");
                        editor.putInt("spinner_normal", 60);
                        editor.apply();
                    } else if (value.equals("2 Minutes")) {
                        g4Manager.resetCommandList();
                        g4Manager.setReportNormal(120);
                        g4Manager.setNormalReportingTime("120");
                        editor.putInt("spinner_normal", 120);
                        editor.apply();
                    } else if (value.equals("4 Minutes")) {
                        g4Manager.resetCommandList();
                        g4Manager.setReportNormal(240);
                        g4Manager.setNormalReportingTime("240");
                        editor.putInt("spinner_normal", 240);
                        editor.apply();
                    } else if (value.equals("6 Minutes")) {
                        g4Manager.resetCommandList();
                        g4Manager.setReportNormal(360);
                        g4Manager.setNormalReportingTime("360");
                        editor.putInt("spinner_normal", 360);
                        editor.apply();
                    } else if (value.equals("10 Minutes")) {
                        g4Manager.resetCommandList();
                        g4Manager.setReportNormal(600);
                        g4Manager.setNormalReportingTime("600");
                        editor.putInt("spinner_normal", 600);
                        editor.apply();
                    } else if (value.equals("15 Minutes")) {
                        g4Manager.resetCommandList();
                        g4Manager.setReportNormal(900);
                        g4Manager.setNormalReportingTime("900");
                        editor.putInt("spinner_normal", 900);
                        editor.apply();
                    }
                    //spinnerLed.setSelection(0);
                    g4Manager.executeCommand(mGattClient);
                    g4Manager.setPastAlertState(g4Manager.isAlertState());
                    //reset command takes a few seconds
                    count++;
                    if (count > 2) {
                        //showDialog();
                    }
                }
                break;
            case R.id.spinner_alerting:
                sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                editor = sharedPref.edit();
                String valueAlert = arrAlert.get(position);
                if(g4Manager!=null) {
                    if (valueAlert.equals("1 Minute")) {
                        g4Manager.resetCommandList();
                        g4Manager.setReportAlerting(60);
                        g4Manager.setAlertReportingTime("60");
                        g4Manager.executeCommand(mGattClient);
                        editor.putInt("spinner_alerting", 60);
                        editor.apply();
                    } else if (valueAlert.equals("2 Minutes")) {
                        g4Manager.resetCommandList();
                        g4Manager.setReportAlerting(120);
                        g4Manager.setAlertReportingTime("120");
                        g4Manager.executeCommand(mGattClient);
                        editor.putInt("spinner_alerting", 120);
                        editor.apply();
                    } else if (valueAlert.equals("4 Minutes")) {
                        g4Manager.resetCommandList();
                        g4Manager.setReportAlerting(240);
                        g4Manager.setAlertReportingTime("240");
                        g4Manager.executeCommand(mGattClient);
                        editor.putInt("spinner_alerting", 240);
                        editor.apply();
                    } else if (valueAlert.equals("6 Minutes")) {
                        g4Manager.resetCommandList();
                        g4Manager.setAlertReportingTime("360");
                        g4Manager.setReportAlerting(360);
                        g4Manager.executeCommand(mGattClient);
                        editor.putInt("spinner_alerting", 360);
                        editor.apply();
                    } else if (valueAlert.equals("10 Minutes")) {
                        g4Manager.resetCommandList();
                        g4Manager.setReportAlerting(600);
                        g4Manager.setAlertReportingTime("600");
                        g4Manager.executeCommand(mGattClient);
                        editor.putInt("spinner_alerting", 600);
                        editor.apply();
                    } else if (valueAlert.equals("15 Minutes")) {
                        g4Manager.resetCommandList();
                        g4Manager.setReportAlerting(900);
                        g4Manager.setAlertReportingTime("900");
                        g4Manager.executeCommand(mGattClient);
                        editor.putInt("spinner_alerting", 900);
                        editor.apply();
                    }
                    //reset command takes a few seconds
                    //spinnerLed.setSelection(0);
                    g4Manager.executeCommand(mGattClient);
                    g4Manager.setPastAlertState(g4Manager.isAlertState());
                    count++;
                    if (count > 2) {
                        //showDialog();
                    }
                }
                break;
            case R.id.spinner_led:
                sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                editor = sharedPref.edit();
                String valueLed = arrLed.get(position);
                if(g4Manager!=null) {
                    if (valueLed.equals("High")) {
                        g4Manager.resetCommandList();
                        g4Manager.setLedBrightness(254);
                        g4Manager.setLed("254");
                        g4Manager.executeCommand(mGattClient);
                        editor.putInt("spinner_led", 254);
                        editor.apply();
                    } else if (valueLed.equals("Low")) {
                        g4Manager.resetCommandList();
                        g4Manager.setLedBrightness(50);
                        g4Manager.setLed("50");
                        g4Manager.executeCommand(mGattClient);
                        editor.putInt("spinner_led", 50);
                        editor.apply();
                    } else if (valueLed.equals("Medium")) {
                        g4Manager.resetCommandList();
                        g4Manager.setLedBrightness(128);
                        g4Manager.setLed("128");
                        g4Manager.executeCommand(mGattClient);
                        editor.putInt("spinner_led", 128);
                        editor.apply();
                    } else if (valueLed.equals("Off")) {
                        g4Manager.resetCommandList();
                        g4Manager.setLedBrightness(0);
                        g4Manager.setLed("0");
                        g4Manager.executeCommand(mGattClient);
                        editor.putInt("spinner_led", 0);
                        editor.apply();
                    }
                }
                break;
            case R.id.spinner_alertmark:

                String valueMarkAlert = arrAlertMark.get(position);
                sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                editor = sharedPref.edit();
                if(g4Manager!=null) {
                    if (valueMarkAlert.equals("Combined")) {
                        g4Manager.resetCommandList();
                        g4Manager.addCommand("AT$DIFC=0,3");
                        g4Manager.executeCommand(mGattClient);
                        editor.putString("mark_alert_mode", "Combined");
                        editor.apply();
                    } else if (valueMarkAlert.equals("Alert Only")) {
                        g4Manager.resetCommandList();
                        g4Manager.addCommand("AT$DIFC=0,2");
                        g4Manager.executeCommand(mGattClient);
                        editor.putString("mark_alert_mode", "Alert Only");
                        editor.apply();
                    } else if (valueMarkAlert.equals("Mark Only")) {
                        g4Manager.resetCommandList();
                        g4Manager.addCommand("AT$DIFC=0,1");
                        g4Manager.executeCommand(mGattClient);
                        editor.putString("mark_alert_mode", "Mark Only");
                        editor.apply();
                    }

                }
                break;
            case R.id.spinner_timeout:
                String timeout = arrMsgTimeout.get(position);
                sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                editor = sharedPref.edit();
                if(timeout.equals("Off"))
                {
                    editor.putInt("message_time_out",0);
                    editor.apply();
                }
                else if(timeout.equals("1 min"))
                {
                    editor.putInt("message_time_out",1);
                    editor.apply();
                }
                else if(timeout.equals("2 min"))
                {
                    editor.putInt("message_time_out",2);
                    editor.apply();
                }
                else if(timeout.equals("3 min"))
                {
                    editor.putInt("message_time_out",3);
                    editor.apply();
                }
                else if(timeout.equals("4 min"))
                {
                    editor.putInt("message_time_out",4);
                    editor.apply();
                }
                else if(timeout.equals("5 min"))
                {
                    editor.putInt("message_time_out",5);
                    editor.apply();
                }
                else if(timeout.equals("6 min"))
                {
                    editor.putInt("message_time_out",6);
                    editor.apply();
                }
                else if(timeout.equals("7 min"))
                {
                    editor.putInt("message_time_out",7);
                    editor.apply();
                }
                else if(timeout.equals("8 min"))
                {
                    editor.putInt("message_time_out",8);
                    editor.apply();
                }
                else if(timeout.equals("9 min"))
                {
                    editor.putInt("message_time_out",9);
                    editor.apply();
                }
                else if(timeout.equals("10 min"))
                {
                    editor.putInt("message_time_out",10);
                    editor.apply();
                }
                else if(timeout.equals("11 min"))
                {
                    editor.putInt("message_time_out",11);
                    editor.apply();
                }
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
    public void showDialog(){
        final ProgressDialog dialog2 = ProgressDialog.show(getContext(), "", "Please wait while G4 updates its configuration. ",true);
        dialog2.setCancelable(false);
        dialog2.setCanceledOnTouchOutside(false);
        new CountDownTimer(12000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onFinish() {
                // TODO Auto-generated method stub

                dialog2.dismiss();
            }
        }.start();

    }

    public void onEventMainThread(Events.ShowProgressDialog event) {
       showDialog();
    }


    @SuppressWarnings("unused")
    public void onEventMainThread(Events.handleDisConnect event) {
        // EventBus.getDefault().removeStickyEvent(event);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EventBus.getDefault().post(new Events.BluetoothStatus(false));
//                Toast toast = Toast.makeText(getActivity(), "Disconnected", Toast.LENGTH_SHORT);
//                if(toast.getView().isShown()){
//                    toast.cancel();
//                }else {
//                    toast.show();
//                }
                if(reconnectHandler!=null) {
                    if (reconnectHandler.isAlive()) {
                        reconnectHandler.stopReconnect();
                    }
                }
                    reconnectHandler = new ReconnectHandler();
            }
        });

    }
    private BluetoothLeScannerCompat mScanner = BluetoothLeScannerCompat.getScanner();
    private void startReconnect() {
        try {
            SharedPreferences prefs = getActivity().getSharedPreferences("mac_address", 0);
            if (prefs.getString("value", "") != null) {
                 address = prefs.getString("value", "");
                DebugLog.d("address:"+address);
            }
        } catch (Exception e) {
        }
        if (address != null) {

           mGattClient.onCreate(getActivity(),address,listenerGat);
        } else {// Stops scanning after a pre-defined scan period.*/
            try {
                mGattClient.startClient(application);
            } catch (Exception e) {
            }
        }
    }
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("Config", "onScanResult: " + result.getDevice().getAddress());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i("Config", "onBatchScanResults: " + results.toString());

            if (!results.isEmpty()) {
                ScanResult result = results.get(0);
                mGattClient.onCreate(getActivity(), address,listenerGat);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w("Config", "Scan failed: " + errorCode);
            mScanner.stopScan(mScanCallback);
        }
    };
    public class ReconnectHandler extends HandlerThread {
        public  Handler mReconnectHandler;
        public final Runnable mReconnectAuto;
        public ReconnectHandler() {
            super(StatusFragment.ReconnectHandler.class.getName(), 1);
            start();
            if (mReconnectHandler==null) {
                mReconnectHandler = new Handler();
            }
            //mReconnectHandler = new Handler(getLooper());

            mReconnectAuto = new mReconnectAuto();
            mReconnectHandler.post(mReconnectAuto);
            Log.d("failed", "Initilaze");
        }
        void stopReconnect() {
            mReconnectHandler.removeCallbacks(mReconnectAuto);
        }
        public final class mReconnectAuto implements Runnable {
            @Override
            public void run() {
                DebugLog.d("reconnect auto:");
                    try {
                        if (g4Manager.isConnectionState() == false) {
                            try {
                                EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
                                ///
                                /*if (!Utils.isAppIsInBackground(getActivity())){
                                    if (isScreen) {
                                        Toast toast = Toast.makeText(getActivity(), "Trying to reconnect to G4", Toast.LENGTH_SHORT);
                                        if (toast.getView().isShown()) {
                                            toast.cancel();
                                        } else {
                                            toast.show();
                                        }
                                    }
                                }*/

                                Log.d("failed", "Enter Reconnect");
                                startReconnect();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            mScanner.stopScan(mScanCallback);
                        }
                    } finally {
                        mReconnectHandler.postDelayed(mReconnectAuto, 15000);
                    }
            }
        };
    }
    @Override
    public void onDestroyView() {
        if (reconnectHandler!=null){
            reconnectHandler.stopReconnect();
            reconnectHandler=null;
        }
        if (handlerMessage!=null) {
            handlerMessage.removeCallbacks(runCheckNewMessage);
            handlerMessage = null;
        }
        super.onDestroyView();
        DebugLog.d("destroyview");
        isScreen=false;

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
