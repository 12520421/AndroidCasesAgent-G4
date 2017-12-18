package com.xzfg.app.fragments.setup.resetup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.activities.SetupActivity;
import com.xzfg.app.fragments.setup.ConnectionAwareness;
import com.xzfg.app.fragments.setup.CreateAccountFragment;
import com.xzfg.app.fragments.setup.InviteFragment;
import com.xzfg.app.fragments.setup.LoginFragment;

import de.greenrobot.event.EventBus;

/**
 * This fragment is our startup screen.
 */
public class ReStartSetupFragment extends Fragment implements ConnectionAwareness {

    private Button scanButton;
    private Button createButton;
    private Button loginButton;
    private Button inviteButton;
    private TextView networkError;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        EventBus.getDefault().post(new Events.DisplayChanged("Settings", R.id.settings));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_resetupactivity, viewGroup, false);
        rootView.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStackImmediate();
            }
        });
        Application application = (Application) getActivity().getApplication();
        ((TextView) rootView.findViewById(R.id.serial)).setText(getString(R.string.serial, application.getScannedSettings().getOrganizationId() + application.getDeviceIdentifier()));
        ((TextView) rootView.findViewById(R.id.app_version)).setText(BuildConfig.VERSION_NAME + "-" + BuildConfig.FLAVOR.toUpperCase());

        networkError = (TextView) rootView.findViewById(R.id.network_error);
        scanButton = (Button) rootView.findViewById(R.id.btn_scan_settings);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.agent_container, new ReScanFragment(), SetupActivity.FRAGMENT_TAG);
                fragmentTransaction.commitAllowingStateLoss();
            }
        });

        createButton = (Button) rootView.findViewById(R.id.btn_create_account);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.agent_container, new CreateAccountFragment(), SetupActivity.FRAGMENT_TAG);
                fragmentTransaction.commitAllowingStateLoss();
            }
        });

        loginButton = (Button) rootView.findViewById(R.id.btn_login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.agent_container, new LoginFragment(), SetupActivity.FRAGMENT_TAG);
                fragmentTransaction.commitAllowingStateLoss();
            }
        });

        inviteButton = (Button) rootView.findViewById(R.id.btn_invite);
        inviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.agent_container, new InviteFragment(), SetupActivity.FRAGMENT_TAG);
                fragmentTransaction.commitAllowingStateLoss();
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        EventBus.getDefault().registerSticky(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
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

    @Override
    public void connectionLost() {
        if (scanButton != null) {
            scanButton.setEnabled(false);
            inviteButton.setEnabled(false);
        }
        if (networkError != null)
            networkError.setVisibility(View.VISIBLE);
    }

    @Override
    public void connectionGained() {
        if (scanButton != null) {
            scanButton.setEnabled(true);
            inviteButton.setEnabled(true);
        }
        if (networkError != null)
            networkError.setVisibility(View.GONE);
    }
}
