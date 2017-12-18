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
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.fragments.dialogs.RegistrationProgressDialogFragment;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.services.InviteService;
import com.xzfg.app.services.RegistrationService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import timber.log.Timber;

public class InviteFragment extends Fragment implements ConnectionAwareness {

    public Button registerButton;
    public EditText inviteCodeInput;
    private AlertDialogFragment alertDialogFragment;
    private RegistrationProgressDialogFragment registrationProgress;

    private static final String DIALOG_TAG = InviteFragment.class + "_DIALOG";

    @Inject
    public Application application;
    @Inject
    Crypto crypto;

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
        View rootView = inflater.inflate(R.layout.fragment_invite, container, false);
        rootView.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dismiss keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && getActivity().getCurrentFocus() != null) {
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                }
                getFragmentManager().popBackStackImmediate();
            }
        });
        registerButton = (Button) rootView.findViewById(R.id.action);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validate();
            }
        });

        inviteCodeInput = (EditText) rootView.findViewById(R.id.invite_code);

        return rootView;
    }

    private void validate() {
        String inviteCode = inviteCodeInput.getText().toString().trim();

        if (inviteCode.length() > 0) {
            InviteService.getInviteData(getActivity(), inviteCode);
        } else {
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

    public void onEventMainThread(Events.InviteDataReceived event) {
        if (event.isStatus() && event.getMessage() != null && !event.getMessage().isEmpty()) {
            // Received invite data by code. Continue registration
            try {
                Result result = new Result();
                result.setBarcodeFormat(BarcodeFormat.QRCODE);
                result.setContents(event.getMessage());

                final ScannedSettings settings = ScannedSettings.parse(application, result);
                settings.validateCrypto(application, crypto);
                EventBus.getDefault().postSticky(new Events.ScanSuccess(settings));

                RegistrationProgressDialogFragment successDialog = RegistrationProgressDialogFragment.newInstance(getString(R.string.success_title), getString(R.string.invite_success));

                Intent registrationIntent = new Intent(application, RegistrationService.class);
                registrationIntent.putExtra(ScannedSettings.class.getName(), settings);
                application.startService(registrationIntent);

                successDialog.show(getFragmentManager(), "invite_success");
            } catch (ScannedSettings.InvalidScanException invalidScanException) {
                Timber.e("Error parsing scan!", invalidScanException);
                AlertDialogFragment errorDialog = AlertDialogFragment.newInstance(getString(R.string.error_title), invalidScanException.getMessage());
                errorDialog.show(getFragmentManager(), "invite_error");
            }
        } else {
            AlertDialogFragment errorDialog = AlertDialogFragment.newInstance(getString(R.string.error_title), event.getMessage());
            errorDialog.show(getFragmentManager(), "invite_error");
        }
    }
}
