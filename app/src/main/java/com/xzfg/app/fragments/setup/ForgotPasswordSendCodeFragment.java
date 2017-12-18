package com.xzfg.app.fragments.setup;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.SetupActivity;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.services.ProfileService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class ForgotPasswordSendCodeFragment extends Fragment implements ConnectionAwareness {

    private View nextButton;
    private EditText urlInput;
    private EditText portInput;
    private EditText usernameInput;
    private AlertDialogFragment alertDialogFragment;

    private static final String DIALOG_TAG = ForgotPasswordSendCodeFragment.class + "_DIALOG";

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
        View v = inflater.inflate(R.layout.fragment_forgot_password_send_code, container, false);
        v.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
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

        nextButton = v.findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = urlInput.getText().toString();
                String port = portInput.getText().toString();
                String username = usernameInput.getText().toString();

                // Validate fields
                if (url.length() < 15 || port.length() < 4 || username.length() < 8) {
                    showAlert(getResources().getString(R.string.fill_in_required_fields));
                    return;
                }

                // Request 6-digit code
                ProfileService.forgotPasswordSendCode(getActivity(), url, Long.parseLong(port), username);
            }
        });

        urlInput = (EditText) v.findViewById(R.id.url);
        portInput = (EditText) v.findViewById(R.id.port);
        usernameInput = (EditText) v.findViewById(R.id.user);

        if (BuildConfig.DEFAULT_URL.length() > 0) {
            urlInput.setVisibility(View.GONE);
            portInput.setVisibility(View.GONE);
            v.findViewById(R.id.urlText).setVisibility(View.GONE);
            v.findViewById(R.id.portText).setVisibility(View.GONE);
            urlInput.setText(BuildConfig.DEFAULT_URL);
        }
        portInput.setText(String.valueOf(BuildConfig.DEFAULT_PORT));

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
            if (alertDialogFragment != null) {
                alertDialogFragment.dismiss();
                alertDialogFragment = null;
            }
        }
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void connectionLost() {
        nextButton.setEnabled(false);
    }

    @Override
    public void connectionGained() {
        nextButton.setEnabled(true);
    }

    private void showAlert(String message) {
        if (alertDialogFragment != null) {
            alertDialogFragment.dismiss();
        }
        alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.forgot_password), message);
        alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.ForgotPasswordCodeReceived event) {
        //Timber.d("Received password code.");
        if (event.getStatus()) {
            // Open "Verify code" fragment
            ForgotPasswordVerifyCodeFragment fragment  = new ForgotPasswordVerifyCodeFragment();
            Bundle args = new Bundle();
            args.putString(Givens.ARG_URL, urlInput.getText().toString());
            args.putLong(Givens.ARG_PORT, Long.parseLong(portInput.getText().toString()));
            args.putString(Givens.ARG_USERNAME, usernameInput.getText().toString());
            fragment.setArguments(args);

            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.addToBackStack("welcome");
            fragmentTransaction.replace(R.id.setup_container, fragment, SetupActivity.FRAGMENT_TAG);
            fragmentTransaction.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        } else {
            showAlert(event.getError());
        }
    }
}
