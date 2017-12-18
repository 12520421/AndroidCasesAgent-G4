package com.xzfg.app.reporting;

import android.content.Context;
import android.widget.Toast;

import timber.log.Timber;

/**
 * This timber module exposes logging statements as toast messages.
 */
public class ToastReportingTree extends Timber.DebugTree {
    Context context;

    public ToastReportingTree(Context context) {
        this.context = context;
    }

    @Override
    public void i(String message, Object... args) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void i(Throwable t, String message, Object... args) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void v(String message, Object... args) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void v(Throwable t, String message, Object... args) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void d(String message, Object... args) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void d(Throwable t, String message, Object... args) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void e(String message, Object... args) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void e(Throwable t, String message, Object... args) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

}
