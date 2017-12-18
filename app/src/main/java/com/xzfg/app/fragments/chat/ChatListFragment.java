package com.xzfg.app.fragments.chat;

import android.content.Intent;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.managers.ChatManager;
import com.xzfg.app.managers.G4Manager;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.Chat;
import com.xzfg.app.model.User;
import com.xzfg.app.services.ChatService;
import com.xzfg.app.services.GattClient;
import com.xzfg.app.util.DateUtil;
import com.xzfg.app.widgets.DividerItemDecoration;

import java.text.NumberFormat;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 */
public class ChatListFragment extends Fragment implements View.OnClickListener {


    @SuppressWarnings("unused")
    @Inject
    Application application;

    @Inject
    ChatManager chatManager;

    @Inject
    G4Manager g4Manager;
    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private GattClient mGattClient;
    private IntentActionDialogFragment intentActionDialog;
    private ContactsListDialogFragment contactsListDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getActivity().getApplication()).inject(this);
        adapter = new ChatListAdapter();
        adapter.setHasStableIds(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chatlist, viewGroup, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.chat_list);
        recyclerView.setHasFixedSize(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        view.findViewById(R.id.clear_messages_button).setOnClickListener(this);
        view.findViewById(R.id.user_list_button).setOnClickListener(this);
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
        onEventMainThread(EventBus.getDefault().getStickyEvent(Events.ChatDataUpdated.class));
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        if (intentActionDialog != null) {
            intentActionDialog.dismissAllowingStateLoss();
            intentActionDialog = null;
        }
        if (contactsListDialog != null) {
            contactsListDialog.dismissAllowingStateLoss();
            contactsListDialog = null;
        }
        super.onPause();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.ChatDataUpdated event) {
        if (event != null) {
            EventBus.getDefault().removeStickyEvent(event);
            adapter.update();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {

            case R.id.clear_messages_button: {
                v.setEnabled(false);
                try {
                    if (intentActionDialog != null) {
                        intentActionDialog.dismissAllowingStateLoss();
                        intentActionDialog = null;
                    }
                    intentActionDialog = IntentActionDialogFragment.newInstance(
                            getString(R.string.warning),
                            getString(R.string.chat_remove_all),
                            new Intent(getActivity(), ChatService.class).putExtra("isService", true).setAction(Givens.ACTION_CHAT_DELETE_MESSAGES),
                            new Intent(getActivity(), ChatService.class)
                    );
                    intentActionDialog.show(getChildFragmentManager(), "clear_from_user");
                } catch (Exception e) {
                    Timber.w(e, "An error occurred showing the clear messages for all users dialog. Is somebody spamming the button?");
                }
                v.setEnabled(true);
                break;
            }
            case R.id.user_list_button: {
                v.setEnabled(false);
                try {
                    if (contactsListDialog != null) {
                        contactsListDialog.dismissAllowingStateLoss();
                        contactsListDialog = null;
                    }
                    contactsListDialog = ContactsListDialogFragment.newInstance();
                    contactsListDialog.show(getChildFragmentManager(), "contacts_list_fragment");
                } catch (Exception e) {
                    Timber.w(e, "An error occurred while displaying the contact list dialog. Is somebody attemtping to spam the button?");
                }
                v.setEnabled(true);
                break;
            }
        }
    }




    private class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder>  {


        public ChatListAdapter() {

        }

        public synchronized void update() {
            notifyDataSetChanged();
        }

        @Override
        public ChatListAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.chatlist_row, viewGroup, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ChatListAdapter.ViewHolder viewHolder, int position) {
            if (chatManager.getChats() == null) {
                Timber.e(new Exception("Chats are null."), "Chats are null");
                return;
            }
            if (position > chatManager.getChats().size()) {
                Timber.e(new Exception("Position is beyond chat size."), "Position: " + position + ", Size: " + chatManager.getChats().size());
                return;
            }

            Chat chat = chatManager.getChats().get(position);
            if (chat == null) {
                Timber.e(new Exception("Chat is null"), "Chat is null");
                return;
            }

            User user = chat.getUser();
            if (user == null) {
                if (BuildConfig.DEBUG) {
                    Crashlytics.setString("Chat", chat.toString());
                }
                Timber.e(new Exception("Chat has a null user"), "Chat has a null user");
                return;
            }

            viewHolder.contactView.setTag(R.id.user,user);

            viewHolder.contactName.setText(user.getUsername());

            if ((user.getDistance() != null && user.getDistance() != 0) || user.getDirection() != null) {
                viewHolder.location.setVisibility(View.VISIBLE);
                if (user.getDistance() != null && user.getDistance() != 0) {
                    viewHolder.distanceText.setText(NumberFormat.getInstance(getResources().getConfiguration().locale).format(user.getDistance()));
                }
                if (user.getDirection() != null) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(user.getDirection(), viewHolder.directionArrow.getDrawable().getIntrinsicWidth() / 2, viewHolder.directionArrow.getDrawable().getIntrinsicHeight() / 2);
                    viewHolder.directionArrow.setScaleType(ImageView.ScaleType.MATRIX);
                    viewHolder.directionArrow.setImageMatrix(matrix);
                }
            } else {
                viewHolder.location.setVisibility(View.INVISIBLE);
            }

            DateUtil.formatDate(viewHolder.timeView, chat.getLastMessageDate());

            if (user.isOnline()) {
                viewHolder.statusIndicator.setVisibility(View.VISIBLE);
            } else {
                viewHolder.statusIndicator.setVisibility(View.INVISIBLE);
            }

            String[] lastMessageDetails = chat.getLastMessage();
            if (lastMessageDetails != null && lastMessageDetails.length == 2) {
                if (user.getUserId().equals("b") || user.getUserId().equals("x") || !lastMessageDetails[0].equals(user.getUsername())) {
                    viewHolder.messageView.setText(String.format(getString(R.string.last_message), lastMessageDetails[0], lastMessageDetails[1]));
                } else {
                    viewHolder.messageView.setText(lastMessageDetails[1]);
                }
            } else {
                viewHolder.messageView.setText("");
            }
        }

        @Override
        public int getItemCount() {
            if (chatManager.getChats() == null) {
                return 0;
            }
            return chatManager.getChats().size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            // each data item is just a string in this case
            public ImageView statusIndicator;
            public TextView contactName;
            public TextView messageView;
            public TextView timeView;
            public TextView distanceText;
            public ImageView directionArrow;
            public LinearLayout location;
            public View contactView;

            public ViewHolder(View contactView) {
                super(contactView);
                this.contactView = contactView;
                contactView.setClickable(true);
                contactView.setOnClickListener(this);
                statusIndicator = (ImageView) contactView.findViewById(R.id.online_status_indicator);
                contactName = (TextView) contactView.findViewById(R.id.contact_name);
                messageView = (TextView) contactView.findViewById(R.id.latest_message);
                timeView = (TextView) contactView.findViewById(R.id.time);
                location = (LinearLayout) contactView.findViewById(R.id.location);
                distanceText = (TextView) contactView.findViewById(R.id.distance);
                directionArrow = (ImageView) contactView.findViewById(R.id.arrow);
            }


            @Override
            public void onClick(View v) {
                User user = (User)v.getTag(R.id.user);
                if (user != null) {
                    EventBus.getDefault().post(new Events.StartChat(user));
                }
            }
        }

    }
}
