package com.xzfg.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by THONG on 11/20/2017.
 */

public abstract class BaseFragment extends Fragment {
    StringBuilder sb=new StringBuilder(192);

    public abstract void binData(String result);
    public abstract void binConnectionState(boolean isConnect);
    public abstract void binRssi(int rssi);
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public boolean onBackPressed() {
        DebugLog.d("check back pressed:");
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);

    }

    public void onEventMainThread(Events.SendData event) {


        if (!sb.toString().contains(event.getData())){
            sb.append(event.getData());
        }
        if (sb.toString().contains("OK")) {
            binData(sb.toString());

            sb.setLength(0);
            DebugLog.d("data: "+sb.toString());

        }
        if (sb.toString().contains("ERROR")){
            //jgbgukbu
        }
    }
    public void onEventMainThread(Events.SendConnectionState event) {
        binConnectionState(event.isConnected());
    }
    public void onEventMainThread(Events.SendRssi event) {
        binRssi(event.getRssi());
    }


}
