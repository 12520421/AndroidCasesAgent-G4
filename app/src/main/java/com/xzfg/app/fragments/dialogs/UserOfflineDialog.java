package com.xzfg.app.fragments.dialogs;

import android.annotation.SuppressLint;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;

import com.xzfg.app.Application;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.model.SendableMessage;
import com.xzfg.app.model.User;
import com.xzfg.app.services.ChatService;

import javax.inject.Inject;

/**
 *
 */
public class UserOfflineDialog extends DialogFragment {
    View contentView;

    User user;

    @Inject
    Application application;

    public static UserOfflineDialog newInstance(User user, SendableMessage message) {
        UserOfflineDialog f = new UserOfflineDialog();
        Bundle args = new Bundle();
        args.putParcelable("user", user);
        args.putParcelable("message", message);
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

        user = getArguments().getParcelable("user");
        final SendableMessage message = getArguments().getParcelable("message");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        contentView = LayoutInflater.from(getActivity()).inflate(R.layout.offline_message, null, false);
        if (user.getHasEmail().equalsIgnoreCase("false")) {
            contentView.findViewById(R.id.email_check).setVisibility(View.GONE);
        }
        if (user.getHasPhone().equalsIgnoreCase("false")) {
            contentView.findViewById(R.id.sms_check).setVisibility(View.GONE);
        }

        return builder
                .setTitle(getString(R.string.user_offline))
                .setCancelable(true)
                .setView(contentView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (((CheckBox) contentView.findViewById(R.id.chat_check)).isChecked()) {
                            Intent i = new Intent(application, ChatService.class).setAction(Givens.ACTION_CHAT_SEND_MESSAGE).putExtra("chat_message", message);
                            application.startService(i);
                        }
                        if (((CheckBox) contentView.findViewById(R.id.email_check)).isChecked()) {
                            Intent i = new Intent(application, ChatService.class).setAction(Givens.ACTION_CHAT_EMAIL_SEND_MESSAGE).putExtra("chat_message", message);
                            application.startService(i);
                        }
                        if (((CheckBox) contentView.findViewById(R.id.sms_check)).isChecked()) {
                            Intent i = new Intent(application, ChatService.class).setAction(Givens.ACTION_CHAT_SMS_SEND_MESSAGE).putExtra("chat_message", message);
                            application.startService(i);
                        }
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
}
