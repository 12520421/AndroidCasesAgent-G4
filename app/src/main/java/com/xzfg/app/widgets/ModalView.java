package com.xzfg.app.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.xzfg.app.R;

/**
 */
public class ModalView extends RelativeLayout {
    ViewGroup container;
    Button button;
    AttributeSet attrs;

    public ModalView(Context context) {
        super(context);
        init();
    }

    public ModalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.attrs = attrs;
        init();
    }

    public ModalView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.attrs = attrs;
        init();
    }

    @Override
    public void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
        if (container != null) {
            container.addView(child, index, params);
        } else {
            super.addView(child, index, params);
        }
    }

    private void init() {
        View contents = LayoutInflater.from(getContext()).inflate(R.layout.modal, this, true);
        container = (ScrollView) contents.findViewById(R.id.scroll_container);
        button = (Button) contents.findViewById(R.id.okButton);

        if (this.attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ModalView, 0, 0);
            Drawable d = a.getDrawable(R.styleable.ModalView_buttonDrawable);
            if (d != null) {
                button.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
            }
            String s = a.getString(R.styleable.ModalView_buttonText);
            if (s != null) {
                button.setText(s);
            }
            a.recycle();
        }
    }

    // pass the clicks to the button.
    @Override
    public void setOnClickListener(OnClickListener l) {
        button.setOnClickListener(l);
    }
}
