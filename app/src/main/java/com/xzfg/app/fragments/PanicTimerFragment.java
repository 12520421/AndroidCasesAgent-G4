package com.xzfg.app.fragments;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.managers.AlertManager;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.receivers.PanicAlarmReceiver;

import java.util.Calendar;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;


public class PanicTimerFragment extends Fragment implements View.OnClickListener, View.OnTouchListener {

    private static final long TIMER_ANIMATION_STEP_DURATION = 1000 / 4;
    private static final long PANIC_ANIMATION_STEP_DURATION = 1000 / 4;
    private static final int PANIC_ANIMATION_TOTAL_DURATION = 4 * 1000;

    private View mCancelLayout;
    private View mTimePickerLayout;
    private View mAnimationLayout;
    private ImageButton mPauseButton;
    private TextView mPauseButtonHintView;
    private TimePicker mTimePicker;

    private TextView mPanicAnimationCounterView;
    private TextView mPanicAnimationActionHintView;
    private ImageView mPanicAnimationImageOddView;
    private ImageView mPanicAnimationImageEvenView;
    private Handler mTimerAnimationHandler;
    private Handler mPanicAnimationHandler;
    private Calendar mPanicAnimationEndTime;

    // Save fragment state
    private static class TimerState {
        int hour = 0;
        int minute = 0;
    }

    private static TimerState mState = null;

    private void saveState() {
        mState = new TimerState();
        if (Build.VERSION.SDK_INT >= 23 ) {
            mState.hour = mTimePicker.getHour();
            mState.minute = mTimePicker.getMinute();
        } else {
            //noinspection deprecation
            mState.hour = mTimePicker.getCurrentHour();
            //noinspection deprecation
            mState.minute = mTimePicker.getCurrentMinute();
        }
    }

    private void restoreState() {
        if (mState != null) {
            if (Build.VERSION.SDK_INT >= 23 ) {
                mTimePicker.setHour(mState.hour);
                mTimePicker.setMinute(mState.minute);
            } else {
                //noinspection deprecation
                mTimePicker.setCurrentHour(mState.hour);
                //noinspection deprecation
                mTimePicker.setCurrentMinute(mState.minute);
            }
            // Forget saved state
            mState = null;
        }
    }

    @Inject
    Application application;
    @Inject
    AlertManager alertManager;


    private Runnable mTimerAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            // Update countdown
            long remainingTime = updateRemainingTime();

            // Swap images every quarter of second during last 3 seconds
            if (remainingTime <= 3000) {
                boolean even = (mPanicAnimationImageOddView.getVisibility() == View.VISIBLE);
                mPanicAnimationImageOddView.setVisibility(even ? View.GONE : View.VISIBLE);
                mPanicAnimationImageEvenView.setVisibility(even ? View.VISIBLE : View.GONE);
            }

            if (remainingTime > 0) {
                // Restart timer
              //  mTimerAnimationHandler.postDelayed(this, TIMER_ANIMATION_STEP_DURATION);
            } else {
                // Stop timer animation
                // AlarmManager will start panic mode separately
                stopTimerAnimation();
                // Clear panic timer date
                AgentSettings settings = application.getAgentSettings();
                settings.setPanicTimerDate(null);
                settings.setRemainingPanicTimer(-1);
                EventBus.getDefault().post(new Events.AgentSettingsAcquired(settings));
            }
        }
    };

    private Runnable mPanicAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            // Update countdown
            long remainingTime = mPanicAnimationEndTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
            long seconds = (remainingTime / 1000) % 60;
            mPanicAnimationCounterView.setText(String.valueOf(seconds));

            if (seconds > 0) {
                // Swap images every quarter of second
                boolean even = (mPanicAnimationImageOddView.getVisibility() == View.VISIBLE);
                mPanicAnimationImageOddView.setVisibility(even ? View.GONE : View.VISIBLE);
                mPanicAnimationImageEvenView.setVisibility(even ? View.VISIBLE : View.GONE);
                // Restart timer
                mPanicAnimationHandler.postDelayed(this, PANIC_ANIMATION_STEP_DURATION);
            } else {
                // Stop animation
                stopPanicAnimation();
                stopTimerAnimation();
                // Initiate Panic mode
                alertManager.startPanicMode(false);
                // Clear panic timer date
                AgentSettings settings = application.getAgentSettings();
                settings.setPanicTimerDate(null);
                settings.setRemainingPanicTimer(-1);
                EventBus.getDefault().post(new Events.AgentSettingsAcquired(settings));
            }
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_panic_timer, viewGroup, false);

        mCancelLayout = v.findViewById(R.id.cancel_layout);
        mTimePickerLayout = v.findViewById(R.id.timer_layout);
        mAnimationLayout = v.findViewById(R.id.animation_layout);
        mTimePicker = (TimePicker) v.findViewById(R.id.panic_time_picker);
        mTimePicker.setIs24HourView(true);
        mTimePicker.setCurrentHour(0);
        mTimePicker.setCurrentMinute(0);
        mPanicAnimationCounterView = (TextView) v.findViewById(R.id.animation_counter);
        mPanicAnimationActionHintView = (TextView) v.findViewById(R.id.animation_action_hint);
        mPanicAnimationImageOddView = (ImageView) v.findViewById(R.id.animation_image_odd);
        mPanicAnimationImageEvenView = (ImageView) v.findViewById(R.id.animation_image_even);

        mPauseButton = (ImageButton) v.findViewById(R.id.panic_pause_button);
        mPauseButton.setOnClickListener(this);
        mPauseButtonHintView = (TextView) v.findViewById(R.id.panic_pause_hint);
        v.findViewById(R.id.cancel).setOnClickListener(this);
        v.findViewById(R.id.panic_timer_button).setOnClickListener(this);
        v.findViewById(R.id.panic_start_button).setOnTouchListener(this);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);

        if (application.getAgentSettings().getPanicTimerDate() != null ||
                application.getAgentSettings().getRemainingPanicTimer() >= 0) {
            startTimerAnimation();
        } else {
            restoreState();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        EventBus.getDefault().unregister(this);
        saveState();
        stopTimerAnimation();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Application) activity.getApplication()).inject(this);
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = ((ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    public void onEventMainThread(Events.PanicAlertCompleted event) {
        // Stop countdown animation if alert completed
        stopTimerAnimation();
    }

    // Set panic alarm
    private void setPanicAlarm(Calendar time) {
        Context ctx = application;
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        // Set alarm
        Intent alarmIntent = new Intent(ctx, PanicAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pendingIntent);

        // Remember panic date
        AgentSettings settings = application.getAgentSettings();
        settings.setPanicTimerDate(time);
        settings.setRemainingPanicTimer(-1);
        EventBus.getDefault().post(new Events.AgentSettingsAcquired(settings));
    }

    // Cancel panic alarm
    private void cancelPanicAlarm() {
        Context ctx = application;
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        // Cancel panic alarm
        Intent alarmIntent = new Intent(ctx, PanicAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);

        // Clear panic timer date
        AgentSettings settings = application.getAgentSettings();
        settings.setPanicTimerDate(null);
        settings.setRemainingPanicTimer(-1);
        EventBus.getDefault().post(new Events.AgentSettingsAcquired(settings));
    }

    // Pause panic alarm
    private void pausePanicAlarm() {
        // Stop runnable
        if (mTimerAnimationHandler != null) {
            mTimerAnimationHandler.removeCallbacks(mTimerAnimationRunnable);
            mTimerAnimationHandler = null;
        }

        // Cancel panic alarm
        AgentSettings settings = application.getAgentSettings();
        // Important: Get remaining time before cancelling timer - it will be set to NULL
        long remainingTime = settings.getPanicTimerDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        cancelPanicAlarm();
        // Remember remaining time
        settings.setPanicTimerDate(null);
        settings.setRemainingPanicTimer(remainingTime);
        EventBus.getDefault().post(new Events.AgentSettingsAcquired(settings));

        // Rename PAUSE button and hint
        mPauseButton.setContentDescription(getString(R.string.resume));
        mPauseButton.setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.starttimer));
        mPauseButtonHintView.setText(getString(R.string.resume_timer));
    }

    // Resume panic alarm
    private void resumePanicAlarm() {
        // Restore panic alarm
        AgentSettings settings = application.getAgentSettings();
        Calendar panicDate = Calendar.getInstance();
        panicDate.setTimeInMillis(panicDate.getTimeInMillis() + settings.getRemainingPanicTimer());
        // This will also set "panic time" and clear "remaining time counter"
        setPanicAlarm(panicDate);

        // Rename PAUSE button and hint
        mPauseButton.setContentDescription(getString(R.string.pause));
        mPauseButton.setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.pausetimer));
        mPauseButtonHintView.setText(getString(R.string.pause_timer));
        // Start runnable
        mTimerAnimationHandler = new Handler();
        mTimerAnimationHandler.postDelayed(mTimerAnimationRunnable, 0);

    }

    // Updates remaining panic countdown time and returns remaining time in milliseconds
    private long updateRemainingTime() {
        Calendar panicTime = application.getAgentSettings().getPanicTimerDate();
        long remainingTime = application.getAgentSettings().getRemainingPanicTimer();
        if (panicTime != null)
            remainingTime = panicTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

        long hours = ((remainingTime / (1000 * 60 * 60)) % 24);
        long minutes = ((remainingTime / (1000 * 60)) % 60);
        long seconds = (remainingTime / 1000) % 60;

        mPanicAnimationCounterView.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

        return remainingTime;
    }


    // Display animation and start countdown
    private void startPanicAnimation() {
        mPanicAnimationEndTime = Calendar.getInstance();
        mPanicAnimationEndTime.add(Calendar.MILLISECOND, PANIC_ANIMATION_TOTAL_DURATION);

        // Hide time picker
        mTimePickerLayout.setVisibility(View.GONE);

        // Update countdown text
        mPanicAnimationCounterView.setText(String.valueOf(PANIC_ANIMATION_TOTAL_DURATION / 1000));
        mPanicAnimationActionHintView.setText(getString(R.string.release_panic_button));
        // Set PAUSE button and hint
        mPauseButton.setContentDescription(getString(R.string.pause));
        mPauseButton.setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.pausetimer));
        mPauseButtonHintView.setText(getString(R.string.pause_timer));
        // Show animation layout
        mAnimationLayout.setVisibility(View.VISIBLE);
        // Start runnable
        mPanicAnimationHandler = new Handler();
        mPanicAnimationHandler.postDelayed(mPanicAnimationRunnable, 0);
    }

    // Stop countdown and hide animation
    private void stopPanicAnimation() {
        if (mPanicAnimationHandler != null) {
            // Stop runnable
            mPanicAnimationHandler.removeCallbacks(mPanicAnimationRunnable);
            mPanicAnimationHandler = null;
        }
        // Update countdown text
        mPanicAnimationActionHintView.setText(getString(R.string.stop_cancel_pause_timer));
    }

    // Stop timer countdown and hide animation
    private void startTimerAnimation() {
        // Hide time picker
        mTimePickerLayout.setVisibility(View.GONE);
        // Show Cancel button
        mCancelLayout.setVisibility(View.VISIBLE);

        // Update action hint text
        mPanicAnimationActionHintView.setText(getString(R.string.stop_cancel_pause_timer));
        // Paused state
        if (application.getAgentSettings().getRemainingPanicTimer() >= 0) {
            // Rename PAUSE button and hint
            mPauseButton.setContentDescription(getString(R.string.resume));
            mPauseButton.setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.starttimer));
            mPauseButtonHintView.setText(getString(R.string.resume_timer));
        }
        // Update remaining time text
        updateRemainingTime();

        // Show animation layout
        mAnimationLayout.setVisibility(View.VISIBLE);
        // Start runnable
        mTimerAnimationHandler = new Handler();
        mTimerAnimationHandler.postDelayed(mTimerAnimationRunnable, 0);
    }

    private void stopTimerAnimation() {
        // Stop runnable
        if (mTimerAnimationHandler != null) {
            mTimerAnimationHandler.removeCallbacks(mTimerAnimationRunnable);
            mTimerAnimationHandler = null;
        }
        // Hide animation layout
        mAnimationLayout.setVisibility(View.GONE);
        // Show time picker layout
        mTimePickerLayout.setVisibility(View.VISIBLE);
        // Hide Cancel button
        mCancelLayout.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                // Clear panic timer date
                cancelPanicAlarm();
                stopTimerAnimation();
                break;

            case R.id.panic_timer_button:
                // Initiate panic timer
                if (0 == mTimePicker.getCurrentHour() + mTimePicker.getCurrentMinute()) {
                    Toast.makeText(getActivity(), "Please set panic timeout.", Toast.LENGTH_SHORT).show();
                } else {
                    Calendar panicDate = Calendar.getInstance();
                    panicDate.add(Calendar.HOUR_OF_DAY, mTimePicker.getCurrentHour());
                    panicDate.add(Calendar.MINUTE, mTimePicker.getCurrentMinute());
                    AgentSettings settings = application.getAgentSettings();
                    settings.setPanicTimerDate(panicDate);
                    EventBus.getDefault().post(new Events.AgentSettingsAcquired(settings));
                    setPanicAlarm(panicDate);
                    startTimerAnimation();
                }
                break;

            case R.id.panic_pause_button:
                // pause panic timer
                if (application.getAgentSettings().getRemainingPanicTimer() < 0) {
                    pausePanicAlarm();
                } else {
                    resumePanicAlarm();
                }
                break;

            default:
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.panic_start_button: {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // display animation and start countdown
                        stopTimerAnimation();
                        startPanicAnimation();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // ignore
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // stop countdown and hide animation
                        stopPanicAnimation();
                        startTimerAnimation();
                        return true;
                }
                break;
            }
        }

        return false;
    }
}

