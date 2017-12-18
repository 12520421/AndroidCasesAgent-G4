package com.xzfg.app.util;

import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;

import com.xzfg.app.BuildConfig;

public final class BatteryWhiteListUtil {

  private BatteryWhiteListUtil() {
  }

  /**
   * Only versions that don't include the notification need to request whitelisting.
   * Whitelisting is not available for builds that need to be deployed to Google Play.
   *
   * This permission must also be added to the manifest of that variant:
   * <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>
   *
   * @param context the context.
   */
  public static void checkBatteryWhiteList(Context context) {
    // check if > Android 6.
    if (VERSION.SDK_INT >= VERSION_CODES.M) {

      // check if using notification for build
      if (BuildConfig.BATTERY_WHITELIST && !BuildConfig.USE_NOTIFICATION) {

        // check if permission is in the manifest
        if (ContextCompat
            .checkSelfPermission(context, permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            == PackageManager.PERMISSION_GRANTED) {

          // get the power manager.
          final PowerManager powerManager = (PowerManager) context
              .getSystemService(Context.POWER_SERVICE);
          final String packageName = context.getApplicationContext().getPackageName();

          // if we're not ignoring battery optimizations, ask the user.
          if (!powerManager
              .isIgnoringBatteryOptimizations(context.getApplicationContext().getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:" + packageName));
            context.startActivity(intent);
          } else {
            //Timber.d("Already ignoring battery optimizations");
          }
        } else {
          //Timber.d("Permission not granted");
        }

      } else {
        //Timber.d("Using Notificaiton");
      }

    } else {
      //Timber.d("Older android.");
    }
  }

}
