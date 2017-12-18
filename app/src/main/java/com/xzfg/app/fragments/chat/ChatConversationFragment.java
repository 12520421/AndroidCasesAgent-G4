package com.xzfg.app.fragments.chat;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.fragments.dialogs.UserOfflineDialog;
import com.xzfg.app.managers.ChatManager;
import com.xzfg.app.model.Chat;
import com.xzfg.app.model.Message;
import com.xzfg.app.model.SendableMessage;
import com.xzfg.app.model.User;
import com.xzfg.app.services.ChatService;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 */
public class ChatConversationFragment extends Fragment implements View.OnClickListener, TextWatcher {

    @Inject
    Application application;
    @Inject
    ChatManager chatManager;
    @Inject
    InputMethodManager inputMethodManager;
    IntentActionDialogFragment intentActionDialogFragment;
    ContactsListDialogFragment contactsListDialogFragment;
    private User contact;
    private EditText input;
    private TextView title;

    private ImageButton sendButton;
    private RecyclerView recyclerView;
    private MessagesAdapter messagesAdapter;
    private LinearLayout buttonBar;
    //private Chat chat;

    public static ChatConversationFragment newInstance(User contact) {
        ChatConversationFragment f = new ChatConversationFragment();
        Bundle args = new Bundle();
        args.putParcelable("contact", contact);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contact = getArguments().getParcelable("contact");
        messagesAdapter = new MessagesAdapter();
        messagesAdapter.setHasStableIds(false);
        ((Application) getActivity().getApplication()).inject(this);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        if (intentActionDialogFragment != null) {
            intentActionDialogFragment.dismissAllowingStateLoss();
            intentActionDialogFragment = null;
        }
        if (contactsListDialogFragment != null) {
            contactsListDialogFragment.dismissAllowingStateLoss();
            contactsListDialogFragment = null;
        }

        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, viewGroup, false);
        input = (EditText) view.findViewById(R.id.message_input);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        title = (TextView) view.findViewById(R.id.chat_title);
        title.setText(Html.fromHtml(String.format(getString(R.string.chatting_with_name), contact.username)));
        recyclerView = (RecyclerView) view.findViewById(R.id.chat_list);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setStackFromEnd(true);

        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(messagesAdapter);

        //clearButton = (Button) view.findViewById(R.id.clear_button);

        sendButton = (ImageButton) view.findViewById(R.id.send_button);
        sendButton.setOnClickListener(this);
        input.addTextChangedListener(this);
        buttonBar = (LinearLayout) view.findViewById(R.id.button_bar);
        view.findViewById(R.id.back_button).setOnClickListener(this);
        view.findViewById(R.id.user_list_button).setOnClickListener(this);
        view.findViewById(R.id.clear_messages_button).setOnClickListener(this);

        return view;
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.clear_messages_button:
                v.setEnabled(false);
                try {
                    if (intentActionDialogFragment != null) {
                        intentActionDialogFragment.dismissAllowingStateLoss();
                        intentActionDialogFragment = null;
                    }
                    intentActionDialogFragment = IntentActionDialogFragment.newInstance(
                            getString(R.string.warning),
                            String.format(getString(R.string.chat_remove_user), contact.getUsername()),
                            new Intent(getActivity(), ChatService.class).putExtra("isService", true).putExtra("userId", contact.getUserId()).setAction(Givens.ACTION_CHAT_DELETE_MESSAGES),
                            new Intent(getActivity(), ChatService.class)
                    );
                    intentActionDialogFragment.show(getChildFragmentManager(), "clear_from_user");
                } catch (Exception e) {
                    Timber.w(e, "An error occurred displaying the clear conversation dialog. Is somebody spamming the button?");
                }
                v.setEnabled(true);
                break;
            case R.id.send_button: {
                v.setEnabled(false);
                try {
                    //Timber.d("Send button pressed.");
                    SendableMessage newMessage = new SendableMessage();
                    newMessage.setUsername(application.getScannedSettings().getUserName());
                    newMessage.setMessage(input.getText().toString());
                    newMessage.setStatus(getString(R.string.sending));
                    newMessage.setToUserId(contact.getUserId());
                    newMessage.setCreated(new Date());
                    //Timber.d("Updating messages adapter with the new message.");
                    messagesAdapter.update(newMessage);


                    //Timber.d("Checking online status.");
                    User freshUser = chatManager.findUserById(contact.getUserId());
                    if (freshUser != null) {
                        //Timber.d("Fresh user acquired. " + freshUser.toString());
                        contact = freshUser;
                    } else {
                        //Timber.d("Fresh user is null. Using most recently known status.");
                    }
                    //Timber.d("Creating sendable message.");


                    if (contact != null && !contact.isOnline() && ((contact.getHasEmail() != null && contact.getHasEmail().equalsIgnoreCase("true")) || (contact.getHasPhone() != null && contact.getHasPhone().equalsIgnoreCase("true")))) {
                        //Timber.d("User is offline, and has email or phone number.");
                        UserOfflineDialog userOfflineDialog = UserOfflineDialog.newInstance(freshUser, newMessage);
                        //Timber.d("Displaying offline message dialog.");
                        userOfflineDialog.show(getFragmentManager(), "UserOffline_" + contact.getUserId());
                    } else {
                        //Timber.d("Sending message to chat service.");
                        Intent i = new Intent(application, ChatService.class).setAction(Givens.ACTION_CHAT_SEND_MESSAGE).putExtra("chat_message", newMessage);
                        application.startService(i);
                    }
                    input.setText("");
                } catch (Exception e) {
                    Timber.e(e, "An error occurred attempting to send a message.");
                }
                v.setEnabled(true);
            }

            break;

            case R.id.back_button:
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                getActivity().onBackPressed();
                break;
            case R.id.user_list_button: {
                v.setEnabled(false);
                try {
                    if (contactsListDialogFragment != null) {
                        contactsListDialogFragment.dismissAllowingStateLoss();
                        contactsListDialogFragment = null;
                    }
                    contactsListDialogFragment = ContactsListDialogFragment.newInstance();
                    contactsListDialogFragment.show(getChildFragmentManager(), "contacts_list_fragment");
                } catch (Exception e) {
                    Timber.w(e, "Error occurred display contact list dialog. Is somebody trying to spam the button?");
                }
                v.setEnabled(true);
                break;
            }

        }

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.NewChatMessages event) {
        messagesAdapter.update(event.getMessages(contact.getUsername()));
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.ChatMessageSent event) {
        SendableMessage sm = event.getSendableMessage();
        if (sm.getToUserId().equals(contact.getUserId())) {
            messagesAdapter.updateSent(event.getSendableMessage());
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.ChatsCleared chatsCleared) {
        if (chatsCleared.getUserId().equals(contact.getUserId())) {
            messagesAdapter.clear();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.KeyBoardShown event) {
        View v = getView();
        if (v != null) {
            if (event.isKeyboardShown()) {
                v.findViewById(R.id.button_bar).setVisibility(View.VISIBLE);
            } else {
                v.findViewById(R.id.button_bar).setVisibility(View.GONE);
            }
        }
    }

    private class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {


        public void update(Message... newMessages) {
            Chat chat = chatManager.getChat(contact);
            notifyDataSetChanged();
            if (recyclerView != null && chat != null && !chat.getMessages().isEmpty())
                recyclerView.scrollToPosition(chat.getMessages().size() - 1);
        }

        public void update(List<Message> newMessages) {
            Chat chat = chatManager.getChat(contact);
            notifyDataSetChanged();
            if (recyclerView != null && chat != null && !chat.getMessages().isEmpty())
                recyclerView.scrollToPosition(chat.getMessages().size() - 1);
        }

        public void updateSent(SendableMessage sm) {
            Chat chat = chatManager.getChat(contact);
            if (chat != null) {
                int x = chat.getMessages().indexOf(sm);
                if (x > -1) {
                    chat.getMessages().set(x, sm);
                } else {
                    chat.getMessages().add(sm);
                }
                Collections.sort(chat.getMessages());
            }
            notifyDataSetChanged();
            if (recyclerView != null && chat != null && !chat.getMessages().isEmpty())
                recyclerView.scrollToPosition(chat.getMessages().size() - 1);
        }

        public void clear() {
            notifyDataSetChanged();
        }

        @Override
        public MessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View itemView;
            if (viewType == 0)
                itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.received_chat_message, viewGroup, false);
            else
                itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.sent_chat_message, viewGroup, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MessagesAdapter.ViewHolder viewHolder, int i) {
            if (i > chatManager.getChat(contact).getMessages().size()) {
                return;
            }
            Message message;
            try {
                message = chatManager.getChat(contact).getMessages().get(i);
            } catch (Exception e) {
                return;
            }
            if (message == null) {
                return;
            }

            if (message.getLanguageCode() != null) {
                if (Build.VERSION.SDK_INT >= 17) {
                    Locale messageLocale = new Locale(message.getLanguageCode());
                    viewHolder.messageText.setTextLocale(messageLocale);
                }
            }

            viewHolder.messageText.setText(message.getMessage());
          //  Toast.makeText(getActivity().getApplicationContext(),message.getMessage(),Toast.LENGTH_LONG).show();
           // viewHolder.messageText.setMovementMethod(new ScrollingMovementMethod());
            if (message instanceof SendableMessage) {
                SendableMessage sendableMessage = (SendableMessage) message;
                if (sendableMessage.getStatus() != null) {
                    viewHolder.messageStatus.setText(sendableMessage.getStatus());
                } else {
                    if (sendableMessage.getCreated() != null) {
                        viewHolder.messageStatus.setText(android.text.format.DateUtils.formatDateTime(getActivity(), message.getCreated().getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
                    }
                }
            } else {
                if (application.getScannedSettings().getUserName().equals(message.getUsername())) {
                    viewHolder.messageStatus.setText(android.text.format.DateUtils.formatDateTime(getActivity(), message.getCreated().getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));

                } else {
                    viewHolder.messageStatus.setText(message.getUsername() + " | " +
                                    android.text.format.DateUtils.formatDateTime(getActivity(), message.getCreated().getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME)
                    );
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            try {
                if (chatManager.getChat(contact).getMessages().get(position).getUsername().equals(application.getScannedSettings().getUserName())) {
                    return 1;
                }
            } catch (Exception e) {
                return 0;
            }
            return 0;
        }

        @Override
        public int getItemCount() {
            try {
                return chatManager.getChat(contact).getMessages().size();
            } catch (Exception e) {
                return 0;
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            // each data item is just a string in this case
            public TextView messageStatus;
            public TextView messageText;

            public ViewHolder(View messageView) {
                super(messageView);
                messageView.setClickable(true);
                messageView.setOnClickListener(this);
                messageText = (TextView) messageView.findViewById(R.id.message);
                messageStatus = (TextView) messageView.findViewById(R.id.message_status);
            }


            @Override
            public void onClick(View v) {
            }
        }

    }
}
