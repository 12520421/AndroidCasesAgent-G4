package com.xzfg.app.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.fragments.setup.PaymentFragment;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.services.ProfileService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * This activity basically acts as a controller, and doesn't do much
 * outside of redirect to the BossActivity or the AgentActivity.
 */
public class MainActivity extends BaseActivity {
    private static final String FRAGMENT_TAG = "main";

    @Inject
    Application application;

    @Inject
    SharedPreferences sharedPreferences;

    AgentSettings agentSettings;


    private final int REQUEST_PERMISSION_BLUETOOTH =1;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getApplication()).inject(this);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Toast.makeText(getApplication(), getString(R.string.android_too_old), Toast.LENGTH_LONG).show();
            finish();
        }

        setContentView(R.layout.activity_mainactivity);
    }

    @Override
    public void onResume() {
        EventBus.getDefault().registerSticky(this);
        super.onResume();

        AgentSettings startAgentSettings;
        Intent startIntent = getIntent();

        if (agentSettings == null) {
            agentSettings = application.getAgentSettings();
        }

        if (startIntent != null) {
            if (startIntent.hasExtra(Givens.EXTRA_AGENT_SETTINGS)) {
                startAgentSettings = startIntent.getParcelableExtra(Givens.EXTRA_AGENT_SETTINGS);
                if (agentSettings == null) {
                    agentSettings = startAgentSettings;
                }

            }
        }

        Intent intent = null;

        if (application.isSetupComplete()) {
            if (!BuildConfig.HEADLESS && !sharedPreferences.getBoolean(Givens.MDM_CONFIG,false) && application.getAgentSettings().getEULA() == 1) {
                intent = new Intent(this, EulaActivity.class);
            } else {
                // Show Splash screen unless we get a TRUE extra value. No value exists on the 1st run but
                // we will come back here with TRUE extra value after we display and close Splash screen
                boolean ignoreSplashScreen = (getIntent().getExtras() != null) && getIntent().getExtras().getBoolean(SplashActivity.EXTRA_IGNORE_SPLASH, false);

                // See if we need to display a Splash screen
                if (!BuildConfig.HEADLESS && !ignoreSplashScreen && getResources().getBoolean(R.bool.enable_splash_screen)) {
                    intent = new Intent(this, SplashActivity.class);
                } else {
                    // Verify subscription if required
                    if (isConnected() && getResources().getBoolean(R.bool.enable_reg_payment)) {
                        // Verify subscription if required
                        ProfileService.verifySubscription(this, application.getScannedSettings().getUserId(), agentSettings);
                        return;
                    } else {
                        // Launch main Agent activity here
                        onEventMainThread(new Events.SubscriptionVerified(true, agentSettings));
                    }
                }
            }
        } else {
            // See if we need to display a Setup screen
            if (!BuildConfig.HEADLESS) {
                intent = new Intent(this, SetupActivity.class);
            }
        }

        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        }

        finish();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.SubscriptionVerified event) {
        //Timber.d("Subscription verified.");
        // We also get here when check subscription from PaymentFragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (event.isValid()) {
            // Open agent activity
            Intent intent = new Intent(this, AgentActivity.class);
            String a = application.getDefaultScreen();
            AgentSettings settings = application.getAgentSettings();
            settings.setScreen(10);
            //turn on bluetooth

            //request permission to turn on bluetooth




            if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED )
            {
                intent.putExtra(Givens.DEFAULT_SCREEN_PREFERENCE, application.getDefaultScreen());
            }
            else{
                intent.putExtra(Givens.DEFAULT_SCREEN_PREFERENCE, application.getDefaultScreen());
            }
            intent.putExtra(Givens.DEFAULT_SCREEN_PREFERENCE, application.getDefaultScreen());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish();
        } else if (fragmentManager.findFragmentByTag(FRAGMENT_TAG) == null) {
            // Clear fragment stack and open payment fragment
            PaymentFragment fragment = new PaymentFragment();
            Bundle args = new Bundle();
            args.putParcelable(PaymentFragment.ARG_AGENT_SETTINGS, event.getAgentSettings());
            fragment.setArguments(args);
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentManager.beginTransaction().add(R.id.main_container, fragment, FRAGMENT_TAG).commit();
        }
        // else->PaymentFragment will display subscription error
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    mBluetoothAdapter.enable();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to enable Bluetooth", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


}
