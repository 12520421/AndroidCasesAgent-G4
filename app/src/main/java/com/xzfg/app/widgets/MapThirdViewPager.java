package com.xzfg.app.widgets;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.xzfg.app.R;
import com.xzfg.app.fragments.RemovablePagerAdapter;

import timber.log.Timber;

public class MapThirdViewPager extends ViewPager {
    private static final int TOUCH_SIZE = 48;

    private final int bezelSize;

    // these must be the raw pixel places on the display that you
    // care about monitoring.
    private Integer top = null;
    private Integer bottom = null;
    private Integer left = null;
    private Integer right = null;

    public MapThirdViewPager(Context context) {
        super(context);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        // view is full width, so left is 0, and right is the width of the display;
        left = 0;
        right = displayMetrics.widthPixels;
        bezelSize = (int) (TOUCH_SIZE * displayMetrics.density);
    }

    public MapThirdViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        // view is full width, so left is 0, and right is the width of the display;
        left = 0;
        right = displayMetrics.widthPixels;
        bezelSize = (int) (TOUCH_SIZE * displayMetrics.density);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            if (((RemovablePagerAdapter)getAdapter()).getPageTitle(getCurrentItem()).equals(getContext().getString(R.string.tab_map))  && event.getAction() == MotionEvent.ACTION_DOWN) {
                return takeTouch(event) && super.onTouchEvent(event);
            }
            return super.onTouchEvent(event);
        } catch (IllegalArgumentException e) {
            Timber.d(e, "Illegal Argument Exception Caught");
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        try {
            if (((RemovablePagerAdapter)getAdapter()).getPageTitle(getCurrentItem()).equals(getContext().getString(R.string.tab_map))  && event.getAction() == MotionEvent.ACTION_DOWN) {
                return takeTouch(event) && super.onInterceptTouchEvent(event);
            }
            return super.onInterceptTouchEvent(event);
        } catch (IndexOutOfBoundsException e) {
            Timber.d(e, "Index Out Of Bounds Exception Caught.");
        } catch (IllegalArgumentException e) {
            Timber.d(e, "Illegal Argument Exception Caught.");
        }
        return false;
    }

    private boolean takeTouch(MotionEvent event) {
        synchronized (MapThirdViewPager.class) {
            if (top == null || bottom == null) {
                View mview = findViewById(R.id.map_frame);
                if (mview != null) {
                    int[] location = new int[2];
                    mview.getLocationOnScreen(location);
                    top = location[1];
                }
                mview = findViewById(R.id.bar_holder);
                if (mview != null) {
                    int[] location = new int[2];
                    mview.getLocationOnScreen(location);
                    bottom = location[1];
                }
            }
        }

        if (top == null | bottom == null) {
            return false;
        }

        boolean mTakeTouch = false;

        // left
        if (event.getRawX() <= left + bezelSize) {
            mTakeTouch = true;
        }

        // right
        if (!mTakeTouch && event.getRawX() >= right - bezelSize) {
            mTakeTouch = true;
        }

        // top
        if (!mTakeTouch && event.getRawY() > top && event.getRawY() <= top + bezelSize) {
            mTakeTouch = true;
        }

        // bottom
        if (!mTakeTouch && event.getRawY() < bottom && event.getRawY() >= bottom - bezelSize) {
            mTakeTouch = true;
        }

        return mTakeTouch;
    }

}