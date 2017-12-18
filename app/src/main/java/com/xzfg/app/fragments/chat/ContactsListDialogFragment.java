package com.xzfg.app.fragments.chat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.model.User;
import com.xzfg.app.services.ChatService;
import com.xzfg.app.widgets.DividerItemDecoration;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 */
public class ContactsListDialogFragment extends DialogFragment implements DialogInterface.OnCancelListener {
    // holds our custom cancel listener.
    private DialogInterface.OnCancelListener customCancelListener;
    private DialogInterface.OnDismissListener customDismissListener;
    private EditText searchBox;
    private RecyclerView recyclerView;
    private ContactsListAdapter contactsListAdapter = new ContactsListAdapter();


    public static ContactsListDialogFragment newInstance() {
        ContactsListDialogFragment f = new ContactsListDialogFragment();
        f.contactsListAdapter.setHasStableIds(false);
        return f;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
        onEventMainThread(EventBus.getDefault().getStickyEvent(Events.ContactsLoaded.class));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // ask for fresh chat users list.
        getActivity().startService(new Intent(getActivity(), ChatService.class).setAction(Givens.ACTION_CHAT_LIST_USERS));
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.ContactsLoaded event) {
        if (event != null) {
            contactsListAdapter.update(event.getContacts());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        Resources res = getActivity().getResources();
        int color = ContextCompat.getColor(getContext(),R.color.redorange);
        int titleId = res.getIdentifier("alertTitle", "id", "android");
        int dividerId = res.getIdentifier("titleDivider", "id", "android");
        //int progressId = res.getIdentifier("progress","id","android");

        TextView titleView = (TextView) dialog.findViewById(titleId);
        if (titleView != null) {
            titleView.setTextColor(color);
        }
        View dividerView = dialog.findViewById(dividerId);
        if (dividerView != null) {
            dividerView.setBackgroundColor(color);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View view = getActivity().getLayoutInflater().inflate(R.layout.contacts, null, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.contact_list);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(contactsListAdapter);
        searchBox = (EditText) view.findViewById(R.id.search_box);
        TextWatcher searchWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                contactsListAdapter.filter(s.toString());
            }
        };
        searchBox.addTextChangedListener(searchWatcher);
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(getString(R.string.contacts))
                .setIcon(R.drawable.contacts_dark)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
    }
    /*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        return view;
    }
    */


    /**
     * This overridden implementation calls our DialogInterface.OnCancelListener.
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        if (this.customCancelListener != null) {
            this.customCancelListener.onCancel(dialog);
        }
        super.onCancel(dialog);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (this.customDismissListener != null) {
            this.customDismissListener.onDismiss(dialog);
        }
        super.onDismiss(dialog);
    }

    /**
     * Allows setting a custom onCancelListener, since the DialogFragment takes
     * control of the AlertDialog's implementation.
     */
    public void setCustomCancelListener(DialogInterface.OnCancelListener cancelListener) {
        this.customCancelListener = cancelListener;
    }

    /**
     * Allows setting a custom onDismissListener, since the DialogFragment takes
     * control of the AlertDialog's implementation.
     */
    public void setCustomDismissListener(DialogInterface.OnDismissListener dismissListener) {
        this.customDismissListener = dismissListener;
    }

    /**
     * Overrides the default implementation issuing an immediate call to manager.executePendingTransactions()
     */
    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
        manager.executePendingTransactions();
    }


    private class ContactsListAdapter extends RecyclerView.Adapter<ContactsListAdapter.ViewHolder> {

        public LinkedList<User> users = new LinkedList<>();
        public LinkedList<User> filtered = new LinkedList<>();
        public String filterString = null;

        public void filter(String filter) {
            if (users.isEmpty()) {
                return;
            }
            if (filter == null || filter.trim().length() == 0) {
                filterString = null;
                filtered.clear();
                filtered.addAll(users);
            } else {
                filtered.clear();
                filterString = filter.trim().toUpperCase();
                for (User user : users) {
                    if (user.getUsername().toUpperCase().contains(filterString)) {
                        filtered.add(user);
                    }
                }
                Collections.sort(filtered);
            }

            notifyDataSetChanged();
        }

        public void update(List<User> newUsers) {

            // if the two lists have the same contents, we want to update.
            if (newUsers.containsAll(this.users) && this.users.containsAll(newUsers)) {
                for (User u : this.users) {
                    if (newUsers.contains(u)) {
                        int position = this.users.indexOf(u);
                        this.users.set(position, newUsers.get(newUsers.indexOf(u)));
                        this.notifyItemChanged(position);
                    }
                    return;
                }
            }

            this.users.clear();
            this.users.addAll(newUsers);
            filter(filterString);
        }

        @Override
        public ContactsListAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.contact, viewGroup, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ContactsListAdapter.ViewHolder viewHolder, int i) {
            if (i > filtered.size()) {
                return;
            }
            User user = filtered.get(i);
            if (user.isOnline()) {
                viewHolder.statusIndicator.setVisibility(View.VISIBLE);
            } else {
                viewHolder.statusIndicator.setVisibility(View.INVISIBLE);
            }
            viewHolder.contactName.setText(user.getUsername());
        }

        @Override
        public int getItemCount() {
            return filtered.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            // each data item is just a string in this case
            public ImageView statusIndicator;
            public TextView contactName;

            public ViewHolder(View contactView) {
                super(contactView);
                contactView.setClickable(true);
                contactView.setOnClickListener(this);
                statusIndicator = (ImageView) contactView.findViewById(R.id.online_status_indicator);
                contactName = (TextView) contactView.findViewById(R.id.contact_name);
            }


            @Override
            public void onClick(View v) {
                User selectedContact = filtered.get(recyclerView.getChildPosition(v));
                ContactsListDialogFragment.this.getDialog().cancel();
                ContactsListDialogFragment.this.getFragmentManager().executePendingTransactions();
                ContactsListDialogFragment.this.getChildFragmentManager().executePendingTransactions();
                EventBus.getDefault().post(new Events.StartChat(selectedContact));
            }
        }

    }

}
