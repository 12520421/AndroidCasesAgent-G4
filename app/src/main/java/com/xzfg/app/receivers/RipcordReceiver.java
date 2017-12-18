package com.xzfg.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.xzfg.app.services.MainService;

public class RipcordReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // passes the intent on to our service.
        Intent serviceIntent = new Intent(context, MainService.class);
        if (intent.getAction() != null) {
            serviceIntent.setAction(intent.getAction());
        }
        if (intent.getExtras() != null) {
            serviceIntent.putExtras(intent.getExtras());
            if (intent.getExtras().getClassLoader() != null)
                serviceIntent.setExtrasClassLoader(intent.getExtras().getClassLoader());
        }
        context.startService(serviceIntent);
    }

    public RipcordReceiver register(Context context) {
        context.getApplicationContext().registerReceiver(this, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        return this;
    }
}
