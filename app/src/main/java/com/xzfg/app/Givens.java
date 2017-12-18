package com.xzfg.app;

import com.xzfg.app.model.url.MessageUrl;

/**
 * This class is a container for constant values.
 * This class cannot be instantiated, and all values must be public static final.
 */
public final class Givens {

    /**
     * Flavors
     */
    public static final String FLAVOR_JDAR = "jdar";
    public static final String FLAVOR_WEATHER = "weather";

    public static final int ADMIN_ACTIVATION_REQUEST_CODE = 42;
    public static final int ADMIN_ENCRYPTION_REQUEST_CODE = 777;
    public static final int MAIN_SERVICE_NOTIFICATION_ID = 7;
    public static final int GALLERY_INTENT = 33;
    public static final int GALLERY_INTENT_KITKAT = 34;
    public static final String ACTION_DEVICE_ADMIN_ENABLED = "com.xzfg.app.ACTION_DEVICE_ADMIN_ENABLED";
    public static final String ACTION_DEVICE_ADMIN_DISABLED = "com.xzfg.app.ACTION_DEVICE_ADMIN_DISABLED";
    public static final String ACTION_APPLICATION_STARTED = "com.xzfg.app.ACTION_APP_STARTED";
    public static final String ACTION_BOSSMODE_DISABLE = "com.xzfg.app.ACTION_BOSSMODE_DISABLE";
    public static final String ACTION_BOSSMODE_EXIT = "com.xzfg.app.ACTION_BOSSMODE_EXIT";
    public static final String ACTION_BOSSMODE_ENABLE = "com.xzfg.app.ACTION_BOSSMODE_ENABLE";
    public static final String DEFAULT_SCREEN_PREFERENCE = "com.xzfg.app.DEFAULT_SCREEN_PREFERENCE";
    public static final String MDM_CONFIG = "com.xzfg.app.MDM_CONFIG";
    public static final int BOSSMODE_TYPE_SLOT = 0;
    public static final int BOSSMODE_TYPE_PINPAD = 1;
    public static final int BOSSMODE_TYPE_WEATHER = 2;
    public static final int AVATAR_SIZE_PIXELS = 150;
    public static final int SELECT_PROFILE_PHOTO = 2010;
    public static final int SKIP_PROFILE_PHOTO = 2011;

    /**
     * This is the Android volume change broadcast identifier.
     * This is an undocumented, private identifier, that in practice works on the vast majority of
     * devices. We can't watch the volume buttons, so we actually have to watch the volume.
     */
    public static final String ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION";
    public static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    public static final String EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";
    public static final String EXTRA_PREV_VOLUME_STREAM_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";

    /**
     * Custom Extras
     */
    public static final String EXTRA_ATTEMPTS = "EXTRA_ATTEMPTS";
    public static final String EXTRA_MAX_DURATION = "EXTRA_MAX_DURATION";
    public static final String EXTRA_AGENT_SETTINGS = "EXTRA_AGENT_SETTINGS";

    /**
     * Custom Arguments
     */
    public static final String ARG_URL = "ARG_URL";
    public static final String ARG_PORT = "ARG_PORT";
    public static final String ARG_USERNAME = "ARG_USERNAME";
    public static final String ARG_CODE = "ARG_CODE";

    /**
     * Chat Action
     */
    public static final String ACTION_CHAT_LIST_USERS = "com.xzfg.app.ACTION_CHAT_LIST_USERS";
    public static final String ACTION_CHAT_LIST_MESSAGES = "com.xzfg.app.ACTION_CHAT_LIST_MESSAGES";
    public static final String ACTION_CHAT_SEND_MESSAGE = "com.xzfg.app.ACTION_CHAT_SEND_MESSAGE";
    public static final String ACTION_CHAT_SIGN_IN = "com.xzfg.app.ACTION_CHAT_SIGN_IN";
    public static final String ACTION_CHAT_PROCESS_QUEUE = "com.zxfg.app.ACTION_CHAT_PROCESS_QUEUE";
    public static final String ACTION_CHAT_DELETE_MESSAGES = "com.xzfg.app.ACTION_CHAT_DELETE_MESSAGES";
    public static final String ACTION_CHAT_EMAIL_SEND_MESSAGE = "com.xzfg.app.ACTION_CHAT_EMAIL_MESSAGE";
    public static final String ACTION_CHAT_SMS_SEND_MESSAGE = "com.xzfg.app.ACTION_CHAT_SMS_MESSAGE";

    /**
     * Config.
     */
    public static final String CONFIG = "com.xzfg.app.CONFIG";
    public static final String SETTINGS = "com.xzfg.app.SETTINGS";
    public static final String WEATHER = "com.xzfg.app.WEATHER";
    public static final String PROFILE = "com.xzfg.app.PROFILE";
    public static final String CONTACTS = "com.xzfg.app.CONTACTS";
    public static final String CANNED_MESSAGES = "com.xzfg.app.CANNED_MESSAGES";
    public static final String SCANNED_SETTINGS = "com.xzfg.app.SCANNED_SETTINGS";
    public static final String DIRECTORY_PREFERENCES = "preferences";
    public static final String MESSAGE = MessageUrl.class.getName();
    public static final String UPLOAD_TYPE_IMG = "IMG";
    public static final String UPLOAD_TYPE_VIDEO = "VIDEO";
    public static final String UPLOAD_TYPE_AUDIO = "AUDIO";
    public static final String UPLOAD_TYPE_SMSLOG = "SMS";
    public static final String UPLOAD_TYPE_PHONELOG = "PHONELOG";
    public static final String UPLOAD_FORMAT_JPEG = "JPG";
    public static final String UPLOAD_FORMAT_MP4 = "MP4";
    public static final String UPLOAD_FORMAT_3GP = "3GP";
    public static final String UPLOAD_FORMAT_TXT = "TXT";
    public static final String EXTERNAL_STORAGE = "EXTERNAL";
    public static final String INTERNAL_STORAGE = "INTERNAL";
    public static final String COLLECT_PHONECALLS_KEY = "com.xzfg.app.PHONECALLS";
    public static final String COLLECT_PHONELOGS_KEY = "com.xzfg.app.PHONELOGS";
    public static final String COLLECT_PHONELOGS_DELIVERY_KEY = "com.xzfg.app.PHONELOGS_DELIVERY";
    public static final String COLLECT_SMS_KEY = "com.xzfg.app.SMS";
    public static final String COLLECT_SMS_DELIVERY_KEY = "com.xzfg.app.SMS_DELIVERY";
    public static final String PHONELOG_FILE_NAME = "xzfg_phonelog.txt";

    /**
     * Collection modes.
     */
    public static final int COLLECT_MODE_UNKNOWN = -1;
    public static final int COLLECT_MODE_VIDEO = 0;
    public static final int COLLECT_MODE_PICTURE = 1;
    public static final int COLLECT_MODE_AUDIO = 2;
    public static final int COLLECT_MODE_VIDEO_LIVE = 3;
    public static final int COLLECT_MODE_AUDIO_LIVE = 4;

    /**
     * DISTANCE values for POI and Collected Media
     */
    public static final int DEBUG_DISTANCE = 500;
    public static final int DEFAULT_DISTANCE = 20;
    public static final int FREE_SPACE_LIMIT = 50;
    public static final String SERVER_SOCKET_NAME = "com.xzfg.app.MEDIA_SERVER_SOCKET";
    public static final int THREAD_PRIORITY = android.os.Process.THREAD_PRIORITY_DEFAULT + (2 * android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);

    public static final int NOTIFICATION_AUDIO_RECORDING_ID = 1;
    public static final int NOTIFICATION_AUDIO_STREAMING_ID = 2;
    // Maximum audio recording duration in milliseconds
    // Set to zero or negative to go non-stop
    public static final int MAX_AUDIO_DURATION_MS = 5*60000;
    // Maximum video recording duration in milliseconds
    public static final int MAX_VIDEO_DURATION_MS = 5*60000;


    private Givens() {
    }
}
