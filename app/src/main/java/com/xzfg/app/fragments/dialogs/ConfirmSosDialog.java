package com.xzfg.app.fragments.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.xzfg.app.Application;
import com.xzfg.app.R;
import com.xzfg.app.managers.AlertManager;

import javax.inject.Inject;

/**
 *
 */
public class ConfirmSosDialog extends DialogFragment {

    @Inject
    AlertManager alertManager;

    public static ConfirmSosDialog newInstance(String title, String message) {
        ConfirmSosDialog f = new ConfirmSosDialog();
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

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Application)activity.getApplication()).inject(this);
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
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getString("title", getString(R.string.error_title)))
                .setMessage(getArguments().getString("message", "An unknown error has occurred."))
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertManager.startPanicMode(false);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
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

    private boolean isConnected() {
        NetworkInfo activeNetwork = ((ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

}
