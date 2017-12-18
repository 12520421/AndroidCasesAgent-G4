package com.xzfg.app.fragments.home;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.managers.AlertManager;
import com.xzfg.app.model.AgentProfile;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.services.ProfileService;
import com.xzfg.app.services.SMSService;
import com.xzfg.app.util.DateUtil;
import com.xzfg.app.util.ImageUtil;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;


public class HomeFragment extends Fragment implements View.OnClickListener, View.OnTouchListener {

    private static final long PANIC_ANIMATION_STEP_DURATION = 1000;
    private static final int PANIC_ANIMATION_STEPS = 3;
    private View mAvatarLayout;
    private View mPanicLayout;
    private View mAddressLayout;
    private View mAnimationLayout;
    private View mPanicButtonLayout;
    private View mEmergencyButtonLayout;
    private View mPanicButton;
    private View mEmergencyButton;
    private ImageView mAvatarView;
    private TextView mNameView;
    private TextView mLastAddressView;
    private TextView mLastAddressDateView;
    private TextView mAddPhotoView;
    private TextView mPanicAnimationCounterView;
    private ImageView mPanicAnimationImageOddView;
    private ImageView mPanicAnimationImageEvenView;
    private Handler mPanicAnimationHandler;
    private Integer mPanicAnimationStep;
    private TextView panicMessage;

    @Inject
    Application application;
    @Inject
    AlertManager alertManager;

    private Runnable mPanicAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            mPanicAnimationStep--;
            //Timber.d("PANIC: step = " + mPanicAnimationStep + ", of: " + PANIC_ANIMATION_STEPS);

            if (mPanicAnimationStep <= 0) {
                // Stop animation
                stopPanicAnimation();
                // Initiate Panic mode
                Toast.makeText(getActivity(), "Going to Panic mode.", Toast.LENGTH_SHORT).show();
                alertManager.startPanicMode(false);
                showPanic();
            } else {
                // Update countdown text
                mPanicAnimationCounterView.setText(mPanicAnimationStep.toString());
                // Update animation image
                boolean even = ((mPanicAnimationStep & 1) == 0);
                mPanicAnimationImageOddView.setVisibility(even ? View.GONE : View.VISIBLE);
                mPanicAnimationImageEvenView.setVisibility(even ? View.VISIBLE : View.GONE);
                // Restart timer
            //    mPanicAnimationHandler.postDelayed(this, PANIC_ANIMATION_STEP_DURATION);
            }
        }
    };


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Application) activity.getApplication()).inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, viewGroup, false);

        AgentSettings settings = application.getAgentSettings();
        AgentProfile profile = application.getAgentProfile();

        v.findViewById(R.id.update).setOnClickListener(this);
        v.findViewById(R.id.call_button).setOnClickListener(this);
        v.findViewById(R.id.cancel_panic).setOnClickListener(this);
        mPanicButtonLayout = v.findViewById(R.id.panic_button_layout);
        mPanicButton = v.findViewById(R.id.panic_button);
        mPanicButton.setOnTouchListener(this);
        mEmergencyButtonLayout = v.findViewById(R.id.emergency_button_layout);

        mEmergencyButton = v.findViewById(R.id.emergency_button);
        mEmergencyButton.setOnClickListener(this);
        if (!TextUtils.isEmpty(settings.getSOSLabel())) {
            TextView emergencyButtonLabel = (TextView)v.findViewById(R.id.emergency_button_label);
            emergencyButtonLabel.setText(settings.getSOSLabel());
        }

        // Hide "Call Ops Center" button if not allowed
        if (TextUtils.isEmpty(settings.getNonEmergencyNumber())) {
                v.findViewById(R.id.call_button_layout).setVisibility(View.GONE);
                v.findViewById(R.id.home_panic_filler).setVisibility(View.GONE);
                ViewGroup.LayoutParams params = mPanicButtonLayout.getLayoutParams();
                params.width = MATCH_PARENT;
                mPanicButtonLayout.setLayoutParams(params);
                params = mEmergencyButtonLayout.getLayoutParams();
                params.width = MATCH_PARENT;
                mEmergencyButtonLayout.setLayoutParams(params);
        }
        else {
            if (!TextUtils.isEmpty(settings.getNonEmergencyLabel())) {
                TextView callButtonLabel = (TextView) v.findViewById(R.id.call_button_label);
                callButtonLabel.setText(settings.getNonEmergencyLabel());
            }
        }

        mNameView = (TextView) v.findViewById(R.id.name);
        // profile is NULL after registration
        if (profile != null && profile.getName() != null) {
            mNameView.setText(profile.getName());
        }

        mAddPhotoView = (TextView) v.findViewById(R.id.add_photo);
        mAddPhotoView.setOnClickListener(this);

        mLastAddressDateView = (TextView) v.findViewById(R.id.date);
        mLastAddressView = (TextView) v.findViewById(R.id.address);
        mLastAddressView.setOnClickListener(this);
        onEventMainThread(new Events.ProfileLastFixReceived(settings.getLastAddress(), settings.getLastAddressDate()));

        mAvatarLayout = v.findViewById(R.id.avatar_layout);
        mPanicLayout = v.findViewById(R.id.panic_layout);
        mAddressLayout = v.findViewById(R.id.address_layout);
        mAnimationLayout = v.findViewById(R.id.animation_layout);
        mAvatarView = (ImageView) v.findViewById(R.id.avatar);
        mPanicAnimationCounterView = (TextView) v.findViewById(R.id.animation_counter);
        mPanicAnimationImageOddView = (ImageView) v.findViewById(R.id.animation_image_odd);
        mPanicAnimationImageEvenView = (ImageView) v.findViewById(R.id.animation_image_even);
        panicMessage = (TextView) v.findViewById(R.id.panic_message);

        // if there's a custom panic message, use it.
        if (!TextUtils.isEmpty(settings.getPanicMessage())) {
            panicMessage.setText(settings.getPanicMessage());
        }
        else {
            // otherwise, check to see if there's a panic_message_number defined,
            // and if the user has a panic number, use that as the panic message.
            if (!TextUtils.isEmpty(settings.getSOSNumber()) && !TextUtils
                .isEmpty(getString(R.string.panic_message_number))) {
                String messageText = getString(R.string.panic_message_number,
                    settings.getSOSNumber());
                if (messageText.length() > settings.getSOSNumber().length()) {
                    panicMessage.setText(messageText);
                }
            }
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);

        // resume Panic mode
        if (application.getAgentSettings().getPanicState() != PanicStates.PANIC_OFF.getValue()&& application.getAgentSettings().getPanicState() != PanicStates.PANIC_DURESS.getValue()) {
            showPanic();
        }

        // load profile photo
        ProfileService.loadAvatar(getActivity(), false);

        // request last address
        ProfileService.getLastAddress(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(Events.AgentSettingsAcquired agentSettingsAcquired) {
        if (agentSettingsAcquired.getAgentSettings().getPanicState() != PanicStates.PANIC_OFF.getValue() && agentSettingsAcquired.getAgentSettings().getPanicState() != PanicStates.PANIC_DURESS.getValue()) {
            showPanic();
        }
        else {
            hidePanic();
        }
    }


    public void onEventMainThread(Events.DisplayChanged event) {
        // resume Panic mode
        if (event.getId() ==  R.id.home) {
            if (application.getAgentSettings().getPanicState() != PanicStates.PANIC_OFF.getValue()&&application.getAgentSettings().getPanicState() != PanicStates.PANIC_DURESS.getValue()) {
                showPanic();
            }
        }
    }

    public void onEventMainThread(Events.ProfilePhotoLoaded event) {
        //Timber.d("Photo loaded.");
        if (event.getImage() != null && event.getImage().length > 0) {
            ImageUtil.setImageView(mAvatarView, event.getImage(), -1, false, true);
            mAddPhotoView.setText(R.string.edit);
        } else {
            mAvatarView.setImageResource(R.drawable.avatar);
            mAddPhotoView.setText(R.string.add_photo);
        }
    }

    public void onEventMainThread(Events.ProfileLastFixReceived event) {
        //Timber.d("Last address received.");
        mLastAddressDateView.setText(DateUtil.formatLastAddressDate(getActivity(), event.getDate()));

        if (event.getAddress() != null && !event.getAddress().isEmpty()) {
            SpannableString spanStr = new SpannableString(event.getAddress());
            spanStr.setSpan(new UnderlineSpan(), 0, spanStr.length(), 0);
            mLastAddressView.setText(spanStr);
        } else {
            mLastAddressView.setText(getActivity().getString(R.string.unknown));
        }
    }

    // Show panic mode UI.
    private void showPanic() {
        // Hide Avatar layout
        mAvatarLayout.setVisibility(View.GONE);
        // Show Panic layout
        mPanicLayout.setVisibility(View.VISIBLE);
        // Hide Panic button layout (button+description)
        mPanicButtonLayout.setVisibility(View.GONE);

        // Show "Dial 911" button only if allowed
        AgentSettings settings = application.getAgentSettings();
        if (!TextUtils.isEmpty(settings.getSOSNumber())) {
            mEmergencyButtonLayout.setVisibility(View.VISIBLE);
        }

    }

    // Stop panic mode
    private void stopPanic(String pin, String reason) {
        // Check for duress pin
        AgentSettings settings = application.getAgentSettings();
        boolean duress = pin.equals(((Integer)(settings.getPanicDuressPin())).toString());
        alertManager.stopPanicMode(duress, reason);
        hidePanic();
    }

    public void hidePanic() {
        // Hide Panic layout
        mPanicLayout.setVisibility(View.GONE);
        // Show Avatar layout
        mAvatarLayout.setVisibility(View.VISIBLE);
        // Hide 911 button layout (button+description)
        mEmergencyButtonLayout.setVisibility(View.GONE);
        // Show Panic button layout (button+description)
        mPanicButtonLayout.setVisibility(View.VISIBLE);
    }

    // Display animation and start countdown
    private void startPanicAnimation() {
        mPanicAnimationStep = PANIC_ANIMATION_STEPS;
        // Update countdown text
        mPanicAnimationCounterView.setText(mPanicAnimationStep.toString());
        // Update animation image
        boolean even = ((mPanicAnimationStep & 1) == 0);
        mPanicAnimationImageOddView.setVisibility(even ? View.GONE : View.VISIBLE);
        mPanicAnimationImageEvenView.setVisibility(even ? View.VISIBLE : View.GONE);
        // Hide avatar and address layouts
        mAvatarLayout.setVisibility(View.GONE);
        mAddressLayout.setVisibility(View.GONE);
        // Show animation layout
        mAnimationLayout.setVisibility(View.VISIBLE);
        mPanicAnimationHandler = new Handler();
       // mPanicAnimationHandler.postDelayed(mPanicAnimationRunnable, PANIC_ANIMATION_STEP_DURATION);
    }

    // Stop countdown and hide animation
    private void stopPanicAnimation() {
        if (mPanicAnimationHandler != null) {
            mAvatarLayout.setVisibility(View.VISIBLE);
            mAddressLayout.setVisibility(View.VISIBLE);
            mAnimationLayout.setVisibility(View.GONE);
            mPanicAnimationHandler.removeCallbacks(mPanicAnimationRunnable);
            mPanicAnimationHandler = null;
        }
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = ((ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_photo: {
                // Pick image
               // application.disableBossMode();
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                getActivity().startActivityForResult(Intent.createChooser(intent, getString(R.string.select_photo)), Givens.SELECT_PROFILE_PHOTO);
                break;
            }
            case R.id.update: {
                // Force send fix
                Toast.makeText(getActivity(), "Updating last address.", Toast.LENGTH_SHORT).show();
                Application application = (Application)(getActivity().getApplication());
                if (!isConnected() && application.getAgentSettings().getSMSPhoneNumber() != null) {
                        Intent smsIntent = new Intent(application, SMSService.class);
                        smsIntent.putExtra(SMSService.TYPE_IDENTIFIER, SMSService.TYPE_POSITION);
                        smsIntent.putExtra(SMSService.PUBLIC_IDENTIFIER, SMSService.PUBLIC);
                        application.startService(smsIntent);
                } else {
                    EventBus.getDefault().post(new Events.SendFixes());
                }
                break;
            }
            case R.id.call_button: {
                if (!TextUtils.isEmpty(application.getAgentSettings().getNonEmergencyNumber())) {
                    // Call operations center
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + application.getAgentSettings().getNonEmergencyNumber()));
                    startActivity(intent);
                }
                break;
            }
            case R.id.emergency_button: {
                if (!TextUtils.isEmpty(application.getAgentSettings().getSOSNumber())) {
                    // Call 911
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + application.getAgentSettings().getSOSNumber()));
                    startActivity(intent);
                }
                break;
            }
            case R.id.address: {
                // Open map
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + mLastAddressView.getText().toString()));
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    intent.setData(Uri.parse("http://maps.google.com/maps?q=" + mLastAddressView.getText().toString()));
                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
                break;
            }
            case R.id.cancel_panic: {
                // Open UI to cancel panic mode
                AgentSettings settings = application.getAgentSettings();
                CancelPanicListDialogFragment dlg = CancelPanicListDialogFragment.newInstance(settings.getPanicPin(), settings.getPanicDuressPin());
                dlg.show(getChildFragmentManager(), "cancel_panic_fragment");
                break;
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.panic_button: {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // display animation and start countdown
                        startPanicAnimation();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // ignore
                        break;

                    case MotionEvent.ACTION_UP:
                        // stop countdown and hide animation
                        stopPanicAnimation();
                        return true;
                }
                break;
            }
        }

        return false;
    }
}
