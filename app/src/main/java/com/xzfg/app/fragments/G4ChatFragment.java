package com.xzfg.app.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.xzfg.app.Application;
import com.xzfg.app.BaseFragment;
import com.xzfg.app.DebugLog;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.managers.G4Manager;
import com.xzfg.app.model.G4Messages;
import com.xzfg.app.model.Message;
import com.xzfg.app.services.GattClient;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import de.greenrobot.event.EventBus;
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import io.github.rockerhieu.emojicon.EmojiconTextView;

public class G4ChatFragment extends BaseFragment {

private static final String moduleLogId = "@f3-ChatFrag:";
Handler handler = new Handler();
View view;
boolean checkSend = true;
Parcelable listState;
TextView txtloading;
@Inject
Application application;
@Inject
G4Manager g4Manager;
List<String> cannedMsg;
private GattClient mGattClient = new GattClient();
private EditText input;
private TextView title;
private ImageButton sendButton;
private ImageButton EditButton;
private ProgressBar chatLoading;
private LinearLayoutManager llm;
private RecyclerView recyclerView;
private G4ChatFragment.MessagesAdapter messagesAdapter;
private MessagesCannedAdaper messagesCannedAdaper;
private int mess_time_out;
private Button clearbutton;
private boolean cannedPressed = false;
private boolean isRefreshRun = false;
private boolean firstRun = false;
private SwipeRefreshLayout swipe;
private ArrayList<G4Messages> listG4Message = new ArrayList<>();
private RefreshHandlerThread refreshHandlerThread;
private boolean isKeyboardShow = false;
	StringBuilder sbChat=new StringBuilder(192);
private int curPosition = 0;
	Handler checkLoadding;
	String temp="";
	boolean state;
public G4ChatFragment() {
	setArguments(new Bundle());
}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		try{

			super.onConfigurationChanged(newConfig);

			if(recyclerView.getAdapter().getItemCount() == 0)
			{
				return;
			}
			else{

				frameLayout. removeAllViews();
				LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
					view = inflater.inflate(R.layout.fragment_chat, null);
				}
				else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
					view = inflater.inflate(R.layout.fragment_chat, null);
				}
				InitView();
				txtloading.setVisibility(View.VISIBLE);
				if(recyclerView.getAdapter().getItemCount() > 1)
				{
					txtloading.setVisibility(View.GONE);
				}

				frameLayout .addView(view);

				recyclerView.scrollToPosition(curPosition);
			}
		}
		catch (Exception e)
		{

		}
	}


	FrameLayout frameLayout;

	public void InitView()
	{
		clearbutton = (Button) view.findViewById((R.id.clear_button));
		input = (EditText) view.findViewById(R.id.message_input);
		swipe = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
		//chatLoading = (ProgressBar) view.findViewById(R.id.chat_loading) ;
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		sendButton = (ImageButton) view.findViewById(R.id.send_button);
		EditButton = (ImageButton) view.findViewById(R.id.clear_messages_button);
		txtloading = (TextView) view.findViewById(R.id.chat_title);
		recyclerView = (RecyclerView) view.findViewById(R.id.chat_list);
		llm = new LinearLayoutManager(getActivity());
		// llm.setStackFromEnd(true);
		messagesAdapter = new MessagesAdapter(listG4Message, getActivity());
		messagesAdapter.setHasStableIds(false);
		recyclerView.setLayoutManager(llm);
		recyclerView.setAdapter(messagesAdapter);
		clearbutton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				input.setText("");
			}
		});
		EditButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				EventBus.getDefault().post(new Events.RefreshChat());
			}
		});

		swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				if (g4Manager != null) {
					swipe.setRefreshing(false);
				}
				else {
					swipe.setRefreshing(false);
				}
			}
		});

		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!input.getText().toString().isEmpty()) {
					if (application.getG4Manager() == null || application.getG4Manager().isConnectionState() == false) {
						if (getActivity()!=null) {
							Toast.makeText(getActivity(), "Please connect to G4", Toast.LENGTH_LONG).show();
						}
					}
					else {
						String message = input.getText().toString();
						message = StringEscapeUtils.escapeJava(message);

						Date now = new Date();
						final Calendar calendar = Calendar.getInstance();
						calendar.setTime(now);
						input.setText("");

						G4Messages g4Message = new G4Messages();
						g4Message.setBody(message);
						g4Message.setID(g4Manager.getLastIdSent() + 1);
						g4Message.setStatus("UNSENT");
						g4Message.setDate(calendar.getTime());

						listG4Message.add(g4Message);
						messagesAdapter.notifyDataSetChanged();

						recyclerView.scrollToPosition(listG4Message.size() - 1);
						g4Manager.executeMessenger(g4Manager.getClient(), 0, false, message);
					}
				}
			}
		});
		input.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DebugLog.d("click:");
				cannedPressed=true;
				EventBus.getDefault().post(new Events.setCannedText());
			}
		});

	}
	@Nullable
@Override
public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

	frameLayout = new FrameLayout(getActivity());
	view = inflater.inflate(R.layout.fragment_chat, container, false);
	InitView();
	frameLayout .addView(view);
		if (checkLoadding==null) {
			checkLoadding = new Handler();
			checkLoadding.postDelayed(runCheckLoading, 3000);
		}else {
			DebugLog.d("loading check:");
		}

	return frameLayout;
}

	@Override
	public boolean onBackPressed() {

		if (cannedPressed){
			DebugLog.d("check back pressed:"+true);
			EventBus.getDefault().post(new Events.setCannedText());
			return true;
		}else {
			DebugLog.d("check back pressed:"+false);
			return false;
		}
	}

	@SuppressWarnings("unused")
public void onEventMainThread(Events.KeyBoardShown event) {
	View v = getView();
	if (v != null) {
		if (event.isKeyboardShown()) {
			if (!isKeyboardShow) {
				isKeyboardShow = true;
			}
		}
		else {
			recyclerView.scrollToPosition(listG4Message.size() - 1);
			isKeyboardShow = false;
		}
	}
}
Runnable runCheckLoading=new Runnable() {
	@Override
	public void run() {
		DebugLog.d("check run:::");
		if (g4Manager!=null&&g4Manager.isConnectionState()) {
			if (txtloading.getVisibility() == View.VISIBLE) {

				if (temp.equals(txtloading.getText().toString())) {
					EventBus.getDefault().post(new Events.RefreshChat());
				}
			}
		}
		temp=txtloading.getText().toString();
		checkLoadding.postDelayed(runCheckLoading,2000);
	}
};
@Override
public void onActivityCreated(Bundle savedInstanceState) {
	super.onActivityCreated(savedInstanceState);
	getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
}

@Override
public void onSaveInstanceState(Bundle outState) {
	super.onSaveInstanceState(outState);
}

@Override
public void onAttach(Activity activity) {
	super.onAttach(activity);
}


    @Override
    public void binData(String result) {
		g4Manager=application.getG4Manager();
		if (result.toString().contains("OK")) {
			if (result.contains("GPMS=0")) {
				if(g4Manager.isChatScreen()) {
					g4Manager.setIDMessendger(result, true);
				}
				g4Manager.detectNewMessage(result);
			}
			if (result.contains("GPMS=1")) {
				if(g4Manager.isChatScreen()) {
					g4Manager.setIDMessendger(result, false);
				}
			}
		}

    }

    @Override
    public void binConnectionState(boolean isConnect) {
		DebugLog.d("check data: "+isConnect);

    }

    @Override
    public void binRssi(int rssi) {

    }

    @Override
public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	((Application) getActivity().getApplication()).inject(this);
	getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
}

@SuppressWarnings("unused")
public void onEventMainThread(Events.setCannedText event) {
	if (application.getCannedMessages() != null) {
		cannedMsg = application.getCannedMessages().getMessages();
	}
//// check scanned message
	if (!cannedPressed) {
		try {
			cannedPressed = true;
			InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			//input.setFocusable(false);
			Collections.sort(cannedMsg, String.CASE_INSENSITIVE_ORDER);
			messagesCannedAdaper = new MessagesCannedAdaper(getActivity(), application.getCannedMessages().getMessages());
			messagesCannedAdaper.setHasStableIds(true);
			recyclerView.setLayoutManager(llm);
			recyclerView.setAdapter(messagesCannedAdaper);
			recyclerView.scrollToPosition(listG4Message.size() - 1);
		}
		catch (Exception e) {

		}
	}
	else {
		try {
			cannedPressed = false;
			input.setFocusableInTouchMode(true);
			input.setFocusable(true);
			input.setImeOptions(EditorInfo.IME_ACTION_DONE);
			recyclerView.setAdapter(messagesAdapter);
			recyclerView.setLayoutManager(llm);
			messagesAdapter.notifyDataSetChanged();
			recyclerView.scrollToPosition(listG4Message.size() - 1);
		}
		catch (Exception e) {
		}
	}
}

public void getMessageTimeOut() {
	try {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mess_time_out = sharedPref.getInt("message_time_out", 5);
	}
	catch (Exception e) {
		mess_time_out = 1;
	}
}

void InitSetting() {
	g4Manager = application.getG4Manager();
	if (g4Manager != null) {
			try {
				mGattClient = g4Manager.getClient();
				mGattClient.setContext(getActivity());
				mGattClient = g4Manager.getClient();
				mGattClient.setListener(new GattClient.OnCounterReadListener() {
					@Override
					public void onCounterRead(String value) {
						/*Log.d("G4Chat1", "value GPMS :" + value);

						if(!sb.toString().contains(value))
						{
							sb.append(value);
						}



						DebugLog.e("message:"+g4Manager.getStringBuider().toString());
						if (sb.toString().contains("OK")) {
							if (sb.toString().contains("GPMS=0")) {
								Log.d("G4Chat1", "value GPMS=0 :" + sb.toString());
								g4Manager.setIDMessendger(sb.toString(), true);
								sb.setLength(0);
							}
						}*/
					}
					@Override
					public void onConnected(boolean success) {
                        DebugLog.d("check isconnect:::"+success);
						EventBus.getDefault().post(new Events.AutoConnect(true));
					}

					@Override
					public void onRSSIChange(int rssi) {
					}
				});
			}
			catch (Exception e) {
			}
	}
}


public void refreshMessageList() {
}

@Override
public void onResume() {
	super.onResume();
	EventBus.getDefault().register(this);
	getMessageTimeOut();
	try {
		g4Manager = application.getG4Manager();
		//g4Manager.empty2List();
		if(g4Manager.isFromConfigScreen())
		{

			if (g4Manager.getListG4MessagesAll() != null) {
				listG4Message = g4Manager.getListG4MessagesAll();
				llm = new LinearLayoutManager(getActivity());
				messagesAdapter = new MessagesAdapter(listG4Message, getActivity());
				messagesAdapter.setHasStableIds(false);
				recyclerView.setLayoutManager(llm);
				recyclerView.setAdapter(messagesAdapter);
				recyclerView.scrollToPosition(listG4Message.size() - 1);
			}
			g4Manager.setFromConfigScreen(false);
		}
		else if(g4Manager.isFromATScreen()){
			g4Manager.setListG4MessagesAll(null);

		}
		else{
			listG4Message = g4Manager.getListG4MessagesAll();
			llm = new LinearLayoutManager(getActivity());
			messagesAdapter = new MessagesAdapter(listG4Message, getActivity());
			messagesAdapter.setHasStableIds(false);
			recyclerView.setLayoutManager(llm);
			recyclerView.setAdapter(messagesAdapter);
			recyclerView.scrollToPosition(listG4Message.size() - 1);
		}
	}
	catch (Exception e) {

	}
}

@Override
public void onDetach() {
	super.onDetach();

}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
        checkLoadding.removeCallbacksAndMessages(null);
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
@Override
public void onPause() {
	super.onPause();
	EventBus.getDefault().unregister(this);
	if (refreshHandlerThread != null) {
		refreshHandlerThread.stopRefresh();
	}

}

@Override
public void onStop() {
	try {
		super.onStop();
		//  handler.removeCallbacks(chatRunnable);
		if (refreshHandlerThread != null) {
			refreshHandlerThread.stopRefresh();
		}
		g4Manager.setChatATScreen(false);
		checkLoadding.removeCallbacks(runCheckLoading);
		chatLoading=null;
	}
	catch (Exception e) {
	}
}
public void onEventMainThread(Events.ReceiverMessageG4 event) {
	if (event.isStart()) {
	}
	else {
	}
}
	public void onEventMainThread(Events.RefreshList event) {
//		DebugLog.d("receive message:");
//		DebugLog.d("receive message:"+listG4Message.size());
//		////
//		messagesAdapter.notifyDataSetChanged();
	}
public void onEventMainThread(Events.UpdateList event) {

	if (txtloading.getVisibility()==View.GONE||txtloading.getVisibility()==View.INVISIBLE) {
		DebugLog.d("update list: "+g4Manager.getListG4MessagesReceive().size()+":"+g4Manager.getListG4MessagesSent().size());
		if(g4Manager.getListG4MessagesReceive().size()>20) {
			do {

				g4Manager.getListG4MessagesReceive().remove(0);
			} while (g4Manager.getListG4MessagesReceive().size() > 20);

			listG4Message.clear();
			Date receive = new Date();
			Date send = new Date();
			int i = 0, j = 0;
			try {
				while (i < g4Manager.getListG4MessagesReceive().size() && j < g4Manager.getListG4MessagesSent().size()) {
					receive = g4Manager.getListG4MessagesReceive().get(i).getDate();
					send = g4Manager.getListG4MessagesSent().get(j).getDate();

					if (receive.before(send)) {

						listG4Message.add(g4Manager.getListG4MessagesReceive().get(i));
						i++;
					}
					else {
						listG4Message.add(g4Manager.getListG4MessagesSent().get(j));
						j++;
					}
				}
				while (i < g4Manager.getListG4MessagesReceive().size()) {
					listG4Message.add(g4Manager.getListG4MessagesReceive().get(i));
					i++;
				}

				while (j < g4Manager.getListG4MessagesSent().size()) {
					listG4Message.add(g4Manager.getListG4MessagesSent().get(j));
					j++;
				}
			}
			catch (Exception e) {
			}


			swipe.setRefreshing(false);
			recyclerView.scrollToPosition(listG4Message.size() - 1);
			g4Manager.setListG4MessagesAll(listG4Message);
			messagesAdapter.notifyDataSetChanged();
		}
		if(g4Manager.getListG4MessagesSent().size()>20) {
			do {
				g4Manager.getListG4MessagesSent().remove(0);
			} while (g4Manager.getListG4MessagesSent().size() > 20);
			EventBus.getDefault().post(new Events.UpdateListChat());
			swipe.setRefreshing(false);
			recyclerView.scrollToPosition(listG4Message.size() - 1);
			g4Manager.setListG4MessagesAll(listG4Message);
			messagesAdapter.notifyDataSetChanged();
		}
		DebugLog.d("update list: "+g4Manager.getListG4MessagesReceive().size()+":"+g4Manager.getListG4MessagesSent().size());
		/*swipe.setRefreshing(false);
		messagesAdapter.notifyDataSetChanged();
		recyclerView.scrollToPosition(listG4Message.size() - 1);
		g4Manager.setListG4MessagesAll(listG4Message);*/
	}
}

@Override
public void setUserVisibleHint(boolean isVisibleToUser) {
	super.setUserVisibleHint(isVisibleToUser);
	try {
		g4Manager = application.getG4Manager();
	}
	catch (Exception e) {
	}

	if (!isVisibleToUser) {
		try {
//			String filePath = Environment.getExternalStorageDirectory() + "/logcat_g4.txt";
//			Runtime.getRuntime().exec(new String[]{"logcat", "-f", filePath, "G4Chat1", "*:S"});
		}
		catch (Exception e) {
		}
		if (refreshHandlerThread != null) {
			refreshHandlerThread.stopRefresh();
			Log.d("G4Chat1", "kill runnable ");
		}

		if (g4Manager != null) {
			g4Manager.setChatATScreen(false);
		}

	}
	else {

		g4Manager=application.getG4Manager();
		if (g4Manager!=null) {
			g4Manager.setChatATScreen(true);
			mGattClient = g4Manager.getClient();
			mGattClient.setListener(new GattClient.OnCounterReadListener() {
				@Override
				public void onCounterRead(String value) {

				}

				@Override
				public void onConnected(boolean success) {
					DebugLog.d("check isconnect:::"+success);
					EventBus.getDefault().post(new Events.AutoConnect(true));
				}

				@Override
				public void onRSSIChange(int rssi) {

				}
			});
		}
		//InitSetting();
		Thread.interrupted();

		if (g4Manager != null && g4Manager.isConnectionState()) {
			g4Manager.setChatATScreen(true);
			g4Manager.setAcceptExecute(true);
			EventBus.getDefault().post(new Events.StopMarkAlerFragment());
			if (listG4Message.size() == 0) {
				txtloading.setVisibility(View.VISIBLE);
			}
			mGattClient = g4Manager.getClient();
			mGattClient.setContext(getActivity());
			mGattClient = g4Manager.getClient();
			if (refreshHandlerThread != null) {
				refreshHandlerThread.stopRefresh();
			}
			refreshHandlerThread = new RefreshHandlerThread();
		}
	}
}

public void onEventMainThread(Events.UpdateListChat event) {
	Log.d("G4Chat2", "Enter Update List Chat");
	DebugLog.d("check message:");
	txtloading.setVisibility(View.GONE);
	listG4Message.clear();

	int i = 0, j = 0;
	Date receive = new Date();
	Date send = new Date();
	try {
		while (i < g4Manager.getListG4MessagesReceive().size() && j < g4Manager.getListG4MessagesSent().size()) {
			receive = g4Manager.getListG4MessagesReceive().get(i).getDate();
			send = g4Manager.getListG4MessagesSent().get(j).getDate();

			if (receive.before(send)) {

				listG4Message.add(g4Manager.getListG4MessagesReceive().get(i));
				i++;
			}
			else {
				listG4Message.add(g4Manager.getListG4MessagesSent().get(j));
				j++;
			}
		}
		while (i < g4Manager.getListG4MessagesReceive().size()) {
			listG4Message.add(g4Manager.getListG4MessagesReceive().get(i));
			i++;
		}

		while (j < g4Manager.getListG4MessagesSent().size()) {
			listG4Message.add(g4Manager.getListG4MessagesSent().get(j));
			j++;
		}
	}
	catch (Exception e) {
	}

	swipe.setRefreshing(false);
	messagesAdapter.notifyDataSetChanged();
	recyclerView.scrollToPosition(listG4Message.size() - 1);
	g4Manager.setListG4MessagesAll(listG4Message);
}

public void onEventMainThread(Events.UpdateLoading event) {
	txtloading.setText("Loading.. " + event.getTextLoading());
}

public void onEventMainThread(Events.RefreshChat event) {
	Log.d("G4Chat1", "Runnable Chat");

	if (refreshHandlerThread != null) {
		if (refreshHandlerThread.isAlive()) {
			Log.d("G4Chat1", "Kill Runnable Chat");
			refreshHandlerThread.stopRefresh();
		}
	}
	else {
	}
	refreshHandlerThread = new RefreshHandlerThread();
}

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {
	int ItemType;
	private ArrayList<G4Messages> listG4Message;
	private Context context;

	public MessagesAdapter(ArrayList<G4Messages> listG4Message, Context context) {
		this.listG4Message = listG4Message;
		this.context = context;
	}

	public void update(Message... newMessages) {
	}

	public void update(Context context, ArrayList<G4Messages> listG4Message) {
		this.listG4Message = listG4Message;
		this.context = context;
		notifyDataSetChanged();
	}

	@Override
	public void onViewRecycled(ViewHolder holder) {
		super.onViewRecycled(holder);
	}

	@Override
	public MessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View itemView;

		if (ItemType == 0)
			itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.received_chat_message, parent, false);

		else
			itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.sent_chat_message, parent, false);
		return new ViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(MessagesAdapter.ViewHolder holder, int position) {
		G4Messages g4Messages = listG4Message.get(position);
		curPosition = position;
		final String messageRetry = g4Messages.getBody();
		String message = g4Messages.getBody().replaceAll(getResources().getString(R.string.emoji_replace),getResources().getString(R.string.emoji));
		message = message.replaceAll("D","d");
		message = message.replaceAll("E","e");
		message = StringEscapeUtils.unescapeJava(message);
		holder.messageText.setText(message);

		if (g4Messages.getStatus() != null) {
			if (g4Messages.getStatus().contains("UNSENT")) {
				Date now = new Date(System.currentTimeMillis());
				long result = now.getTime() - listG4Message.get(position).getDate().getTime();

				if (mess_time_out != 0) {
					if (result > mess_time_out * 60 * 1000) {
						holder.messageStatus.setText("Failed to send " + g4Messages.getLocalDate());
						holder.btnresend.setVisibility(View.VISIBLE);
					}
					else {
						holder.messageStatus.setText("Sending " + g4Messages.getLocalDate());
						holder.btnresend.setVisibility(View.GONE);
					}
				}
				else {
					holder.messageStatus.setText("Sending " + g4Messages.getLocalDate());
					holder.btnresend.setVisibility(View.GONE);
				}
			}
			else {
				holder.messageStatus.setText("Sent " + g4Messages.getLocalDate());
				holder.btnresend.setVisibility(View.GONE);
			}
			holder.btnresend.setOnClickListener(new View.OnClickListener() {
			@Override
				public void onClick(View v) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
								case DialogInterface.BUTTON_POSITIVE:

//                                        final String message = input.getText().toString();
//                                        input.setText("");
									Date now = new Date();
									final Calendar calendar = Calendar.getInstance();
									calendar.setTime(now);
									G4Messages g4Message = new G4Messages();
									g4Message.setBody(messageRetry);
									g4Message.setID(g4Manager.getLastIdSent() + 1);
									g4Message.setStatus("UNSENT");
									g4Message.setDate(calendar.getTime());
									listG4Message.add(g4Message);
									messagesAdapter.notifyDataSetChanged();

									recyclerView.scrollToPosition(listG4Message.size() - 1);
									g4Manager.executeMessenger(g4Manager.getClient(), 0, false, messageRetry);


								case DialogInterface.BUTTON_NEGATIVE:
									//No button clicked
									break;
							}
						}
					};
					AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.myDialog));
					builder.setMessage("Retry sending the message?").setPositiveButton("Yes", dialogClickListener)
							.setNegativeButton("No", dialogClickListener).show();
				}
			});
		}
	}

	@Override
	public int getItemCount() {
		if (listG4Message != null) {
			return listG4Message.size();
		}
		else {
			return 0;
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		try {
			if (listG4Message.get(position).getStatus().contains("SEN")) {
				ItemType = 1;
				return ItemType;
				//    Log.d("G4Chat1","SEN" + MessageCheckData.get(position) +" : "+  MessageData.get(position));
			}
			else if (!listG4Message.get(position).getStatus().contains("READ")) {
				ItemType = 1;
				return ItemType;
				//    Log.d("G4Chat","SEN" + MessageCheckData.get(position) +" : "+  MessageData.get(position));
			}
			else {
				ItemType = 0;
				return ItemType;
				//  Log.d("G4Chat","Read" + MessageCheckData.get(position) +" : "+  MessageData.get(position));
			}
		}
		catch (Exception e) {
			ItemType = 0;
		}
		return position;
	}

	public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		public TextView messageStatus;
		public EmojiconTextView messageText;
		public ImageButton btnresend;
		public RelativeLayout cardView;

		public ViewHolder(View messageView) {
			super(messageView);
			messageView.setClickable(true);
			messageView.setOnClickListener(this);
			btnresend = (ImageButton) messageView.findViewById(R.id.reload);
			messageText = (EmojiconTextView) messageView.findViewById(R.id.message);
			messageStatus = (TextView) messageView.findViewById(R.id.message_status);
			cardView = (RelativeLayout) messageView.findViewById(R.id.carView);
		}
		@Override
		public void onClick(View v) {
		}
	}
}

public class MessagesCannedAdaper extends RecyclerView.Adapter<MessagesCannedAdaper.ViewHolder> {
	private List<String> MessageCannedData;
	private Context context;

	public MessagesCannedAdaper(Context context, List<String> listMsg) {
		this.MessageCannedData = listMsg;
		this.context = context;
	}
	@Override
	public int getItemViewType(int position) {
		return position;
	}

	@Override
	public int getItemCount() {
		if (MessageCannedData != null) {
			return MessageCannedData.size();
		}
		return 0;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View itemView;
		itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.canned_message, parent, false);

		return new ViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(final MessagesCannedAdaper.ViewHolder holder, final int position) {
		holder.messcanned.setText(MessageCannedData.get(position));
		holder.canned.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				input.setFocusableInTouchMode(true);
				input.setFocusable(true);
				input.setImeOptions(EditorInfo.IME_ACTION_DONE);
				input.setText(input.getText() + MessageCannedData.get(position).toString());
				input.setSelection(input.getText().length());
				cannedPressed = false;
				try {
					recyclerView.setAdapter(messagesAdapter);
					recyclerView.scrollToPosition(listG4Message.size() - 1);
				}
				catch (Exception e) {
				}
			}
		});
	}

	@Override
	public void onViewRecycled(MessagesCannedAdaper.ViewHolder holder) {
		super.onViewRecycled(holder);
	}

	public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		public TextView messcanned;
		public LinearLayout canned;

		public ViewHolder(View messageView) {
			super(messageView);
			messageView.setClickable(true);
			messageView.setOnClickListener(this);
			messcanned = (TextView) messageView.findViewById(R.id.canned);
			canned = (LinearLayout) messageView.findViewById(R.id.canned_msg);
		}

		@Override
		public void onClick(View v) {
		}
	}
}

public class RefreshHandlerThread extends HandlerThread {
	private final Handler handler;
	private final Runnable runnable;

	public RefreshHandlerThread() {
		super(RefreshHandlerThread.class.getName(), 1);
		start();
		handler=new Handler();
		//handler = new Handler(getLooper());
		runnable = new chatRunnable();
		handler.post(runnable);
	}

	void stopRefresh() {
		handler.removeCallbacks(runnable);


	}
	public void getAllListMessager(){
		g4Manager.getClient().writeInteractor("AT$GMGL=ALL", false);
	}
	private final class chatRunnable implements Runnable {
		StringBuilder sb = new StringBuilder(192);
		String temp="";
		@Override
		public void run() {
			try {
				DebugLog.d("check run:");
				if (g4Manager != null ) {


						Log.d("G4Chat2", "Enter ChatRunnable");
						if(g4Manager.getListG4MessagesReceive().size()>20) {
							do {
								g4Manager.getListG4MessagesReceive().remove(0);
							} while (g4Manager.getListG4MessagesReceive().size() > 20);
						}
						if(g4Manager.getListG4MessagesSent().size()>20) {
							do {
								g4Manager.getListG4MessagesSent().remove(0);
							} while (g4Manager.getListG4MessagesSent().size() > 20);
						}
						if(!g4Manager.getFailQueue().isEmpty()) {
							String messageSend = g4Manager.getFailQueue().poll();
							g4Manager.executeMessenger(g4Manager.getClient(),0,false,messageSend);
						}
						if (g4Manager.isAcceptExecute()) {
							if (g4Manager.isDoneListSent() || g4Manager.getListG4MessagesReceive().size()==0) {
								Log.d("G4Chat2", "get List Receive");
								g4Manager.getClient().writeInteractor("AT$GPMS=0", false);
							}else if(!g4Manager.isDoneListSent()) {
								Log.d("G4Chat2", "get List Sent");
								g4Manager.getClient().writeInteractor("AT$GPMS=1", false);
							}
							Log.d("G4Chat1", "run AT$GPMS=0 ");
							DebugLog.d("check message:");
							g4Manager.getClient().setListener(new GattClient.OnCounterReadListener() {
								@Override
								public void onCounterRead(String value) {
								/*Log.d("G4Chat1", "Value return G4: "+ value);
								if (!sb.toString().contains(value)) {
									sb.append(value);

								}
								if (sb.toString().contains("OK")) {
									if (sb.toString().contains("GPMS=0")) {
										Log.d("G4Chat1", "@onCounterRead-ChatFragment GPMS=0 DATA:" + sb.toString());
										String data = sb.toString();
										sb.setLength(0);
										g4Manager.setIDMessendger(data, true);
									}

									if (sb.toString().contains("GPMS=1")) {
										Log.d("G4Chat1", "@onCounterRead-ChatFragment GPMS=1 DATA:" + sb.toString());
										String data = sb.toString();
										sb.setLength(0);
										g4Manager.setIDMessendger(data, false);
									}
								}*/
								}

								@Override
								public void onConnected(boolean success) {
									DebugLog.d("check isconnect:::" + success);
									if (success)
										g4Manager.setConnectionState(true);
									if (success == false) {
										EventBus.getDefault().post(new Events.AutoConnect(true));
									} else {
										EventBus.getDefault().postSticky(new Events.BluetoothStatus(true));
									}
								}

								@Override
								public void onRSSIChange(int rssi) {

								}
							});
						}
					}

			} finally {
				handler.postDelayed(this, 25000);
			}

		}
	}
	;
}
	public void onEventMainThread(Events.BluetoothData event) {
		//DebugLog.e("data:"+event.getData());
	}
}