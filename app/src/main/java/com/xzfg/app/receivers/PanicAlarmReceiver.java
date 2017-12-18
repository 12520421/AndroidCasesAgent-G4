package com.xzfg.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.fragments.home.PanicStates;
import com.xzfg.app.managers.AlertManager;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.url.MessageUrl;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * This receiver is called by AlarmManager.
 */
public class PanicAlarmReceiver extends BroadcastReceiver {
    public final static String PARAM_PANIC_MESSAGE = "PARAM_PANIC_MESSAGE";

    @Inject
    AlertManager alertManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        Application application = ((Application) context.getApplicationContext());
        application.inject(this);
        // Start panic
        String sosMessage = MessageUrl.MESSAGE_SOS;
        // Add extra message if not empty
        if (extras != null && extras.containsKey(PARAM_PANIC_MESSAGE)) {
            if (!extras.getString(PARAM_PANIC_MESSAGE, "").isEmpty()) {
                sosMessage += extras.getString(PARAM_PANIC_MESSAGE, "");
            }
        }
        alertManager.startPanicMode(false, sosMessage);
        // Clear panic timer date
        AgentSettings settings = application.getAgentSettings();
        settings.setPanicTimerDate(null);
        settings.setRemainingPanicTimer(-1);
        settings.setPanicState(PanicStates.PANIC_ON.getValue());
        EventBus.getDefault().post(new Events.AgentSettingsAcquired(settings));
        EventBus.getDefault().post(new Events.PanicAlertCompleted());
    }

}