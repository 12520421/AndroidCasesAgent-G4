package com.xzfg.app.fragments.setup;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.soundcloud.android.crop.Crop;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.activities.MainActivity;
import com.xzfg.app.activities.SetupActivity;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.fragments.dialogs.RegistrationProgressDialogFragment;
import com.xzfg.app.fragments.dialogs.YesNoDialogFragment;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.services.CreateAccountService;
import com.xzfg.app.services.ProfileService;
import com.xzfg.app.util.ImageUtil;

import java.io.File;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class SignupFragment extends Fragment implements ConnectionAwareness {

    public TextView addPhotoButton;
    public View registerButton;
    private ImageView avatarView;
    public EditText urlInput;
    public EditText portInput;
    public EditText orgNameInput;
    public EditText nameInput;
    public EditText usernameInput;
    public EditText passwordInput;
    public EditText confirmPasswordInput;
    public EditText phoneNumberInput;
    public EditText emailInput;
    private AlertDialogFragment alertDialogFragment;
    private YesNoDialogFragment yesNoDialogFragment;
    private RegistrationProgressDialogFragment registrationProgress;
    private Uri contentUri = null;

    private static final String DIALOG_TAG = SignupFragment.class + "_DIALOG";

    @Inject
    public Application application;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            ((Application) ((Activity) context).getApplication()).inject(this);
        }
    }

    @Override
    public void onResume() {
        EventBus.getDefault().registerSticky(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        if (application.getAgentSettings() != null && application.getAgentSettings().getBoss() == 1) {
            if (registrationProgress != null) {
                registrationProgress.dismiss();
                registrationProgress = null;
            }
            if (alertDialogFragment != null) {
                alertDialogFragment.dismiss();
                alertDialogFragment = null;
            }
            if (yesNoDialogFragment != null) {
                yesNoDialogFragment.dismiss();
                yesNoDialogFragment = null;
            }
        }
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_signup, container, false);
        rootView.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dismiss keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && getActivity().getCurrentFocus() != null) {
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                }
                getFragmentManager().popBackStackImmediate();
            }
        });

        addPhotoButton = (TextView) rootView.findViewById(R.id.add_photo);
        addPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Pick image
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_photo)), Givens.SELECT_PROFILE_PHOTO);
            }
        });
        registerButton = rootView.findViewById(R.id.next);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validate(true);
            }
        });
        avatarView = (ImageView) rootView.findViewById(R.id.avatar);

        urlInput = (EditText) rootView.findViewById(R.id.url);

        portInput = (EditText) rootView.findViewById(R.id.port);

        if (BuildConfig.DEFAULT_URL.length() > 0) {
            urlInput.setVisibility(View.GONE);
            portInput.setVisibility(View.GONE);
            rootView.findViewById(R.id.urlText).setVisibility(View.GONE);
            rootView.findViewById(R.id.portText).setVisibility(View.GONE);
            urlInput.setText(BuildConfig.DEFAULT_URL);
        }

        portInput.setText(String.valueOf(BuildConfig.DEFAULT_PORT));

        orgNameInput = (EditText) rootView.findViewById(R.id.org);
        if (BuildConfig.DEFAULT_ORG.length() > 0) {
            orgNameInput.setVisibility(View.GONE);
            rootView.findViewById(R.id.orgText).setVisibility(View.GONE);
            orgNameInput.setText(String.valueOf(BuildConfig.DEFAULT_ORG));
        }

        nameInput = (EditText) rootView.findViewById(R.id.name);
        usernameInput = (EditText) rootView.findViewById(R.id.user);
        passwordInput = (EditText) rootView.findViewById(R.id.password);
        confirmPasswordInput = (EditText) rootView.findViewById(R.id.confpassword);
        emailInput = (EditText) rootView.findViewById(R.id.email);
        phoneNumberInput = (EditText) rootView.findViewById(R.id.phone);

        // TODO: DEBUG!!!
        /*orgNameInput.setText("knowmadics");
        nameInput.setText("segue smith");
        usernameInput.setText("segue");
        passwordInput.setText("Password#1");
        confirmPasswordInput.setText("Password#1");
        emailInput.setText("segue@e.com");
        phoneNumberInput.setText("12021234567");
        contentUri = Uri.fromFile(new File("/storage/emulated/0/DCIM/Camera/kot2.jpg"));*/

        return rootView;
    }

    private void validate(boolean checkAvatar) {
        String url = urlInput.getText().toString().trim();
        String port = portInput.getText().toString().trim();
        String orgName = orgNameInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        String username = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        String email = emailInput.getText().toString();
        String phone = phoneNumberInput.getText().toString();
        boolean requireAvatar = getResources().getBoolean(R.bool.require_reg_avatar);

        if (password.trim().length() > 0 && confirmPassword.trim().length() > 0 && url.length() > 0 && port.length() > 0 &&
                orgName.length() > 0 && name.length() > 0 && username.length() > 0) {
            if (password.equals(confirmPassword)) {

                if (checkAvatar && contentUri == null) {
                    if (requireAvatar) {
                        if (alertDialogFragment != null) {
                            alertDialogFragment.dismiss();
                        }
                        alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.profile_picture), getString(R.string.profile_picture_required));
                        alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
                    } else {
                        // Ask confirmation to skip avatar
                        if (yesNoDialogFragment != null) {
                            yesNoDialogFragment.dismiss();
                        }
                        yesNoDialogFragment = YesNoDialogFragment.newInstance(getString(R.string.profile_picture), getString(R.string.add_profile_picture));
                        yesNoDialogFragment.setTargetFragment(this, Givens.SKIP_PROFILE_PHOTO);
                        yesNoDialogFragment.show(getFragmentManager(), DIALOG_TAG);
                    }
                    return;
                }

                registrationProgress = RegistrationProgressDialogFragment.newInstance(getString(R.string.registering), getString(R.string.registration_in_progress));
                registrationProgress.show(getFragmentManager(), DIALOG_TAG);
                CreateAccountService.createAccount(
                        getActivity(),
                        url,
                        port,
                        orgName,
                        name,
                        username,
                        password,
                        email,
                        phone
                );
            } else {
                if (alertDialogFragment != null) {
                    alertDialogFragment.dismiss();
                }
                alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.password_confirmation), getString(R.string.password_not_confirmed));
                alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
            }
        } else {
            if (alertDialogFragment != null) {
                alertDialogFragment.dismiss();
            }
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.invalid_registration), getString(R.string.fill_in_required_fields));
            alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
        }

    }


    @Override
    public void connectionLost() {
        registerButton.setEnabled(false);
    }

    @Override
    public void connectionGained() {
        registerButton.setEnabled(true);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.Registration registration) {
        //Timber.d("Registration received");
        if (registrationProgress != null) {
            registrationProgress.dismiss();
            registrationProgress = null;
        }
        if (alertDialogFragment != null) {
            alertDialogFragment.dismiss();
            alertDialogFragment = null;
        }

        if (registration.getStatus()) {
            // Send avatar to server
            if (contentUri != null) {
                ProfileService.saveAvatar(getActivity(), Uri.fromFile(getActivity().getFileStreamPath(ProfileService.AVATAR_FILE_NAME)), true, false);
                // TODO: DEBUG!!!
                // ProfileService.saveAvatar(getActivity(), contentUri, true, false);
            }

            final AgentSettings agentSettings = registration.getAgentSettings();
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.success_title), registration.getMessage());
            alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
            alertDialogFragment.setCustomCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if (getResources().getBoolean(R.bool.enable_reg_payment)) {
                        // Verify subscription if required
                        ProfileService.verifySubscription(getActivity(), application.getScannedSettings().getUserId(), agentSettings);
                    } else {
                        // Open main activity
                        onEventMainThread(new Events.SubscriptionVerified(true, agentSettings));
                    }
                }
            });

        } else {
            alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.error_title), registration.getMessage());
            alertDialogFragment.show(getFragmentManager(), DIALOG_TAG);
        }
        EventBus.getDefault().removeStickyEvent(registration);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.SubscriptionVerified event) {
        //Timber.d("Subscription verified.");
        if (event.isValid()) {
            // Open main activity
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Givens.EXTRA_AGENT_SETTINGS, event.getAgentSettings());
            getActivity().startActivity(intent);
            getActivity().finish();
        } else {
            // Clear fragment stack and open payment fragment
            PaymentFragment fragment = new PaymentFragment();
            Bundle args = new Bundle();
            args.putParcelable(PaymentFragment.ARG_AGENT_SETTINGS, event.getAgentSettings());
            fragment.setArguments(args);
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentManager.beginTransaction().add(R.id.setup_container, fragment, SetupActivity.FRAGMENT_TAG).commit();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.ProfilePhotoLoaded event) {
        //Timber.d("Photo loaded.");
        if (event.getImage() != null && event.getImage().length > 0) {
            ImageUtil.setImageView(avatarView, event.getImage(), -1, false, true);
            addPhotoButton.setText(R.string.edit);
        } else {
            avatarView.setImageResource(R.drawable.avatar);
            addPhotoButton.setText(R.string.add_photo);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) return;
        // Getting back from external "Select Picture" activity
        if (requestCode == Givens.SELECT_PROFILE_PHOTO) {
            contentUri = data.getData();
            File outputFile = ProfileService.createTempFile();
            Crop.of(contentUri, Uri.fromFile(outputFile)).asSquare().withMaxSize(Givens.AVATAR_SIZE_PIXELS, Givens.AVATAR_SIZE_PIXELS).start(getActivity(), this);
        } else if (requestCode == Crop.REQUEST_CROP) {
            contentUri = Crop.getOutput(data);
            ProfileService.saveAvatar(getActivity(), contentUri, false, true);
        } else if (requestCode == Givens.SKIP_PROFILE_PHOTO) {
            //Timber.d("User wants to skip Avatar");
            validate(false);
        }
    }

}
