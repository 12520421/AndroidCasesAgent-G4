package com.xzfg.app.managers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Keeps track of our battery state.
 * Actually, doesn't do much. Listening to battery update broadcasts isn't necessary, as we
 * can request it as needed, instead of every .0001% of battery change.
 */
public class BatteryManager {

    public BatteryManager() {
    }

    public Integer getBatteryLevel(Context context) {
        double battery = 0;
        Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null && batteryIntent.hasExtra(android.os.BatteryManager.EXTRA_LEVEL) && batteryIntent.hasExtra(android.os.BatteryManager.EXTRA_SCALE)) {
            int level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            if (level == -1 || scale == -1) {
                battery = 0;
            } else {
                battery = Math.floor(((float) level / (float) scale) * 100.0f);
            }
        }

        return (int) battery;
    }
}
