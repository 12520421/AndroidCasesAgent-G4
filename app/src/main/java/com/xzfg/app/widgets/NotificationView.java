package com.xzfg.app.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.model.AgentRoleComponent;
import com.xzfg.app.model.AgentRoles;

import de.greenrobot.event.EventBus;

/**
 * This widget implements the custom elements in the action bar. It listens for various events, and
 * updates the icons inside it.
 */
public class NotificationView extends LinearLayout implements AgentRoleComponent {

    ImageView chatStatusView;
   // ImageView audioStatusView;
  //  ImageView trackingStatusView;
 //   ImageView videoStatusView;
 //   ImageView fileStatusView;
  //  TextView fileCountTextView;
    ImageView bluetoothStatusView;

    int[] enabledState = new int[]{android.R.attr.state_enabled};
    int[] disabledState = new int[]{-android.R.attr.state_enabled};

    public NotificationView(Context context) {
        super(context);
        init();
    }

    public NotificationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NotificationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void init() {
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contents = inflater.inflate(R.layout.notification_view, this, true);
        //signalStatusButton = (ImageView)contents.findViewById(R.id.signalStatus);

        bluetoothStatusView = (ImageView) contents.findViewById(R.id.bluetoothStatus);
        chatStatusView = (ImageView) contents.findViewById(R.id.chatStatus);
     //   audioStatusView = (ImageView) contents.findViewById(R.id.audioStatus);
     //   videoStatusView = (ImageView) contents.findViewById(R.id.videoStatus);
     //   fileStatusView = (ImageView) contents.findViewById(R.id.fileStatus);
     //   fileCountTextView = (TextView) contents.findViewById(R.id.fileCount);
     //   trackingStatusView = (ImageView) contents.findViewById(R.id.trackingStatus);

        // disable all by default
        chatStatusView.setImageState(disabledState, false);
    //     audioStatusView.setImageState(disabledState, false);
    //      videoStatusView.setImageState(disabledState, false);
    //     fileStatusView.setImageState(disabledState, false);
    //       trackingStatusView.setImageState(disabledState, false);
        bluetoothStatusView.setImageState(disabledState,false);

        EventBus.getDefault().registerSticky(this);
        Context appContext = getContext().getApplicationContext();
        if (appContext instanceof Application) {
            updateRoles(((Application) appContext).getAgentSettings().getAgentRoles());
        }
    }

    /*
    @SuppressWarnings("unused")
    public void onEventMainThread(Events.SignalStatus signalStatus) {
        switch (signalStatus.getLevel()) {
            // 0 bars
            case 0:

            // 1 bar
            case 1:

            // 2 bars
            case 2:

            // 3 bars
            case 3:

            // 4 bars
            case 4:

            // 5 bars
            case 5:
        }

    }
    */

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.TrackingStatus trackingStatus) {
    //    trackingStatusView.setImageState(
    //            trackingStatus.isTracking() ? enabledState : disabledState, false
    //    );
     //   trackingStatusView.setVisibility(trackingStatus.isTracking() ? VISIBLE : GONE);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.ChatStatus chatStatus) {
        chatStatusView.setImageState(
                chatStatus.hasChats() ? enabledState : disabledState, false
        );
     //   chatStatusView.setVisibility(chatStatus.hasChats() ? VISIBLE : GONE);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.FileStatus fileStatus) {
    /*    fileStatusView.setImageState(
                (fileStatus.getCount() > 0) ? enabledState : disabledState, false
        );
        if (fileStatus.getCount() == 0) {
            fileCountTextView.setText(String.valueOf(0));
            fileCountTextView.setVisibility(View.GONE);
        } else {
            fileCountTextView.setText(String.valueOf(fileStatus.getCount()));
            fileCountTextView.setVisibility(View.VISIBLE);
        }
        fileStatusView.setVisibility(fileStatus.getCount() > 0 ? VISIBLE : GONE);*/
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.AudioStatus audioStatus) {
        // if audio is recording, turn on the icon.
      /*  audioStatusView.setImageState(
                audioStatus.isRecording() ? enabledState : disabledState, false
        );
        audioStatusView.setVisibility(audioStatus.isRecording() ? VISIBLE : GONE);*/
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.VideoStatus videoStatus) {
        // if video is recording, turn on the icon.
     /*   videoStatusView.setImageState(
                videoStatus.isRecording() ? enabledState : disabledState, false
        );
        videoStatusView.setVisibility(videoStatus.isRecording() ? VISIBLE : GONE);*/
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.AgentRolesUpdated agentRolesUpdated) {
        updateRoles(agentRolesUpdated.getAgentRoles());
    }
    @SuppressWarnings("unused")
    public void onEventMainThread(Events.BluetoothStatus bluetoothStatus) {
        // if video is recording, turn on the icon.
        bluetoothStatusView.setImageState(
                bluetoothStatus.isBluetoothOn() ? enabledState : disabledState, false
        );
       // bluetoothStatusView.setVisibility(bluetoothStatus.isBluetoothOn() ? VISIBLE : GONE);
    }
    @Override
    public void updateRoles(AgentRoles roles) {
        chatStatusView.setVisibility(roles.chat() ? View.VISIBLE : View.GONE);
     /*   audioStatusView.setVisibility(roles.collect() && (roles.audiorecord() || roles.audiostream() || roles.videorecord()) ? View.VISIBLE : View.GONE);
        videoStatusView.setVisibility(roles.collect() && (roles.videorecord() || roles.videostream()) ? View.VISIBLE : View.GONE);
        fileStatusView.setVisibility(roles.collect() ? View.VISIBLE : View.GONE);
        fileCountTextView.setVisibility(roles.collect() ? View.VISIBLE : View.GONE);
        trackingStatusView.setVisibility(roles.map() ? View.VISIBLE : View.GONE);*/
    }
}
