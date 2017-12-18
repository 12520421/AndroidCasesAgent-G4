package com.xzfg.app.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.xzfg.app.model.BatteryInfo;

/**
 * Keeps track of our battery state.
 * Actually, doesn't do much. Listening to battery update broadcasts isn't necessary, as we
 * can request it as needed, instead of every .0001% of battery change.
 */
public class BatteryUtil {

    private BatteryUtil() {
    }

    public static Integer getBatteryLevel(Context context) {
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

    public static BatteryInfo getBatteryInfo(Context context) {
        final BatteryInfo result = new BatteryInfo();
        final IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        final Intent batteryStatus = context.getApplicationContext().registerReceiver(null, ifilter);

        final int status = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
        final int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
        final int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100);

        result.isCharging = (status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
            status == android.os.BatteryManager.BATTERY_STATUS_FULL);
        result.percentCharged = (level * 100) / scale;

        return result;
    }
}
