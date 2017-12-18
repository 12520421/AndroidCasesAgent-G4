package com.xzfg.app.services;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.google.gson.Gson;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.managers.FixManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * This handles logging to the server.
 */
public class SMSService extends BackWakeIntentService {
    private static final String TAG = SMSService.class.getName();
    public static final String TYPE_IDENTIFIER = SMSService.class.getName()+"_type";
    public static final String PUBLIC_IDENTIFIER = SMSService.class.getName()+"_public";

    public static final int TYPE_POSITION = 0;
    public static final int TYPE_SOS = 1;
    public static final int TYPE_SOS_CANCEL = 2;
    public static final int TYPE_SOS_DURESS = 3;
    public static final int PRIVATE = 0;
    public static final int PUBLIC = 1;

    private static final String dateFormat = "yyyy-MM-dd HH:mm:ss";

    @Inject
    Application application;
    @Inject
    FixManager fixManager;

    private Gson gson;

    public SMSService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
        gson = new Gson();
    }

    /**
     * Be careful about logging here. An exception in this class could cause
     * and infinte loop --- SO DON'T USE Timber.e() HERE!
     */
    @SuppressLint("CommitPrefEdits")
    @Override
    protected void doWork(Intent intent) {
        int type = intent.getIntExtra(TYPE_IDENTIFIER, -1);
        int pub = intent.getIntExtra(PUBLIC_IDENTIFIER, -1);

        StringBuilder sb = new StringBuilder();
        sb.append("CA=");
        sb.append(application.getScannedSettings().getOrganizationId()).append(application.getDeviceIdentifier());
        Location location = fixManager.getLastLocation();
        if (location != null) {
          /*  // ,latitude,longitude,accuracy,altitude,speed,heading,date
            sb.append(",").append(String.valueOf(location.getLatitude()));
            sb.append(",").append(String.valueOf(location.getLongitude()));
            sb.append(",").append(String.valueOf(location.getAccuracy()));
            sb.append(",").append(String.valueOf(location.getAltitude()));
            sb.append(",").append(String.valueOf(location.getSpeed()));
            sb.append(",").append(String.valueOf(location.getBearing()));
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            sb.append(",").append(sdf.format(new Date(location.getTime())));*/
        }
        else {
            // no values for these.
            sb.append(",,,,,,,");
        }
        sb.append(",").append(String.valueOf(type));
        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager.getDefault()
                .sendTextMessage(application.getAgentSettings().getSMSPhoneNumber(), null,
                    sb.toString(), null, null);
        }
        else {
            if (BuildConfig.DEBUG) {
                Timber.e("SMS PERMISSIONS HAVE BEEN DENIED. THIS IS NOT SUPPORTED IN THE "
                    + "APPLICATION, AND WILL RESULT IN A CRASH IN THE RELEASE BUILDS.");
                Toast.makeText(
                    getApplicationContext(),
                    "SMS PERMISSIONS HAVE BEEN DENIED. THIS IS NOT SUPPORTED IN THE APPLICATION, "
                        + "AND WILL RESULT IN A CRASH IN THE RELEASE BUILDS.",
                    Toast.LENGTH_LONG
                ).show();
            }
            else {
                throw new RuntimeException("SMS PERMISSIONS DENIED BY USER");
            }
        }

        // if the intent says it's a "public" message, show a toast.
        if (pub > 0) {
        //   Toast.makeText(this,getString(R.string.sms_sent),Toast.LENGTH_SHORT).show();
        }

        if (type == TYPE_SOS) {
            EventBus.getDefault().post(new Events.SosMessageSent());
        }
    }


}
