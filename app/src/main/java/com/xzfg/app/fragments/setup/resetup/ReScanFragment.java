package com.xzfg.app.fragments.setup.resetup;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.fragments.dialogs.RegistrationProgressDialogFragment;
import com.xzfg.app.fragments.setup.ConnectionAwareness;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.services.RegistrationService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;
import timber.log.Timber;

/**
 * This fragment provides the scanner screen.
 */
public class ReScanFragment extends Fragment implements
        ZBarScannerView.ResultHandler,
        ConnectionAwareness {
    private static final List<BarcodeFormat> formats = new ArrayList<BarcodeFormat>() {{
        add(BarcodeFormat.QRCODE);
    }};
    @Inject
    Crypto crypto;
    @Inject
    Application application;
    private boolean scanning = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("scanning")) {
                scanning = savedInstanceState.getBoolean("scanning");
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((Application) getActivity().getApplication()).inject(this);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("scanning")) {
                scanning = savedInstanceState.getBoolean("scanning");
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("scanning", scanning);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_scanner, viewGroup, false);
        v.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStackImmediate();
            }
        });
        ZBarScannerView scannerView = (ZBarScannerView) v.findViewById(R.id.scanner);
        scannerView.setResultHandler(this);
        scannerView.setAutoFocus(getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS));
        scannerView.setFlash(false);
        scannerView.setFormats(formats);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this, 1);
        startScan();
    }

    @Override
    public void onPause() {
        stopScan();
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.Registration registration) {
        EventBus.getDefault().removeStickyEvent(registration);
        // get rid of the registration in progress dialog.
        RegistrationProgressDialogFragment f = (RegistrationProgressDialogFragment) getFragmentManager().findFragmentByTag("qrcode_success");
        if (f != null) {
            f.setCustomCancelListener(null);
            f.setCustomDismissListener(null);
            f.setCustomOnDestroyListener(null);
            f.dismissAllowingStateLoss();
            getFragmentManager().executePendingTransactions();
        }

        stopScan();

        if (registration.getStatus()) {
            final AgentSettings agentSettings = registration.getAgentSettings();
            AlertDialogFragment successFragment = AlertDialogFragment.newInstance(getString(R.string.success_title), registration.getMessage());
            successFragment.show(getFragmentManager(), "success");
            successFragment.setCustomCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    try {
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Givens.EXTRA_AGENT_SETTINGS, agentSettings);
                        getActivity().startActivity(intent);
                        getActivity().finish();
                    }
                    catch(Exception ex) {
                        Timber.e(ex, "Something bad happened.");
                    }
                }
            });

        } else {
            AlertDialogFragment errorDialog = AlertDialogFragment.newInstance(getString(R.string.error_title), registration.getMessage());
            errorDialog.setCustomCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    getFragmentManager().popBackStackImmediate();
                }
            });
            errorDialog.show(getFragmentManager(), "registration_error");
        }
    }

    @Override
    public void handleResult(Result rawResult) {
        // turn off the scanner.
        stopScan();

        Vibrator v = (Vibrator) application.getSystemService(Context.VIBRATOR_SERVICE);
        if (v.hasVibrator()) {
            v.vibrate(250);
        }

        try {
            final ScannedSettings settings = ScannedSettings.parse(application, rawResult);
            settings.validateCrypto(application, crypto);
            EventBus.getDefault().postSticky(new Events.ScanSuccess(settings));

            RegistrationProgressDialogFragment successDialog = RegistrationProgressDialogFragment.newInstance(getString(R.string.success_title), getString(R.string.qrcode_success));
            successDialog.setCustomCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    // re-enable the scanner.
                    startScan(true);
                }
            });
            successDialog.setCustomDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    // re-enable the scanner.
                    startScan(true);
                }
            });

            Intent registrationIntent = new Intent(application, RegistrationService.class);
            registrationIntent.putExtra(ScannedSettings.class.getName(), settings);
            application.startService(registrationIntent);

            successDialog.show(getFragmentManager(), "qrcode_success");
        } catch (ScannedSettings.InvalidScanException invalidScanException) {
            Timber.e("Error parsing scan!", invalidScanException);
            AlertDialogFragment errorDialog = AlertDialogFragment.newInstance(getString(R.string.error_title), invalidScanException.getMessage());
            errorDialog.setCustomCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    startScan(true);
                }
            });
            errorDialog.show(getFragmentManager(), "qrcode_error");
        }

    }

    // if we loose connectivity while scanning, go back.
    @Override
    public void connectionLost() {
        getFragmentManager().popBackStackImmediate();
    }

    @Override
    public void connectionGained() {
        // no-op
    }

    public void stopScan() {
        View view = getView();
        if (view != null) {
            ZBarScannerView scannerView = (ZBarScannerView) view.findViewById(R.id.scanner);
            if (scannerView != null) {
                scannerView.stopCamera();
                scanning = false;
            }
        }
    }

    public void startScan(boolean force) {
        if (!scanning && force) {
            scanning = true;
            View view = getView();
            if (view != null) {
                ZBarScannerView scannerView = (ZBarScannerView) view.findViewById(R.id.scanner);
                if (scannerView != null) {
                    scannerView.startCamera();
                }
            }
        } else {
            if (scanning) {
                View view = getView();
                if (view != null) {
                    ZBarScannerView scannerView = (ZBarScannerView) view.findViewById(R.id.scanner);
                    if (scannerView != null) {
                        scannerView.startCamera();
                    }
                }

            }
        }
    }

    public void startScan() {
        startScan(false);
    }


}
