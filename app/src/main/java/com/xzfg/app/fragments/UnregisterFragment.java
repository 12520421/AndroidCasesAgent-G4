package com.xzfg.app.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.fragments.dialogs.ConfirmWipeDialog;
import com.xzfg.app.managers.G4Manager;
import com.xzfg.app.widgets.ModalView;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * Created by VYLIEM on 5/29/2017.
 */

public class UnregisterFragment extends Fragment implements View.OnClickListener {
    @Nullable



    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.unregister_fragment,container,false);
        ModalView modalView = (ModalView) v.findViewById(R.id.wipeview);
      //  ((TextView) v.findViewById(R.id.description)).setText(Html.fromHtml(getString(R.string.wipe_data_description)));
        modalView.setOnClickListener(this);
        EventBus.getDefault().post(new Events.DisplayChanged("Unregister", R.id.unregister));
        return v;

    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onClick(View v) {


        ConfirmWipeDialog dialog = ConfirmWipeDialog.newInstance(getString(R.string.wipe_data), getString(R.string.wipe_confirmation));
        dialog.show(getFragmentManager(), "confirm");
    }
}
