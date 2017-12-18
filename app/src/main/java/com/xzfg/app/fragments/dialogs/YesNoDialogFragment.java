package com.xzfg.app.fragments.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.xzfg.app.R;

/**
 */
public class YesNoDialogFragment extends DialogFragment implements DialogInterface.OnCancelListener {
    // holds our custom cancel listener.
    private DialogInterface.OnCancelListener customCancelListener;
    private DialogInterface.OnDismissListener customDismissListener;

    /**
     * Get a new AlertDialogFragment instance.
     */
    public static YesNoDialogFragment newInstance(String title, String message) {
        YesNoDialogFragment f = new YesNoDialogFragment();
        Bundle args = new Bundle();
        if (title != null) {
            args.putString("title", title);
        }
        if (message != null) {
            args.putString("message", message);
        }
        f.setArguments(args);
        return f;
    }

    /**
     * Gets a new alert dialog instance, but with no title. Will
     * use the R.string.error_title.
     */
    public static YesNoDialogFragment newInstance(String message) {
        return YesNoDialogFragment.newInstance(null, message);
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
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getString("title", getString(R.string.error_title)))
                .setMessage(getArguments().getString("message", "An unknown error has occurred."))
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, getActivity().getIntent());
                        dialog.cancel();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, getActivity().getIntent());
                        dialog.cancel();
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
     */
    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
        manager.executePendingTransactions();
    }

}
