package com.xzfg.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.xzfg.app.services.MainService;

/**
 * The connectivity events are pretty verbose. We only want to use this when we really need to
 * watch for connectivity changes, such as when we've tried to connect and it's failed.
 */
public class ConnectivityReceiver extends BroadcastReceiver {

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

    public ConnectivityReceiver register(Context context) {
        context.getApplicationContext().registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        return this;
    }

    public void unregister(Context context) {
        context.getApplicationContext().unregisterReceiver(this);
    }
}
