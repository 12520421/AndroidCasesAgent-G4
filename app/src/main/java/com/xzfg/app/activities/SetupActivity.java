package com.xzfg.app.activities;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.fragments.setup.ConnectionAwareness;
import com.xzfg.app.fragments.setup.KdcStartSetupFragment;
import com.xzfg.app.receivers.ConnectivityReceiver;
import com.xzfg.app.util.BatteryWhiteListUtil;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * This activity handles the initial setup.
 */
public class SetupActivity extends BaseActivity {

    public static final String FRAGMENT_TAG = "setup";
    private final ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setCustomView(R.layout.settings_actionbar);
        }

        setContentView(R.layout.activity_setupactivity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.setup_container, new KdcStartSetupFragment(), FRAGMENT_TAG).commit();
        }

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        Timber.e("On Start");
        BatteryWhiteListUtil.checkBatteryWhiteList(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
        EventBus.getDefault().getStickyEvent(Events.NetworkStatus.class);
        connectivityReceiver.register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        connectivityReceiver.unregister(this);
        super.onPause();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.NetworkStatus networkStatus) {
        Fragment f = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (f instanceof ConnectionAwareness) {
            if (networkStatus.isUp()) {
                ((ConnectionAwareness) f).connectionGained();
            } else {
                ((ConnectionAwareness) f).connectionLost();
            }
        }
    }
}
