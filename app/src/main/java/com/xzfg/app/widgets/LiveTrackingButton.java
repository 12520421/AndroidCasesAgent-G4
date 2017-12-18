package com.xzfg.app.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import com.xzfg.app.Events;
import com.xzfg.app.R;

import de.greenrobot.event.EventBus;

/**
 *
 */
public class LiveTrackingButton extends Button {
    public LiveTrackingButton(Context context) {
        this(context, null);
    }

    public LiveTrackingButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        EventBus.getDefault().registerSticky(this);
    }

    public LiveTrackingButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        EventBus.getDefault().registerSticky(this);
    }

    @SuppressWarnings("deprecation")
    public void onEventMainThread(Events.TrackingStatus event) {
        if (event.isTracking()) {
            this.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.livetrack_on), null, null, null);
        } else {
            this.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.livetrack_off), null, null, null);
        }
    }

}
