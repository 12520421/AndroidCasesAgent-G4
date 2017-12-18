package com.xzfg.app.services;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Events;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.MediaManager;
import com.xzfg.app.managers.SessionManager;
import com.xzfg.app.model.AgentContacts;
import com.xzfg.app.model.AgentProfile;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.CannedMessages;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.model.Submission;
import com.xzfg.app.model.UploadPackage;
import com.xzfg.app.model.UserSubscription;
import com.xzfg.app.model.url.MessageUrl;
import com.xzfg.app.model.url.SessionUrl;
import com.xzfg.app.security.Crypto;
import com.xzfg.app.util.ImageUtil;
import com.xzfg.app.util.Network;
import com.xzfg.app.util.XmlUtil;

import org.apache.commons.io.IOUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.RegistryMatcher;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class ProfileService extends BackWakeIntentService {

    public final static String AVATAR_FILE_NAME = "7cbd5d4e5e904396869f70593d5dde77";
    public final static String TEMP_AVATAR_FILE = "7344a3620a674749b7f4d636b5d5904b.delete";
    private final static String XML_RESPONSE_OK = "<response>ok</response>";
    private final static int ACTION_SAVE_AVATAR = 1;
    private final static int ACTION_LOAD_AVATAR = 2;
    private final static int ACTION_GET_LAST_FIX = 3;
    private final static int ACTION_GET_AGENT_PROFILE = 4;
    private final static int ACTION_POST_AGENT_PROFILE = 5;
    private final static int ACTION_SAVE_PRIVATE_FILE = 6;
    private final static int ACTION_LOAD_PRIVATE_FILE = 7;
    private final static int ACTION_GET_CANNED_MESSAGES = 8;
    private final static int ACTION_GET_AGENT_CONTACTS = 9;
    private final static int ACTION_POST_AGENT_CONTACTS = 10;
    private final static int ACTION_LOAD_THUMBNAIL = 11;
    private final static int ACTION_ACCEPT_ALERT = 12;
    private final static int ACTION_VERIFY_SUBSCRIPTION = 13;
    private final static int ACTION_FORGOT_PASSWORD_SEND_CODE = 14;
    private final static int ACTION_FORGOT_PASSWORD_VERIFY_CODE = 15;
    private final static int ACTION_FORGOT_PASSWORD_CHANGE_PASSWORD = 16;
    private final static String PARAM_ACTION = "PARAM_ACTION";
    private final static String PARAM_IMAGE_URI = "PARAM_IMAGE_URI";
    private final static String PARAM_LOAD_AFTER_SAVE = "PARAM_LOAD_AFTER_SAVE";
    private final static String PARAM_SEND_TO_SERVER = "PARAM_SEND_TO_SERVER";
    private final static String PARAM_GET_FROM_SERVER = "PARAM_GET_FROM_SERVER";
    private final static String PARAM_AGENT_PROFILE = "PARAM_AGENT_PROFILE";
    private final static String PARAM_AGENT_CONTACTS = "PARAM_AGENT_CONTACTS";
    private final static String PARAM_IS_IMAGE = "PARAM_IS_IMAGE";
    private final static String PARAM_FILE_DATA = "PARAM_FILE_DATA";
    private final static String PARAM_FILE_NAME = "PARAM_FILE_NAME";
    private final static String PARAM_FILE_TAG = "PARAM_FILE_TAG";
    private final static String PARAM_ALERT_ID = "PARAM_ALERT_ID";
    private final static String PARAM_ALERT_STATUS = "PARAM_ALERT_STATUS";
    private final static String PARAM_USER_ID = "PARAM_USER_ID";
    private final static String PARAM_AGENT_SETTINGS = "PARAM_AGENT_SETTINGS";
    private final static String PARAM_FORGOT_PASSWORD_URL = "PARAM_FORGOTTEN_PASSWORD_URL";
    private final static String PARAM_FORGOT_PASSWORD_PORT = "PARAM_FORGOTTEN_PASSWORD_PORT";
    private final static String PARAM_FORGOT_PASSWORD_USERNAME = "PARAM_FORGOT_PASSWORD_USERNAME";
    private final static String PARAM_FORGOT_PASSWORD_CODE = "PARAM_FORGOT_PASSWORD_CODE";
    private final static String PARAM_FORGOT_PASSWORD_NEW_PASSWORD = "PARAM_FORGOT_PASSWORD_NEW_PASSWORD";

    @Inject
    Application application;
    @Inject
    SessionManager sessionManager;
    @Inject
    MediaManager mediaManager;
    @Inject
    FixManager fixManager;
    @Inject
    Crypto crypto;
    @Inject
    OkHttpClient httpClient;
    @Inject
    SSLSocketFactory socketFactory;

    private class LastAddress {
        private String address;
        private Calendar date;

        public LastAddress(String address, Calendar date) {
            this.address = address;
            this.date = date;
        }

        public String getAddress() {
            return address;
        }

        public Calendar getDate() {
            return date;
        }
    }

    public static void forgotPasswordSendCode(
            Context context, String url, long port, String username) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_FORGOT_PASSWORD_SEND_CODE);
        bundle.putString(PARAM_FORGOT_PASSWORD_URL, url);
        bundle.putLong(PARAM_FORGOT_PASSWORD_PORT, port);
        bundle.putString(PARAM_FORGOT_PASSWORD_USERNAME, username);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void forgotPasswordVerifyCode(
            Context context, String url, long port, String username, String code) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_FORGOT_PASSWORD_VERIFY_CODE);
        bundle.putString(PARAM_FORGOT_PASSWORD_URL, url);
        bundle.putLong(PARAM_FORGOT_PASSWORD_PORT, port);
        bundle.putString(PARAM_FORGOT_PASSWORD_USERNAME, username);
        bundle.putString(PARAM_FORGOT_PASSWORD_CODE, code);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void forgotPasswordChangePassword(
            Context context, String url, long port, String username, String code, String password) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_FORGOT_PASSWORD_CHANGE_PASSWORD);
        bundle.putString(PARAM_FORGOT_PASSWORD_URL, url);
        bundle.putLong(PARAM_FORGOT_PASSWORD_PORT, port);
        bundle.putString(PARAM_FORGOT_PASSWORD_USERNAME, username);
        bundle.putString(PARAM_FORGOT_PASSWORD_CODE, code);
        bundle.putString(PARAM_FORGOT_PASSWORD_NEW_PASSWORD, password);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void verifySubscription(Context context, String userId, AgentSettings settings) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_VERIFY_SUBSCRIPTION);
        bundle.putString(PARAM_USER_ID, userId);
        // This is optional parameter used to pass agent settings back to callback
        if (settings != null) {
            bundle.putParcelable(PARAM_AGENT_SETTINGS, settings);
        }
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void saveBitmap(Context context, Bitmap bitmap, String fileName) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_SAVE_PRIVATE_FILE);
        bundle.putString(PARAM_FILE_NAME, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        bundle.putByteArray(PARAM_FILE_DATA, out.toByteArray());
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void loadThumbnail(Context context, Uri uri, boolean isImage, String tag) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_LOAD_THUMBNAIL);
        bundle.putString(PARAM_FILE_NAME, uri.toString());
        bundle.putBoolean(PARAM_IS_IMAGE, isImage);
        bundle.putString(PARAM_FILE_TAG, tag);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void loadBitmap(Context context, String fileName, String tag) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_LOAD_PRIVATE_FILE);
        bundle.putString(PARAM_FILE_NAME, fileName);
        bundle.putString(PARAM_FILE_TAG, tag);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void saveAvatar(Context context, Uri imageUri, boolean sendToServer, boolean loadAfterSave) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_SAVE_AVATAR);
        bundle.putString(PARAM_IMAGE_URI, imageUri.toString());
        bundle.putBoolean(PARAM_SEND_TO_SERVER, sendToServer);
        bundle.putBoolean(PARAM_LOAD_AFTER_SAVE, loadAfterSave);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void loadAvatar(Context context, boolean getFromServer) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_LOAD_AVATAR);
        bundle.putBoolean(PARAM_GET_FROM_SERVER, getFromServer);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void getCannedMessages(Context context) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_GET_CANNED_MESSAGES);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void getLastAddress(Context context) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_GET_LAST_FIX);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void getAgentProfile(Context context) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_GET_AGENT_PROFILE);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void postAgentProfile(Context context, AgentProfile profile) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_POST_AGENT_PROFILE);
        bundle.putParcelable(PARAM_AGENT_PROFILE, profile);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void getAgentContacts(Context context) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_GET_AGENT_CONTACTS);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void postAgentContacts(Context context, AgentContacts contacts) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_POST_AGENT_CONTACTS);
        bundle.putParcelable(PARAM_AGENT_CONTACTS, contacts);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static void acceptAlert(Context context, long alertId, int status) {
        Intent i = new Intent(context, ProfileService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_ACTION, ACTION_ACCEPT_ALERT);
        bundle.putLong(PARAM_ALERT_ID, alertId);
        bundle.putInt(PARAM_ALERT_STATUS, status);
        i.putExtras(bundle);
        context.startService(i);
    }

    public static File createTempFile() {
        return createTempFile("");
    }

    public static File createTempFile(String fileExt) {
        String status = Environment.getExternalStorageState();
        boolean isSDCARDMounted = status.equals(Environment.MEDIA_MOUNTED);

        if (isSDCARDMounted) {
            File f = new File(Environment.getExternalStorageDirectory(), ProfileService.TEMP_AVATAR_FILE + fileExt);
            try {
                f.createNewFile();
            } catch (IOException e) {
                // TODO: Auto-generated catch block
            }
            return f;
        } else {
            return null;
        }
    }


    public static boolean isTempFile(Uri uri) {
        String status = Environment.getExternalStorageState();
        boolean isSDCARDMounted = status.equals(Environment.MEDIA_MOUNTED);

        if (isSDCARDMounted) {
            File f = new File(Environment.getExternalStorageDirectory(), ProfileService.TEMP_AVATAR_FILE);
            return uri.toString().contains(f.getPath());
        }

        return false;
    }

    //
    public ProfileService() {
        super(ProfileService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
    }

    @Override
    protected void doWork(Intent intent) {
        Bundle request = intent.getExtras();
        int action = request.getInt(PARAM_ACTION);

        if (!isConnected()) {
           /* EventBus.getDefault().post(new Events.ProfilePhotoLoaded(false, getString(R.string.network_error)));
            return;*/
            // Read avatar from file in internal storage
            byte[] imageData = loadByteArrayFromPrivateFile(AVATAR_FILE_NAME);

            // Send notification to UIAVATAR_FILE_NAME
            if (imageData != null) {
                EventBus.getDefault().post(new Events.ProfilePhotoLoaded(true, imageData));
            }
        }

        try {
            // Request forgotten password code
            if (action == ACTION_FORGOT_PASSWORD_SEND_CODE) {
                String url = request.getString(PARAM_FORGOT_PASSWORD_URL, "");
                long port = request.getLong(PARAM_FORGOT_PASSWORD_PORT);
                String username = request.getString(PARAM_FORGOT_PASSWORD_USERNAME, "");
                //Timber.d("Requesting code for forgotten password: [" + username + "] ...");

                Events.ForgotPasswordCodeReceived result = forgotPasswordSendCode(url, port, username);
                // Send notification to UI
                EventBus.getDefault().post(result);
            }
            // Verify forgotten password code
            else if (action == ACTION_FORGOT_PASSWORD_VERIFY_CODE) {
                String url = request.getString(PARAM_FORGOT_PASSWORD_URL, "");
                long port = request.getLong(PARAM_FORGOT_PASSWORD_PORT);
                String username = request.getString(PARAM_FORGOT_PASSWORD_USERNAME, "");
                String code = request.getString(PARAM_FORGOT_PASSWORD_CODE, "");
                //Timber.d("Verifying code for forgotten password: [" + username + "] ...");

                Events.ForgotPasswordCodeVerified result = forgotPasswordVerifyCode(url, port, username, code);
                // Send notification to UI
                EventBus.getDefault().post(result);
            }
            // Change forgotten password
            else if (action == ACTION_FORGOT_PASSWORD_CHANGE_PASSWORD) {
                String url = request.getString(PARAM_FORGOT_PASSWORD_URL, "");
                long port = request.getLong(PARAM_FORGOT_PASSWORD_PORT);
                String username = request.getString(PARAM_FORGOT_PASSWORD_USERNAME, "");
                String code = request.getString(PARAM_FORGOT_PASSWORD_CODE, "");
                String newPassword = request.getString(PARAM_FORGOT_PASSWORD_NEW_PASSWORD, "");
                //Timber.d("Verifying code for forgotten password: [" + username + "] ...");

                Events.ForgotPasswordPasswordChanged result = forgotPasswordChangePassword(
                        url, port, username, code, newPassword);
                // Send notification to UI
                EventBus.getDefault().post(result);
            }
            // Verify subscription
            else if (action == ACTION_VERIFY_SUBSCRIPTION) {
                String userId = request.getString(PARAM_USER_ID, "");
                AgentSettings settings = request.getParcelable(PARAM_AGENT_SETTINGS);
                //Timber.d("Verifying subscription: [" + userId + "] ...");

                boolean validSubscription = verifySubscription(userId);
                // Send notification to UI
                EventBus.getDefault().post(new Events.SubscriptionVerified(validSubscription, settings));
            }
            // Save avatar
            else if (action == ACTION_SAVE_AVATAR) {
                //Timber.d("Saving avatar...");
                boolean sendToServer = request.getBoolean(PARAM_SEND_TO_SERVER, false);
                boolean loadAfterSave = request.getBoolean(PARAM_LOAD_AFTER_SAVE, false);
                Uri imageUri = Uri.parse(request.getString(PARAM_IMAGE_URI));

                // Save avatar to internal storage
                byte[] imageData = saveAvatarToFile(imageUri);

                // Add image file to upload queue that will send it to the server
                if (sendToServer) {
                    sendAvatarToServer(imageData);
                }

                // Send notification to UI
                if (loadAfterSave && imageData != null) {
                    EventBus.getDefault().post(new Events.ProfilePhotoLoaded(true, imageData));
                }
            }
            // Load avatar
            else if (action == ACTION_LOAD_AVATAR) {
                //Timber.d("Loading avatar...");
                boolean getFromServer = request.getBoolean(PARAM_GET_FROM_SERVER, false);

                // Read avatar from file in internal storage
                byte[] imageData = loadByteArrayFromPrivateFile(AVATAR_FILE_NAME);

                // Send notification to UI
                if (imageData != null) {
                    EventBus.getDefault().post(new Events.ProfilePhotoLoaded(true, imageData));
                }

                // Get avatar from server if asked or if cached image is empty
                if (getFromServer || imageData == null) {
                    imageData = getAvatarFromServer();

                    if (imageData != null) {
                        // Save avatar to file in internal memory
                        saveByteArrayToPrivateFile(imageData, AVATAR_FILE_NAME);
                        // Send notification to UI
                        EventBus.getDefault().post(new Events.ProfilePhotoLoaded(true, imageData));
                    }
                }

            }
            // Get last location address
            else if (action == ACTION_GET_LAST_FIX) {
                //Timber.d("Getting last address...");
                // Get last address from server
                LastAddress fixInfo = getLastAddress();
                // Send notification to UI
                EventBus.getDefault().post(new Events.ProfileLastFixReceived(fixInfo.getAddress(), fixInfo.getDate()));
            }
            // Get profile fields
            else if (action == ACTION_GET_AGENT_PROFILE) {
                //Timber.d("Getting profile fields from server...");
                String xmlProfile = getProfileFields();
                AgentProfile profile = AgentProfile.parse(application, xmlProfile);
                // Send notification
                EventBus.getDefault().post(new Events.AgentProfileAcquired(profile));
            }
            // Send profile fields
            else if (action == ACTION_POST_AGENT_PROFILE) {
                //Timber.d("Sending profile fields to server...");
                AgentProfile profile = request.getParcelable(PARAM_AGENT_PROFILE);
                postProfileFields(profile);
            } else if (action == ACTION_SAVE_PRIVATE_FILE) {
                String name = request.getString(PARAM_FILE_NAME);
                byte[] data = request.getByteArray(PARAM_FILE_DATA);
                //Timber.d("Saving file to internal memory...");
                saveByteArrayToPrivateFile(data, name);
            } else if (action == ACTION_LOAD_PRIVATE_FILE) {
                String name = request.getString(PARAM_FILE_NAME);
                String tag = request.getString(PARAM_FILE_TAG);
                //Timber.d("Loading file from internal memory...");
                byte[] data = loadByteArrayFromPrivateFile(name);
                if (data != null) {
                    // Send notification to UI
                    EventBus.getDefault().post(new Events.PrivateFileLoaded(data, tag));
                }
            } else if (action == ACTION_LOAD_THUMBNAIL) {
                String name = request.getString(PARAM_FILE_NAME);
                boolean isImage = request.getBoolean(PARAM_IS_IMAGE);
                String tag = request.getString(PARAM_FILE_TAG);
                //Timber.d("Loading file from URI...");
                byte[] data = loadThumbnailFromUri(Uri.parse(name), isImage);
                if (data != null) {
                    // Send notification to UI
                    EventBus.getDefault().post(new Events.ThumbnailLoaded(data, tag));
                }
            }
            // Get canned messages
            else if (action == ACTION_GET_CANNED_MESSAGES) {
                //Timber.d("Getting canned messages for check-in from server...");
                CannedMessages messages = getCannedMessages();
                if (messages != null) {
                    application.setCannedMessages(messages);
                    // Send notification
                    EventBus.getDefault().post(new Events.CannedMessagesAcquired(messages));
                }
            }
            // Get contacts
            else if (action == ACTION_GET_AGENT_CONTACTS) {
                //Timber.d("Getting contacts from server...");
                AgentContacts contacts = getAgentContacts();
                if (contacts != null) {
                    application.setAgentContacts(contacts);
                    // Send notification
                    EventBus.getDefault().post(new Events.AgentContactsAcquired(contacts));
                }
            }
            // Send agent contacts
            else if (action == ACTION_POST_AGENT_CONTACTS) {
                //Timber.d("Sending agent contacts to server...");
                AgentContacts contacts = request.getParcelable(PARAM_AGENT_CONTACTS);
                if (postAgentContacts(contacts)) {
                    // Request updated contacts from server (need IDs for new contacts)
                    contacts = getAgentContacts();
                    if (contacts != null) {
                        application.setAgentContacts(contacts);
                        // Send notification
                        EventBus.getDefault().post(new Events.AgentContactsAcquired(contacts));
                    }
                }
            }
            // Accept alert
            else if (action == ACTION_ACCEPT_ALERT) {
                //Timber.d("Sending accept alert to server...");
                long alertId = request.getLong(PARAM_ALERT_ID);
                int status = request.getInt(PARAM_ALERT_STATUS);
                acceptAlert(alertId, status);
            }

        } catch (Exception e) {
            String msg = getString(R.string.network_error);
            if (e instanceof SSLHandshakeException) {
                msg = getString(R.string.ssl_error);
            }
            EventBus.getDefault().post(new Events.ProfilePhotoLoaded(false, msg));
            if (!Network.isNetworkException(e)) {
                Timber.w(e, "An error occurred in Profile Service.");
            }
        }

    }

    // Save data to local file
    private void saveByteArrayToPrivateFile(byte[] data, String name) throws IOException {
        // Encrypt
        byte[] encryptedData = crypto.encrypt(data);

        // Save image to local file
        FileOutputStream stream = openFileOutput(name, Context.MODE_PRIVATE);
        stream.write(encryptedData);
        stream.flush();
        stream.close();
    }

    // Load data from local file
    private byte[] loadByteArrayFromPrivateFile(String name) {
        byte[] imageData = null;

        try {
            // Read encrypted data
            FileInputStream stream = openFileInput(name);
            if (stream != null) {
                imageData = ImageUtil.readStreamAsBytes(stream);
                stream.close();
            }

            // Decrypt
            if (imageData != null) {
                imageData = crypto.decrypt(imageData);
            }
        } catch (FileNotFoundException e) {
            //
        } catch (IOException e) {
            Timber.w(e, "An error occurred in ProfileService:loadByteArrayFromPrivateFile");
        }

        return imageData;
    }

    // Load data from Uri
    private byte[] loadThumbnailFromUri(Uri uri, boolean isImage) {
        byte[] imageData = null;

        try {
            if (isImage) {
                InputStream stream = getContentResolver().openInputStream(uri);
                if (stream != null) {
                    imageData = ImageUtil.readStreamAsBytes(stream);
                    stream.close();
                }
            } else {
                Bitmap thumbBitmap = ImageUtil.createVideoThumbnail(getContentResolver(), uri);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                if (thumbBitmap != null) {
                    thumbBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    imageData = stream.toByteArray();
                }
            }

        } catch (FileNotFoundException e) {
            //
        } catch (IOException e) {
            Timber.w(e, "An error occurred in ProfileService:loadByteArrayFromPrivateFile");
        }

        return imageData;
    }

    // Get last known address from server
    private LastAddress getLastAddress() throws IOException, ParseException {
        String xmlPrefix = "?>";
        String address = application.getAgentSettings().getLastAddress();
        Calendar date = application.getAgentSettings().getLastAddressDate();
        int attemptsLeft = 3;

        SessionUrl url = new SessionUrl(application.getScannedSettings(),
                application.getString(R.string.get_last_address_endpoint),
                sessionManager.getSessionId());
        HashMap<String, String> param = new HashMap<String, String>() {{
            put("model", getString(R.string.model));
            put("password", application.getScannedSettings().getPassword());
            put("serial", application.getScannedSettings().getOrganizationId() + application.getDeviceIdentifier());
        }};
        url.setParamData(param);

        while (attemptsLeft-- > 0) {
            //Timber.d("Calling url: " + url.toString());
            Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
            String responseBody = response.body().string().trim();
            response.body().close();

            if (responseBody != null && !responseBody.isEmpty() && responseBody.contains(xmlPrefix)) {
                String error = XmlUtil.getXmlTagText(responseBody, "error");
                if (error == null) {
                    responseBody = responseBody.substring(responseBody.indexOf(xmlPrefix) + xmlPrefix.length());
                    String[] items = responseBody.split("\\|");
                    if (items.length > 0) {
                        address = items[0];
                        if (items.length > 1) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
                            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                            date = Calendar.getInstance();
                            date.setTime(sdf.parse(items[1]));
                        }
                    }
                    break;
                } else if (error.equalsIgnoreCase("Invalid SessionId")) {
                    // Sleep and try again id SessionId is not set yet
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (address != null && !address.isEmpty()) {
            application.getAgentSettings().setLastAddress(address);
            application.getAgentSettings().setLastAddressDate(date);
        }

        return new LastAddress(address, date);
    }

    // Read image from external source and save to file in internal storage
    private byte[] saveAvatarToFile(Uri imageUri) throws IOException {
        // Read data from temporary cropped image file
        InputStream is = getContentResolver().openInputStream(imageUri);
        byte[] imageData = ImageUtil.readStreamAsBytes(is);

        // Save image to file in internal storage
        saveByteArrayToPrivateFile(imageData, AVATAR_FILE_NAME);

        // Delete temporary file
        try {
            final String filePrefix = "file://";
            String imagePath = imageUri.toString();
            Log.d("Liem","image path:"+imagePath);
            if (imagePath.indexOf(filePrefix) == 0 && imagePath.contains(TEMP_AVATAR_FILE)) {
                File tempFile = new File(imagePath.substring(filePrefix.length()));
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        } catch (Exception ex) {
            // Deleting temp file is not important...
        }

        return imageData;
    }

    // Add image file to upload queue that will send it to the server
    private void sendAvatarToServer(byte[] data) {
        UploadPackage uploadPackage = new UploadPackage();
        uploadPackage.setIsAvatar(true);
        uploadPackage.setType(Givens.UPLOAD_TYPE_IMG);
        uploadPackage.setFormat(Givens.UPLOAD_FORMAT_JPEG);
        uploadPackage.setDate(new Date());
        uploadPackage.setCaseNumber(application.getAgentSettings().getCaseNumber());
        uploadPackage.setCaseDescription(application.getAgentSettings().getCaseDescription());
        Location location = fixManager.getLastLocation();
        if (location != null) {
            uploadPackage.setLatitude(location.getLatitude());
            uploadPackage.setLongitude(location.getLongitude());
            if (location.getAccuracy() != 0) {
                uploadPackage.setAccuracy(location.getAccuracy());
            }
        }
        //Timber.d("submitting upload package: " + uploadPackage);
        Submission submission = new Submission(uploadPackage, data);
        mediaManager.submitUpload(submission);
    }

    // Get avatar image from server
    private byte[] getAvatarFromServer() throws IOException {
        byte[] responseBody = null;
        SessionUrl url = new SessionUrl(application.getScannedSettings(),
                application.getString(R.string.get_avatar_endpoint),
                sessionManager.getSessionId());

        //Timber.d("Calling url: " + url.toString());
        Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
        responseBody = response.body().bytes();
        response.body().close();

        return responseBody;
    }

    // Get profile fields from server
    private String getProfileFields() throws IOException {
        String responseBody = null;
        SessionUrl url = new SessionUrl(application.getScannedSettings(),
                application.getString(R.string.get_profile_fields_endpoint),
                sessionManager.getSessionId());

        //Timber.d("Calling url: " + url.toString());
        Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
        responseBody = response.body().string().trim();
        response.body().close();

        return responseBody;
    }

    // Send profile fields to server
    private void postProfileFields(AgentProfile profile) throws IOException {
        String responseBody = null;
        SessionUrl url = new SessionUrl(application.getScannedSettings(),
                application.getString(R.string.set_profile_fields_endpoint),
                sessionManager.getSessionId());
        url.setParamData(new HashMap<>(profile.getUrlParams()));

        //Timber.d("Calling url: " + url.toString());
        Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
        responseBody = response.body().string().trim();
        response.body().close();
    }

    // Get canned messages for check-in from server
    private CannedMessages getCannedMessages() throws Exception {
        String responseBody = null;
        SessionUrl url = new SessionUrl(application.getScannedSettings(),
                application.getString(R.string.get_canned_messages_endpoint),
                sessionManager.getSessionId());

        //Timber.d("Calling url: " + url.toString());
        Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
        responseBody = response.body().string().trim();
        response.body().close();

        if (responseBody != null && !responseBody.isEmpty()) {
            return CannedMessages.parse(application, responseBody);
        }

        return null;
    }

    // Get contacts from server
    private AgentContacts getAgentContacts() throws Exception {
        String responseBody = null;
        MessageUrl url = new MessageUrl(application.getScannedSettings(),
                application.getString(R.string.get_contacts_endpoint),
                application.getDeviceIdentifier());
        url.setSessionId(sessionManager.getSessionId());

        //Timber.d("Calling url: " + url.toString());
        Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
        responseBody = response.body().string().trim();
        response.body().close();

        if (responseBody != null && !responseBody.isEmpty()) {
            return AgentContacts.parse(application, responseBody);
        }

        return null;
    }

    // Send contacts to server
    private boolean postAgentContacts(AgentContacts contacts) throws Exception {
        int attemptsLeft = 10;
        String responseBody = "";
        String xml = "PostContacts=xml=" +
                contacts.toXml(application.getScannedSettings().getOrganizationId() + application.getDeviceIdentifier(),
                        application.getScannedSettings().getPassword()) + "\n";

        String content = crypto.encryptToHex(xml);
        String ipAddress = application.getScannedSettings().getIpAddress();
        if (ipAddress.contains("://")) {
            ipAddress = ipAddress.split("://")[1];
        }

        SSLSocket socket = null;
        OutputStream os = null;
        InputStream is = null;

        while (attemptsLeft-- > 0) {
            try {
                socket = (SSLSocket) socketFactory.createSocket();
                socket.connect(new InetSocketAddress(ipAddress, application.getScannedSettings().getTrackingPort().intValue()), 20000);
                os = socket.getOutputStream();
                os.write(content.getBytes("UTF-8"));
                os.flush();

                is = socket.getInputStream();
                if (is != null) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(is));
                    String line = null;
                    StringBuilder response = new StringBuilder();
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    responseBody = response.toString().trim();
                }
            } catch (Exception ex) {
                // TODO: Handle SSL errors here...
                String msg = ex.getMessage();
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                if (os != null) {
                    IOUtils.closeQuietly(os);
                }
                if (is != null) {
                    IOUtils.closeQuietly(is);
                }
            }

            // if we don't receive a 200 ok, it's a network error.
            if (responseBody.contains("Error") || responseBody.contains("ERROR") || responseBody.contains("HTTP/1.0 404 File not found") || responseBody.startsWith("ok")) {
                if (BuildConfig.DEBUG) {
                    Crashlytics.setString("Url", ipAddress);
                    Crashlytics.setString("Content", xml);
                    Crashlytics.setString("Response", responseBody);
                }
                throw new Exception("Server did not respond appropriately.");
            }

            responseBody = crypto.decryptFromHexToString(responseBody).trim();

            if (responseBody.contains("ERROR") || responseBody.contains("Error") || responseBody.contains("Invalid SessionId") || responseBody.contains("Invalid Session Id") || responseBody.startsWith("ok")) {
                if (BuildConfig.DEBUG) {
                    Crashlytics.setString("Url", ipAddress);
                    Crashlytics.setString("Content", xml);
                    Crashlytics.setString("Response", responseBody);
                }
                throw new Exception("Server returned us an error.");
            }

            if (responseBody.startsWith("OK")) {
                return true;
            }

            // Sleep and try again
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        return false;
    }

    // Accept alert
    private void acceptAlert(final long alertId, final int status) throws Exception {
        SessionUrl url = new SessionUrl(application.getScannedSettings(),
                application.getString(R.string.alert_acceptance_endpoint),
                sessionManager.getSessionId());

        HashMap<String, String> param = new HashMap<String, String>() {{
            put("alertId", Long.toString(alertId));
            put("accept", Integer.toString(status));
        }};
        url.setParamData(param);

        //Timber.d("Calling url: " + url.toString());
        Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
        response.body().close();
    }

    private boolean verifySubscription(final String userId) {
        String responseBody = null;
        SessionUrl url = new SessionUrl(application.getScannedSettings(),
                application.getString(R.string.subscription_endpoint),
                sessionManager.getSessionId());

        HashMap<String, String> param = new HashMap<String, String>() {{
            put("userId", userId);
        }};
        url.setParamData(param);

        //Timber.d("Calling url: " + url.toString());
        try {
            Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
            responseBody = response.body().string().trim();
            response.body().close();

            RegistryMatcher matcher = new RegistryMatcher();
            matcher.bind(Date.class, new UserSubscription.DateFormatTransformer());
            Serializer serializer = new Persister(matcher);

            boolean validated = false;

            try {
                validated = serializer.validate(UserSubscription.class, responseBody);
            } catch (Exception e) {
            }

            if (!validated) {
                if (BuildConfig.DEBUG) {
                    Crashlytics.setString("Url", url.toString());
                    Crashlytics.setLong("Response Code", response.code());
                    Crashlytics.setString("Response", responseBody);
                }
                throw new Exception("XML Received Could Not Be Validated.");
            }
            UserSubscription subscription = serializer.read(UserSubscription.class, responseBody);

            return subscription.getSubscriptionValid();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private Events.ForgotPasswordCodeReceived forgotPasswordSendCode(final String requestUrl, final long requestPort, final String username) {
        ScannedSettings settings = new ScannedSettings(requestUrl, requestPort);
        SessionUrl url = new SessionUrl(settings,
                application.getString(R.string.forgot_password_endpoint),
                null);

        HashMap<String, String> param = new HashMap<String, String>() {{
            put("function", "SendCode");
            put("username", username);
        }};
        url.setParamData(param);

        //Timber.d("Calling url: " + url.toString());
        try {
            Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
            String responseBody = response.body().string().trim();
            response.body().close();

            String error = XmlUtil.getXmlTagText(responseBody, "error");
            if (error == null && responseBody.indexOf(XML_RESPONSE_OK) > 0) {
                return new Events.ForgotPasswordCodeReceived(true, "ok");
            } else {
                return new Events.ForgotPasswordCodeReceived(false, error);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Events.ForgotPasswordCodeReceived(false, "Unknown error.");
    }

    private Events.ForgotPasswordCodeVerified forgotPasswordVerifyCode(
            final String requestUrl, final long requestPort, final String username, final String code) {

        ScannedSettings settings = new ScannedSettings(requestUrl, requestPort);
        SessionUrl url = new SessionUrl(settings,
                application.getString(R.string.forgot_password_endpoint),
                null);

        HashMap<String, String> param = new HashMap<String, String>() {{
            put("function", "VerifyCode");
            put("username", username);
            put("code", code);
        }};
        url.setParamData(param);

        //Timber.d("Calling url: " + url.toString());
        try {
            Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
            String responseBody = response.body().string().trim();
            response.body().close();

            String error = XmlUtil.getXmlTagText(responseBody, "error");
            if (error == null && responseBody.indexOf(XML_RESPONSE_OK) > 0) {
                return new Events.ForgotPasswordCodeVerified(true, "ok");
            } else {
                return new Events.ForgotPasswordCodeVerified(false, error);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Events.ForgotPasswordCodeVerified(false, "Unknown error.");
    }

    private Events.ForgotPasswordPasswordChanged forgotPasswordChangePassword(
            final String requestUrl, final long requestPort, final String username,
            final String code, final String newPassword) {

        ScannedSettings settings = new ScannedSettings(requestUrl, requestPort);
        SessionUrl url = new SessionUrl(settings,
                application.getString(R.string.forgot_password_endpoint),
                null);

        HashMap<String, String> param = new HashMap<String, String>() {{
            put("function", "ChangePassword");
            put("username", username);
            put("code", code);
            put("password", newPassword);
        }};
        url.setParamData(param);

        //Timber.d("Calling url: " + url.toString());
        try {
            Response response = httpClient.newCall(new Request.Builder().url(url.toString(crypto)).build()).execute();
            String responseBody = response.body().string().trim();
            response.body().close();

            String error = XmlUtil.getXmlTagText(responseBody, "error");
            if (error == null && responseBody.indexOf(XML_RESPONSE_OK) > 0) {
                return new Events.ForgotPasswordPasswordChanged(true, "ok");
            } else {
                return new Events.ForgotPasswordPasswordChanged(false, error);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Events.ForgotPasswordPasswordChanged(false, "Unknown error.");
    }

}
