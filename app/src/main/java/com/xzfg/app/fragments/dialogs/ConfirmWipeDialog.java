package com.xzfg.app.fragments.dialogs;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.activities.SetupActivity;
import com.xzfg.app.codec.Encoder;
import com.xzfg.app.managers.G4Manager;
import com.xzfg.app.services.GattClient;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 *
 */
public class ConfirmWipeDialog extends DialogFragment {

    @Inject
    Application application;
    @Inject
    G4Manager g4Manager;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getActivity().getApplication()).inject(this);
    }
    public static ConfirmWipeDialog newInstance(String title, String message) {
        ConfirmWipeDialog f = new ConfirmWipeDialog();
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
                .setPositiveButton("Wipe Data", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        g4Manager = application.getG4Manager();
                        GattClient mGattClient = new GattClient();
                        try{
                            try {
                                mGattClient = g4Manager.getClient();
                                mGattClient.onDestroy();
                                //  g4Manager.setClient(null);
                            }catch (Exception e){
                                EventBus.getDefault().postSticky(new Events.handleDisConnect("Connect"));
                                EventBus.getDefault().postSticky(new Events.ActivityPaused());
                            }
                            EventBus.getDefault().postSticky(new Events.handleDisConnect("Connect"));
                            EventBus.getDefault().postSticky(new Events.ActivityPaused());
                            SystemClock.sleep(500);
                            /*Intent intent=new Intent(getActivity(),SetupActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                            getActivity().startActivity(intent);*/
                            startActivity(new Intent(getActivity(), SetupActivity.class));
                           // ((ActivityManager)getActivity().getSystemService(Context.ACTIVITY_SERVICE))
                             //       .clearApplicationUserData();
                        }
                        catch (Exception e)
                        {
                            e.toString();
                        }
                        //EventBus.getDefault().postSticky(new Events.ActivityPaused());
                        startActivity(new Intent(getActivity(), SetupActivity.class));
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
