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
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.fragments.BaseWebViewFragment;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.services.ProfileService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class PaymentFragment extends Fragment implements ConnectionAwareness {

    private WebView webView;
    private View nextButton;
    private AlertDialogFragment alertDialogFragment;
    private AgentSettings agentSettings = null;

    private static final String DIALOG_TAG = PaymentFragment.class + "_DIALOG";
    public static final String ARG_AGENT_SETTINGS = "ARG_AGENT_SETTINGS";

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
        View v = inflater.inflate(R.layout.fragment_payment, container, false);

        Bundle args = getArguments();
        if (args != null) {
            agentSettings = args.getParcelable(PaymentFragment.ARG_AGENT_SETTINGS);
        }

        nextButton = v.findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verify subscription
                ProfileService.verifySubscription(getActivity(), application.getScannedSettings().getUserId(), agentSettings);
            }
        });

        FrameLayout frame = (FrameLayout) v.findViewById(R.id.frame);
        webView = BaseWebViewFragment.createBaseWebView(getActivity());
        frame.addView(webView);
        webView.loadUrl(getString(R.string.payment_url) + application.getScannedSettings().getUserId());

        return v;
    }

    @Override
    public void onResume() {
        EventBus.getDefault().registerSticky(this);
        webView.onResume();
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
        webView.stopLoading();
        webView.onPause();
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
    public void onEventMainThread(Events.SubscriptionVerified event) {
        //Timber.d("Subscription verified.");
        if (event.isValid()) {
            // Open main activity
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Givens.EXTRA_AGENT_SETTINGS, event.getAgentSettings());
            getActivity().startActivity(intent);
            getActivity().finish();
        } else {
            if (alertDialogFragment != null) {
                alertDialogFragment.dismiss();
            }
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.subscription), getString(R.string.invalid_subscription));
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
