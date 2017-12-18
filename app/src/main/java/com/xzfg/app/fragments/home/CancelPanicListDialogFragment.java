package com.xzfg.app.fragments.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.R;
import com.xzfg.app.managers.AlertManager;

import javax.inject.Inject;

public class CancelPanicListDialogFragment extends DialogFragment implements DialogInterface.OnCancelListener, View.OnClickListener {
    private static final String PARAM_PANIC_PIN = "PARAM_PANIC_PIN";
    private static final String PARAM_DURESS_PIN = "PARAM_DURESS_PIN";
    private View mPinRing1View;
    private View mPinRing2View;
    private View mPinRing3View;
    private View mPinRing4View;
    private TextView mReasonView;
    private String mPin = "";
    private int mPanicPin;
    private int mDuressPin;

    @Inject
    AlertManager alertManager;

    public static CancelPanicListDialogFragment newInstance(int panicPin, int duressPin) {

        CancelPanicListDialogFragment dlg = new CancelPanicListDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_PANIC_PIN, panicPin);
        bundle.putInt(PARAM_DURESS_PIN, duressPin);
        dlg.setArguments(bundle);

        return dlg;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Application)activity.getApplication()).inject(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    // Clear PIN
    private void clearPin() {
        mPin = "";
        mPinRing1View.setBackgroundResource(R.drawable.pin_ring_empty);
        mPinRing2View.setBackgroundResource(R.drawable.pin_ring_empty);
        mPinRing3View.setBackgroundResource(R.drawable.pin_ring_empty);
        mPinRing4View.setBackgroundResource(R.drawable.pin_ring_empty);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_cancel_panic, null, false);

        mReasonView = (TextView) view.findViewById(R.id.reason);
        mPinRing1View = view.findViewById(R.id.pin_ring1);
        mPinRing2View = view.findViewById(R.id.pin_ring2);
        mPinRing3View = view.findViewById(R.id.pin_ring3);
        mPinRing4View = view.findViewById(R.id.pin_ring4);

        view.findViewById(R.id.cancel).setOnClickListener(this);
        view.findViewById(R.id.submit).setOnClickListener(this);
        view.findViewById(R.id.delete).setOnClickListener(this);

        view.findViewById(R.id.pad0).setOnClickListener(this);
        view.findViewById(R.id.pad1).setOnClickListener(this);
        view.findViewById(R.id.pad2).setOnClickListener(this);
        view.findViewById(R.id.pad3).setOnClickListener(this);
        view.findViewById(R.id.pad4).setOnClickListener(this);
        view.findViewById(R.id.pad5).setOnClickListener(this);
        view.findViewById(R.id.pad6).setOnClickListener(this);
        view.findViewById(R.id.pad7).setOnClickListener(this);
        view.findViewById(R.id.pad8).setOnClickListener(this);
        view.findViewById(R.id.pad9).setOnClickListener(this);

        Bundle bundle = getArguments();
        mPanicPin = bundle.getInt(PARAM_PANIC_PIN);
        mDuressPin = bundle.getInt(PARAM_DURESS_PIN);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .create();
    }

    // Overrides the default implementation issuing an immediate call to manager.executePendingTransactions()
    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
        manager.executePendingTransactions();
    }

    // Add digit to PIN and update "pin rings"
    private boolean addDigitToPin(String digit) {
        if (mPin.length() < 4) {
            mPin += digit;

            switch (mPin.length()) {
                case 1:
                    mPinRing1View.setBackgroundResource(R.drawable.pin_ring_full);
                    break;
                case 2:
                    mPinRing2View.setBackgroundResource(R.drawable.pin_ring_full);
                    break;
                case 3:
                    mPinRing3View.setBackgroundResource(R.drawable.pin_ring_full);
                    break;
                case 4:
                    mPinRing4View.setBackgroundResource(R.drawable.pin_ring_full);
                    break;
            }

            return true;
        }

        return false;
    }

    private boolean validate() {
        if (mReasonView.getText().toString().trim().isEmpty()) {
            Toast.makeText(getActivity(), R.string.enter_reason_cancel_panic, Toast.LENGTH_SHORT).show();
            mReasonView.requestFocus();
            return false;
        }
        if (mPin.trim().length() < 4) {
            Toast.makeText(getActivity(), R.string.enter_pin_cancel_panic, Toast.LENGTH_SHORT).show();
            return false;
        }

        int pin = Integer.parseInt(mPin);
        if (pin != mPanicPin && pin != mDuressPin) {
            Toast.makeText(getActivity(), R.string.invalid_pin_try_again, Toast.LENGTH_SHORT).show();
            clearPin();
            return false;
        }


        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel: {
                dismiss();
                break;
            }
            case R.id.submit: {
                if (validate()) {

                   /* alertManager.stopPanicMode(false,mReasonView.getText().toString());
                    dismiss();*/
                   int pin = Integer.parseInt(mPin);
                   if(pin == mPanicPin){
                       alertManager.stopPanicMode(false,mReasonView.getText().toString());
                       dismiss();
                   }
                   if(pin == mDuressPin){
                  //     alertManager.stopPanicMode(true,mReasonView.getText().toString());
                       dismiss();
                        alertManager.cancelPanicDuress(false,mReasonView.getText().toString());

                   }

                }
                break;
            }
            case R.id.delete: {
                // Clear PIN
                clearPin();
                break;
            }
            case R.id.pad0: {
                addDigitToPin("0");
                break;
            }
            case R.id.pad1: {
                addDigitToPin("1");
                break;
            }
            case R.id.pad2: {
                addDigitToPin("2");
                break;
            }
            case R.id.pad3: {
                addDigitToPin("3");
                break;
            }
            case R.id.pad4: {
                addDigitToPin("4");
                break;
            }
            case R.id.pad5: {
                addDigitToPin("5");
                break;
            }
            case R.id.pad6: {
                addDigitToPin("6");
                break;
            }
            case R.id.pad7: {
                addDigitToPin("7");
                break;
            }
            case R.id.pad8: {
                addDigitToPin("8");
                break;
            }
            case R.id.pad9: {
                addDigitToPin("9");
                break;
            }
        }
        //v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }
}
