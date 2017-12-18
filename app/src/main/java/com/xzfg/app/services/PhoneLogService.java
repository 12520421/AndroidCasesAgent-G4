package com.xzfg.app.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;

import com.xzfg.app.Application;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.PhoneLogManager;
import com.xzfg.app.security.Fuzz;
import com.xzfg.app.util.SMSUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.inject.Inject;



public class PhoneLogService extends IntentService {
    private static final String TAG = PhoneLogService.class.getName();

    public static final String CALL_TIME = TAG + "_call_time";
    public static final String PHONE_NUMBER = TAG + "_phone_number";
    public static final String CALL_DIRECTION = TAG + "_call_direction";

    private Context context;

    Double callTime;
    String phoneNumber;
    String callDirection;

    @Inject
    Application application;

    @Inject
    FixManager fixManager;

    @Inject
    PhoneLogManager phoneLogManager;

    @Inject
    SharedPreferences sharedPreferences;

    public PhoneLogService() {
        super(TAG);
    }

    /**
     * Be careful about logging here. An exception in this class could cause
     * and infinte loop --- SO DON'T USE Timber.e() HERE!
     */
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        ((Application) getApplication()).inject(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null && intent.hasExtra(CALL_TIME)) {
            callTime = intent.getDoubleExtra(CALL_TIME, 0);
        }
        if (intent != null && intent.hasExtra(PHONE_NUMBER)) {
            phoneNumber = intent.getStringExtra(PHONE_NUMBER);
        }
        if (intent != null && intent.hasExtra(CALL_DIRECTION)) {
            callDirection = intent.getStringExtra(CALL_DIRECTION);
        }
        updateLog();
    }

    private void updateLog(){
        String filePath = SMSUtil.getFilePath(application);
        try
        {
            File root = new File(filePath);
            if (!root.exists()) {
                root.mkdirs();
            }
            File logFile = new File(root, Givens.PHONELOG_FILE_NAME);
            FileWriter writer = new FileWriter(logFile,true);

            writer.append("<message");
            writer.append(" datetime=\"" + SMSUtil.getFormattedCurrentDate() + "\"");
            writer.append(" type=\"" + getString(R.string.log_type_phone) + "\"");
            writer.append(" direction=\"" + callDirection + "\"");
            if ( phoneNumber != null) {
                writer.append(" phoneNumber=\"" + phoneNumber.replace("+", "") + "\"");
            } else {writer.append(" phoneNumber=\"" + "\"");}
            writer.append(" duration=\"" + callTime + "\"");

            Location location = fixManager.getLastLocation();
            if (location != null) {
                writer.append(" bearing=\"" +location.getBearing() + "\"");
            }
            writer.append("></message>");
            writer.flush();
            writer.close();

            String settingsPhoneLogs = Fuzz.en(Givens.COLLECT_PHONELOGS_DELIVERY_KEY, application.getDeviceIdentifier());
            if (sharedPreferences.contains(settingsPhoneLogs)) {
                int collectPhoneLogs = sharedPreferences.getInt(settingsPhoneLogs, 0);
                if (collectPhoneLogs==1) {
                    phoneLogManager.submitFile(logFile);
                }
            }
        }
        catch(IOException e)
        {
            //Log.e(TAG, e.getLocalizedMessage());
        }
    }
}
