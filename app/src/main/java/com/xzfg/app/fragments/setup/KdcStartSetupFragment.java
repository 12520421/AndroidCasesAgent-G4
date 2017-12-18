package com.xzfg.app.fragments.setup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.activities.SetupActivity;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * This fragment is our registration screen.
 */
public class KdcStartSetupFragment extends Fragment implements ConnectionAwareness {
    @Inject
    Application application;
    @Inject
    SharedPreferences sharedPreferences;

    private Button scanButton;
    private Button createButton;
    private Button inviteButton;
    private Button cancelButton;
    private TextView networkError;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            ((Application) ((Activity) context).getApplication()).inject(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_kdc_setupactivity, viewGroup, false);
        //((TextView) rootView.findViewById(R.id.app_version)).setText(BuildConfig.VERSION_NAME + "-" + BuildConfig.FLAVOR.toUpperCase());
        networkError = (TextView) rootView.findViewById(R.id.network_error);

        cancelButton = (Button) rootView.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        if (!application.isSetupComplete()) {
            cancelButton.setVisibility(View.GONE);
        }

        scanButton = (Button) rootView.findViewById(R.id.btn_scan_settings);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFragmentToStack(new ScanFragment());
            }
        });
        if (!getResources().getBoolean(R.bool.enable_reg_barcode)) {
            scanButton.setVisibility(View.GONE);
        }

        createButton = (Button) rootView.findViewById(R.id.btn_create_account);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFragmentToStack(new SignupFragment());
            }
        });
        if (!getResources().getBoolean(R.bool.enable_reg_signup)) {
            createButton.setVisibility(View.GONE);
        }

        inviteButton = (Button) rootView.findViewById(R.id.btn_invite);
        inviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFragmentToStack(new InviteFragment());
            }
        });
        if (!getResources().getBoolean(R.bool.enable_reg_invite)) {
            inviteButton.setVisibility(View.GONE);
        }

        // Hide tagline if it is empty
        TextView taglineView = (TextView) rootView.findViewById(R.id.registration_tagline);
        if (taglineView.getText().toString().trim().isEmpty()) {
            taglineView.setVisibility(View.GONE);
        }

        // Setup links at the bottom of the screen
        TextView links = (TextView) rootView.findViewById(R.id.registration_links);
        String linksText = getString(R.string.registration_links).trim();
        if (!linksText.isEmpty()) {
            links.setMovementMethod(LinkMovementMethod.getInstance());
            links.setText(addClickableLinks(linksText), TextView.BufferType.SPANNABLE);
        }

        // Top icon
        rootView.findViewById(R.id.registration_icon).setVisibility(
                getResources().getBoolean(R.bool.enable_reg_icon) ? View.VISIBLE : View.GONE
        );
        // Background
        if (getResources().getBoolean(R.bool.enable_reg_background)) {
            rootView.setBackgroundResource(R.drawable.registration_background);
        }
        //hide qwerty keyboard
        getActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
        return rootView;
    }

    private void addFragmentToStack(Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack("welcome");
        fragmentTransaction.replace(R.id.setup_container, fragment, SetupActivity.FRAGMENT_TAG);
        fragmentTransaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
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
            createButton.setEnabled(false);
            inviteButton.setEnabled(false);
        }
        if (networkError != null)
            networkError.setVisibility(View.VISIBLE);
    }

    @Override
    public void connectionGained() {
        if (scanButton != null) {
            scanButton.setEnabled(true);
            createButton.setEnabled(true);
            inviteButton.setEnabled(true);
        }
        if (networkError != null)
            networkError.setVisibility(View.GONE);
    }

    private void onClickableLink(String linkTag) {
        switch (linkTag) {
            case "tos":
                addFragmentToStack(new TOSViewerFragment());
                break;
            case "privacy":
                addFragmentToStack(new PrivacyPolicyViewerFragment());
                break;
            case "coppa":
                addFragmentToStack(new COPPAPolicyViewerFragment());
                break;
            case "login":
                addFragmentToStack(new LoginFragment());
                break;
        }

    }

    private SpannableStringBuilder addClickableLinks(String text) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        int idx1 = text.indexOf("[");
        int idx2 = 0;
        int deleted = 0;

        while (idx1 != -1) {
            idx2 = text.indexOf("]", idx1) + 1;
            String[] link = text.substring(idx1 + 1, idx2 - 1).split(",");
            final String linkTag = link[1];
            ssb.replace(idx1 - deleted, idx2 - deleted, link[0]);
            ssb.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    onClickableLink(linkTag.toLowerCase());
                }
            }, idx1 - deleted, idx2 - linkTag.length() - deleted - 3, 0);
            idx1 = text.indexOf("[", idx2);
            deleted += (linkTag.length() + 3);
        }

        return ssb;
    }
}
