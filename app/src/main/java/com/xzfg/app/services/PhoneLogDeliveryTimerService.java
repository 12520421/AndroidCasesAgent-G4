package com.xzfg.app.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;

import com.xzfg.app.Application;
import com.xzfg.app.Givens;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.PhoneLogManager;
import com.xzfg.app.util.SMSUtil;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;


public class PhoneLogDeliveryTimerService extends IntentService {
    private static final String TAG = PhoneLogDeliveryTimerService.class.getName();
    public static final String DELIVERY_INTERVAL = TAG + "_deliveryInterval";
    // run on another Thread to avoid crash
    private Handler mHandler = new Handler();
    // timer handling
    private Timer mTimer = null;

    private int deliveryInterval = 7200; //2 hours as default
    private File logFile = null;

    @Inject
    Application application;

    @Inject
    FixManager fixManager;

    @Inject
    PhoneLogManager phoneLogManager;

    public PhoneLogDeliveryTimerService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Be careful about logging here. An exception in this class could cause
     * and infinte loop --- SO DON'T USE Timber.e() HERE!
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        String sFilePath = SMSUtil.getFilePath(application);
        if (sFilePath.length()>0) {
            logFile = new File(SMSUtil.getFilePath(application),  Givens.PHONELOG_FILE_NAME);
        }

        if (intent != null && intent.hasExtra(DELIVERY_INTERVAL)) {
            deliveryInterval = intent.getIntExtra(DELIVERY_INTERVAL, 7200);
        }
        deliveryInterval = deliveryInterval*1000;

        // cancel if already existed
        if(mTimer != null) {
            mTimer.cancel();
        } else {
            // recreate new
            mTimer = new Timer();
        }
        // schedule task
        mTimer.scheduleAtFixedRate(new PhoneLogDeliveryTimerTask(), 0, deliveryInterval);
    }

    class PhoneLogDeliveryTimerTask extends TimerTask {

        @Override
        public void run() {
            // run on another thread
            if(logFile != null && logFile.exists()) {
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        phoneLogManager.submitFile(logFile);
                    }

                });
            }
        }
    }
}
