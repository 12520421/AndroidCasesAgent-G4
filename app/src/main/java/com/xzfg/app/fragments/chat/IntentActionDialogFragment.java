package com.xzfg.app.fragments.chat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.xzfg.app.R;

import de.greenrobot.event.EventBus;

/**
 */
public class IntentActionDialogFragment extends DialogFragment {

    public static IntentActionDialogFragment newInstance(String title, String message, Intent positiveIntent, Intent negativeIntent) {
        IntentActionDialogFragment df = new IntentActionDialogFragment();
        Bundle args = new Bundle();
        if (positiveIntent != null)
            args.putParcelable("positive_intent", positiveIntent);
        if (negativeIntent != null)
            args.putParcelable("negative_intent", negativeIntent);
        if (title != null)
            args.putString("title", title);
        if (message != null)
            args.putString("message", message);

        df.setArguments(args);
        return df;
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
        Bundle arguments = getArguments();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(arguments.getString("title", getString(R.string.error_title)));
        builder.setMessage(arguments.getString("message", "An unknown error has occurred."));
        if (!arguments.containsKey("positive_intent") && !arguments.containsKey("negative_intent")) {
            builder.setCancelable(true);
        } else {
            builder.setCancelable(false);
        }
        if (arguments.containsKey("positive_intent")) {
            final Intent positiveIntent = arguments.getParcelable("positive_intent");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (positiveIntent.getBooleanExtra("isService", false)) {
                        getActivity().startService(positiveIntent);
                    }
                    if (positiveIntent.getBooleanExtra("isActivity", false)) {
                        getActivity().startActivity(positiveIntent);
                    }
                    if (positiveIntent.hasExtra("event")) {
                        EventBus.getDefault().post(positiveIntent.getParcelableExtra("event"));
                    }
                    dialog.cancel();
                }
            });
        }
        if (arguments.containsKey("negative_intent")) {
            final Intent negativeIntent = arguments.getParcelable("negative_intent");
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (negativeIntent.getBooleanExtra("isService", false)) {
                        getActivity().startService(negativeIntent);
                    }
                    if (negativeIntent.getBooleanExtra("isActivity", false)) {
                        getActivity().startActivity(negativeIntent);
                    }
                    if (negativeIntent.hasExtra("event")) {
                        EventBus.getDefault().post(negativeIntent.getParcelableExtra("event"));
                    }
                    dialog.cancel();
                }
            });
        }
        return builder.create();
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
