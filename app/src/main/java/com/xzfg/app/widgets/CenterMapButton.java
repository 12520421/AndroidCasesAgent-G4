package com.xzfg.app.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

import com.xzfg.app.Events;
import com.xzfg.app.R;

import de.greenrobot.event.EventBus;

/**
 *
 */
public class CenterMapButton extends ImageButton {
    public CenterMapButton(Context context) {
        this(context, null);
    }

    public CenterMapButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        EventBus.getDefault().registerSticky(this);
    }

    public CenterMapButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        EventBus.getDefault().registerSticky(this);
    }


    @SuppressWarnings("deprecation")
    public void onEventMainThread(Events.MapCentered event) {
       /* if (event.isCentered()) {
            setImageDrawable(getResources().getDrawable(R.drawable.center_off));
            this.setEnabled(false);
        } else {*/
            setImageDrawable(getResources().getDrawable(R.drawable.center_on));
            this.setEnabled(true);
       // }
    }
}
