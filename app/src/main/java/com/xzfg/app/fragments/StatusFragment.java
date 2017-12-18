package com.xzfg.app.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.BaseFragment;
import com.xzfg.app.BuildConfig;
//import com.xzfg.app.DebugLog;
import com.xzfg.app.DebugLog;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.managers.G4Manager;
import com.xzfg.app.services.GattClient;
import com.xzfg.app.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import static android.content.Context.BLUETOOTH_SERVICE;

public class StatusFragment extends BaseFragment {
	private static final Object lock = new Object();
private static final String moduleLogId = "@f1-StatFrag:";
private static final long SCAN_TIMEOUT_MS = 10000;
private static final int REQUEST_PERMISSION_LOCATION = 1;
private static final String TAG = "";
private final static int REQUEST_ENABLE_BT = 1;
public static int MAX_CONNECT_attempt = 3;
public static UUID DESCRIPTOR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
public static UUID DESCRIPTOR_USER_DESC = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");
public static UUID SERVICE_UUID = UUID.fromString("1D5688DE-866D-3AA4-EC46-A1BDDB37ECF6");
public static UUID CHARACTERISTIC_COUNTER_UUID = UUID.fromString("AF20fBAC-2518-4998-9AF7-AF42540731B3");
public static UUID CHARACTERISTIC_INTERACTOR_UUID = UUID.fromString("AF20fBAC-2518-4998-9AF7-AF42540731B3");
static boolean result = false;
public ImageView imgGPS_Signal;
public ImageView imgIridium_Signal;
public ImageView imgBluetooth_Signal;
ExecutorService threadPoolExecutor;
View v;
ProgressDialog progressDialog;
Activity activity = new Activity();
Future longRunningTaskFuture;
boolean isConnected = false;
SharedPreferences sharedPref;
SharedPreferences.Editor editor;
@Inject
G4Manager g4Manager;
@Inject
Application application;
@Inject
SharedPreferences sharedPreferences;
Handler handler;
BluetoothManager mBluetoothManager;
BluetoothAdapter mBluetoothAdapter;
	CountDownTimer countDownConnect;
	CountDownTimer firstCowndown;
	boolean isReconnect=false;
	boolean isScreen=false;
Runnable stopBluetooth = new Runnable() {

	@Override
	public void run() {
		try {
			DebugLog.d("check run:");
			try {
				BluetoothManager mBluetoothManager1 = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
				BluetoothAdapter mBluetoothAdapter1 = mBluetoothManager1.getAdapter();
				if (!mBluetoothAdapter1.isEnabled()) {
					EventBus.getDefault().postSticky(new Events.handleDisConnect("Connect"));
				}
				else {
					handler.removeCallbacks(stopBluetooth);
				}
			}
			catch (Exception e) {
			}

		} finally {
			handler.postDelayed(stopBluetooth, 3000);
		}
	}
};
private ReconnectHandler reconnectHandler;
private ProgressDialog dialog2;
private String address = null;
private TextView ConnectionState;
private boolean mScanning;
private TextView txt_imei;
private getStatus get;
private TextView name_device;
private TextView txt_firmware;
private boolean StatusUpdate = false;
private boolean StatusIMEIUpdate = false;
private boolean StatusNameUpdate = false;
private boolean StatusFirmwareUpdate = false;
private boolean StatusNormalReport = false;
private boolean StatusAlertReport = false;
private boolean StatusLedBrightness = false;
private int conntectionState = 0;
private String Firmware;
private boolean pressConnect = false;
private GattClient mGattClient = new GattClient();
private BluetoothLeScannerCompat mScanner = BluetoothLeScannerCompat.getScanner();
private Handler mStopScanHandler = new Handler();
private int attempt = 1;
private Button testtest;
boolean isAuto=true;
private Runnable mStopScanRunnable = new Runnable() {
	@Override
	public void run() {
		stopLeScan();
		try {
			DebugLog.d("check run:");
			if (g4Manager.isConnectionState() == false) {
				if (attempt < MAX_CONNECT_attempt) {
					attempt++;
					startLeScan();
				}
				else {
					ConnectionState.setText("Connection failed");
					attempt = 1;
				}
			}
		}
		catch (Exception e) {
			if (g4Manager == null) {
				attempt = 1;
				ConnectionState.setText("Connection failed");
			}
		}
	}
};
private Button btn_connect;
	String aa;
int isFirst=0;

	private final ScanCallback mScanCallback = new ScanCallback() {
	@Override
	public void onScanResult(int callbackType, ScanResult result) {
		Log.i(TAG, "onScanResult: " + result.getDevice().getAddress());
	}

	@Override
	public void onBatchScanResults(List<ScanResult> results) {
		Log.i(TAG, "onBatchScanResults: " + results.toString());

		if (!results.isEmpty()) {
			ScanResult result = results.get(0);
			startInteractActivity(result.getDevice());
		}
		else {

		}
	}

	@Override
	public void onScanFailed(int errorCode) {
		Log.w(TAG, "Scan failed: " + errorCode);
		stopLeScan();
	}
};
Runnable RunSignal = new Runnable() {
	@Override
	public void run() {
	try {
		if (g4Manager.isStatusScreen()) {
			if(g4Manager.isConnectionState())
			Log.d("G4Chat1", moduleLogId + "RunSignal:Enter");
			InitSetting();
			g4Manager.resetCommandList();
			g4Manager.getGPSFix();
			g4Manager.getSignalStrengths();
			// LQL
			g4Manager.addCommand("AT$GPMS=0");
			g4Manager.executeCommand(mGattClient);
			ShowSignal();
		}
	} finally {
		handler.postDelayed(RunSignal, 8000);
	}
}
};

public static String getDeviceName() {
	String manufacturer = Build.MANUFACTURER;
	String model = Build.MODEL;
	if (model.startsWith(manufacturer)) {
		return model;
	}
	return /*capitalize(manufacturer) + " " +*/ model;
}

@Override
public void onAttach(Activity activity) {
	super.onAttach(activity);
	((Application) activity.getApplication()).inject(this);
	this.activity = activity;
}


	@Override
	public void binData(String result) {
		g4Manager=application.getG4Manager();
		if (result.contains("OK")) {
			if (result.indexOf("$GMGL") > 0 || result.indexOf("$GMGL") > 0) {
				g4Manager.handleMessageScreeen(g4Manager.getStringBuider());
			}
			else {
				g4Manager.processingData(new StringBuilder(result));
			}
			if (result.contains("GPMS=0")) {
				DebugLog.d("aaaa: "+result);
				g4Manager.detectNewMessage(result);
			}
			g4Manager.executeCommand(mGattClient);
			//g4Manager.setStringBuider(null);
		}
		if (StatusUpdate == false) {
			if (g4Manager.getIMEI() != null && StatusIMEIUpdate == false) {
				StatusIMEIUpdate = true;
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						txt_imei.setText(g4Manager.getIMEI());
					}
				});

			}
			if (g4Manager.getDeviceName() != null && StatusNameUpdate == false) {
				StatusNameUpdate = true;
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						name_device.setText(g4Manager.getDeviceName());
					}
				});

			}
			if (g4Manager.getFirmware() != null && StatusFirmwareUpdate == false) {
				if (g4Manager.getFirmware().contains("ATI") != true) {
					StatusFirmwareUpdate = true;
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							txt_firmware.setText(g4Manager.getFirmware());
						}
					});

				}
			}
			if (StatusAlertReport == false && g4Manager.getAlertReportingTime() != null) {
				StatusAlertReport = true;
			}
			if (StatusNormalReport == false && g4Manager.getNormalReportingTime() != null) {
				StatusNormalReport = true;
			}
			if (StatusLedBrightness == false && g4Manager.getLed() != null) {
				StatusLedBrightness = true;
			}

			if (StatusIMEIUpdate == true && StatusNameUpdate == true && StatusFirmwareUpdate == true
					&& StatusAlertReport == true && StatusNormalReport == true && StatusLedBrightness == true) {
				StatusUpdate = true;
				if (get!=null) {
					get.cancel(true);
				}
				g4Manager.resetCommandList();
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						///
						//Toast.makeText(getActivity(), "Get Status Complete", Toast.LENGTH_SHORT).show();
					}
				});

			}
		}
		ShowSignal();
	}

	@Override
	public void binConnectionState(boolean isConnect) {
		DebugLog.d("send command:");
		if (g4Manager!=null) {
			/*if (isConnect) {
				CheckAlertState();
			}*/
			if (g4Manager.isPastAlertState()) {
				DebugLog.d("send command:");
				DebugLog.d("check mark"+g4Manager.isPastAlertState());
				if (g4Manager != null) {
					g4Manager = application.getG4Manager();
				}
				if (!g4Manager.isConnectionState()) {
					return;
				}
				g4Manager.setlastMarkedDate(new Date());
				g4Manager.resetCommandList();
				g4Manager.setAlertOn();
				g4Manager.getGPSFix();
				g4Manager.setAlertState(true);
				g4Manager.setAlertClicked(true);
				//  g4Manager = application.getG4Manager();
				g4Manager.executeCommand(mGattClient);
			}
		}
	}

	@Override
	public void binRssi(int rssi) {

	}

	@Override
public void onCreate(@Nullable Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	((Application) getActivity().getApplication()).inject(this);
	EventBus.getDefault().register(this);
}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		try
		{
			super.onConfigurationChanged(newConfig);
			frameLayout. removeAllViews();
			LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
				v = inflater.inflate(R.layout.fragment_status, null);
			}
			else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
				v = inflater.inflate(R.layout.fragment_status, null);
			}
			InitView();
			frameLayout .addView(v);


			try {
				if(g4Manager.isConnectionState())
				{
					if (g4Manager.getIMEI() != null) {
						txt_imei.setText(g4Manager.getIMEI());
					}
					if (g4Manager.getDeviceName() != null) {
						StatusNameUpdate = true;
						name_device.setText(g4Manager.getDeviceName());
					}

					if (g4Manager.getFirmware() != null) {
						StatusFirmwareUpdate = true;
						txt_firmware.setText(g4Manager.getFirmware());
					}
					btn_connect.setText("Disconnect");
					ConnectionState.setText("Device connected");
				}


			}
			catch (Exception e) {
			}
		}
		catch (Exception e)
		{

		}

	}


	public void InitView()
	{
		getActivity().getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
		);
		imgGPS_Signal = (ImageView) v.findViewById(R.id.gps_signal);
		imgIridium_Signal = (ImageView) v.findViewById(R.id.iridium_signal);
		imgBluetooth_Signal = (ImageView) v.findViewById(R.id.bluetooth_signal);
		ConnectionState = (TextView) v.findViewById(R.id.ConnectionState);

		ConnectionState = (TextView) v.findViewById(R.id.ConnectionState);
		btn_connect = (Button) v.findViewById(R.id.button_connect);
		name_device = (TextView) v.findViewById(R.id.txtDevice);
		txt_firmware = (TextView) v.findViewById(R.id.txtFirmware);
		txt_imei = (TextView) v.findViewById(R.id.txtImei);

		((TextView) v.findViewById(R.id.Version)).setText(BuildConfig.VERSION_NAME);
		imgGPS_Signal = (ImageView) v.findViewById(R.id.gps_signal);
		imgIridium_Signal = (ImageView) v.findViewById(R.id.iridium_signal);
		imgBluetooth_Signal = (ImageView) v.findViewById(R.id.bluetooth_signal);


		btn_connect = (Button) v.findViewById(R.id.button_connect);
		btn_connect.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				if (btn_connect.getText().toString().trim().contains("Disconnect")){
					isAuto=false;
					DebugLog.d("auto::"+isAuto);
				}else {
					isAuto=true;
					DebugLog.d("auto::"+isAuto);
				}
				if (conntectionState == 0) {
					SharedPreferences prefs = getActivity().getSharedPreferences("mac_address", 0);
					if (g4Manager.getAddress() == null) {
						ConnectionState.setText("");
						attempt = 1;
						prepareForScan();
					}
					else {
						try {
							mBluetoothManager = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
							mBluetoothAdapter = mBluetoothManager.getAdapter();
							if (mBluetoothAdapter.isEnabled()) {
								// Prompt for runtime permission
								if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
								}
								else {
									ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
								}
							}
							else {
								Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
								startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
								return;
							}
							if (application.getAuto())
								ConnectionState.setText("Connecting... Please press the Connect button again to refresh after long wait.");
							handler.removeCallbacks(mStopScanRunnable);
							mGattClient.startClient(getActivity());
							InitSetting();

						}
						catch (Exception e) {
							e.toString();
							//ConnectionState.setText("Connect failed");
							try {
								mGattClient.onDestroy();
							}
							catch (Exception ex) {
								ex.toString();//mGattClient = new GattClient();}
							}
						}
					}
				}
				else {

					conntectionState = 0;
					stopLeScan();
					EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
					try {
						mGattClient.onDestroy();
					}
					catch (Exception e) {
					}
					isConnected = false;
					handler.removeCallbacks(RunSignal);
					EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
					EventBus.getDefault().post(new Events.ReceiverMessageG4(false));
					g4Manager.setConnectionState(false);
					conntectionState = 0;

					if (get != null) {
						if (get.getStatus() == AsyncTask.Status.RUNNING) {
							get.cancel(true);
						}
					}
					EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
					g4Manager.setConnectionState(false);
					btn_connect.setText("Connect");
					ConnectionState.setText("Device Disconnected");
					imgBluetooth_Signal.setImageResource(0);
					imgIridium_Signal.setImageResource(0);
					imgGPS_Signal.setImageResource(0);
					pressConnect = true;
					if(reconnectHandler!=null){
						reconnectHandler.stopReconnect();
					}
				}
				application.setAuto(false);

			}
		});
		name_device = (TextView) v.findViewById(R.id.txtDevice);
		txt_firmware = (TextView) v.findViewById(R.id.txtFirmware);
		txt_imei = (TextView) v.findViewById(R.id.txtImei);
		((TextView) v.findViewById(R.id.Version)).setText(BuildConfig.VERSION_NAME);
	}
	FrameLayout frameLayout;

	@Nullable
@Override
public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
	frameLayout = new FrameLayout(getActivity());
	v = inflater.inflate(R.layout.fragment_status, container, false);
	//hide qwerty keyboard
		InitView();
	handler = new android.os.Handler();
	threadPoolExecutor = Executors.newSingleThreadExecutor();
		frameLayout .addView(v);
		if (g4Manager!=null) {
			if (!g4Manager.isConnectionState()){
				FirstConnect();
			}
		}else {
			FirstConnect();
		}
		isScreen=true;
	return frameLayout;
}

/////
private void doAutoConnect(){
	DebugLog.d("connecting:");
	if (conntectionState == 0) {
		if (g4Manager.getAddress() == null) {
			ConnectionState.setText("");
			attempt = 1;
			prepareForScan();
		}
		else {
			try {
				mBluetoothManager = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
				mBluetoothAdapter = mBluetoothManager.getAdapter();
				if (mBluetoothAdapter.isEnabled()) {
					// Prompt for runtime permission
					if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					}
					else {
						ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
					}
				}
				else {
					Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
					return;
				}
				if (application.getAuto()) {
					ConnectionState.setText("Connecting... Please press the Connect button again to refresh after long wait.");
				}
				handler.removeCallbacks(mStopScanRunnable);
				mGattClient.startClient(getActivity());
				InitSetting();

			}
			catch (Exception e) {
				e.toString();
				ConnectionState.setText("Connect failed");
				try {
					mGattClient.onDestroy();
				}
				catch (Exception ex) {
					ex.toString();//mGattClient = new GattClient();}
				}
			}
		}
	}
	else {

		conntectionState = 0;
		stopLeScan();
		EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
		try {
			mGattClient.onDestroy();
		}
		catch (Exception e) {
		}
		isConnected = false;
		handler.removeCallbacks(RunSignal);
		EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
		EventBus.getDefault().post(new Events.ReceiverMessageG4(false));
		g4Manager.setConnectionState(false);
		conntectionState = 0;

		if (get != null) {
			if (get.getStatus() == AsyncTask.Status.RUNNING) {
				get.cancel(true);
			}
		}

	}
}
@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {
	super.onActivityResult(requestCode, resultCode, data);
	if (resultCode == activity.RESULT_OK) {
		result = true;
		SystemClock.sleep(500);
	}
	else {
		getActivity().sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
	}
}
void  InitSetting() {
	Log.d("check","check init:");
	if (application.getG4Manager() != null) {

		try {
			g4Manager = application.getG4Manager();
			mGattClient = g4Manager.getClient();
			mGattClient.setContext(getActivity());
		}
		catch (Exception e) {
		}

		final StringBuilder databuilder = new StringBuilder(200);
		if (mGattClient != null) {
			mGattClient.setListener(new GattClient.OnCounterReadListener() {
				@Override
				public void onCounterRead(String value) {

					/*if (!g4Manager.getStringBuider().toString().contains(value)) {
						g4Manager.addBuilder(value);
					}
					//find OK to get data
					if (g4Manager.getStringBuider().toString().contains("OK")) {
						Log.d("G4Chat2", g4Manager.getStringBuider().toString());
						if (g4Manager.getStringBuider().indexOf("$GMGL") > 0 || g4Manager.getStringBuider().indexOf("$GMGL") > 0) {
							g4Manager.handleMessageScreeen(g4Manager.getStringBuider());
						}
						else {
							g4Manager.processingData(g4Manager.getStringBuider());
						}
						if (g4Manager.getStringBuider().indexOf("GPMS=0") > 0) {
							g4Manager.detectNewMessage(g4Manager.getStringBuider().toString());
						}
						g4Manager.executeCommand(mGattClient);
						g4Manager.setStringBuider(null);
					}
					if (StatusUpdate == false) {
						if (g4Manager.getIMEI() != null && StatusIMEIUpdate == false) {
							StatusIMEIUpdate = true;
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									txt_imei.setText(g4Manager.getIMEI());
								}
							});

						}
						if (g4Manager.getDeviceName() != null && StatusNameUpdate == false) {
							StatusNameUpdate = true;
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									name_device.setText(g4Manager.getDeviceName());
								}
							});

						}
						if (g4Manager.getFirmware() != null && StatusFirmwareUpdate == false) {
							if (g4Manager.getFirmware().contains("ATI") != true) {
								StatusFirmwareUpdate = true;
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										txt_firmware.setText(g4Manager.getFirmware());
									}
								});

							}
						}
						if (StatusAlertReport == false && g4Manager.getAlertReportingTime() != null) {
							StatusAlertReport = true;
						}
						if (StatusNormalReport == false && g4Manager.getNormalReportingTime() != null) {
							StatusNormalReport = true;
						}
						if (StatusLedBrightness == false && g4Manager.getLed() != null) {
							StatusLedBrightness = true;
						}

						if (StatusIMEIUpdate == true && StatusNameUpdate == true && StatusFirmwareUpdate == true
								&& StatusAlertReport == true && StatusNormalReport == true && StatusLedBrightness == true) {
							StatusUpdate = true;
							get.cancel(true);
							g4Manager.resetCommandList();
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getActivity(), "Get Status Complete", Toast.LENGTH_SHORT).show();
								}
							});

						}
					}
					ShowSignal();*/

				}

				@Override
				public void onConnected(final boolean success) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (!success) {
								//Reconnect();
							//	Toast.makeText(getActivity(),"Disconnect",Toast.LENGTH_LONG).show();
								Log.d("failed","onconected 3");
								reconnect1();
								mGattClient.setListener(new GattClient.OnCounterReadListener() {
									@Override
									public void onCounterRead(String value) {
									}

									@Override
									public void onConnected(boolean success) {
									}
									@Override
									public void onRSSIChange(int rssi) {
										imgBluetooth_Signal.setImageResource(0);
									}
								});
								try {
									mGattClient.onDestroy();
								}
								catch (Exception e) {
								}
								btn_connect = (Button) v.findViewById(R.id.button_connect);
								txt_imei = (TextView) v.findViewById(R.id.txtImei);
								name_device = (TextView) v.findViewById(R.id.txtDevice);
								ConnectionState = (TextView) v.findViewById(R.id.ConnectionState);
								txt_firmware = (TextView) v.findViewById(R.id.txtFirmware);
								//InitSetting();
								btn_connect.setText("Connect");
								ConnectionState.setText("Disconnected");
								//EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
								imgBluetooth_Signal.setImageResource(0);
								attempt = 1;
								g4Manager.setConnectionState(false);
								handler.removeCallbacks(RunSignal);
								EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
								EventBus.getDefault().post(new Events.ReceiverMessageG4(false));
								g4Manager.setConnectionState(false);
								imgGPS_Signal.setImageResource(android.R.color.transparent);
								imgIridium_Signal.setImageResource(android.R.color.transparent);
								imgBluetooth_Signal.setImageResource(android.R.color.transparent);
								imgGPS_Signal.setImageResource(0);
								imgIridium_Signal.setImageResource(0);
								imgBluetooth_Signal.setImageResource(0);
								conntectionState = 0;
								if (get != null) {
									if (get.getStatus() == AsyncTask.Status.RUNNING) {
										get.cancel(true);
									}
								}
								mBluetoothManager = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
								mBluetoothAdapter = mBluetoothManager.getAdapter();

							}
							else {
								///
								/*if (!Utils.isAppIsInBackground(getActivity())) {
									Toast toast = Toast.makeText(getActivity(), "Connected Successfully", Toast.LENGTH_SHORT);
									if (toast.getView().isShown()) {
										toast.cancel();
									} else {
										toast.show();
									}
								}else {
									return;
								}*/
								isConnected = true;
								g4Manager.setConnectionState(true);
								//  longRunningTaskFuture = threadPoolExecutor.submit(RunSignal);
								if(reconnectHandler!=null) {
									reconnectHandler.stopReconnect();
								}
								attempt = 1;
								conntectionState = 1;
								InitSetting();
								btn_connect.setText("Disconnect");
								ConnectionState.setText("Device connected");
								g4Manager.setConnectionState(true);
								EventBus.getDefault().postSticky(new Events.BluetoothStatus(true));
								g4Manager.setLed("254");
								g4Manager.setClient(mGattClient);
								if (address != null) {
									g4Manager.setAddress(address);
								}
								application.setG4Manager(g4Manager);
								try {
									sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
									editor = sharedPref.edit();
									editor.putInt("message_time_out", 5);
									editor.apply();
								}
								catch (Exception e) {
								}
								pressConnect = false;
								EventBus.getDefault().post(new Events.RunSignal(true));
							}
						}
					});
				}

				@Override
				public void onRSSIChange(final int rssi) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							ShowSignal();
							if (rssi <= -60 && rssi > 0) {
								imgBluetooth_Signal.setImageResource(R.drawable.signal_ic_4);
							}
							else if (rssi <= -60 && rssi > -75) {
								imgBluetooth_Signal.setImageResource(R.drawable.signal_ic_3);
							}
							else if (rssi <= -75 && rssi > -90) {
								imgBluetooth_Signal.setImageResource(R.drawable.signal_ic_2);
							}
							else if (rssi <= -90) {
								imgBluetooth_Signal.setImageResource(R.drawable.signal_ic_1);
							}
						}
					});
				}
			});
		}
		if (g4Manager.isConnectionState()) {
			conntectionState = 1;
		}
		try {
			if (g4Manager.getIMEI() != null) {
				txt_imei.setText(g4Manager.getIMEI());
			}
			if (g4Manager.getDeviceName() != null) {
				StatusNameUpdate = true;
				name_device.setText(g4Manager.getDeviceName());
			}

			if (g4Manager.getFirmware() != null) {
				StatusFirmwareUpdate = true;
				txt_firmware.setText(g4Manager.getFirmware());
			}
		}
		catch (Exception e) {
		}

		try {
			if (mGattClient != null) {
				mGattClient.setContext(getActivity().getApplicationContext());
			}
		}
		catch (Exception e) {
		}
	}
}


	@Override
public void onResume() {
	super.onResume();
	application.setAuto(true);
	SharedPreferences prefs = getActivity().getSharedPreferences("ScanbarCode", 0);
	if (prefs.getBoolean("scan", false)) {
		EventBus.getDefault().post(new Events.handleDisConnect("Connect"));
		prepareForScan();
		SharedPreferences.Editor editor = getActivity().getSharedPreferences("ScanbarCode", 0).edit();
		editor.putBoolean("scan", false);
		editor.apply();
	}

	try {
		if (g4Manager.isConnectionState() == true) {
			InitSetting();
			SharedPreferences prefs1 = getActivity().getSharedPreferences("ignoreStatus", 0);
			btn_connect.setText("Disconnect");
			ConnectionState.setText("Device connected");
		}
		else {
			btn_connect.setText("Connect");
			ConnectionState.setText("");
		}
	}
	catch (Exception e) {
	}
	/*activity.runOnUiThread(new Runnable() {
		@Override
		public void run() {
			if (application.getG4Manager() != null) {
				g4Manager = application.getG4Manager();
			}
			if (g4Manager.isConnectionState() == true) {
				InitSetting();
				btn_connect.setText("Disconnect");
				ConnectionState.setText("Device connected");
			}
			else {
				btn_connect.setText("Connect");
				ConnectionState.setText("");
			}
			if (result) {
				SharedPreferences prefs = getActivity().getSharedPreferences("mac_address", 0);
				if (prefs.getString("value", "") != null) {

					if (prefs.getString("value", "") != null) {
						try {
							ConnectionState.setText("Connecting... Please press the Connect button again if screen does not refresh after long wait.");
							//  attempt = 1;
							InitSetting();
						}
						catch (Exception e) {
							ConnectionState.setText("Connection failed");
						}
					}
					else {
						startLeScan();
						result = false;
					}
				}
				else {
					try {
						ConnectionState.setText("Connecting... Please press the Connect button again if screen does not refresh after long wait.");
						mGattClient.startClient(application);
						final StringBuilder databuilder = new StringBuilder(200);
						InitSetting();
					}
					catch (Exception e) {
						ConnectionState.setText("Connection failed");
					}
				}
			}
		}
	});*/
	if (!g4Manager.isConnectionState()&&isAuto==true){
		/*Toast b=Toast.makeText(getActivity(), "Trying to reconnect to G4", Toast.LENGTH_SHORT);
		b.setGravity(Gravity.CENTER,0,0);
		b.show();*/
	}
	/*if (g4Manager!=null) {
		if (!g4Manager.isConnectionState()){
			FirstConnect();
		}
	}else {
		FirstConnect();
	}*/
	/*handlerReconnect=new Handler();
		handlerReconnect.postDelayed(runnable,40000);*/
	//Reconnect();

		try {
			SharedPreferences preferences = getActivity().getSharedPreferences("mac_address", 0);
			if (prefs.getString("value", "") != null) {
				aa = prefs.getString("value", "");
			}
		}
		catch (Exception e) {
		}

		if (g4Manager!=null) {
		if (!g4Manager.isConnectionState()){
			FirstConnect();
		}
	}else {
		FirstConnect();
	}

	super.onResume();

}
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		//deleteCache(getActivity());
		if (countDownConnect!=null) {
			countDownConnect.cancel();
			countDownConnect = null;
		}
		if (firstCowndown!=null) {
			firstCowndown.cancel();
			firstCowndown = null;
		}
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
	private void FirstConnect(){
		if (firstCowndown==null) {
			///
			/*if (!Utils.isAppIsInBackground(getActivity())) {
				if (getActivity() != null) {
					Toast b = Toast.makeText(getActivity(), "Trying to reconnect to G4", Toast.LENGTH_SHORT);
					b.setGravity(Gravity.CENTER, 0, 0);
					b.show();
				}
			}else {
				return;
			}*/
			doAutoConnect();
			firstCowndown = new CountDownTimer(40000, 40000) {
				@Override
				public void onTick(long millisUntilFinished) {
					if (Utils.isAppIsInBackground(getActivity())){
						firstCowndown.cancel();
						firstCowndown=null;
						return;
					}
				}

				@Override
				public void onFinish() {
					if (g4Manager.isConnectionState() == false) {
						firstCowndown.cancel();
						firstCowndown = null;
						FirstConnect();
					} else {
						firstCowndown.cancel();
						firstCowndown = null;
					}

				}
			};
			firstCowndown.start();
		}else {
			DebugLog.d("count available:");
		}
	}
	///// giam time countdown

public void reconnect1(){
	DebugLog.d("isReconnect:"+isReconnect);
	DebugLog.d("auto:"+isAuto);
	if (!isScreen)
		return;
	if (Utils.isAppIsInBackground(getActivity())){
		return;
	}
	if (countDownConnect==null) {
		if (isAuto == true) {
			if (g4Manager.isConnectionState()) {
				EventBus.getDefault().postSticky(new Events.BluetoothStatus(true));
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						btn_connect.setText("Disconnect");
						ConnectionState.setText("Device connected");
					}
				});
				return;
			} else {
				EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
			}
			if (isReconnect == false) {
				isReconnect = true;
				///
				if (!Utils.isAppIsInBackground(getActivity())){
					if (!g4Manager.isConnectionState() && isAuto == true) {
						if (getActivity() != null) {
							Toast b = Toast.makeText(getActivity(), "Trying to reconnect to G4", Toast.LENGTH_SHORT);
							b.setGravity(Gravity.CENTER, 0, 0);
							b.show();
						}
					}
				}

				if (g4Manager.isConnectionState()) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							btn_connect.setText("Disconnect");
							ConnectionState.setText("Device connected");
						}
					});
					EventBus.getDefault().postSticky(new Events.BluetoothStatus(true));
					return;
				} else {
					EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
				}

				doAutoConnect();
				countDownConnect = new CountDownTimer(20000, 5000) {
					@Override
					public void onTick(long millisUntilFinished) {
						if (Utils.isAppIsInBackground(getActivity())){
							countDownConnect.cancel();
							countDownConnect=null;
							return;
						}
						if (!isScreen){
							countDownConnect.cancel();
							countDownConnect=null;
						}
					}

					@Override
					public void onFinish() {
						countDownConnect.cancel();
						countDownConnect = null;
						if (!g4Manager.isConnectionState()) {
							EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
							isReconnect = false;
							reconnect1();
						} else {
							isReconnect = false;
							EventBus.getDefault().postSticky(new Events.BluetoothStatus(true));
							DebugLog.d("break::");
							btn_connect.setText("Disconnect");
							ConnectionState.setText("Device connected");
					/*if (getActivity()!=null) {
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast toast = Toast.makeText(getActivity(), "Connected Successfully", Toast.LENGTH_SHORT);
								toast.setGravity(Gravity.CENTER, 0, 0);
								toast.show();
							}
						});
					}*/
							return;
						}

					}
				}.start();
			} else {
				return;
			}
		}
	}else {
		DebugLog.d("count available:");
	}
}

@Override
public void onPause() {
	super.onPause();

	result = false;
	if (get != null) {
		if (get.getStatus() == AsyncTask.Status.RUNNING) {
			get.cancel(true);
		}
	}

	try {
		stopRepeatingTask();
		longRunningTaskFuture.cancel(true);
	}
	catch (Exception e) {
	}
	stopRepeatingTask();
}

private void prepareForScan() {
	if (isBleSupported()) {
		// Ensures Bluetooth is enabled on the device
		BluetoothManager btManager = (BluetoothManager) getActivity().getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);

		BluetoothAdapter btAdapter = btManager.getAdapter();
		if (btAdapter.isEnabled()) {
			// Prompt for runtime permission
			if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				startLeScan();
			}
			else {
				ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
			}
		}
		else {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}
	else {
		//Toast.makeText(getActivity(), "BLE is not supported", Toast.LENGTH_LONG).show();
	}
}

private boolean isBleSupported() {
	boolean a=false;
	try{
		a=getActivity().getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
	}catch (Exception e){
		e.toString();
	}
	return a;
}

private void startLeScan() {
	activity.runOnUiThread(new Runnable() {
		@Override
		public void run() {
			if (mScanning == true) {
				stopLeScan();
			}
			mScanning = true;
			ScanSettings settings = new ScanSettings.Builder()
					.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
					.setReportDelay(1000)
					.build();
			List<ScanFilter> filters = new ArrayList<>();
			filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE_UUID)).build());
			try {
				mScanner.startScan(filters, settings, mScanCallback);
			}
			catch (Exception e) {
			}

			if (attempt > 3) {
				attempt = 1;
			}
			ConnectionState = (TextView) v.findViewById(R.id.ConnectionState);
			ConnectionState.setText("Connect attempt " + attempt + " of " + MAX_CONNECT_attempt + "...");
			mStopScanHandler.postDelayed(mStopScanRunnable, SCAN_TIMEOUT_MS);
		}
	});
}



private void startReconnect() {
	try {
		SharedPreferences prefs = getActivity().getSharedPreferences("mac_address", 0);
		if (prefs.getString("value", "") != null) {
			address = prefs.getString("value", "");
		}
	}
	catch (Exception e) {
	}
	if (address != null) {
//		if (mScanning == true) {
//			stopLeScan();
//		}
//		mScanning = true;
//		ScanSettings settings = new ScanSettings.Builder()
//				.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//				.setReportDelay(1000)
//				.build();
//		List<ScanFilter> filters = new ArrayList<>();
//		filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE_UUID)).build());
//		try {
//			mScanner.startScan(filters, settings, mScanCallback);
//		}
//		catch (Exception e) {
//		}
		mGattClient.onCreate(getActivity(), address, new GattClient.OnCounterReadListener() {
			@Override
			public void onCounterRead(final String value) {
				/*DebugLog.d("data::"+value);
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!g4Manager.getStringBuider().toString().contains(value)) {
							g4Manager.addBuilder(value);
						}
						//find OK to get data
						if (g4Manager.getStringBuider().toString().contains("OK")) {
							Log.d("G4Chat2", g4Manager.getStringBuider().toString());
							if (g4Manager.getStringBuider().indexOf("$GMGL") > 0 || g4Manager.getStringBuider().indexOf("$GMGL") > 0) {
								g4Manager.handleMessageScreeen(g4Manager.getStringBuider());
							}
							else {
								g4Manager.processingData(g4Manager.getStringBuider());
							}
							if (g4Manager.getStringBuider().toString().contains("GPMS=0")) {
								g4Manager.setIDMessendger(g4Manager.getStringBuider().toString(), true);
							}
							g4Manager.executeCommand(mGattClient);
							g4Manager.setStringBuider(null);
						}

						if (StatusUpdate == false) {
							if (g4Manager.getIMEI() != null && StatusIMEIUpdate == false) {
								StatusIMEIUpdate = true;
								txt_imei.setText(g4Manager.getIMEI());
							}
							if (g4Manager.getDeviceName() != null && StatusNameUpdate == false) {
								StatusNameUpdate = true;
								name_device.setText(g4Manager.getDeviceName());
							}

							if (g4Manager.getFirmware() != null && StatusFirmwareUpdate == false) {
								if (g4Manager.getFirmware().contains("ATI") != true) {
									StatusFirmwareUpdate = true;
									txt_firmware.setText(g4Manager.getFirmware());
								}
							}
							if (StatusAlertReport == false && g4Manager.getAlertReportingTime() != null) {
								StatusAlertReport = true;
							}
							if (StatusNormalReport == false && g4Manager.getNormalReportingTime() != null) {
								StatusNormalReport = true;
							}
							if (StatusLedBrightness == false && g4Manager.getLed() != null) {
								StatusLedBrightness = true;
							}
							if (StatusIMEIUpdate == true && StatusNameUpdate == true && StatusFirmwareUpdate == true
									&& StatusAlertReport == true && StatusNormalReport == true && StatusLedBrightness == true) {
								StatusUpdate = true;
								get.cancel(true);
								g4Manager.resetCommandList();
								EventBus.getDefault().post(new Events.RunSignal(true));
								Toast.makeText(getActivity(), "Get Status Complete", Toast.LENGTH_SHORT).show();
							}
						}
						ShowSignal();
					}
				});*/
			}

			@Override
			public void onRSSIChange(final int rssi) {

				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						ShowSignal();
						if (rssi <= -60 && rssi > 0) {
							imgBluetooth_Signal.setImageResource(R.drawable.signal_ic_4);
						}
						else if (rssi <= -60 && rssi > -75) {
							imgBluetooth_Signal.setImageResource(R.drawable.signal_ic_3);
						}
						else if (rssi <= -75 && rssi > -90) {
							imgBluetooth_Signal.setImageResource(R.drawable.signal_ic_2);
						}
						else if (rssi <= -90) {
							imgBluetooth_Signal.setImageResource(R.drawable.signal_ic_1);
						}
					}
				});
			}

			@Override
			public void onConnected(final boolean success) {

				Log.d("failed","onconected 7");
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!success) {
							/*Toast toast = Toast.makeText(getActivity(), "Disconnected", Toast.LENGTH_SHORT);
							if(toast.getView().isShown()){
								toast.cancel();
							}else {
								toast.show();
							}*/
							/*mGattClient.setListener(new GattClient.OnCounterReadListener() {
								@Override
								public void onCounterRead(String value) {
								}

								@Override
								public void onConnected(boolean success) {
								}

								@Override
								public void onRSSIChange(int rssi) {
									imgBluetooth_Signal.setImageResource(0);
								}
							});*/
							SystemClock.sleep(500);
							try {
								mGattClient.onDestroy();
							}
							catch (Exception e) {

							}
							btn_connect.setText("Connect");
							BluetoothManager mBluetoothManager1 = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
							BluetoothAdapter mBluetoothAdapter1 = mBluetoothManager1.getAdapter();
							if (!mBluetoothAdapter1.isEnabled()) {
								ConnectionState.setText("Device Disconnected");
							}
							else {
								//	ConnectionState.setText("Connection failed");

							}
							imgBluetooth_Signal.setImageResource(0);
							attempt = 1;
							mBluetoothManager = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
							mBluetoothAdapter = mBluetoothManager.getAdapter();
//							if (!pressConnect && mBluetoothAdapter.isEnabled()) {
//                                Log.d("failed","Run mReconnectAuto 7");
        //								reconnectHandler = new ReconnectHandler();
//							}

							g4Manager.setConnectionState(false);
							imgGPS_Signal.setImageResource(android.R.color.transparent);
							imgIridium_Signal.setImageResource(android.R.color.transparent);
							imgBluetooth_Signal.setImageResource(android.R.color.transparent);
							EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
							EventBus.getDefault().post(new Events.ReceiverMessageG4(false));
							g4Manager.setConnectionState(false);
							conntectionState = 0;
							if (get != null) {
								if (get.getStatus() == AsyncTask.Status.RUNNING) {
									get.cancel(true);
								}
							}
							EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
							EventBus.getDefault().post(new Events.RunSignal(false));
						}
						else {
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									/*Toast toast = Toast.makeText(getActivity(), "Connected Successfully", Toast.LENGTH_SHORT);
									if(toast.getView().isShown()){
										toast.cancel();
									}else {
										toast.show();
									}*/
								}
							});


							isConnected = true;
							attempt = 1;
							conntectionState = 1;
							InitSetting();
							btn_connect.setText("Disconnect");
							ConnectionState.setText("Device connected");
							g4Manager.setConnectionState(true);
							EventBus.getDefault().postSticky(new Events.BluetoothStatus(true));
							g4Manager.setLed("254");
							g4Manager.setClient(mGattClient);
							if (address != null) {
								g4Manager.setAddress(address);
							}
							application.setG4Manager(g4Manager);
							try {
								sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
								editor = sharedPref.edit();
								editor.putInt("message_time_out", 5);
								editor.apply();
							}
							catch (Exception e) {
							}
							if(reconnectHandler!=null) {
								reconnectHandler.stopReconnect();
							}
							pressConnect = false;
							get = new getStatus(getActivity());
							get.execute();
							EventBus.getDefault().post(new Events.RunSignal(true));
						}
					}
				});
			}
		});
	}
	else {// Stops scanning after a pre-defined scan period.*/
		try {
			mGattClient.startClient(application);
			final StringBuilder databuilder = new StringBuilder(200);
			InitSetting();
		}
		catch (Exception e) {
			ConnectionState.setText("Device Disconnected");
			try {
				if (mGattClient != null)
					mGattClient.onDestroy();
			}
			catch (Exception e2) {
			}
		}
	}
}

private void stopLeScan() {
	if (mScanning) {
		mScanning = false;
		mScanner.stopScan(mScanCallback);
		mStopScanHandler.removeCallbacks(mStopScanRunnable);
	}
}

@Override
public void onStop() {
	super.onStop();
	EventBus.getDefault().unregister(this);
	stopRepeatingTask();
	if (get != null) {
		if (get.getStatus() == AsyncTask.Status.RUNNING) {
			get.cancel(true);
		}
	}
	try {
		longRunningTaskFuture.cancel(true);
	}
	catch (Exception e) {
	}
	if(reconnectHandler!=null){
		reconnectHandler.stopReconnect();
	}
	g4Manager.setStatusScreen(false);
	EventBus.getDefault().post(new Events.RunSignal(false));
}

private void startInteractActivity(BluetoothDevice device) {
	try {
		address = device.getAddress();
		SharedPreferences.Editor editor1 = getActivity().getSharedPreferences("mac_address", 0).edit();
		editor1.putString("value", address);
		editor1.apply();
	}
	catch (Exception e) {
	}
	final StringBuilder databuilder = new StringBuilder(200);

	try {
		mGattClient.onCreate(getActivity(), address, new GattClient.OnCounterReadListener() {
			@Override
			public void onCounterRead(final String value) {
				/*DebugLog.d("data::"+value);
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!g4Manager.getStringBuider().toString().contains(value)) {
							g4Manager.addBuilder(value);
						}
						//find OK to get data
						if (g4Manager.getStringBuider().toString().contains("OK")) {
							Log.d("G4Chat2", g4Manager.getStringBuider().toString());
							if (g4Manager.getStringBuider().indexOf("$GMGL") > 0 || g4Manager.getStringBuider().indexOf("$GMGL") > 0) {
								g4Manager.handleMessageScreeen(g4Manager.getStringBuider());
							}
							else {
								g4Manager.processingData(g4Manager.getStringBuider());
							}
							if (g4Manager.getStringBuider().toString().contains("GPMS=0")) {
								g4Manager.setIDMessendger(g4Manager.getStringBuider().toString(), true);
							}
							g4Manager.executeCommand(mGattClient);
							g4Manager.setStringBuider(null);
						}

						if (StatusUpdate == false) {
							if (g4Manager.getIMEI() != null && StatusIMEIUpdate == false) {
								StatusIMEIUpdate = true;
								txt_imei.setText(g4Manager.getIMEI());
							}
							if (g4Manager.getDeviceName() != null && StatusNameUpdate == false) {
								StatusNameUpdate = true;
								name_device.setText(g4Manager.getDeviceName());
							}

							if (g4Manager.getFirmware() != null && StatusFirmwareUpdate == false) {
								if (g4Manager.getFirmware().contains("ATI") != true) {
									StatusFirmwareUpdate = true;
									txt_firmware.setText(g4Manager.getFirmware());
								}
							}
							if (StatusAlertReport == false && g4Manager.getAlertReportingTime() != null) {
								StatusAlertReport = true;
							}
							if (StatusNormalReport == false && g4Manager.getNormalReportingTime() != null) {
								StatusNormalReport = true;
							}
							if (StatusLedBrightness == false && g4Manager.getLed() != null) {
								StatusLedBrightness = true;
							}
							if (StatusIMEIUpdate == true && StatusNameUpdate == true && StatusFirmwareUpdate == true
									&& StatusAlertReport == true && StatusNormalReport == true && StatusLedBrightness == true) {
								StatusUpdate = true;
								get.cancel(true);
								g4Manager.resetCommandList();
								EventBus.getDefault().post(new Events.RunSignal(true));
								Toast.makeText(getActivity(), "Get Status Complete", Toast.LENGTH_SHORT).show();
							}
						}
						ShowSignal();
					}
				});*/
			}

			@Override
			public void onRSSIChange(final int rssi) {

				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						ShowSignal();
						if (rssi <= -60 && rssi > 0) {
							imgBluetooth_Signal.setImageResource(R.drawable.signal_ic_4);
						}
						else if (rssi <= -60 && rssi > -75) {
							imgBluetooth_Signal.setImageResource(R.drawable.signal_ic_3);
						}
						else if (rssi <= -75 && rssi > -90) {
							imgBluetooth_Signal.setImageResource(R.drawable.signal_ic_2);
						}
						else if (rssi <= -90) {
							imgBluetooth_Signal.setImageResource(R.drawable.signal_ic_1);
						}
					}
				});
			}

			@Override
			public void onConnected(final boolean success) {

				Log.d("failed","onconected 7");
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!success) {
							//reconnect1();
							if (isScreen) {
								///
								//Toast.makeText(getActivity(), "Disconnected", Toast.LENGTH_SHORT).show();
							}
							if (mGattClient!=null) {
								mGattClient.setListener(new GattClient.OnCounterReadListener() {
									@Override
									public void onCounterRead(String value) {
									}

									@Override
									public void onConnected(boolean success) {
									}

									@Override
									public void onRSSIChange(int rssi) {
										imgBluetooth_Signal.setImageResource(0);
									}
								});
								SystemClock.sleep(500);
								try {
									mGattClient.onDestroy();
								} catch (Exception e) {

								}
								btn_connect.setText("Connect");
								BluetoothManager mBluetoothManager1 = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
								BluetoothAdapter mBluetoothAdapter1 = mBluetoothManager1.getAdapter();
								if (!mBluetoothAdapter1.isEnabled()) {
									ConnectionState.setText("Device Disconnected");
								} else {
									//	ConnectionState.setText("Connection failed");

								}
								imgBluetooth_Signal.setImageResource(0);
								attempt = 1;
								mBluetoothManager = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
								mBluetoothAdapter = mBluetoothManager.getAdapter();
//							if (!pressConnect && mBluetoothAdapter.isEnabled()) {
//                                Log.d("failed","Run mReconnectAuto 7");
//								reconnectHandler = new ReconnectHandler();
//							}

								g4Manager.setConnectionState(false);
								imgGPS_Signal.setImageResource(android.R.color.transparent);
								imgIridium_Signal.setImageResource(android.R.color.transparent);
								imgBluetooth_Signal.setImageResource(android.R.color.transparent);
								EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
								EventBus.getDefault().post(new Events.ReceiverMessageG4(false));
								g4Manager.setConnectionState(false);
								conntectionState = 0;
								if (get != null) {
									if (get.getStatus() == AsyncTask.Status.RUNNING) {
										get.cancel(true);
									}
								}
								EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
								EventBus.getDefault().post(new Events.RunSignal(false));
							}
						}
						else {
							///
							/*if (getActivity()!=null) {
								Toast.makeText(getActivity(), "Connection Successfully", Toast.LENGTH_SHORT).show();
							}*/
							isConnected = true;
							attempt = 1;
							conntectionState = 1;
							InitSetting();
							btn_connect.setText("Disconnect");
							ConnectionState.setText("Device connected");
							g4Manager.setConnectionState(true);
							EventBus.getDefault().postSticky(new Events.BluetoothStatus(true));
							g4Manager.setLed("254");
							g4Manager.setClient(mGattClient);
							if (address != null) {
								g4Manager.setAddress(address);
							}
							application.setG4Manager(g4Manager);
							try {
								sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
								editor = sharedPref.edit();
								editor.putInt("message_time_out", 5);
								editor.apply();
							}
							catch (Exception e) {
							}
							if(reconnectHandler!=null) {
								reconnectHandler.stopReconnect();
							}
							pressConnect = false;
							get = new getStatus(getActivity());
							get.execute();
							EventBus.getDefault().post(new Events.RunSignal(true));
						}
					}
				});
			}
		});
	}
	catch (Exception e) {
		e.toString();
	}
}

@Override
public void onDetach() {
	super.onDetach();
	activity = null;
}

public void ShowSignal() {
	try {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (g4Manager.isConnectionState()) {
					imgGPS_Signal = (ImageView) v.findViewById(R.id.gps_signal);
					imgIridium_Signal = (ImageView) v.findViewById(R.id.iridium_signal);
					imgGPS_Signal.setVisibility(View.VISIBLE);
					imgIridium_Signal.setVisibility(View.VISIBLE);

					if (g4Manager.getSATS() != null) {
						try {
							double signalSATS = Double.parseDouble(g4Manager.getSATS());

							if (signalSATS > 0.1 && signalSATS <= 0.2) {
								imgGPS_Signal.setImageResource(R.drawable.signal_ic_1);
							} else if (signalSATS > 0.2 && signalSATS <= 0.3) {
								imgGPS_Signal.setImageResource(R.drawable.signal_ic_2);
							} else if (signalSATS > 0.3 && signalSATS <= 0.4) {
								imgGPS_Signal.setImageResource(R.drawable.signal_ic_3);
							} else if (signalSATS > 0.4) {
								imgGPS_Signal.setImageResource(R.drawable.signal_ic_4);
							} else if (signalSATS == 0) {
								imgGPS_Signal.setImageResource(0);
							}
						} catch (NumberFormatException e) {
							imgGPS_Signal.setImageResource(0);
						}
					}
					if (g4Manager.getGPSStrength() != null) {
						try {
							double signalIridium = Double.parseDouble(g4Manager.getGPSStrength());
							if (signalIridium > 0.1 && signalIridium <= 0.2) {
								imgIridium_Signal.setImageResource(R.drawable.signal_ic_1);
							} else if (signalIridium > 0.2 && signalIridium <= 0.3) {
								imgIridium_Signal.setImageResource(R.drawable.signal_ic_2);
							} else if (signalIridium > 0.3 && signalIridium <= 0.4) {
								imgIridium_Signal.setImageResource(R.drawable.signal_ic_3);
							} else if (signalIridium > 0.4) {
								imgIridium_Signal.setImageResource(R.drawable.signal_ic_4);
							} else if (signalIridium == 0) {
								imgIridium_Signal.setImageResource(0);
							}
						} catch (NumberFormatException e) {
							imgIridium_Signal.setImageResource(0);
						}
					}
				}
			}
		});
	}
	catch (Exception e) {
	}
}

public void onEventMainThread(Events.RunSignal event) {
		if (event.isStart()) {
			RunSignal.run();
		}
		else {
			handler.removeCallbacks(RunSignal);
		}
	}
	public void onEventMainThread(Events.AutoConnect event) {
		DebugLog.d("receive connect:");
		reconnect1();

	}
	public void onEventMainThread(final Events.BluetoothStatus event) {
		DebugLog.d("receive connect:");
		if (event.isBluetoothOn()) {
			g4Manager.setConnectionState(true);
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					btn_connect.setText("Disconnect");
					ConnectionState.setText("Device connected");
				}
			});
		}
	}

void stopRepeatingTask() {
	//handler.removeCallbacks(RunSignal);
	if (handler != null) {
		Log.d("G4Chat1", moduleLogId + "stopRepeatingTask:Enter");
		handler.removeCallbacks(RunSignal);
	}
}

@SuppressWarnings("unused")
public void onEventMainThread(Events.stopStatusSignal event) {
	stopRepeatingTask();
}

public void onEventMainThread(Events.InitSettingStatus event) {
	InitSetting();
}

@SuppressWarnings("unused")
public void onEventMainThread(Events.handleDisConnect event) {
	// EventBus.getDefault().removeStickyEvent(event);

	try {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (activity != null) {
					View v = getView();
					if (v != null) {
						try {
							EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
							isConnected = false;
							// address = null;
							handler.removeCallbacks(RunSignal);
							EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
							EventBus.getDefault().post(new Events.ReceiverMessageG4(false));
							g4Manager.setConnectionState(false);
							conntectionState = 0;

							if (get != null) {
								if (get.getStatus() == AsyncTask.Status.RUNNING) {
									get.cancel(true);
								}
							}
							EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
							g4Manager.setConnectionState(false);
							btn_connect.setText("Connect");
							ConnectionState.setText("Device Disconnected");
							imgBluetooth_Signal.setImageResource(0);
							imgIridium_Signal.setImageResource(0);
							imgGPS_Signal.setImageResource(0);
							txt_imei = (TextView) v.findViewById(R.id.txtImei);
							txt_firmware = (TextView) v.findViewById(R.id.txtFirmware);
							name_device = (TextView) v.findViewById(R.id.txtDevice);
							imgBluetooth_Signal = (ImageView) v.findViewById(R.id.bluetooth_signal);
							handler.removeCallbacks(RunSignal);
							EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
							EventBus.getDefault().post(new Events.ReceiverMessageG4(false));
							g4Manager.setConnectionState(false);
							conntectionState = 0;
							if (get != null) {
								if (get.getStatus() == AsyncTask.Status.RUNNING) {
									get.cancel(true);
								}
							}
							EventBus.getDefault().post(new Events.UpdateButton());
							g4Manager.setConnectionState(false);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
						imgBluetooth_Signal.setImageResource(0);
						imgGPS_Signal.setImageResource(0);
						mBluetoothManager = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
						mBluetoothAdapter = mBluetoothManager.getAdapter();
						if(reconnectHandler!=null) {
							if (reconnectHandler.isAlive()) {
								reconnectHandler.stopReconnect();
							}
						}
						if (!pressConnect && mBluetoothAdapter.isEnabled()) {
                            Log.d("failed","Run mReconnectAuto 8");
							reconnectHandler = new ReconnectHandler();
						}
						try {
							//mGattClient.onDestroy();
						}
						catch (Exception e) {
							e.toString();
						}
					}
					else {
						//  Toast.makeText(getActivity(),"view null",Toast.LENGTH_LONG).show();
						mGattClient.startClient(getActivity());
					}
				}
				else {

				}
			}
		});

	}
	catch (Exception e) {
		//Toast.makeText(getActivity(), "activity null", Toast.LENGTH_LONG).show();
		try {
			if (mGattClient != null) {
				mGattClient.onDestroy();
			}
		}
		catch (Exception e3) {
		}
	}
}



@SuppressWarnings("unused")
public void onEventMainThread(Events.UpdateButton event) {
	ConnectionState = (TextView) v.findViewById(R.id.ConnectionState);
	btn_connect = (Button) v.findViewById(R.id.button_connect);
	conntectionState = 0;
	ConnectionState.setText("Device Disconnected");
	btn_connect.setText("Connect");
}
	public void onEventMainThread(Events.BluetoothData event) {
		//DebugLog.e("data:"+event.getData());
	}
public void onEventMainThread(Events.ScanbarCodeConnect event) {
}

public void onEventMainThread(Events.Interac event) {
	startInteractActivity(event.bluetoothDevice());
}

public void onEventMainThread(Events.Reconnect event)
{
	startReconnect();
}

@Override
public void onStart() {
	super.onStart();

}

@Override
public void onDestroy() {
	super.onDestroy();
	EventBus.getDefault().unregister(this);
	isFirst=0;
	DebugLog.d("destroy view:");
	stopLeScan();

}

@Override
public void setUserVisibleHint(boolean isVisibleToUser) {
	super.setUserVisibleHint(isVisibleToUser);

	if (g4Manager!=null) {
		DebugLog.d("check is connect:" + g4Manager.isConnectionState());
		//EventBus.getDefault().post(new Events.UpdateAlertState());
	}
	if (!isVisibleToUser) {
		stopRepeatingTask();
		if (g4Manager != null) {
			if (g4Manager.isConnectionState())
				g4Manager.setStatusScreen(false);
			if(get!=null) {
				get.cancel(true);
			}
		}
	}
	else {
		g4Manager.setStatusScreen(true);
		if (g4Manager.isConnectionState()) {
			EventBus.getDefault().postSticky(new Events.BluetoothStatus(true));
			btn_connect.setText("Disconnect");
			ConnectionState.setText("Device connected");
			EventBus.getDefault().post(new Events.StopMarkAlerFragment());
			RunSignal.run();
			g4Manager.setStatusScreen(true);
		}else {
			btn_connect.setText("Connect");
			ConnectionState.setText("Disconnected");
		}
	}
}

class getStatus extends AsyncTask<Void, Integer, Void> {
	Activity contextCha;

	public getStatus(Activity ctx) {
		contextCha = ctx;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		if(!isCancelled()) {
			Log.d("G4Chat1", moduleLogId + "doInBackground:Enter");
			Log.d("G4Chat1", moduleLogId + "doInBackground:Send initital commands to G4");

			g4Manager.executeImmediateCommand("AT$GMGF=1");
			g4Manager.executeImmediateCommand("AT$GNMI=1");
			g4Manager.executeImmediateCommand("AT$GSMI=1");
			g4Manager.executeImmediateCommand("AT$GMGF=1");
			g4Manager.executeImmediateCommand("AT$FFAER=1");
			g4Manager.executeImmediateCommand("AT$FFARP?");
			g4Manager.executeImmediateCommand("AT$DIFC=");
			g4Manager.executeImmediateCommand("AT$MCSQ");

			SystemClock.sleep(2000);
			Log.d("G4Chat1", moduleLogId + "doInBackground:Wait for status update");

			// wait for all status report come in.  this flag is set in the read event loop above
			while (StatusUpdate == false) {
				if (g4Manager.isStatusScreen()) {
					Log.d("G4Chat1", moduleLogId + "doInBackground:Send initital commands to G4");
					InitSetting();
					g4Manager.resetCommandList();
					//Check connection
					if (conntectionState == 0) {
						break;
					}

					if (StatusNameUpdate == false) {
						g4Manager.getName();
					}
					if (StatusLedBrightness == false) {
						g4Manager.getLedBrightness();
					}
					if (StatusAlertReport == false) {
						g4Manager.getAlertReport();
					}
					if (StatusNormalReport == false) {
						g4Manager.getNormalReport();
					}
					if (StatusIMEIUpdate == false) {

						g4Manager.getDeviceIMEI();

				}
				if (StatusFirmwareUpdate == false) {
					g4Manager.getDeviceFirmware();
				}
				g4Manager.executeCommand(mGattClient);
				SystemClock.sleep(2000);
			}
		}
			//LQL - Why?  What to execute here?
			//g4Manager.executeCommand(mGattClient);
			//SystemClock.sleep(800);
			Log.d("G4Chat1", moduleLogId + "doInBackground:Got status, update signal");
			RunSignal.run();
			publishProgress(1);
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		if (g4Manager.getIMEI() != null) {
			txt_imei.setText(g4Manager.getIMEI());
		}
		if (g4Manager.getDeviceName() != null) {
			StatusNameUpdate = true;
			name_device.setText(g4Manager.getDeviceName());
		}

		if (g4Manager.getFirmware() != null) {
			StatusFirmwareUpdate = true;
			txt_firmware.setText(g4Manager.getFirmware());
		}
		if (g4Manager.getGPSStrength() != null) {
			try {
				double signalIridium = Double.parseDouble(g4Manager.getGPSStrength());
				if (signalIridium > 0.1 && signalIridium <= 0.2) {
					imgIridium_Signal.setImageResource(R.drawable.signal_ic_1);
				}
				else if (signalIridium > 0.2 && signalIridium <= 0.3) {
					imgIridium_Signal.setImageResource(R.drawable.signal_ic_2);
				}
				else if (signalIridium > 0.3 && signalIridium <= 0.4) {
					imgIridium_Signal.setImageResource(R.drawable.signal_ic_3);
				}
				else if (signalIridium > 0.4) {
					imgIridium_Signal.setImageResource(R.drawable.signal_ic_4);
				}
			}
			catch (NumberFormatException e) {

			}
		}
	}
}
public class ReconnectHandler extends HandlerThread{
	public final Handler mReconnectHandler;
	public final Runnable mReconnectAuto;
	public ReconnectHandler() {
		super(ReconnectHandler.class.getName(), 1);
		//start();
		mReconnectHandler = new Handler(getLooper());
		mReconnectAuto = new mReconnectAuto();
		mReconnectHandler.post(mReconnectAuto);
		Log.d("failed", "Initilaze");
	}
	void stopReconnect() {
		ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor();
		Future longRunningTaskFuture = threadPoolExecutor.submit(mReconnectAuto);
		longRunningTaskFuture.cancel(true);
		mReconnectHandler.removeCallbacks(mReconnectAuto);

	}
	public final class mReconnectAuto implements Runnable {
		@Override
		public void run() {

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
							DebugLog.d("failed:"+"Enter Reconnect");
							startReconnect();
							imgBluetooth_Signal.setImageResource(0);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						stopLeScan();
					}
				} finally {
					mReconnectHandler.postDelayed(mReconnectAuto, 8000);
				}
			}

	};
}


}