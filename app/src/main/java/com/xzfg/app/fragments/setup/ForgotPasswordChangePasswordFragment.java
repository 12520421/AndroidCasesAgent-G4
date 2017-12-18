package com.xzfg.app.fragments.setup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.fragments.dialogs.RegistrationProgressDialogFragment;
import com.xzfg.app.services.LoginService;
import com.xzfg.app.services.ProfileService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class ForgotPasswordChangePasswordFragment extends Fragment implements ConnectionAwareness {

    private View loginButton;
    private EditText passwordInput;
    private CheckBox showPasswordCheck;
    private AlertDialogFragment alertDialogFragment;
    private RegistrationProgressDialogFragment registrationProgress;
    private String url;
    private Long port;
    private String username;
    private String code;

    private static final String DIALOG_TAG = ForgotPasswordChangePasswordFragment.class + "_DIALOG";

    @Inject
    public Application application;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            ((Application) ((Activity) context).getApplication()).inject(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_forgot_password_change_password, container, false);

        loginButton = v.findViewById(R.id.login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Change password
                validate();
            }
        });

        passwordInput = (EditText) v.findViewById(R.id.password);
        showPasswordCheck = (CheckBox) v.findViewById(R.id.show_password);
        showPasswordCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                passwordInput.setTransformationMethod(isChecked ? null : new PasswordTransformationMethod());
            }
        });

        Bundle args = getArguments();
        if (args != null) {
            url = args.getString(Givens.ARG_URL);
            port = args.getLong(Givens.ARG_PORT);
            username = args.getString(Givens.ARG_USERNAME);
            code = args.getString(Givens.ARG_CODE);
        }

        return v;
    }

    @Override
    public void onResume() {
        EventBus.getDefault().registerSticky(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        if (application.getAgentSettings() != null && application.getAgentSettings().getBoss() == 1) {
            clearDialogs();
        }
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void connectionLost() {
        loginButton.setEnabled(false);
    }

    @Override
    public void connectionGained() {
        loginButton.setEnabled(true);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.ForgotPasswordPasswordChanged event) {
        //Timber.d("Changed password.");
        clearDialogs();

        if (event.getStatus()) {
            registrationProgress = RegistrationProgressDialogFragment.newInstance(getString(R.string.registering), getString(R.string.registration_in_progress));
            registrationProgress.show(getFragmentManager(), DIALOG_TAG);

            // Login
            LoginService.login(
                    getActivity(),
                    url,
                    Long.toString(port),
                    username,
                    passwordInput.getText().toString()
            );
        } else {
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.forgot_password), event.getError());
            alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
        }

    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.Registration registration) {
        EventBus.getDefault().removeStickyEvent(registration);
        if (registration.getStatus()) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
            getActivity().finish();
        }
    }

    private void clearDialogs() {
        if (registrationProgress != null) {
            registrationProgress.dismiss();
            registrationProgress = null;
        }
        if (alertDialogFragment != null) {
            alertDialogFragment.dismiss();
        }
    }

    private void validate() {
        String password = passwordInput.getText().toString();

        clearDialogs();

        if (password.trim().length() > 7) {
            registrationProgress = RegistrationProgressDialogFragment.newInstance(getString(R.string.registering), "Changing password...");
            registrationProgress.show(getFragmentManager(), DIALOG_TAG);

            // Change password
            ProfileService.forgotPasswordChangePassword(getActivity(), url, port, username, code, password);
        } else {
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.forgot_password), getString(R.string.password_must_be_8_characters));
            alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
        }
    }

}
