package com.xzfg.app.fragments;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.adapters.AgentContactsAdapter;
import com.xzfg.app.model.AgentContacts;
import com.xzfg.app.services.ProfileService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;


public class ContactsFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {
    private ListView mContactsListView;
    private View mContactsLayout;
    private View mAddContactLayout;
    private TextView mContactId;
    private EditText mNameInput;
    private EditText mEmailInput;
    private EditText mMobileInput;
    private Switch mFamilyMemberSwitch;
    private Button mDeleteContactButton;
    private AgentContactsAdapter mContactsListAdapter = null;

    @Inject
    Application application;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Application) activity.getApplication()).inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_contacts, viewGroup, false);

        mContactsLayout = v.findViewById(R.id.contacts_layout);
        mAddContactLayout = v.findViewById(R.id.add_contact_layout);
        v.findViewById(R.id.add).setOnClickListener(this);
        v.findViewById(R.id.done).setOnClickListener(this);
        v.findViewById(R.id.cancel).setOnClickListener(this);
        mDeleteContactButton = (Button) v.findViewById(R.id.delete_contact);
        mDeleteContactButton.setOnClickListener(this);

        mContactId = (TextView) v.findViewById(R.id.hidden_contact_id);
        mNameInput = (EditText) v.findViewById(R.id.name);
        mEmailInput = (EditText) v.findViewById(R.id.email);
        mMobileInput = (EditText) v.findViewById(R.id.mobile);
        mFamilyMemberSwitch = (Switch) v.findViewById(R.id.family_member_switch);

        mContactsListView = (ListView) v.findViewById(android.R.id.list);
        mContactsListView.setOnItemClickListener(this);

        AgentContacts contacts = application.getAgentContacts();
        if (contacts != null && contacts.getContacts() != null) {
            String email = application.getAgentProfile() == null ? "" : application.getAgentProfile().getEmailAddress();
            // There was a case when user had no email assigned it caused crash
            if (email == null) email = "";
            mContactsListAdapter = new AgentContactsAdapter(getActivity(), contacts.getContacts(), email);
            mContactsListView.setAdapter(mContactsListAdapter);
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = ((ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    public void onEventMainThread(Events.AgentContactsAcquired event) {
        AgentContacts contacts = event.getAgentContacts();
        if (contacts != null && contacts.getContacts() != null) {
            String email = application.getAgentProfile() == null ? "" : application.getAgentProfile().getEmailAddress();
            if (email == null) email = "";
            mContactsListAdapter = new AgentContactsAdapter(getActivity(), contacts.getContacts(), email);
            mContactsListView.setAdapter(mContactsListAdapter);
        }
    }

    private void hideAddContactLayout(boolean hide) {
        if (hide) {
            // Hide add-contact UI and show contacts list
            mAddContactLayout.setVisibility(View.GONE);
            mContactsLayout.setVisibility(View.VISIBLE);
        } else {
            // Hide contacts list and show add-contact UI
            mContactsLayout.setVisibility(View.GONE);
            mAddContactLayout.setVisibility(View.VISIBLE);
        }
    }

    private void fillEditorFromContact(AgentContacts.AgentContact contact) {
        mContactId.setText(contact.getId());
        mNameInput.setText(contact.getName());
        mEmailInput.setText(contact.getEmailAddress());
        mMobileInput.setText(contact.getPhoneNumber());
        mFamilyMemberSwitch.setChecked(contact.isFamilyMember());
    }

    private AgentContacts.AgentContact getContactFromEditor() {
        return new AgentContacts.AgentContact(
                mContactId.getText().toString(),
                mNameInput.getText().toString(),
                mEmailInput.getText().toString(),
                mMobileInput.getText().toString(),
                mFamilyMemberSwitch.isChecked()
        );
    }

    private void clearEditor() {
        mContactId.setText(null);
        mNameInput.getText().clear();
        mEmailInput.getText().clear();
        mMobileInput.getText().clear();
        mFamilyMemberSwitch.setChecked(false);
    }

    private boolean validate(AgentContacts.AgentContact contact) {
        if (contact == null || contact.getName().isEmpty() || contact.getEmailAddress().isEmpty() || contact.getPhoneNumber().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        AgentContacts contacts = application.getAgentContacts();
        AgentContacts.AgentContact contact = getContactFromEditor();
        String contactId = mContactId.getText().toString();

        switch (v.getId()) {
            case R.id.add:
                if (isConnected()) {
                    mDeleteContactButton.setVisibility(View.GONE);
                    clearEditor();
                    hideAddContactLayout(false);
                }
                break;

            case R.id.done:
                if (!validate(contact)) {
                    Toast.makeText(getActivity(), R.string.fill_in_required_fields, Toast.LENGTH_SHORT).show();
                    break;
                }
                hideAddContactLayout(true);
                if (contactId.isEmpty()) {
                    // Add new contact to adapter and global contact list
                    Timber.d("Sending new contact to server.");
                    contacts.getContacts().add(contact);
                    mContactsListAdapter.add(contact);
                }
                else {
                    Timber.d("Sending updated contact to server.");
                    // Update global contact list
                    for (AgentContacts.AgentContact c : contacts.getContacts()) {
                        if (contactId.equals(c.getId())) {
                            c.set(contact);
                            break;
                        }
                    }
                    // Update list adapter
                    for (int i = 0; i < mContactsListAdapter.getCount(); i++) {
                        AgentContacts.AgentContact c = mContactsListAdapter.getItem(i);
                        if (contactId.equals(c.getId())) {
                            c.set(contact);
                            break;
                        }
                    }
                }
                mContactsListAdapter.notifyDataSetChanged();
                clearEditor();
                // Save contacts in local preferences
                application.setAgentContacts(contacts);
                // Send contacts to server
                ProfileService.postAgentContacts(getActivity(), contacts);
                break;

            case R.id.cancel:
                hideAddContactLayout(true);
                clearEditor();
                break;

            case R.id.delete_contact:
                Timber.d("Deleting contact on server.");
                // Create copy of contacts to send to the server
                AgentContacts newContacts = new AgentContacts(contacts);
                for (AgentContacts.AgentContact c : newContacts.getContacts()) {
                    if (contactId.equals(c.getId())) {
                        c.setName("");
                        break;
                    }
                }
                // Update global contact list
                for (AgentContacts.AgentContact c : contacts.getContacts()) {
                    if (contactId.equals(c.getId())) {
                        contacts.getContacts().remove(c);
                        break;
                    }
                }
                // Update list adapter
                for (int i = 0; i < mContactsListAdapter.getCount(); i++) {
                    AgentContacts.AgentContact c = mContactsListAdapter.getItem(i);
                    if (contactId.equals(c.getId())) {
                        mContactsListAdapter.remove(c);
                        break;
                    }
                }
                hideAddContactLayout(true);
                mContactsListAdapter.notifyDataSetChanged();
                clearEditor();
                // Save contacts in local preferences
                application.setAgentContacts(contacts);
                // Send contacts to server
                ProfileService.postAgentContacts(getActivity(), newContacts);
                break;

            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        if (mContactsListAdapter != null && isConnected()) {
            mDeleteContactButton.setVisibility(View.VISIBLE);
            fillEditorFromContact(mContactsListAdapter.getItem(position));
            hideAddContactLayout(false);
        }
    }
}
