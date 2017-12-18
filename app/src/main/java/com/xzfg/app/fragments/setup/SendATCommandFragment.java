package com.xzfg.app.fragments.setup;



import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.support.v4.app.DialogFragment;

import com.xzfg.app.R;

/**
 * Created by APP-PC on 5/31/2017.
 */

public class SendATCommandFragment extends DialogFragment {
    public SendATCommandFragment()
    {

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_send_at_command, container);
        // This shows the title, replace My Dialog Title. You can use strings too.
        getDialog().setTitle("");
        // If you want no title, use this code
        // getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return view;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_send_at_command, new LinearLayout(getActivity()), false);

        // Retrieve layout elements


        // Set values


        // Build dialog
        Dialog builder = new Dialog(getActivity());
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setContentView(view);
        return builder;
    }
}
