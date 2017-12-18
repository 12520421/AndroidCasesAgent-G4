package com.xzfg.app.fragments;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.SwitchCompat;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.xzfg.app.Application;
//import com.xzfg.app.DebugLog;
import com.xzfg.app.BaseFragment;
import com.xzfg.app.DebugLog;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.activities.AgentActivity;
import com.xzfg.app.managers.BluetoothManager;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.G4Manager;
import com.xzfg.app.managers.PingManager;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.services.GattClient;
import com.xzfg.app.services.SMSService;
import com.xzfg.app.util.DateUtil;

import android.location.Location;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Handler;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;


public class MarkAlertFragment extends BaseFragment {
	private static final String moduleLogId = "@F2-AlrtFrag:";
	static boolean isMarkspot = false;
	android.os.Handler handlerReportNormal = new android.os.Handler();
	android.os.Handler handlerReportAlert;
	android.os.Handler handlerRefreshAlert = new android.os.Handler();
	android.os.Handler handler = new android.os.Handler();
	boolean isActiveReport = true;
	@Inject
	Application application;
	@Inject
	FixManager fixManager;
	/*  @Inject
      G4Manager g4Manager;*/
	G4Manager g4Manager;
	private Button btnAlertOn;
	private Button btnAlertOff;
	private Button MarkSpot;
	private TextView currentAddress;
	private TextView alertLocation;
	private Location location;
	private Calendar calendar;
	private boolean pastAlertState = false;
	private boolean isShowing=false;
	private GattClient mGattClient = new GattClient();
	ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor();
	private View v;

	Future longRunningTaskFuture1;
	Future longRunningTaskFuture2;
	Future longRunningTaskFuture3;
	Future longRunningTaskFuture4;

	CountDownTimer countArlet;

	Runnable ReportNormal = new Runnable() {
		@Override
		public void run() {
			DebugLog.d("check run:");
			try {
				if (g4Manager.isMarkAlertScreen()) {
					//Toast.makeText(getActivity(),"hahahaha",Toast.LENGTH_LONG).show();
					Log.d("dmdmdm", "MarkAlert");
					Log.d("G4Chat", "Report Normal");
					Log.d("G4Chat1", "Report Normal");
					InitSetting();
					g4Manager.resetCommandList();
					g4Manager.getGPSFix();
					g4Manager.executeCommand(mGattClient);
				}
			} finally {
				try {
					handlerReportNormal.postDelayed(ReportNormal, 1000 * Integer.parseInt(g4Manager.getNormalReportingTime()));
				}
				catch (Exception e) {
				}
			}
		}
	};
//run config repost arlet
	Runnable ReportAlert = new Runnable() {
		@Override
		public void run() {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
			int mode = sharedPref.getInt("spinner_alerting",0);
			DebugLog.d("g4manager:"+g4Manager.getAlertReportingTime());
			DebugLog.d("g4manager:"+mode);
			try {
				if (g4Manager.isMarkAlertScreen()) {
					if(g4Manager.isAlertState())
					{
						/*Log.d("dmdmdm", "MarkAlert");
						Log.d("G4Chat", "Report Normal");
						Log.d("G4Chat1", "Report Normal");
						InitSetting();
						g4Manager.resetCommandList();
						g4Manager.getGPSFix();
						g4Manager.executeCommand(mGattClient);*/
						if (g4Manager!=null){
							if (g4Manager.isMarkAlertScreen()){
									DebugLog.d("send command:");
									g4Manager.getClient().writeInteractor("AT$GPSO", false);
									new CountDownTimer(300, 300) {
										@Override
										public void onTick(long millisUntilFinished) {

										}

										@Override
										public void onFinish() {
											setTextAlert();
										}
									}.start();

							}
						}

					}
					//Toast.makeText(getActivity(),"hahahaha",Toast.LENGTH_LONG).show();

				}
			} finally {
				try {
					//handlerReportAlert.postDelayed(ReportAlert, 1000 * mode);
					handlerReportAlert.postDelayed(ReportAlert, 1000 * Integer.parseInt(g4Manager.getAlertReportingTime()));
				}
				catch (Exception e) {
				}
			}
		}
	};

	Runnable RefreshMarkAlert = new Runnable() {
		@Override
		public void run() {
			DebugLog.d("check run:");
			try {
				try {
					//g4Manager.DeleteAllArray();
					g4Manager = application.getG4Manager();
					if (g4Manager.isConnectionState()) {
						if (g4Manager.isMarkAlertScreen()) {
							Log.d("G4Chat1", "MarkAlert");
							InitSetting();
							g4Manager.resetCommandList();
							g4Manager.addCommand("AT$FFALERT?");
							// g4Manager.getGPSFix();
							g4Manager.executeCommand(mGattClient);
							Log.d("MarkAlert", "Send Command, Check Alert Sate G4");
						}
					}
				}
				catch (Exception e) {
				}
			} finally {
				handlerRefreshAlert.postDelayed(RefreshMarkAlert, 4000);
			}
		}
	};
	Runnable MarkAlertScreen = new Runnable() {
		@Override
		public void run() {
			DebugLog.d("check run:");
			try {
				try {
					Log.d("G4Chat1","@Mark/Alert Screen");
					g4Manager = application.getG4Manager();
					if (g4Manager != null) {
						if (g4Manager.isMarkAlertScreen()) {

							if (g4Manager.isConnectionState()) {
								InitSetting();
								g4Manager.resetCommandList();
								g4Manager.addCommand("AT$GPMS=0");
								g4Manager.executeCommand(mGattClient);

							}
						}
						else{
							handler.removeCallbacks(MarkAlertScreen);
						}
					}
				}
				catch (Exception e) {

				}
			} finally {
				handler.postDelayed(MarkAlertScreen, 20000);
			}
		}
	};
	private void setTextAlert(){
		DebugLog.d("set text arlet:");
		if (g4Manager.getLongtiltude() != null && g4Manager.getAltiltude() != null) {
			g4Manager.setlastMarkedDate(new Date());
			if (g4Manager.isAlertState() == true) {
				alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
						", Lat =" + g4Manager.getAltiltude() +
						"\nAlert Sent  " + getLastMarkedUtcTime());
			} else if (g4Manager.isAlertClicked() == true) {
				alertLocation.setText("Long = " + g4Manager.getLongtiltude() + ", " +
						"Lat =" + g4Manager.getAltiltude() +
						"\nAlert Stopped  " + getLastMarkedUtcTime());
			} else {
				alertLocation.setText("");
			}
			if (g4Manager.isMarkClicked()) {
				DebugLog.d("test location:" + "Long = " + g4Manager.getLongtiltude()
						+ ", Lat = " + g4Manager.getAltiltude()
						+ "\nMarked   "
						+ getLastMarkedUtcTime());
				currentAddress.setText("Long = " + g4Manager.getLongtiltude()
						+ ", Lat = " + g4Manager.getAltiltude()
						+ "\nMarked   "
						+ getLastMarkedUtcTime());
			}
		}
		else {
			currentAddress.setText("");
			g4Manager.setlastMarkedDate(new Date());
			alertLocation.setText(/*"Long = ?" + "\nLat = ?" + g4Manager.getAltiltude()+*/""  /*+ getLastAlertOffUtcTime()*/);
		}
	}
	private boolean gpsStatus = false;
	private Geocoder geocoder;
	private List<Address> addresses;
	private String Now;
	//   boolean check = false;
	private boolean success1 = false;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((Application) activity.getApplication()).inject(this);
	}
private void refreshArlet(){
	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
	int mode = sharedPref.getInt("spinner_alerting",0);
	countArlet=new CountDownTimer(mode,mode) {
		@Override
		public void onTick(long millisUntilFinished) {

		}

		@Override
		public void onFinish() {
				if (!g4Manager.isConnectionState()){
					countArlet.cancel();
					countArlet=null;
				}
				if (g4Manager.isMarkAlertScreen()) {
					if (g4Manager.isAlertState()) {
						if (g4Manager != null) {
							if (g4Manager.isMarkAlertScreen()) {

								g4Manager.getClient().writeInteractor("AT$FFALERT=1", false);
								new CountDownTimer(300, 300) {
									@Override
									public void onTick(long millisUntilFinished) {

									}

									@Override
									public void onFinish() {
										setTextAlert();
										countArlet.cancel();
										countArlet=null;
										if (g4Manager.isMarkAlertScreen()){
											refreshArlet();
										}
									}
								}.start();

							}
						}

					}
				}
		}
	}.start();
}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		try{
			super.onConfigurationChanged(newConfig);
			frameLayout. removeAllViews();
			LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
				v = inflater.inflate(R.layout.fragment_markalert, null);
			}
			else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
				v = inflater.inflate(R.layout.fragment_markalert, null);
			}
			InitView();
			frameLayout .addView(v);
			setTextMarkAlert();
			CheckAlertState();
		}
		catch (Exception e)
		{

		}

	}

	FrameLayout frameLayout;

	public void setTextMarkAlert(){
		try{
			if (isMarkspot == false) {
				if (g4Manager.getLongtiltude() != null && g4Manager.getAltiltude() != null) {
						if (g4Manager.isAlertState() == true) {
							alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
									", Lat =" + g4Manager.getAltiltude() +
									"\nAlert Sent " + getLastMarkedUtcTime());
						} else if (g4Manager.isAlertClicked() == true) {
							alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
									", Lat =" + g4Manager.getAltiltude() +
									"\nAlert Stopped  " + getLastMarkedUtcTime());
						} else {
							alertLocation.setText("");
						}
				} else {
					alertLocation.setText(/*"Long = ?" + "\nLat = ?" + g4Manager.getAltiltude()+*/""  /*+ getLastAlertOffUtcTime()*/);
				}

			} else if (isMarkspot == true) {
				if (g4Manager.getLongtiltude() != null && g4Manager.getAltiltude() != null) {
						if (g4Manager.isAlertState() == true) {
							alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
									", Lat =" + g4Manager.getAltiltude() +
									"\nAlert Sent  " + getLastMarkedUtcTime());
						} else if (g4Manager.isAlertClicked() == true) {
							alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
									", Lat =" + g4Manager.getAltiltude() +
									"\nAlert Stopped " + getLastMarkedUtcTime());
						} else {
							alertLocation.setText("");
						}
						DebugLog.d("test location:" + "Long = " + g4Manager.getLongtiltude()
								+ ", Lat = " + g4Manager.getAltiltude()
								+ "\nMarked   "
								+ getLastMarkedUtcTime());
						currentAddress.setText("Long = " + g4Manager.getLongtiltude()
								+ ", Lat = " + g4Manager.getAltiltude()
								+ "\nMarked   "
								+ getLastMarkedUtcTime());
				} else {
					alertLocation.setText("");
					//	currentAddress.setText("");
				}
			}
		}
		catch (Exception e)
		{

		}

	}
	public void InitView()
	{
		btnAlertOn = (Button) v.findViewById(R.id.alerton);
		btnAlertOff = (Button) v.findViewById(R.id.alertoff);
		MarkSpot = (Button) v.findViewById(R.id.btn_mark_spot);
		alertLocation = (TextView) v.findViewById(R.id.alert_location);
		currentAddress = (TextView) v.findViewById(R.id.location_spot);
		AgentSettings settings = application.getAgentSettings();

		btnAlertOn.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				DebugLog.d("click:");
				if (g4Manager != null) {
					g4Manager = application.getG4Manager();
				}
				if (application.getG4Manager() == null || application.getG4Manager().isConnectionState() == false) {
					Toast.makeText(getActivity(), "Please connect to G4", Toast.LENGTH_LONG).show();
				}
				else {
					InitSetting();
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
				}
				CheckAlertState();
			}
		});
        /////
		btnAlertOff.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				DebugLog.d("click:");
				if (application.getG4Manager() == null || application.getG4Manager().isConnectionState() == false) {
					Toast.makeText(getActivity(), "Please connect to G4", Toast.LENGTH_LONG).show();
				}
				else {
					InitSetting();
					g4Manager.setlastMarkedDate(new Date());
					g4Manager.resetCommandList();
					pastAlertState = g4Manager.isAlertState();
					g4Manager.setAlertState(false);
					Log.d("StateAlert", "Click Alert Off" + String.valueOf(pastAlertState));
					g4Manager.resetCommandList();
					g4Manager.setAlertOff();
					g4Manager.getGPSFix();
					//g4Manager.setAlertState(true);

					g4Manager.executeCommand(mGattClient);
					stopRepeatingTask();
				}
				CheckAlertState();

			}
		});
		MarkSpot.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (application.getG4Manager() == null || application.getG4Manager().isConnectionState() == false) {
					Toast.makeText(getActivity(), "Please connect to G4", Toast.LENGTH_LONG).show();
				}
				else {
					InitSetting();
					g4Manager.setlastMarkedDate(new Date());
					g4Manager.resetCommandList();
					g4Manager.addCommand("AT$FFMARK");
					g4Manager.executeCommand(mGattClient);
					SystemClock.sleep(200);
					g4Manager.resetCommandList();
					g4Manager.getGPSFix();
					g4Manager.executeCommand(mGattClient);
					isMarkspot = true;
					g4Manager.setMarkClicked(true);
					SystemClock.sleep(500);

					if (g4Manager.getLongtiltude() != null && g4Manager.getAltiltude() != null) {
							DebugLog.d("test location:" + "Long = " + g4Manager.getLongtiltude()
									+ ", Lat = " + g4Manager.getAltiltude()
									+ "\nMarked   "
									+ getLastMarkedUtcTime());
							currentAddress.setText("Long = " + g4Manager.getLongtiltude()
									+ ", Lat = " + g4Manager.getAltiltude()
									+ "\nMarked   "
									+ getLastMarkedUtcTime());
					}
					else {
						currentAddress.setText("");
					}
					if (g4Manager.isAlertClicked() != true) {
						SystemClock.sleep(500);
						if (g4Manager.getLongtiltude() != null && g4Manager.getAltiltude() != null) {
								if (g4Manager.isAlertState() == true) {
									alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
											", Lat =" + g4Manager.getAltiltude() +
											"\nAlert Sent " + getLastMarkedUtcTime());
								} else if (g4Manager.isAlertClicked() == true) {
									alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
											", Lat =" + g4Manager.getAltiltude() +
											"\nAlert Stopped " + getLastMarkedUtcTime());
								} else {
									alertLocation.setText("");
								}
						}
						else {
							currentAddress.setText("");
						}
					}
				}
			}
		});
		alertLocation.setGravity(Gravity.CENTER_HORIZONTAL);
		currentAddress.setGravity(Gravity.CENTER_HORIZONTAL);
		checkMarkAlertMode();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		frameLayout = new FrameLayout(getActivity());
		v = inflater.inflate(R.layout.fragment_markalert, container, false);
		if (handlerReportAlert==null){
			handlerReportAlert=new android.os.Handler();
		}
		InitView();
		frameLayout.addView(v);
		return frameLayout;

	}

	private String getLastMarkedUtcTime() {
		// return last marked date in UTC
		// return last marked date in UTC
		if (g4Manager != null) {
			SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss z");
			f.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date date=new Date();
			return (f.format(g4Manager.getlastMarkedDate()));
		}
		else {
			return "Unknown time";
		}
	}

	private String getLastAlertOnUtcTime() {
		if (g4Manager != null) {
			SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss z");
			f.setTimeZone(TimeZone.getTimeZone("UTC"));
			return (f.format(g4Manager.getlastAlertOnDate()));
		}
		else {
			return "Unknown time";
		}
	}

	private String getLastAlertOffUtcTime() {
		if (g4Manager != null) {
			SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss z");
			f.setTimeZone(TimeZone.getTimeZone("UTC"));
			return (f.format(g4Manager.getlastAlertOffDate()));
		}
		else {
			return "Unknown time";
		}
	}

	private boolean isConnected() {
		NetworkInfo activeNetwork = ((ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
	}

	public void onEventMainThread(Events.ProfileLastFixReceived event) {
	}

	void InitSetting() {
		Log.d("G4Chat1", moduleLogId+"InitSetting: Enter");
		AgentSettings settings = application.getAgentSettings();
		g4Manager = application.getG4Manager();
		if (g4Manager != null) {
			//pastAlertState = g4Manager.isAlertState();
			Log.d("StateAlert", "Init Setting :" + String.valueOf(pastAlertState));
			if (g4Manager.getClient().getListCommandAll() != null) {
				btnAlertOn = (Button) v.findViewById(R.id.alerton);
				btnAlertOff = (Button) v.findViewById(R.id.alertoff);

				mGattClient = g4Manager.getClient();
				if (mGattClient != null) {
					mGattClient.setContext(getActivity());
				}
				final StringBuilder databuilder = new StringBuilder(200);
				if (mGattClient != null) {
					mGattClient.setListener(new GattClient.OnCounterReadListener() {
						@Override
						public void onCounterRead(final String value) {
							/*if (!TextUtils.isEmpty(value)) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Log.d("G4Chat1", moduleLogId + "InitSetting:onCounterRead:read: [[" + value + "]]");
										if (!g4Manager.getStringBuider().toString().contains(value)) {
											g4Manager.addBuilder(value);
										}

										//find OK to get data
										//DebugLog.e("message:"+g4Manager.getStringBuider().toString());
										if (g4Manager.getStringBuider().toString().contains("OK")) {
											//got message with OK from G4 - monitor alert messages from G4
											Log.d("G4Chat1", "MarkAlertFragment:InitSetting:onCounterRead:read: [[[[" + g4Manager.getStringBuider() + "]]]]");
											if (g4Manager.getStringBuider().indexOf("$GMGL") > 0 || g4Manager.getStringBuider().indexOf("$GMGL") > 0) {
												g4Manager.handleMessageScreeen(g4Manager.getStringBuider());
											} else {
												g4Manager.processingData(g4Manager.getStringBuider());
											}
											if (g4Manager.getStringBuider().toString().contains("GPMS=0")) {
												//   Log.d("G4Chat1", " 2 value GPMS=0 :" + sb.toString());
												g4Manager.detectNewMessage(g4Manager.getStringBuider().toString());
											}
											if (g4Manager.isMarkAlertScreen()) {
												g4Manager.executeCommand(mGattClient);
											}
											if (g4Manager.getStringBuider().toString().contains("GPSO")) {
												Log.d("MarkAlert", databuilder.toString());
											}
											if (g4Manager.getStringBuider().toString().contains("ALERT")) {
												Log.d("StateAlert", String.valueOf(pastAlertState) + "equal" + String.valueOf(g4Manager.isAlertState()));
												if (pastAlertState != g4Manager.isAlertState()) {
													g4Manager.setAlertClicked(true);
													CheckAlertState();
													//InitSetting();
													g4Manager.resetCommandList();
													g4Manager.getGPSFix();
													g4Manager.executeCommand(mGattClient);
													g4Manager.setlastMarkedDate(new Date());
												}
											}
											g4Manager.setStringBuider(null);
											//   CheckAlertState();
										}
										if (isMarkspot == false) {
											if (g4Manager.getLongtiltude() != null && g4Manager.getAltiltude() != null) {

												if (g4Manager.isAlertState() == true) {
													alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
															", Lat =" + g4Manager.getAltiltude() +
															"\nAlert Sent " + getLastMarkedUtcTime());
												} else if (g4Manager.isAlertClicked() == true) {
													alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
															", Lat =" + g4Manager.getAltiltude() +
															"\nAlert Stopped  " + getLastMarkedUtcTime());
												} else {
													alertLocation.setText("");
												}
											} else {
												alertLocation.setText("Long = ?" + "\nLat = ?" + g4Manager.getAltiltude()+""  + getLastAlertOffUtcTime());
											}

										} else if (isMarkspot == true) {
											if (g4Manager.getLongtiltude() != null && g4Manager.getAltiltude() != null) {

												if (g4Manager.isAlertState() == true) {
													alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
															", Lat =" + g4Manager.getAltiltude() +
															"\nAlert Sent  " + getLastMarkedUtcTime());
												} else if (g4Manager.isAlertClicked() == true) {
													alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
															", Lat =" + g4Manager.getAltiltude() +
															"\nAlert Stopped " + getLastMarkedUtcTime());
												} else {
													alertLocation.setText("");
												}
												DebugLog.d("test location:"+"Long = " + g4Manager.getLongtiltude()
														+ ", Lat = " + g4Manager.getAltiltude()
														+ "\nMarked   "
														+ getLastMarkedUtcTime());
												currentAddress.setText("Long = " + g4Manager.getLongtiltude()
														+ ", Lat = " + g4Manager.getAltiltude()
														+ "\nMarked   "
														+ getLastMarkedUtcTime());
											} else {
												alertLocation.setText("");
												//	currentAddress.setText("");
											}
										}
									}
								});

							}*/
						}

						@Override
						public void onConnected(boolean success) {
							if (!success) {
								EventBus.getDefault().post(new Events.AutoConnect(true));

								//  EventBus.getDefault().post(new Events.handleDisConnect("Connect"));
								g4Manager.setConnectionState(false);
								try {
									mGattClient.onDestroy();
									//     mGattClient = new GattClient();
								}
								catch (Exception e) {
								}
								//g4Manager.setConnectionState(false);
								//application.deleteG4Manager(g4Manager);
								//g4Manager = new G4Manager(application);
								application.setG4Manager(g4Manager);
								EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
								//    EventBus.getDefault().post(new Events.ReceiverMessageG4(false));
								g4Manager.setConnectionState(false);
							}
							else {
								EventBus.getDefault().postSticky(new Events.BluetoothStatus(true));
								g4Manager.setConnectionState(true);
								EventBus.getDefault().postSticky(new Events.BluetoothStatus(true));
							}
						}

						@Override
						public void onRSSIChange(int rssi) {

						}
					});
				}
			}
			if (g4Manager.getMarkClient() != mGattClient) {
				g4Manager.setMarkClient(mGattClient);
			}
			// CheckAlertState();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d("G4Chat1", moduleLogId + "onResume:Alert state:start"+pastAlertState);
	}

	@Override
	public void onResume() {
		super.onResume();
		//InitSetting();
		Log.d("G4Chat1", moduleLogId + "onResume:Alert state:OK"+pastAlertState);
		EventBus.getDefault().register(this);
		try {
			checkMarkAlertMode();
			g4Manager = application.getG4Manager();
			//pastAlertState = g4Manager.isAlertState();
			g4Manager.setlastMarkedDate(new Date());
			Log.d("G4Chat1", moduleLogId + "onResume:Alert state:"+pastAlertState);
			CheckAlertState();
			if (g4Manager.getLongtiltude() != null && g4Manager.getAltiltude() != null) {
					g4Manager.setlastMarkedDate(new Date());
					if (g4Manager.isAlertState() == true) {
						alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
								", Lat =" + g4Manager.getAltiltude() +
								"\nAlert Sent  " + getLastMarkedUtcTime());
					} else if (g4Manager.isAlertClicked() == true) {
						alertLocation.setText("Long = " + g4Manager.getLongtiltude() + ", " +
								"Lat =" + g4Manager.getAltiltude() +
								"\nAlert Stopped  " + getLastMarkedUtcTime());
					} else {
						alertLocation.setText("");
					}
					if (g4Manager.isMarkClicked()) {
						DebugLog.d("test location:" + "Long = " + g4Manager.getLongtiltude()
								+ ", Lat = " + g4Manager.getAltiltude()
								+ "\nMarked   "
								+ getLastMarkedUtcTime());
						currentAddress.setText("Long = " + g4Manager.getLongtiltude()
								+ ", Lat = " + g4Manager.getAltiltude()
								+ "\nMarked   "
								+ getLastMarkedUtcTime());
					}
			}
			else {
				currentAddress.setText("");
				g4Manager.setlastMarkedDate(new Date());
				alertLocation.setText(/*"Long = ?" + "\nLat = ?" + g4Manager.getAltiltude()+*/""  /*+ getLastAlertOffUtcTime()*/);
			}


		}
		catch (Exception e) {
		}
	}

	void startRepeatingTask() {
		//ReportNormal.run();
		longRunningTaskFuture1= threadPoolExecutor.submit(ReportNormal);
		longRunningTaskFuture4= threadPoolExecutor.submit(ReportAlert);
// add method here
	}
	void stopRepeatingTask() {

		handlerReportNormal.removeCallbacks(ReportNormal);
		handlerReportAlert.removeCallbacks(ReportAlert);
	}

	public void CheckAlertState() {
        DebugLog.d("check alert:");
		try {
			btnAlertOn = (Button) v.findViewById(R.id.alerton);
			btnAlertOff = (Button) v.findViewById(R.id.alertoff);
			if (g4Manager != null) {
				//check alert State
				if (g4Manager.isAlertState()) {
					Log.d("G4Chat1", moduleLogId + "CheckAlertState:Alert On");
					btnAlertOn.setBackgroundResource(R.drawable.bolder_alerton2);
					btnAlertOn.setText("Alerting On");
					btnAlertOn.setTextColor(getResources().getColor(R.color.white));

					btnAlertOff.setBackgroundResource(R.drawable.bolder_alertoff2);
					btnAlertOff.setText("Alerting Off");
					btnAlertOff.setTextColor(getResources().getColor(R.color.red));
					isActiveReport = false;
				}
				else {
					Log.d("G4Chat1", moduleLogId + "CheckAlertState:Alert Off");
					btnAlertOn.setBackgroundResource(R.drawable.bolder_alerton);
					btnAlertOn.setText("Alerting On");
					btnAlertOn.setTextColor(getResources().getColor(R.color.green));

					btnAlertOff.setBackgroundResource(R.drawable.bolder_alertoff);
					btnAlertOff.setText("Alerting Off");
					btnAlertOff.setTextColor(getResources().getColor(R.color.white));
					isActiveReport = true;
				}
			}
		}
		catch (Exception e) {
		}
	}

	public void CheckAlertState1(boolean active) {
		try {
			if (g4Manager != null) {
				//check alert State
				if (g4Manager.isAlertState() == true && active) {
					startRepeatingTask();
				}
				else {
					stopRepeatingTask();
				}
			}
		}
		catch (Exception e) {
		}
	}

	public void onEventMainThread(Events.checkAlert event) {
		Log.d("G4Chat1", moduleLogId + "Events.checkAlert");
		InitSetting();
		CheckAlertState();
		if (g4Manager != null) {
			if (g4Manager.isAlertState() == true) {
				Log.d("G4Chat1", moduleLogId + "onEventMainThread");
				g4Manager.resetCommandList();
				//g4Manager.setAlertOn();
				g4Manager.getGPSFix();
				g4Manager.setAlertState(true);
				g4Manager.setAlertClicked(true);
				g4Manager.executeCommand(mGattClient);
			}
		}
		CheckAlertState();
	}

	public void onEventMainThread(Events.InitSetting event) {
	}

	@Override
	public void onStop() {
		super.onStop();
		try {
			Log.d("G4Chat1", moduleLogId + "On Stop");
			handlerRefreshAlert.removeCallbacks(RefreshMarkAlert);
			stopRepeatingTask();
			g4Manager.setMarkAlertScreen(false);
		}
		catch (Exception e) {
		}
	}

	public void checkMarkAlertMode() {
		try {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
			String mode = sharedPref.getString("mark_alert_mode", "");
			switch (mode) {
				case "Combined":
					MarkSpot.setVisibility(View.VISIBLE);
					currentAddress.setVisibility(View.VISIBLE);
					btnAlertOn.setVisibility(View.VISIBLE);
					btnAlertOff.setVisibility(View.VISIBLE);
					alertLocation.setVisibility(View.VISIBLE);
					break;
				case "Alert Only":
					MarkSpot.setVisibility(View.GONE);
					currentAddress.setVisibility(View.GONE);
					btnAlertOn.setVisibility(View.VISIBLE);
					btnAlertOff.setVisibility(View.VISIBLE);
					alertLocation.setVisibility(View.VISIBLE);
					break;
				case "Mark Only":
					MarkSpot.setVisibility(View.VISIBLE);
					currentAddress.setVisibility(View.VISIBLE);
					btnAlertOn.setVisibility(View.GONE);
					btnAlertOff.setVisibility(View.GONE);
					alertLocation.setVisibility(View.GONE);
					break;
			}
		}
		catch (Exception e) {
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		EventBus.getDefault().unregister(this);
		Log.d("G4Chat1", moduleLogId+"OnPause");
		handlerRefreshAlert.removeCallbacks(RefreshMarkAlert);
		stopRepeatingTask();

	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		isShowing=isVisibleToUser;
		DebugLog.d("isShowing:"+isShowing);
		try {
			g4Manager = application.getG4Manager();
		}
		catch (Exception e) {
		}

		if (!isVisibleToUser) {
			Log.d("G4Chat1", moduleLogId+"setUserVisibleHint:False");
//		handler.removeCallbacks(MarkAlertScreen);
//		handlerRefreshAlert.removeCallbacks(RefreshMarkAlert);
//		stopRepeatingTask();
			try {
				g4Manager.setMarkAlertScreen(false);
				new Thread(new Runnable() {
					@Override
					public void run() {


						if(longRunningTaskFuture1!=null) {
							longRunningTaskFuture1.cancel(true);
						}
						if(longRunningTaskFuture2!=null) {
							longRunningTaskFuture2.cancel(true);
						}
						if(longRunningTaskFuture3!=null) {
							longRunningTaskFuture3.cancel(true);
						}
						if(longRunningTaskFuture4!=null) {
							longRunningTaskFuture4.cancel(true);
						}




					}
				}).start();

			}
			catch (Exception e) {
			}
			/*if (g4Manager!=null) {
				DebugLog.d("g4manager:"+g4Manager.isAlertState()+":"+g4Manager.toString());
				if (g4Manager.isAlertState()) {
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
				} else {
					g4Manager.setlastMarkedDate(new Date());
					g4Manager.resetCommandList();
					pastAlertState = g4Manager.isAlertState();
					g4Manager.setAlertState(false);
					Log.d("StateAlert", "Click Alert Off" + String.valueOf(pastAlertState));
					g4Manager.resetCommandList();
					g4Manager.setAlertOff();
					g4Manager.getGPSFix();
					//g4Manager.setAlertState(true);

					g4Manager.executeCommand(mGattClient);
					stopRepeatingTask();
				}
			}
			CheckAlertState();*/
		}
		else {
			try {
				if (g4Manager.isConnectionState()) {
					//  EventBus.getDefault().post(new Events.stopStatusSignal());
					new Thread(new Runnable() {
						@Override
						public void run() {
							DebugLog.d("check run:");
							startRepeatingTask();
							longRunningTaskFuture2 =threadPoolExecutor.submit(RefreshMarkAlert);
							longRunningTaskFuture3 = threadPoolExecutor.submit(MarkAlertScreen);

						}
					}).start();

					try {
						g4Manager.setlastMarkedDate(new Date());
						g4Manager.setMarkAlertScreen(true);
					}
					catch (Exception e) {
					}
				}
				//g4Manager.setMarkAlertScreen(true);
			}
			catch (Exception e) {
			}
			/*new Thread(new Runnable() {
				@Override
				public void run() {
					if (g4Manager!=null) {
						DebugLog.d("g4manager:"+g4Manager.isAlertState()+":"+g4Manager.toString());
						if (g4Manager.isAlertState()) {
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
						} else {
							g4Manager.setlastMarkedDate(new Date());
							g4Manager.resetCommandList();
							pastAlertState = g4Manager.isAlertState();
							g4Manager.setAlertState(false);
							Log.d("StateAlert", "Click Alert Off" + String.valueOf(pastAlertState));
							g4Manager.resetCommandList();
							g4Manager.setAlertOff();
							g4Manager.getGPSFix();
							//g4Manager.setAlertState(true);

							g4Manager.executeCommand(mGattClient);
							stopRepeatingTask();
						}
					}
					CheckAlertState();
				}
			}).start();*/

		}
	}

	public void onEventMainThread(Events.StopMarkAlerFragment event) {
		stopRepeatingTask();
		handlerRefreshAlert.removeCallbacks(RefreshMarkAlert);
	}
	public void onEventMainThread(Events.UpdateAlertState event) {
		DebugLog.d("send command:");
		if (g4Manager==null)
			return;

		if (g4Manager!=null) {
			/*if (isConnect) {
				CheckAlertState();
			}*/
			if (g4Manager.isPastAlertState()) {
				g4Manager.getClient().writeInteractor("AT$FFALERT=1",false);
				DebugLog.d("check mark: "+g4Manager.isPastAlertState());
				/*new CountDownTimer(300, 300) {
					@Override
					public void onTick(long millisUntilFinished) {

					}

					@Override
					public void onFinish() {
						g4Manager.getClient().writeInteractor("AT$FFALERT=1",false);
						DebugLog.d("check mark: "+g4Manager.isPastAlertState());
					}
				}.start();*/

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

		//.e("data:"+event.getData());
        /*if (g4Manager.isConnectionState()){
            if (g4Manager.isAlertState()){
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
            }
        }
        CheckAlertState();*/
	}

	@Override
	public void binData(String result) {

		g4Manager=application.getG4Manager();
		if (result.contains("OK")) {
			//got message with OK from G4 - monitor alert messages from G4
			/*if (result.indexOf("$GMGL") > 0 || result.indexOf("$GMGL") > 0) {
				g4Manager.handleMessageScreeen(new StringBuilder(result));
			} else {
				g4Manager.processingData(new StringBuilder(result));
			}
			if (result.contains("GPMS=0")) {
				//   Log.d("G4Chat1", " 2 value GPMS=0 :" + sb.toString());
				g4Manager.detectNewMessage(result);
			}
			if (g4Manager.isMarkAlertScreen()) {
				g4Manager.executeCommand(mGattClient);
			}
			if (result.toString().contains("GPSO")) {
				Log.d("MarkAlert", result.toString());
			}*/
			if (result.contains("ALERT")) {
				DebugLog.d("check alert: "+result);
                /*if (result.contains("$FFALERT=0")){
                    g4Manager.setAlertState(false);
                }else if (result.contains("$FFALERT=1")){
                    g4Manager.setAlertState(true);
                }
                CheckAlertState();
				g4Manager.resetCommandList();
				g4Manager.getGPSFix();
				g4Manager.executeCommand(mGattClient);
				g4Manager.setlastMarkedDate(new Date());*/

				if (pastAlertState != g4Manager.isAlertState()) {
					DebugLog.d("check alert: "+result);
					g4Manager.setAlertClicked(true);
					pastAlertState =g4Manager.isAlertState();
					CheckAlertState();
					//InitSetting();
					g4Manager.resetCommandList();
					g4Manager.getGPSFix();
					g4Manager.executeCommand(mGattClient);
					g4Manager.setlastMarkedDate(new Date());
					//setTextAlert();
				}
			}
			//g4Manager.setStringBuider(null);
			//   CheckAlertState();
		}

		if (isMarkspot == false) {
			if (g4Manager.getLongtiltude() != null && g4Manager.getAltiltude() != null) {
					if (g4Manager.isAlertState() == true) {
						alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
								", Lat =" + g4Manager.getAltiltude() +
								"\nAlert Sent " + getLastMarkedUtcTime());
					} else if (g4Manager.isAlertClicked() == true) {
						alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
								", Lat =" + g4Manager.getAltiltude() +
								"\nAlert Stopped  " + getLastMarkedUtcTime());
					} else {
						alertLocation.setText("");
					}
			} else {
				DebugLog.d("MarkAlet:"+result);
				//alertLocation.setText("Long = ?" + "\nLat = ?" + g4Manager.getAltiltude()+""  + getLastAlertOffUtcTime());
			}

		} else if (isMarkspot == true) {
			if (g4Manager.getLongtiltude() != null && g4Manager.getAltiltude() != null) {
				if (g4Manager.isAlertState() == true) {
					alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
							", Lat =" + g4Manager.getAltiltude() +
							"\nAlert Sent  " + getLastMarkedUtcTime());
				} else if (g4Manager.isAlertClicked() == true) {
					alertLocation.setText("Long = " + g4Manager.getLongtiltude() +
							", Lat =" + g4Manager.getAltiltude() +
							"\nAlert Stopped " + getLastMarkedUtcTime());
				} else {
					alertLocation.setText("");
				}
				DebugLog.d("test location:"+"Long = " + g4Manager.getLongtiltude()
						+ ", Lat = " + g4Manager.getAltiltude()
						+ "\nMarked   "
						+ getLastMarkedUtcTime());
				currentAddress.setText("Long = " + g4Manager.getLongtiltude()
						+ ", Lat = " + g4Manager.getAltiltude()
						+ "\nMarked   "
						+ getLastMarkedUtcTime());
			} else {
				alertLocation.setText("");
				currentAddress.setText("");
			}
		}
	}

////
    @Override
	public void binConnectionState(boolean isConnect) {

		if (g4Manager!=null) {
			/*if (isConnect) {
				CheckAlertState();
			}*/
			if (g4Manager.isPastAlertState()) {
				new CountDownTimer(100, 100) {
					@Override
					public void onTick(long millisUntilFinished) {

					}

					@Override
					public void onFinish() {
						g4Manager.getClient().writeInteractor("AT$FFALERT=1",false);
						DebugLog.d("check mark: "+g4Manager.isPastAlertState());
					}
				}.start();

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
		if (g4Manager==null)
			return;
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
			//g4Manager.setAlertOn();
			g4Manager.getGPSFix();
			g4Manager.setAlertState(true);
			g4Manager.setAlertClicked(true);
			//  g4Manager = application.getG4Manager();
			g4Manager.executeCommand(mGattClient);
		}
		//CheckAlertState();
	}

	@Override
	public void binRssi(int rssi) {
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		//deleteCache(getActivity());
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
