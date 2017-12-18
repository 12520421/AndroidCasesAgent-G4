package com.xzfg.app.fragments.setup;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
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
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.SetupActivity;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.services.ProfileService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class ForgotPasswordVerifyCodeFragment extends Fragment implements ConnectionAwareness {

    private View nextButton;
    private EditText codeInput;
    private AlertDialogFragment alertDialogFragment;
    private String url;
    private Long port;
    private String username;

    private static final String DIALOG_TAG = ForgotPasswordVerifyCodeFragment.class + "_DIALOG";

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
        View v = inflater.inflate(R.layout.fragment_forgot_password_verify_code, container, false);
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
                // Verify 6-digit code
                ProfileService.forgotPasswordVerifyCode(
                        getActivity(), url, port, username, codeInput.getText().toString());
            }
        });

        codeInput = (EditText) v.findViewById(R.id.code);

        Bundle args = getArguments();
        if (args != null) {
            url = args.getString(Givens.ARG_URL);
            port = args.getLong(Givens.ARG_PORT);
            username = args.getString(Givens.ARG_USERNAME);
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

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.ForgotPasswordCodeVerified event) {
        //Timber.d("Verified password code.");
        if (event.getStatus()) {
            // Open "Change password" fragment
            ForgotPasswordChangePasswordFragment fragment  = new ForgotPasswordChangePasswordFragment();
            Bundle args = new Bundle();
            args.putString(Givens.ARG_URL, url);
            args.putLong(Givens.ARG_PORT, port);
            args.putString(Givens.ARG_USERNAME, username);
            args.putString(Givens.ARG_CODE, codeInput.getText().toString());
            fragment.setArguments(args);

            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.addToBackStack("welcome");
            fragmentTransaction.replace(R.id.setup_container, fragment, SetupActivity.FRAGMENT_TAG);
            fragmentTransaction.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        } else {
            if (alertDialogFragment != null) {
                alertDialogFragment.dismiss();
            }
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.forgot_password), event.getError());
            alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
            alertDialogFragment.setCustomCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    dialog.cancel();
                }
            });
        }
    }
}
