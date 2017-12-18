package com.xzfg.app.fragments.dialogs;

import android.os.Bundle;

import com.xzfg.app.Events;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 */
public class RegistrationProgressDialogFragment extends IndeterminateProgressDialogFragment {


    public static RegistrationProgressDialogFragment newInstance(String title, String message) {
        RegistrationProgressDialogFragment f = new RegistrationProgressDialogFragment();
        Bundle args = new Bundle();
        if (title != null) {
            args.putString("title", title);
        }
        if (message != null) {
            args.putString("message", message);
        }
        f.setArguments(args);
        return f;
    }

    /**
     * Gets a new dialog instance, but with no title. Will
     * use the R.string.error_title.
     */
    public static RegistrationProgressDialogFragment newInstance(String message) {
        return RegistrationProgressDialogFragment.newInstance(null, message);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.Registration registrationEvent) {
        // if the registration failed, dismiss this, after suspending listeners.
        if (!registrationEvent.getStatus()) {
            Timber.e("Registration failure received in ProgressDialog - suspending listeners and dismissing");
            suspendListeners(true);
            dismiss();
            suspendListeners(false);
        }
    }

}
