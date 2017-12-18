package com.xzfg.app.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.widgets.ModalView;

import de.greenrobot.event.EventBus;

/**
 */
public class SetupBossFragment extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.boss, viewGroup, false);
        ModalView modalView = (ModalView) v.findViewById(R.id.bossview);
        modalView.setOnClickListener(this);
        EventBus.getDefault().post(new Events.DisplayChanged("Boss Mode", R.id.boss));
        return v;
    }

    @Override
    public void onClick(View v) {
        ((Application) getActivity().getApplication()).enableBossMode();
        EventBus.getDefault().post(new Events.BossModeStatus(Givens.ACTION_BOSSMODE_ENABLE));
    }
}
