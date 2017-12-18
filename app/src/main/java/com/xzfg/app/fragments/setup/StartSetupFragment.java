package com.xzfg.app.fragments.setup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.activities.SetupActivity;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.security.Fuzz;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * This fragment is our startup screen.
 */
public class StartSetupFragment extends Fragment implements ConnectionAwareness {
    @Inject
    Application application;
    @Inject
    SharedPreferences sharedPreferences;

    private Button scanButton;
    private Button loginButton;
    private Button createButton;
    private Button inviteButton;

    private TextView networkError;
    private Spinner languageSpinner;
    private ArrayAdapter<String> languageAdapter;
    private List<String> languageCodes;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Application) activity.getApplication()).inject(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_setupactivity, viewGroup, false);
        ((TextView) rootView.findViewById(R.id.app_version)).setText(BuildConfig.VERSION_NAME + "-" + BuildConfig.FLAVOR.toUpperCase());
        networkError = (TextView) rootView.findViewById(R.id.network_error);
        scanButton = (Button) rootView.findViewById(R.id.btn_scan_settings);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.addToBackStack("welcome");
                fragmentTransaction.replace(R.id.setup_container, new ScanFragment(), SetupActivity.FRAGMENT_TAG);
                fragmentTransaction.commitAllowingStateLoss();
                fragmentManager.executePendingTransactions();
            }
        });

        createButton = (Button) rootView.findViewById(R.id.btn_create_account);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.addToBackStack("welcome");
                fragmentTransaction.replace(R.id.setup_container, new CreateAccountFragment(), SetupActivity.FRAGMENT_TAG);
                fragmentTransaction.commitAllowingStateLoss();
                fragmentManager.executePendingTransactions();
            }
        });

        loginButton = (Button) rootView.findViewById(R.id.btn_login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.addToBackStack("welcome");
                fragmentTransaction.replace(R.id.setup_container, new LoginFragment(), SetupActivity.FRAGMENT_TAG);
                fragmentTransaction.commitAllowingStateLoss();
                fragmentManager.executePendingTransactions();
            }
        });

        inviteButton = (Button) rootView.findViewById(R.id.btn_invite);
        inviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.addToBackStack("welcome");
                fragmentTransaction.replace(R.id.setup_container, new InviteFragment(), SetupActivity.FRAGMENT_TAG);
                fragmentTransaction.commitAllowingStateLoss();
                fragmentManager.executePendingTransactions();
            }
        });

        languageCodes = Arrays.asList(getResources().getStringArray(R.array.language_code));
        languageAdapter = new ArrayAdapter<>(getActivity(), R.layout.start_spinner_item, Arrays.asList(getResources().getStringArray(R.array.languages)));
        languageSpinner = (Spinner) rootView.findViewById(R.id.language_spinner);
        languageSpinner.setAdapter(languageAdapter);

        String selectedLanguage = sharedPreferences.getString(Fuzz.en("language", application.getDeviceIdentifier()), null);
        if (selectedLanguage != null) {
            languageSpinner.setSelection(languageCodes.indexOf(Fuzz.de(selectedLanguage, application.getDeviceIdentifier())));
        } else {
            int current = languageCodes.indexOf(Locale.getDefault().getLanguage());
            if (current >= 0) {
                languageSpinner.setSelection(current);
            } else {
                languageSpinner.setSelection(languageCodes.indexOf(null));
            }
        }

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean initialized = false;

            @SuppressLint("ApplySharedPref")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!initialized) {
                    initialized = true;
                    return;
                }

                if (position == 0) {
                    if (sharedPreferences.contains(Fuzz.en("language", application.getDeviceIdentifier()))) {
                        sharedPreferences.edit().remove(Fuzz.en("language", application.getDeviceIdentifier())).commit();
                        AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(
                                getString(R.string.language_changed),
                                getString(R.string.language_restart)
                        );
                        alertDialogFragment.show(getFragmentManager(), "alert-dialog");
                    }
                    return;
                }

                String code = languageCodes.get(position);
                String currentCode = null;

                if (sharedPreferences.contains(Fuzz.en("language", application.getDeviceIdentifier()))) {
                    currentCode = Fuzz.de(sharedPreferences.getString(Fuzz.en("language", application.getDeviceIdentifier()), null), application.getDeviceIdentifier());
                }

                if (currentCode != null && code.equals(currentCode)) {
                    return;
                }

                Resources res = application.getResources();
                // Change locale settings in the app.
                DisplayMetrics dm = res.getDisplayMetrics();
                Configuration conf = res.getConfiguration();


                boolean committed = false;

                // if the code is null, or the code matches the current system language, remove the setting
                if (code == null || code.equals(Locale.getDefault().getLanguage())) {
                    conf.locale = Locale.getDefault();
                    conf.setLayoutDirection(conf.locale);
                    committed = sharedPreferences.edit().remove(Fuzz.en("language", application.getDeviceIdentifier())).commit();
                } else {
                    conf.locale = new Locale(code);
                    conf.setLayoutDirection(conf.locale);
                    committed = sharedPreferences.edit().putString(Fuzz.en("language", application.getDeviceIdentifier()), Fuzz.en(code, application.getDeviceIdentifier())).commit();
                }

                if (committed) {
                    // in order to get the application to display the new language, we have to
                    // restart the activity so that it reloads and re-displays all the locale
                    // specific assets.
                    res.updateConfiguration(conf, dm);
                    Activity activity = getActivity();
                    Intent intent = activity.getIntent();
                    activity.overridePendingTransition(0, 0);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    activity.finish();
                    activity.overridePendingTransition(0, 0);
                    startActivity(intent);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

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
            loginButton.setEnabled(false);
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
            loginButton.setEnabled(true);
            createButton.setEnabled(true);
            inviteButton.setEnabled(true);
        }
        if (networkError != null)
            networkError.setVisibility(View.GONE);
    }
}
