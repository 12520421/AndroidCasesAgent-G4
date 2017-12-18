package com.xzfg.app.fragments.dialogs;

import android.os.Bundle;

import com.xzfg.app.Events;

import de.greenrobot.event.EventBus;
import timber.log.Timber;


/**
 */
public class CreatePoiProgressDialogFragment extends IndeterminateProgressDialogFragment {


    public static CreatePoiProgressDialogFragment newInstance(String title, String message) {
        CreatePoiProgressDialogFragment f = new CreatePoiProgressDialogFragment();
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
    public static CreatePoiProgressDialogFragment newInstance(String message) {
        return CreatePoiProgressDialogFragment.newInstance(null, message);
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
    public void onEventMainThread(Events.PoiCreated poiCreatedEvent) {
        // if the registration failed, dismiss this, after suspending listeners.
        if (!poiCreatedEvent.getStatus()) {
            Timber.e("Registration failure received in ProgressDialog - suspending listeners and dismissing");
            suspendListeners(true);
            dismiss();
            suspendListeners(false);
        }
    }

}
