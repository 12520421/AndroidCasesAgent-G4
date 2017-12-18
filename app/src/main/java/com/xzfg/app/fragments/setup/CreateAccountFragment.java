package com.xzfg.app.fragments.setup;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.fragments.dialogs.RegistrationProgressDialogFragment;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.services.CreateAccountService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class CreateAccountFragment extends Fragment implements ConnectionAwareness {

    public Button registerButton;
    public EditText urlInput;
    public EditText portInput;
    public EditText orgNameInput;
    public EditText nameInput;
    public EditText usernameInput;
    public EditText passwordInput;
    public EditText confirmPasswordInput;
    public EditText phoneNumberInput;
    public EditText emailInput;
    private AlertDialogFragment alertDialogFragment;
    private RegistrationProgressDialogFragment registrationProgress;

    private static final String DIALOG_TAG = CreateAccountFragment.class + "_DIALOG";

    @Inject
    public Application application;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Application) activity.getApplication()).inject(this);
    }

    @Override
    public void onResume() {
        EventBus.getDefault().registerSticky(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        if (application.getAgentSettings() != null && application.getAgentSettings().getBoss() == 1) {
            if (registrationProgress != null) {
                registrationProgress.dismiss();
                registrationProgress = null;
            }
            if (alertDialogFragment != null) {
                alertDialogFragment.dismiss();
                alertDialogFragment = null;
            }
        }
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_createaccount,container,false);
        rootView.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            // Dismiss keyboard
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getActivity().getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);}
            getFragmentManager().popBackStackImmediate();
            }
        });
        registerButton = (Button)rootView.findViewById(R.id.action);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validate();
            }
        });

        urlInput = (EditText)rootView.findViewById(R.id.url);

        portInput = (EditText)rootView.findViewById(R.id.port);

        if (BuildConfig.DEFAULT_URL.length() > 0) {
            urlInput.setVisibility(View.GONE);
            portInput.setVisibility(View.GONE);
            rootView.findViewById(R.id.urlText).setVisibility(View.GONE);
            rootView.findViewById(R.id.portText).setVisibility(View.GONE);
            urlInput.setText(BuildConfig.DEFAULT_URL);
        }

        portInput.setText(String.valueOf(BuildConfig.DEFAULT_PORT));

        orgNameInput = (EditText)rootView.findViewById(R.id.org);
        if (BuildConfig.DEFAULT_ORG.length() > 0) {
            orgNameInput.setVisibility(View.GONE);
            rootView.findViewById(R.id.orgText).setVisibility(View.GONE);
            orgNameInput.setText(String.valueOf(BuildConfig.DEFAULT_ORG));
        }

        nameInput = (EditText)rootView.findViewById(R.id.name);
        usernameInput = (EditText)rootView.findViewById(R.id.user);
        passwordInput = (EditText)rootView.findViewById(R.id.password);
        confirmPasswordInput = (EditText)rootView.findViewById(R.id.confpassword);
        emailInput = (EditText)rootView.findViewById(R.id.email);
        phoneNumberInput = (EditText)rootView.findViewById(R.id.phone);

        return rootView;
    }

    private void validate() {
        String url = urlInput.getText().toString().trim();
        String port = portInput.getText().toString().trim();
        String orgName = orgNameInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        String email = emailInput.getText().toString();
        String phone = phoneNumberInput.getText().toString();

        if (password.trim().length() > 0 && confirmPassword.trim().length() > 0 && url.length() > 0 && port.length() > 0 &&
                orgName.length() > 0 && username.length() > 0 && username.length() > 0) {
            if (password.equals(confirmPassword)) {

                registrationProgress = RegistrationProgressDialogFragment.newInstance(getString(R.string.registering), getString(R.string.registration_in_progress));
                registrationProgress.show(getFragmentManager(),DIALOG_TAG);
                CreateAccountService.createAccount(
                        getActivity(),
                        url,
                        port,
                        orgName,
                        name,
                        username,
                        password,
                        email,
                        phone
                );
            }
            else {
                if (alertDialogFragment != null) {
                    alertDialogFragment.dismiss();
                }
                alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.password_confirmation), getString(R.string.password_not_confirmed));
                alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
            }
        }
        else {
            if (alertDialogFragment != null) {
                alertDialogFragment.dismiss();
            }
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.invalid_registration), getString(R.string.fill_in_required_fields));
            alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
        }

    }

    @Override
    public void connectionLost() {
        registerButton.setEnabled(false);
    }

    @Override
    public void connectionGained() {
        registerButton.setEnabled(true);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.Registration registration) {
        //Timber.d("Registration received");
        if (registrationProgress != null) {
            registrationProgress.dismiss();
            registrationProgress = null;
        }
        if (alertDialogFragment != null) {
            alertDialogFragment.dismiss();
            alertDialogFragment = null;
        }

        if (registration.getStatus()) {
            final AgentSettings agentSettings = registration.getAgentSettings();
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.success_title), registration.getMessage());
            alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
            alertDialogFragment.setCustomCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Givens.EXTRA_AGENT_SETTINGS, agentSettings);
                    getActivity().startActivity(intent);
                    getActivity().finish();
                }
            });

        } else {
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.error_title), registration.getMessage());
            alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
        }
        EventBus.getDefault().removeStickyEvent(registration);
    }
}
