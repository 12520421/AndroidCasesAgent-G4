package com.xzfg.app.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.fragments.dialogs.ConfirmWipeDialog;
import com.xzfg.app.widgets.ModalView;

import de.greenrobot.event.EventBus;

/**
 *
 */
public class WipeFragment extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.wipe, viewGroup, false);
        ModalView modalView = (ModalView) v.findViewById(R.id.wipeview);
        ((TextView) v.findViewById(R.id.description)).setText(Html.fromHtml(getString(R.string.wipe_data_description)));
        modalView.setOnClickListener(this);
        EventBus.getDefault().post(new Events.DisplayChanged(getString(R.string.wipe_data), R.id.wipe));
        return v;
    }

    @Override
    public void onClick(View v) {
        ConfirmWipeDialog dialog = ConfirmWipeDialog.newInstance(getString(R.string.wipe_data), getString(R.string.wipe_confirmation));
        dialog.show(getFragmentManager(), "confirm");

    }
}
