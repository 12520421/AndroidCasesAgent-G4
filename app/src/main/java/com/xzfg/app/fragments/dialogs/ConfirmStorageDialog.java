package com.xzfg.app.fragments.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.MessageUtil;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 *
 */
public class ConfirmStorageDialog extends DialogFragment {
    View contentView;
    @Inject
    Application application;
    @Inject
    Crypto crypto;

    // holds our custom cancel listener.
    private DialogInterface.OnCancelListener customCancelListener;
    private DialogInterface.OnDismissListener customDismissListener;

    public static ConfirmStorageDialog newInstance() {
        ConfirmStorageDialog f = new ConfirmStorageDialog();
        Bundle args = new Bundle();
        f.setArguments(args);
        return f;
    }


    /**
     * The Android theme resources for the existing dialog implementations are not
     * public/changeable through XML styles, so we do it here through code.
     */
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        Resources res = getActivity().getResources();
        int color = ContextCompat.getColor(getContext(),R.color.redorange);
        int titleId = res.getIdentifier("alertTitle", "id", "android");
        int dividerId = res.getIdentifier("titleDivider", "id", "android");
        TextView titleView = (TextView) dialog.findViewById(titleId);
        if (titleView != null) {
            titleView.setTextColor(color);
        }
        View dividerView = dialog.findViewById(dividerId);
        if (dividerView != null) {
            dividerView.setBackgroundColor(color);
        }
    }

    /**
     * Provides the customized AlertDialog.
     */
    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        ((Application) getActivity().getApplication()).inject(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        contentView = LayoutInflater.from(getActivity()).inflate(R.layout.storage_confirmation, null, false);
        ((TextView) contentView.findViewById(R.id.message1)).setText(getString(R.string.confirm_description_1, getString(R.string.agree)));

        String expectedFormat = crypto.getExpectedFormat();
        String expectedString;
        if (expectedFormat != null) {
            switch (expectedFormat) {
                case "AES256":
                    expectedString = getString(R.string.aes_256);
                    break;
                case "AES128":
                    expectedString = getString(R.string.aes_128);
                    break;
                case "TripleDES":
                    expectedString = getString(R.string.des);
                    break;
                default:
                    expectedString = getString(R.string.no_encryption);
            }
        } else {
            expectedString = getString(R.string.no_encryption);
        }

        ((TextView) contentView.findViewById(R.id.message2)).setText(getString(R.string.confirm_description_2, expectedString));

        return builder
                .setTitle(getString(R.string.confirm_change))
                .setCancelable(true)
                .setView(contentView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (((EditText) contentView.findViewById(R.id.confirmation_box)).getText().toString().equalsIgnoreCase(application.getString(R.string.agree))) {
                            //Timber.d("Setting To Medium Security");
                            MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Set Settings|SecurityCASESAgent@0"));
                            application.getAgentSettings().setSecurity(0);
                            EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                            EventBus.getDefault().post(new Events.SecurityLevelChanged(application.getAgentSettings().getSecurity()));
                        } else {
                            MessageUtil.sendMessage(application, MessageUtil.getMessageUrl(application, "Set Settings|SecurityCASESAgent@0"));
                            EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                            EventBus.getDefault().post(new Events.SecurityLevelNotChanged());
                        }
                        dialog.dismiss();

                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EventBus.getDefault().post(new Events.SecurityLevelChanged(application.getAgentSettings().getSecurity()));
                        EventBus.getDefault().post(new Events.AgentSettingsAcquired(application.getAgentSettings()));
                        dialog.dismiss();
                    }
                })
                .create();
    }

    /**
     * This overridden implementation calls our DialogInterface.OnCancelListener.
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        if (this.customCancelListener != null) {
            this.customCancelListener.onCancel(dialog);
        }
        super.onCancel(dialog);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (this.customDismissListener != null) {
            this.customDismissListener.onDismiss(dialog);
        }
        super.onDismiss(dialog);
    }

    /**
     * Allows setting a custom onCancelListener, since the DialogFragment takes
     * control of the AlertDialog's implementation.
     */
    public void setCustomCancelListener(DialogInterface.OnCancelListener cancelListener) {
        this.customCancelListener = cancelListener;
    }

    /**
     * Allows setting a custom onDismissListener, since the DialogFragment takes
     * control of the AlertDialog's implementation.
     */
    public void setCustomDismissListener(DialogInterface.OnDismissListener dismissListener) {
        this.customDismissListener = dismissListener;
    }



    /**
     * Overrides the default implementation issuing an immediate call to manager.executePendingTransactions()
     *
     * @param manager FragmentManager
     * @param tag     a tag
     */
    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
        manager.executePendingTransactions();
    }
}
