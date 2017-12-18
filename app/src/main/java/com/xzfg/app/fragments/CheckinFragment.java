package com.xzfg.app.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.PermissionChecker;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.adapters.CheckinContactsAdapter;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.model.AgentContacts;
import com.xzfg.app.model.CannedMessages;
import com.xzfg.app.model.Checkin;
import com.xzfg.app.model.Submission;
import com.xzfg.app.model.UploadPackage;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.services.ProfileService;
import com.xzfg.app.util.ImageUtil;
import com.xzfg.app.util.MessageUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;


public class CheckinFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemClickListener {

    public static final int PICK_CHECKIN_ATTACHMENT = 1031;
    private static final String EXT_IMAGE = ".image";
    private static final String EXT_VIDEO = ".video";

    private View mMessagesLayout;
    private View mCheckinLayout;
    private ImageView mAttachButton;
    private TextView mSendButton;
    private ListView mMsgListView;
    private TextView mMsgLenView;
    private EditText mMsgView;
    private ListView mContactsListView;
    private CheckinContactsAdapter mContactsListAdapter = null;
    private ProgressDialog mProgressDlg = null;

    private Uri mOutputImageFileUri = null;
    private Uri mOutputVideoFileUri = null;
    private Uri mSelectedImageUri = null;

    @Inject
    Application application;

    @Inject
    FixManager fixManager;

    @Inject
    MediaManager mediaManager;


    // Save fragment state
    private static class CheckinState {
        boolean messagesVisible = true;
        String message = null;
        Uri outputImageFileUri = null;
        Uri outputVideoFileUri = null;
        Uri selectedImageUri = null;
        Set<String> contacts = null;
    }

    private static CheckinState mState = null;

    private void saveState() {
        mState = new CheckinState();
        mState.messagesVisible = (mMessagesLayout.getVisibility() == View.VISIBLE);
        mState.message = mMsgView.getText().toString();
        mState.outputImageFileUri = mOutputImageFileUri;
        mState.outputVideoFileUri = mOutputVideoFileUri;
        mState.selectedImageUri = mSelectedImageUri;
        mState.contacts = new HashSet<>();

        if (mContactsListAdapter != null) {
            for (int i = 0; i < mContactsListAdapter.getCount(); i++) {
                if (mContactsListAdapter.getItem(i).isSelected()) {
                    mState.contacts.add(mContactsListAdapter.getItem(i).getId());
                }
            }
        }
    }

    private void restoreState() {
        if (mState != null) {
            mMsgLenView.setText(getString(R.string.checkin) + "(" + mState.message.length() + ")");
            mMsgView.setText(mState.message);
            mSelectedImageUri = mState.selectedImageUri;
            mOutputImageFileUri = mState.outputImageFileUri;
            mOutputVideoFileUri = mState.outputVideoFileUri;

            if (mContactsListAdapter != null && !mState.contacts.isEmpty()) {
                for (int i = 0; i < mContactsListAdapter.getCount(); i++) {
                    boolean selectedValue = mState.contacts.contains(mContactsListAdapter.getItem(i).getId());
                    mContactsListAdapter.getItem(i).setSelected(selectedValue);
                }
                mContactsListAdapter.notifyDataSetChanged();
            }
            mMessagesLayout.setVisibility(mState.messagesVisible ? View.VISIBLE : View.GONE);
            mCheckinLayout.setVisibility(mState.messagesVisible ? View.GONE : View.VISIBLE);
            // Forget saved state
            mState = null;
        }
    }

    private class CheckinMessageWatcher implements TextWatcher {

        public void afterTextChanged(Editable s) {
            mMsgLenView.setText(getString(R.string.checkin) + "(" + s.length() + ")");
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_checkin, viewGroup, false);

        mMessagesLayout = v.findViewById(R.id.messages_layout);
        mCheckinLayout = v.findViewById(R.id.checkin_layout);
        mMsgLenView = (TextView) v.findViewById(R.id.message_length);
        mMsgView = (EditText) v.findViewById(R.id.message);
        mMsgView.addTextChangedListener(new CheckinMessageWatcher());

        mAttachButton = (ImageView) v.findViewById(R.id.attach_button);
        mAttachButton.setOnClickListener(this);
        mSendButton = (TextView) v.findViewById(R.id.send);
        mSendButton.setOnClickListener(this);
        v.findViewById(R.id.cancel).setOnClickListener(this);

        mMsgListView = (ListView) v.findViewById(R.id.messages_list);
        mMsgListView.setOnItemClickListener(this);

        // Canned messages are NULL right after registration
        if (application.getCannedMessages() != null) {
            ArrayAdapter<String> msgListAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, application.getCannedMessages().getMessages());
            mMsgListView.setAdapter(msgListAdapter);
        }

        mContactsListView = (ListView) v.findViewById(R.id.contacts_list);
        mContactsListView.setOnItemClickListener(this);

        AgentContacts contacts = application.getAgentContacts();
        if (contacts != null && contacts.getContacts() != null) {
            List<CheckinContactsAdapter.CheckinContact> list = new ArrayList<>(contacts.getContacts().size());
            for (AgentContacts.AgentContact contact : contacts.getContacts()) {
                list.add(new CheckinContactsAdapter.CheckinContact(contact, false));
            }
            mContactsListAdapter = new CheckinContactsAdapter(getActivity(), list);
            mContactsListView.setAdapter(mContactsListAdapter);
        }

        // Load saved state
        //restoreState();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);

        // Load saved state
        restoreState();
        //application.enableBossMode();
    }

    @Override
    public void onPause() {
        // Save state
        saveState();

        super.onPause();
        EventBus.getDefault().unregister(this);
    }
    @Override
    public void onStop(){
        super.onStop();
       // EventBus.getDefault().post(new Events.BossModeStatus(Givens.ACTION_BOSSMODE_ENABLE));
    }





    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Application) activity.getApplication()).inject(this);

    }

    // Create progress dialog
    public void CreateProgressDlg() {
        if (mProgressDlg != null) {
            DismissProgressDlg();
        }
        mProgressDlg = new ProgressDialog(getActivity());
        mProgressDlg.setTitle(getString(R.string.loading));
        mProgressDlg.setMessage(getString(R.string.please_wait_loading));
        mProgressDlg.show();
    }

    // Dismiss progress dialog
    public void DismissProgressDlg() {
        if (mProgressDlg != null) {
            // Dismiss progress dialog
            mProgressDlg.dismiss();
            mProgressDlg = null;
        }
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = ((ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isAvailable() && activeNetwork.isConnected();
    }

    public void onEventMainThread(Events.AgentContactsAcquired event) {
        AgentContacts contacts = event.getAgentContacts();
        if (contacts != null && contacts.getContacts() != null) {
            List<CheckinContactsAdapter.CheckinContact> list = new ArrayList<>(contacts.getContacts().size());
            for (AgentContacts.AgentContact contact : contacts.getContacts()) {
                list.add(new CheckinContactsAdapter.CheckinContact(contact, false));
            }
            saveState();
            mContactsListAdapter = new CheckinContactsAdapter(getActivity(), list);
            mContactsListView.setAdapter(mContactsListAdapter);
            restoreState();
        }
    }

    public void onEventMainThread(Events.CannedMessagesAcquired event) {
        CannedMessages messages = event.getMessages();
        if (messages != null && !messages.isEmpty()) {
            ArrayAdapter<String> msgListAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, messages.getMessages());
            mMsgListView.setAdapter(msgListAdapter);
        }
    }

    public void onEventMainThread(Events.ThumbnailLoaded event) {
        if (event.getImage() != null && event.getImage().length > 0) {
            ImageUtil.setThumbnailView(mAttachButton, event.getImage(), -1, false);
        }
        DismissProgressDlg();
    }

    public void onEventMainThread(Events.CheckinAttachmentSelected event) {
        final boolean isCamera;
        Intent data = event.getIntent();

        // Hide messages layout
        mMessagesLayout.setVisibility(View.GONE);
        // Show checkin layout
        mCheckinLayout.setVisibility(View.VISIBLE);

        if (data == null) {
            isCamera = true;
        } else {
            final String action = data.getAction();
            if (action == null) {
                isCamera = false;
            } else {
                isCamera = action.equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            }
        }

        if (isCamera) {
            File imageFile = new File(mOutputImageFileUri.getPath());
            if (imageFile.exists())
                mSelectedImageUri = mOutputImageFileUri;
            else
                mSelectedImageUri = mOutputVideoFileUri;
        } else {
            mSelectedImageUri = data == null ? null : data.getData();
        }

        // Create thumbnail for selected media
        try {
            // Figure out MIME type
            ContentResolver cr = getActivity().getContentResolver();
            String type = cr.getType(mSelectedImageUri);
            if (type == null) {
                type = mSelectedImageUri.getPath().substring(mSelectedImageUri.getPath().lastIndexOf("."));
                type = type.equals(EXT_IMAGE) ? "image" : "video";
            } else {
                type = type.substring(0, type.indexOf("/"));
            }
            switch (type) {
                case "video":
                    // Got video
                    CreateProgressDlg();
                    ProfileService.loadThumbnail(getActivity(), mSelectedImageUri, false, null);
                    break;
                case "image":
                    // Got image
                    CreateProgressDlg();
                    ProfileService.loadThumbnail(getActivity(), mSelectedImageUri, true, null);
                    break;
                default:
                    // Something else - error
                    Toast.makeText(getActivity(), "Selected media not supported.",
                        Toast.LENGTH_SHORT).show();
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideMessageList(boolean hide) {
        if (hide) {
            // Hide canned messages list and show check-in UI
            mMessagesLayout.setVisibility(View.GONE);
            mCheckinLayout.setVisibility(View.VISIBLE);
        } else {
            // Hide check-in UI and show canned messages list
            mCheckinLayout.setVisibility(View.GONE);
            mMessagesLayout.setVisibility(View.VISIBLE);
        }
    }

    private boolean isCameraPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PermissionChecker.checkSelfPermission(application,
                    Manifest.permission.CAMERA) == PermissionChecker.PERMISSION_GRANTED;
        }
        return true;
    }

    private void openImageIntent() {
        // Determine Uri of camera image to save
        mOutputImageFileUri = Uri.fromFile(ProfileService.createTempFile(EXT_IMAGE));
        mOutputVideoFileUri = Uri.fromFile(ProfileService.createTempFile(EXT_VIDEO));
        mSelectedImageUri = null;

        // Camera
        final List<Intent> cameraIntents = new ArrayList<>();
        final PackageManager packageManager = getActivity().getPackageManager();
        final Intent captureImageIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        for (ResolveInfo res : packageManager.queryIntentActivities(captureImageIntent, 0)) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureImageIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mOutputImageFileUri);
            cameraIntents.add(intent);
        }
        final Intent captureVideoIntent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        for (ResolveInfo res : packageManager.queryIntentActivities(captureVideoIntent, 0)) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureVideoIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mOutputVideoFileUri);
            cameraIntents.add(intent);
        }

        // File system
        final Intent galleryIntent;
        if (Build.VERSION.SDK_INT < 19) {
            galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/* video/*");
        } else {
            galleryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
            galleryIntent.setType("image/*");
            galleryIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        }

        // Chooser of file system options
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

        // User could've revoked camera permission for our app in Android 6.0
        if (isCameraPermissionGranted()) {
            // Add the camera options
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));
        }

        // Delete temp files
        (new File(mOutputImageFileUri.getPath())).delete();
        (new File(mOutputVideoFileUri.getPath())).delete();
        // Open intent
        getActivity().startActivityForResult(chooserIntent, PICK_CHECKIN_ATTACHMENT);
    }

    private void submitCheckinMessage() {
        Location location = fixManager.getLastLocation();
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));

        Checkin checkin = new Checkin();
        checkin.setMessage(mMsgView.getText().toString());
        if (mContactsListAdapter != null) {
            String contacts = "";
            for (int i = 0; i < mContactsListAdapter.getCount(); i++) {
                if (mContactsListAdapter.getItem(i).isSelected()) {
                    contacts += mContactsListAdapter.getItem(i).getId() + "|";
                }
            }
            if (contacts.length() > 1) {
                contacts = contacts.substring(0, contacts.length() - 1);
                checkin.setContacts(contacts);
            }
        }
        checkin.setDate(dateFormatGmt.format(new Date()));
        if (location != null) {
            checkin.setLatitude(String.valueOf(location.getLatitude()));
            checkin.setLongitude(String.valueOf(location.getLongitude()));
        }
        UploadPackage
         uploadPackage = new UploadPackage();
        uploadPackage.setBatch(UUID.randomUUID().toString());
        uploadPackage.setCheckin(checkin);
        uploadPackage.setDate(new Date());
        uploadPackage.setCaseNumber(application.getAgentSettings().getCaseNumber());
        uploadPackage.setCaseDescription(application.getAgentSettings().getCaseDescription());

        if (location != null) {
            uploadPackage.setLatitude(location.getLatitude());
            uploadPackage.setLongitude(location.getLongitude());
            if (location.getAccuracy() != 0) {
                uploadPackage.setAccuracy(location.getAccuracy());
            }
        }

        Submission submission;
        if (mSelectedImageUri != null) {
            // Figure out MIME type
            ContentResolver cr = getActivity().getContentResolver();
            String type = cr.getType(mSelectedImageUri);
            if (type == null) {
                type = mSelectedImageUri.getPath().substring(mSelectedImageUri.getPath().lastIndexOf("."));
                type = type.equals(EXT_IMAGE) ? "image" : "video";
            } else {
                type = type.substring(0, type.indexOf("/"));
            }
            if (type.equals("video")) {
                // Got video
                uploadPackage.setType(Givens.UPLOAD_TYPE_VIDEO);
                uploadPackage.setFormat(Givens.UPLOAD_FORMAT_MP4);
            } else if (type.equals("image")) {
                // Got image
                uploadPackage.setType(Givens.UPLOAD_TYPE_IMG);
                uploadPackage.setFormat(Givens.UPLOAD_FORMAT_JPEG);
            }

            submission = new Submission(uploadPackage, mSelectedImageUri);
        } else {
            submission = new Submission(uploadPackage, Uri.parse("checkin://"));
        }
        Timber.d("Submitting upload (check-in) package: " + uploadPackage);
        if(mSelectedImageUri!=null) {
            //
           // String message = "Checkin : i live in HCM city";

            // if we are connected, attempt to send the message via the network
           // MessageUrl messageUrl = MessageUtil.getMessageUrl(application, message);
           // MessageUtil.sendMessage(application.getApplicationContext(), messageUrl);

            mediaManager.submitUpload(submission);
            Toast.makeText(getActivity(), "Check-In message submitted.", Toast.LENGTH_SHORT).show();
           clearCheckin();
       }
        else {
           Toast.makeText(getActivity(),"Can't send ,need a media files",Toast.LENGTH_LONG).show();
        }
    }

    private void clearCheckin() {
        hideMessageList(false);

        // Clear message
        mMsgView.setText("");
        // Clear selected attachment
        clearAttachment();
        // Unselect all contacts
        if (mContactsListAdapter != null) {
            for (int i = 0; i < mContactsListAdapter.getCount(); i++) {
                mContactsListAdapter.getItem(i).setSelected(false);
            }
            mContactsListAdapter.notifyDataSetChanged();
        }
    }

    // Clear selected attachment
    private void clearAttachment() {
        mOutputImageFileUri = null;
        mOutputVideoFileUri = null;
        mSelectedImageUri = null;
        mAttachButton.setImageResource(R.drawable.attachphotoorvideo);
        mAttachButton.setPadding(0, 0, 0, 0);
        mAttachButton.setBackground(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
       // application.enableBossMode();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
     //  application.enableBossMode();
    }

    @Override
    public void onDetach() {
        super.onDetach();
      // application.enableBossMode();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                clearCheckin();
                break;

            case R.id.send:
                application.enableBossMode();
                submitCheckinMessage();
               // clearCheckin();
                break;

            case R.id.attach_button:
                application.disableBossMode();
                if (mSelectedImageUri != null) {
                    // Clear selected attachment
                    clearAttachment();
                } else {
                    // Select new attachment
                    openImageIntent();
                }
                //application.enableBossMode();
            break;
          //  application.enableBossMode();

            default:
            //     application.enableBossMode();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        switch (parent.getId()) {
            case R.id.messages_list:
                mMsgView.setText(((TextView) v).getText());
                hideMessageList(true);
                break;

            default:
                break;
        }
    }

}
