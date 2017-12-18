package com.xzfg.app.receivers;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

import com.xzfg.app.Application;
import com.xzfg.app.R;
import com.xzfg.app.services.PhoneLogService;
import com.xzfg.app.services.PhoneService;


public class PhoneReceiver extends BroadcastReceiver {
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private String phone_number = "";
    private double call_time = 0;
    private String call_direction = "Incoming";
    private String call_prev_state = "";
    private long cStart = 0;
    private long cEnd = 0;


    @SuppressLint("ApplySharedPref")
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = context.getSharedPreferences("PhoneReceiver", Context.MODE_PRIVATE);
        Application app = (Application) context.getApplicationContext();
        int recordPhoneCalls = app.getRecordPhoneCalls();
        boolean collectPhoneLogs = app.getPhoneLogs();
        String prev_phone_number = phone_number;
        phone_number = context.getString(R.string.outgoing_phonenumber);

        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
            call_direction = "Outgoing";
            cStart = System.currentTimeMillis();
            if (intent.hasExtra(Intent.EXTRA_PHONE_NUMBER)) {
                phone_number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                prev_phone_number = phone_number;
            }
            if (recordPhoneCalls == 1 || recordPhoneCalls == 2) {
                startRecording(context, phone_number);
            }
        } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            // Check that it is incoming call - phone was ringing
            if (call_prev_state != null && call_prev_state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                call_direction = "Incoming";
                cStart = System.currentTimeMillis();
                phone_number = preferences.getString("phone_number", context.getString(R.string.outgoing_phonenumber));
                if (recordPhoneCalls == 1 || recordPhoneCalls == 3) {
                    startRecording(context, phone_number);
                }
            }
            // This is for outgouing calls on Nexus 6 AT&T
            else if (call_prev_state == null && call_direction.equals("Outgoing")) {
                phone_number = prev_phone_number;
            }
        } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            cEnd = System.currentTimeMillis();
            long cDelta = cEnd - cStart;
            call_time = cDelta / 1000.0;
            if (recordPhoneCalls > 0) {
                stopRecording(context);
            }
            if (collectPhoneLogs) {
                logPhoneCall(context, prev_phone_number, call_time, call_direction);
                //Timber.d("LOGG: " +  prev_phone_number + "   " + call_direction);
            }
            cStart = 0;
            cEnd = 0;
            call_time = 0;
        } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            if (intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
                phone_number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("phone_number", phone_number);
                editor.commit();
            }
        }

        call_prev_state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
    }

    private void startRecording(Context context, String phoneNumber) {
        Intent intent = new Intent(context, PhoneService.class);
        intent.putExtra(PhoneService.ACTION, PhoneService.CALL_STARTED);
        intent.putExtra(PhoneService.PHONE_NUMBER, phoneNumber);
        context.startService(intent);
    }

    private void stopRecording(Context context) {
        Intent intent = new Intent(context, PhoneService.class);
        intent.putExtra(PhoneService.ACTION, PhoneService.CALL_STOPPED);
        context.startService(intent);
    }

    private void logPhoneCall(Context context, String phoneNumber, Double callTime, String direction) {
        Intent intent = new Intent(context, PhoneLogService.class);
        intent.putExtra(PhoneLogService.CALL_TIME, callTime);
        intent.putExtra(PhoneLogService.PHONE_NUMBER, phoneNumber);
        intent.putExtra(PhoneLogService.CALL_DIRECTION, direction);
        context.startService(intent);
    }
}
