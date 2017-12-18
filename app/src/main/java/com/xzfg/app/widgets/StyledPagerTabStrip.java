package com.xzfg.app.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v4.view.PagerTabStrip;
import android.util.AttributeSet;

import com.xzfg.app.R;

/**
 * The default PagerTabStrip doesn't have the ability to change the color of the underline, so we
 * provide it.
 */
public class StyledPagerTabStrip extends PagerTabStrip {

    public StyledPagerTabStrip(Context context) {
        super(context);
    }

    public StyledPagerTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.StyledPagerTabStrip);
        int currentTextColor = styledAttributes.getColor(R.styleable.StyledPagerTabStrip_android_textColor, Color.WHITE);
        setTabIndicatorColor(styledAttributes.getColor(R.styleable.StyledPagerTabStrip_indicatorColor, currentTextColor));
        styledAttributes.recycle();
    }
}
