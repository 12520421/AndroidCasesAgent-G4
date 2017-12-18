package com.xzfg.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v13.BuildConfig;
import android.util.Log;

import com.xzfg.app.services.SMSReceiverService;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG)
            Log.d("SMSReceiver", "onReceive()");
        intent.setClass(context, SMSReceiverService.class);
        intent.putExtra("result", getResultCode());
        context.startService(intent);
    }
}