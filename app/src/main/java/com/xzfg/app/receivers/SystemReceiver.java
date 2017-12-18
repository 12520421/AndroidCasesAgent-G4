package com.xzfg.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.xzfg.app.services.MainService;

/**
 * Handles system events.
 */
public class SystemReceiver extends BroadcastReceiver {

    /**
     * Upon receiving a system related broadcast, we copy the details and send it over to the
     * MainService for actual processing.
     */
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

}
