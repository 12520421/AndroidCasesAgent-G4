package com.xzfg.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class VolumeSOSReceiver extends BroadcastReceiver {

    private Context context = null;
    private static final String TAG = "LaunchReciever";
    private static final int SLEEP_TIME = 5000;
    int volumetrap = 0;

    @Override
    public void onReceive(Context arg0, Intent intent) {
        context = arg0;
        Bundle extras = intent.getExtras();
        if (extras != null) {
            int volume = (Integer) extras.get("android.media.EXTRA_VOLUME_STREAM_VALUE");
            Toast.makeText(arg0, "Receiver call & volume is:" + volume, Toast.LENGTH_SHORT).show();
            if (volume == 0) {
                Log.d(TAG, "onReceive: volume=");
            }

        }

    }
}