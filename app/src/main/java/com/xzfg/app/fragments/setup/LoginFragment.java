package com.xzfg.app.fragments.setup;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.activities.SetupActivity;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.fragments.dialogs.RegistrationProgressDialogFragment;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.services.LoginService;
import com.xzfg.app.util.Network;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

public class LoginFragment extends Fragment {

    private Button registerButton;
    private View forgotPasswordButton;
    private EditText urlInput;
    private EditText portInput;
    private EditText usernameInput;
    private EditText passwordInput;
    private RegistrationProgressDialogFragment registrationProgress;
    private AlertDialogFragment alertDialogFragment;
    private static final String DIALOG_TAG = LoginFragment.class + "_DIALOG";

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
            try {
                if (registrationProgress != null) {
                    registrationProgress.dismissAllowingStateLoss();
                    registrationProgress = null;
                }
                if (alertDialogFragment != null) {
                    alertDialogFragment.dismissAllowingStateLoss();
                    alertDialogFragment = null;
                }
            } catch (NullPointerException npe) {
                // this shouldn't happen, but apparently sometimes it does.
                Timber.w("A dialog that shouldn't be null is null. But shouldn't be null.");
            }

        }
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);
        rootView.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStackImmediate();
            }
        });

        forgotPasswordButton = rootView.findViewById(R.id.btn_forgot_password);
        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.addToBackStack("welcome");
                fragmentTransaction.replace(R.id.setup_container, new ForgotPasswordSendCodeFragment(), SetupActivity.FRAGMENT_TAG);
                fragmentTransaction.commitAllowingStateLoss();
                fragmentManager.executePendingTransactions();
            }
        });
        registerButton = (Button) rootView.findViewById(R.id.action);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Network.isConnected(getActivity())) {
                    validate();
                } else {
                    showError(getString(R.string.network_error));
                }
            }
        });

        urlInput = (EditText) rootView.findViewById(R.id.url);
        portInput = (EditText) rootView.findViewById(R.id.port);

        if (BuildConfig.DEFAULT_URL.length() > 0) {
            urlInput.setVisibility(View.GONE);
            portInput.setVisibility(View.GONE);
            rootView.findViewById(R.id.urlText).setVisibility(View.GONE);
            rootView.findViewById(R.id.portText).setVisibility(View.GONE);
            urlInput.setText(BuildConfig.DEFAULT_URL);
        }
        portInput.setText(String.valueOf(BuildConfig.DEFAULT_PORT));

        usernameInput = (EditText) rootView.findViewById(R.id.user);
        passwordInput = (EditText) rootView.findViewById(R.id.password);

        return rootView;
    }

    private void validate() {
        String url = urlInput.getText().toString().trim();
        String port = portInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (url.length() <= 0 || port.length() <= 0 || username.length() <= 0 || password.length() <= 0) {
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.invalid_registration), getString(R.string.fill_in_required_fields));
            alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
            return;
        } else {
            registrationProgress = RegistrationProgressDialogFragment.newInstance(getString(R.string.registering), getString(R.string.registration_in_progress));
            registrationProgress.show(getFragmentManager(), DIALOG_TAG);

            LoginService.login(
                    getActivity(),
                    url,
                    port,
                    username,
                    password
            );
        }
    }

    private void showError(String message) {
        if (registrationProgress != null) {
            registrationProgress.dismiss();
            registrationProgress = null;
        }

        if (alertDialogFragment != null) {
            alertDialogFragment.dismiss();
            alertDialogFragment = null;
        }

        alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.error_title), message);
        alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
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
            AlertDialogFragment errorDialog = AlertDialogFragment.newInstance(getString(R.string.error_title), registration.getMessage());
            errorDialog.show(getFragmentManager(), DIALOG_TAG);
        }
        EventBus.getDefault().removeStickyEvent(registration);

    }

}
