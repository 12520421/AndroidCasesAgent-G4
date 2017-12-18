package com.xzfg.app.fragments;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;

import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.DataCommandListener;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.activities.AgentActivity;
import com.xzfg.app.debug.DebugLog;
import com.xzfg.app.fragments.chat.IntentActionDialogFragment;
import com.xzfg.app.fragments.setup.SendATCommandFragment;
import com.xzfg.app.managers.BluetoothManager;
import com.xzfg.app.managers.G4Manager;
import com.xzfg.app.model.AgentSettings;

import com.xzfg.app.model.ListATCommand;
import com.xzfg.app.services.GattClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import static android.content.Context.BLUETOOTH_SERVICE;
import static com.xzfg.app.fragments.StatusFragment.SERVICE_UUID;
import static java.lang.System.in;

public class ATCommandsFragment extends Fragment {
final ATCommandsFragment context = this;
@Inject
InputMethodManager inputMethodManager;
IntentActionDialogFragment intentActionDialogFragment;
RecyclerView recyclerView;
List<ListATCommand> commandList;
@Inject
Application application;
@Inject
G4Manager g4Manager;
	GattClient gattClient=new GattClient();

private ImageButton btn_edit;
private EditText atcommand;
private RecyclerView.Adapter mAdapter;
private GattClient mGattClient;
private G4Manager bleManager;
private RecyclerView.LayoutManager mLayoutManager;
private Context mContext;
private boolean checkAll = true;
private String content;
private Button enter;
private Button all;
private ImageButton send;
private ImageButton delete;
private File g4log;
StringBuilder databuilder = new StringBuilder(200);
	String address = null;
	ReconnectHandler reconnectHandler;
private GattClient.OnCounterReadListener gatListener = new GattClient.OnCounterReadListener() {
	@Override
	public void onCounterRead(final String value) {
		//Log.d("check send data:","check send data");
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (databuilder.indexOf(value) < 0) {
					databuilder.append(value);
				}
				if(!value.contains("OK")) {
					//g4Manager.addCommand2List(value);
				}
				//find OK to get data
				if (databuilder.toString().contains("OK")) {
					if(databuilder.toString().contains("GMGL")){
						//  DebugLog.d("data liem: "+sb.toString());
						Log.d("liem2",databuilder.toString());
					}
					g4Manager.processingData(databuilder);
					g4Manager.executeCommand(mGattClient);
					databuilder.delete(0, databuilder.length());
					if (checkAll == true) {
						UpdateList();
					}
					else {
						UpdateListEnter();
					}
				}
				else {
				}
									/*if (databuilder.toString().contains("ERROR")) {
										g4Manager.processingData(databuilder);
										g4Manager.executeCommand(mGattClient);
										databuilder.delete(0, databuilder.length());
										if (checkAll == true) {
											UpdateList();
										}
										else {
											UpdateListEnter();
										}
									}
									else {
									}*/
			}
		});
	}

	@Override
	public void onConnected(boolean success) {
		if (!success) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					///
					/*Toast toast = Toast.makeText(getActivity(), "Disconnected", Toast.LENGTH_SHORT);
					if(toast.getView().isShown()){
						toast.cancel();
					}else {
						toast.show();
					}*/
				}
			});
			EventBus.getDefault().post(new Events.sendEvent());
			g4Manager.setConnectionState(false);
			EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
		}
		else {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					EventBus.getDefault().post(new Events.BluetoothStatus(true));
					///
					/*Toast toast = Toast.makeText(getActivity(), "Connection Successfully", Toast.LENGTH_SHORT);
					if(toast.getView().isShown()){
						toast.cancel();
					}else {
						toast.show();
					}*/
					g4Manager.setConnectionState(true);

					if (reconnectHandler != null) {
						reconnectHandler.stopReconnect();
					}
				}
			});


		}
	}

	@Override
	public void onRSSIChange(int rssi) {
	}
};
String temp;
@Override
public void onAttach(Activity activity) {
	super.onAttach(activity);
	EventBus.getDefault().post(new Events.DisplayChanged("AT Command", R.id.atcommands));
	((Application) activity.getApplication()).inject(this);
}




	public void InitView()
	{
		btn_edit = (ImageButton) v.findViewById(R.id.edit_button);
		enter = (Button) v.findViewById(R.id.btn_at_enter);
		delete = (ImageButton) v.findViewById(R.id.delete_button);
		all = (Button) v.findViewById(R.id.btn_at_all);
		atcommand = (EditText) v.findViewById(R.id.commands);
		atcommand.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

		//check log file
		g4log = new File(Environment.getExternalStorageDirectory(), "g4log.txt");
		if (!g4log.exists()) {
			try {
				g4log.createNewFile();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		send = (ImageButton) v.findViewById(R.id.send_at_button);
		mContext = v.getContext();
		recyclerView = (RecyclerView) v.findViewById(R.id.atcommand_list);


/*	atcommand.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				SendCommand();

				if (checkAll == true) {
					UpdateList();
				}
				else {
					UpdateListEnter();
				}

				atcommand.setText("");
				return true;
			}
			else {
				recyclerView.scrollToPosition(commandList.size() - 1);
			}
			return true;
		}
	});*/

		recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
			@Override

			public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
				try {
					recyclerView.scrollToPosition(commandList.size() - 1);
				}
				catch (Exception e) {
				}
			}
		});
		///fix display all data
		send.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				if (application.getG4Manager() == null || application.getG4Manager().isConnectionState() == false) {
					///
					//Toast.makeText(getActivity(), "Please connect to G4", Toast.LENGTH_LONG).show();
				}
				else {
					g4Manager.executeImmediateATCommandScreen(atcommand.getText().toString());
					//SendCommand();



					if (checkAll == true) {
						UpdateList();

					}
					else {
						UpdateListEnter();

					}
					atcommand.setText("");
				}
			}
		});
		btn_edit.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				try {
					String fileLocation = "g4log.txt";
					File filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), fileLocation);
					FileWriter writer = new FileWriter(filelocation, false);
					writer.write("");
					writer = new FileWriter(filelocation, true);
					for (int i = 0; i < commandList.size(); i++) {
						writer.write(commandList.get(i).getData());
						writer.write("\n");
					}
					writer.flush();
					writer.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				SendMessageUsingIntent();
			}
		});
		delete.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case DialogInterface.BUTTON_POSITIVE:
								commandList.clear();
								g4Manager.deleteAllList();
								recyclerView.scrollToPosition(1);
								//Yes button clicked
								break;

							case DialogInterface.BUTTON_NEGATIVE:
								//No button clicked
								break;
						}
					}
				};
				AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.myDialog));
				builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
						.setNegativeButton("No", dialogClickListener).show();

			}
		});
		enter.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				all.setBackgroundResource(R.drawable.all_button2);
				all.setText("All");
				all.setTextColor(getResources().getColor(R.color.textgrey));
				all.setTextSize(10);
				enter.setBackgroundResource(R.drawable.entered_button_2);
				enter.setText("Entered");
				enter.setTextColor(getResources().getColor(R.color.white));
				enter.setTextSize(10);
				try {
					UpdateListEnter();
				}
				catch (Exception e) {
				}
				checkAll = false;

			}
		});
		all.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				all.setBackgroundResource(R.drawable.all_button);
				all.setText("All");
				all.setTextColor(getResources().getColor(R.color.white));
				all.setTextSize(10);
				enter.setBackgroundResource(R.drawable.entered_button);
				enter.setText("Entered");
				enter.setTextColor(getResources().getColor(R.color.textgrey));
				enter.setTextSize(10);
				try {
					UpdateList();
				}
				catch (Exception e) {
				}
				checkAll = true;
			}
		});
		LinearLayoutManager llm = new LinearLayoutManager(getActivity());
		llm.setStackFromEnd(true);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		try{
			super.onConfigurationChanged(newConfig);
			frameLayout. removeAllViews();
			LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
				v = inflater.inflate(R.layout.fragment_atcommands, null);
			}
			else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
				v = inflater.inflate(R.layout.fragment_atcommands, null);
			}
			InitView();
			frameLayout .addView(v);
			if (checkAll == true) {
				UpdateList();
			}
			else {
				UpdateListEnter();
			}
		}
		catch (Exception e)
		{
			e.toString();
		}


	}
	FrameLayout frameLayout;
	View v;
@Nullable
public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
	frameLayout = new FrameLayout(getActivity());
	v = inflater.inflate(R.layout.fragment_atcommands, container, false);
	InitView();
	InitSetting();
	frameLayout.addView(v);

	return frameLayout;
}
	public void onEventMainThread(Events.SendDataCommand event) {

		/*if (event.getCommand().contains(g4Manager.getCommandListAll().get(g4Manager.getCommandListAll().size()-1))&&event.getCommand()
				.contains("OK")){
			g4Manager.addCommand2List(event.getCommand());
		}*/
	}

@Override
public void onResume() {
	super.onResume();

	try {
		EventBus.getDefault().register(this);


		g4Manager.setATCommandScreen(true);
		g4Manager.setFromATScreen(true);
		g4Manager.setFromConfigScreen(false);
		g4Manager.setDoneListReceiver(false);
	}
	catch (Exception e) {
	}
}

@Override
public void onStop() {
	try {
		super.onStop();
		g4Manager.setATCommandScreen(false);
		if(reconnectHandler!=null){
			reconnectHandler.stopReconnect();
		}
	}
	catch (Exception e) {
	}

}
//receive data,,,,,,,,,,,,,,,,,,,,,,,,
public void InitSetting() {

	//AgentSettings settings = application.getAgentSettings();
	try {
		if (g4Manager != null) {
			g4Manager = application.getG4Manager();


			if (g4Manager.isConnectionState()) {
				if (g4Manager.getClient().getListCommandAll() != null) {
					mGattClient = g4Manager.getClient();
					mGattClient.setContext(getActivity());
					mGattClient.setListener(gatListener);
				}
			}
			if (checkAll == true) {
				UpdateList();
			}
			else {
				UpdateListEnter();
			}
		}
	}
	catch (Exception e) {
	}
}

@Override
public void onPause() {
	EventBus.getDefault().unregister(this);
	if (intentActionDialogFragment != null) {
		intentActionDialogFragment.dismissAllowingStateLoss();
		intentActionDialogFragment = null;
	}
	super.onPause();
}

@Override
public void onActivityCreated(Bundle savedInstanceState) {
	super.onActivityCreated(savedInstanceState);
	getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
}

@SuppressWarnings("unused")
public void onEventMainThread(Events.KeyBoardShown event) {
	try{
		View v = getView();
		if (v != null) {
			if (event.isKeyboardShown()) {
				v.findViewById(R.id.button_bar_at_command).setVisibility(View.VISIBLE);
			}
			else {
				v.findViewById(R.id.button_bar_at_command).setVisibility(View.GONE);
			}
		}
	}
	catch (Exception e)
	{

	}

}
	private void UpdateList() {
		if(g4Manager.getClient().getListCommandAll()!=null) {
			g4Manager.getCommandListAll().addAll(g4Manager.getClient().getListCommandAll());
			g4Manager.getClient().getListCommandAll().clear();
		}
        if( g4Manager.getCommandListAll().size()>1000)
        {
			//Log.d("Size","size :"+g4Manager.getCommandListAll().size());
            for(int i=0;i<=g4Manager.getCommandListAll().size()/2;i++){
                g4Manager.getCommandListAll().remove(0);
            }
        }
        Log.d("Size","size :" + g4Manager.getCommandListAll().size());
		commandList = new ArrayList(g4Manager.getCommandListAll());
		mLayoutManager = new GridLayoutManager(mContext, 1);
		recyclerView.setLayoutManager(mLayoutManager);
		mAdapter = new AnimalsAdapter(mContext, commandList);
		recyclerView.setAdapter(mAdapter);
		recyclerView.scrollToPosition(commandList.size() - 1);
		mAdapter.notifyDataSetChanged();
	}

private void UpdateListEnter() {

	if(g4Manager.getClient().getListCommandEnter()!=null) {
		g4Manager.getCommandListEnter().addAll(g4Manager.getClient().getListCommandEnter());
		g4Manager.getClient().getListCommandEnter().clear();
	}
    if( g4Manager.getCommandListEnter().size()>1000)
    {
		//Log.d("Size","size :"+g4Manager.getCommandListEnter().size());
        for(int i=0;i<= g4Manager.getCommandListEnter().size()/2;i++){
            g4Manager.getCommandListEnter().remove(0);
        }
    }
	Log.d("Size","size :" + g4Manager.getCommandListEnter().size());
	commandList = new ArrayList(g4Manager.getCommandListEnter());
	mLayoutManager = new GridLayoutManager(mContext, 1);
	recyclerView.setLayoutManager(mLayoutManager);
	mAdapter = new AnimalsAdapter(mContext, commandList);
	recyclerView.setAdapter(mAdapter);
	recyclerView.scrollToPosition(commandList.size() - 1);
	mAdapter.notifyDataSetChanged();
}

public void SendMessageUsingIntent() {
	String fileLocation = "g4log.txt";
	File filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), fileLocation);

	Intent intent = new Intent(Intent.ACTION_SEND);
	intent.setType("text/html");
	intent.putExtra(Intent.EXTRA_EMAIL, "");
	intent.putExtra(Intent.EXTRA_SUBJECT, "");
	intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + filelocation));
	intent.putExtra(Intent.EXTRA_TEXT, "");

	startActivity(Intent.createChooser(intent, "Send Email"));
}

//public void SendCommand() {
//	InitSetting();
//	String command = atcommand.getText().toString();
//	if (command.contains("AT$GCSET=USER_CFG.ALERT_TX_TIME,60")) {
//		StringBuilder str = new StringBuilder(command);
//		str.insert(32, " ");
//		command = str.toString();
//	}
//	if (command == null) {
//		return;
//	}
//	g4Manager.addCommandSentEnter(command);
//	g4Manager.resetCommandList();
//	g4Manager.addCommand(command);
//	g4Manager.executeCommand(mGattClient);
//
//}

public class AnimalsAdapter extends RecyclerView.Adapter<AnimalsAdapter.ViewHolder> {
	private List<ListATCommand> mDataSet;
	private Context mContext;
	//private Random mRandom = new Random();

	public AnimalsAdapter(Context context, List<ListATCommand> list) {
		mDataSet = list;
		mContext = context;
	}

	public void update(List<ListATCommand> list) {
		mDataSet.clear();
		mDataSet.addAll(list);
	}


	@Override
	public AnimalsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		// Create a new View
		View v = LayoutInflater.from(mContext).inflate(R.layout.receive_atcommand, parent, false);
		ViewHolder vh = new ViewHolder(v);
		return vh;
	}

	@Override
	public long getItemId(int position) {
		return super.getItemId(position);
	}

	@Override
	public int getItemViewType(int position) {
		return super.getItemViewType(position);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, final int position) {
		try {
			holder.setIsRecyclable(false);
			if (mDataSet.get(position).getData() != null) {
				holder.mTextView.setText(mDataSet.get(position).getData().trim());
			}
			if (mDataSet.get(position).isType()) {
				Log.d("ATCommand", mDataSet.get(position).getData());
				holder.mAtFrame.setBackgroundColor(Color.parseColor("#FFFF00"));
			}

			// Emboss the TextView text
			applyEmbossMaskFilter(holder.mTextView);

			// Set a click listener for TextView
			holder.mTextView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
				}
			});
		}catch (Exception e){}
	}

	@Override
	public int getItemCount() {
		return mDataSet.size();
	}

	// Custom method to apply emboss mask filter to TextView
	protected void applyEmbossMaskFilter(TextView tv) {
		EmbossMaskFilter embossFilter = new EmbossMaskFilter(
				new float[]{1f, 5f, 1f}, // direction of the light source
				0.8f, // ambient light between 0 to 1
				8, // specular highlights
				7f // blur before applying lighting
		);
		tv.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		tv.getPaint().setMaskFilter(embossFilter);
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		public TextView mTextView;
		public CardView mAtFrame;
		public RelativeLayout mRelativeLayout;

		public ViewHolder(View v) {
			super(v);
			mTextView = (TextView) v.findViewById(R.id.messageAT);
			mAtFrame = (CardView) v.findViewById(R.id.atFrame);
		}
	}
}
	@SuppressWarnings("unused")
	public void onEventMainThread(Events.handleDisConnect event) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
//				Toast toast = Toast.makeText(getActivity(), "Disconnected", Toast.LENGTH_SHORT);
//				if(toast.getView().isShown()){
//					toast.cancel();
//				}else {
//					toast.show();
//				}
//				if(toast.getView().isShown()){
//					toast.cancel();
//				}else {
//					toast.show();
//				}
				if(reconnectHandler!=null) {
					if (reconnectHandler.isAlive()) {
						reconnectHandler.stopReconnect();
					}
				}
				reconnectHandler = new ReconnectHandler();
			}
		});
		// EventBus.getDefault().removeStickyEvent(event);

	}

	private BluetoothLeScannerCompat mScanner = BluetoothLeScannerCompat.getScanner();
	private void startReconnect() {
		try {
			SharedPreferences prefs = getActivity().getSharedPreferences("mac_address", 0);
			if (prefs.getString("value", "") != null) {
				address = prefs.getString("value", "");
			}
		} catch (Exception e) {
		}
		if (address != null) {
            mGattClient.onCreate(getActivity(),address,gatListener);

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
				mGattClient.onCreate(getActivity(), address, gatListener);
			}
		}

		@Override
		public void onScanFailed(int errorCode) {
			Log.w("Config", "Scan failed: " + errorCode);
			mScanner.stopScan(mScanCallback);
		}
	};
	//check callsupper.run()???
	public class ReconnectHandler extends HandlerThread {
		public final Handler mReconnectHandler;
		public final Runnable mReconnectAuto;
		public ReconnectHandler() {
			super(StatusFragment.ReconnectHandler.class.getName(), 1);
			start();
			mReconnectHandler = new Handler(getLooper());
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
				try {
					if (g4Manager.isConnectionState() == false) {
						try {
							EventBus.getDefault().postSticky(new Events.BluetoothStatus(false));
							///
							/*Toast toast = Toast.makeText(getActivity(), "Trying to reconnect to G4", Toast.LENGTH_SHORT);
							if(toast.getView().isShown()){
								toast.cancel();
							}else {
								toast.show();
							}*/
							Log.d("failed", "Enter Reconnect1");
							startReconnect();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						mScanner.stopScan(mScanCallback);
					}
				} finally {
					mReconnectHandler.postDelayed(mReconnectAuto, 40000);
				}
			}
		};
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		deleteCache(getActivity());
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






