package com.xzfg.app.util;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Inbox;
import android.provider.Telephony.TextBasedSmsColumns;
import android.provider.Telephony.Threads;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.R;
import com.xzfg.app.model.SmsMmsMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@TargetApi(VERSION_CODES.KITKAT)
public class SMSUtil {
    private static final String TAG = SMSUtil.class.getName();

    public static final String LOG_FILE_NAME_SMS = "xzfg_smslog.txt";

    // Content URIs for SMS app, these may change in future SDK
    public static final Uri MMS_SMS_CONTENT_URI = MmsSms.CONTENT_URI;
    public static final Uri THREAD_ID_CONTENT_URI = Uri.withAppendedPath(MMS_SMS_CONTENT_URI, "threadID");
    public static final Uri CONVERSATION_CONTENT_URI = Threads.CONTENT_URI;
    public static final String SMSTO_SCHEMA = "smsto:";
    private static final String UNREAD_CONDITION = TextBasedSmsColumns.READ + "=0";

    public static final Uri SMS_CONTENT_URI = Sms.CONTENT_URI;
    public static final Uri SMS_INBOX_CONTENT_URI = Inbox.CONTENT_URI;

    public static final Uri MMS_CONTENT_URI = Mms.CONTENT_URI;
    public static final Uri MMS_INBOX_CONTENT_URI = Mms.Inbox.CONTENT_URI;

    public static final String SMS_MIME_TYPE = "vnd.android-dir/mms-sms";
    public static final int READ_THREAD = 1;


    public static boolean hasHoneycomb() {
        // Can use static final constants like HONEYCOMB, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasICS() {
        // Can use static final constants like ICS, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT;
    }

    /**
     * Looks up a contacts display name by contact lookup key - if not found,
     * the address (phone number) will be formatted and returned instead.
     *
     * @param context   Context.
     * @param lookupKey Contact lookup key.
     * @param contactId contactid
     * @return Contact name or null if not found.
     */
    public static ContactIdentification getPersonNameByLookup(Context context, String lookupKey,
                                                              String contactId) {

        // Check for id, if null return the formatting phone number as the name
        if (lookupKey == null) {
            return null;
        }

        Uri.Builder builder = Contacts.CONTENT_LOOKUP_URI.buildUpon();
        builder.appendPath(lookupKey);
        if (contactId != null) {
            builder.appendPath(contactId);
        }
        Uri uri = builder.build();

        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME},
                null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    final String newId = cursor.getString(0);
                    final String newLookup = cursor.getString(1);
                    final String contactName = cursor.getString(2);
                    if (BuildConfig.DEBUG) Log.v(TAG, "Contact Display Name: " + contactName);
                    return new ContactIdentification(newId, newLookup, contactName);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return null;
    }

    /*
     * Class to hold contact lookup info (as of Android 2.0+ we need the id and lookup key)
     */
    public static class ContactIdentification {
        public String contactId = null;
        public String contactLookup = null;
        public String contactName = null;

        public ContactIdentification(String _contactId, String _contactLookup, String _contactName) {
            contactId = _contactId;
            contactLookup = _contactLookup;
            contactName = _contactName;
        }
    }

    /**
     * Looks up a contacts id, given their address (phone number in this case). Returns null if not
     * found
     */
    public static ContactIdentification getPersonIdFromPhoneNumber(
            Context context, String address) {

        if (address == null) {
            return null;
        }

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address)),
                    new String[]{PhoneLookup._ID, PhoneLookup.DISPLAY_NAME, PhoneLookup.LOOKUP_KEY},
                    null, null, null);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "getPersonIdFromPhoneNumber(): " + e.toString());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "getPersonIdFromPhoneNumber(): " + e.toString());
            return null;
        }

        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    String contactId = String.valueOf(cursor.getLong(0));
                    String contactName = cursor.getString(1);
                    String contactLookup = cursor.getString(2);

                    if (BuildConfig.DEBUG)
                        Log.v(TAG, "Found person: " + contactId + ", " + contactName + ", "
                                + contactLookup);
                    return new ContactIdentification(contactId, contactLookup, contactName);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return null;
    }

    /**
     * Looks up a contacts id, given their email address. Returns null if not found
     */
    public static ContactIdentification getPersonIdFromEmail(Context context, String email) {
        if (email == null)
            return null;

        Cursor cursor;
        try {
            cursor = context.getContentResolver().query(
                    Uri.withAppendedPath(
                            Email.CONTENT_LOOKUP_URI,
                            Uri.encode(extractAddrSpec(email))),
                    new String[]{Email.CONTACT_ID, Email.DISPLAY_NAME_PRIMARY, Email.LOOKUP_KEY},
                    null, null, null);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "getPersonIdFromEmail(): " + e.toString());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "getPersonIdFromEmail(): " + e.toString());
            return null;
        }

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {

                    String contactId = String.valueOf(cursor.getLong(0));
                    String contactName = cursor.getString(1);
                    String contactLookup = cursor.getString(2);

                    if (BuildConfig.DEBUG)
                        Log.v(TAG, "Found person: " + contactId + ", " + contactName + ", "
                                + contactLookup);
                    return new ContactIdentification(contactId, contactLookup, contactName);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return null;
    }


      /**
     * Tries to locate the message thread id given the address (phone or email) of the message
     * sender.
     *
     * @param context a context to use
     * @param address phone number or email address of sender
     * @return the thread id (or 0 if there was a problem)
     */
    public static long findThreadIdFromAddress(Context context, String address) {
        if (address == null)
            return 0;

        String THREAD_RECIPIENT_QUERY = "recipient";

        Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();
        uriBuilder.appendQueryParameter(THREAD_RECIPIENT_QUERY, address);

        long threadId = 0;

        Cursor cursor = null;
        try {

            cursor = context.getContentResolver().query(
                    uriBuilder.build(),
                    new String[]{Contacts._ID},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                threadId = cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return threadId;
    }

    /**
     * Tries to locate the message id (from the system database), given the message thread id, the
     * timestamp of the message and the type of message (sms/mms)
     */
    public static long findMessageId(Context context, long threadId, long timestamp,
                                     String body, int messageType) {

        long id = 0;
        String selection = "body = " + DatabaseUtils.sqlEscapeString(body != null ? body : "");
        selection += " and " + UNREAD_CONDITION;
        final String sortOrder = "date DESC";
        final String[] projection = new String[]{"_id", "date", "thread_id", "body"};

        if (threadId > 0) {
            if (BuildConfig.DEBUG)
                Log.v(TAG, "Trying to find message ID");
            if (SmsMmsMessage.MESSAGE_TYPE_MMS == messageType) {
                // It seems MMS timestamps are stored in a seconds, whereas SMS timestamps are in
                // millis
                selection += " and date = " + (timestamp / 1000);
            }

            Cursor cursor = context.getContentResolver().query(
                    ContentUris.withAppendedId(CONVERSATION_CONTENT_URI, threadId),
                    projection,
                    selection,
                    null,
                    sortOrder);

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    id = cursor.getLong(0);
                    if (BuildConfig.DEBUG)
                        Log.v(TAG, "Message id found = " + id);
                    // Log.v("Timestamp = " + cursor.getLong(1));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        if (BuildConfig.DEBUG && id == 0) {
            Log.v(TAG, "Message id could not be found");
        }

        return id;
    }

    /**
     * Fetches a list of unread messages from the system database
     *
     * @param context app context
     * @return ArrayList of SmsMmsMessage
     */
    public static ArrayList<SmsMmsMessage> getUnreadSms(Context context) {
        ArrayList<SmsMmsMessage> messages = null;

        final String[] projection =
                new String[]{"_id", "thread_id", "address", "date", "body"};
        String selection = UNREAD_CONDITION + " and date>0 and body is not null and body != ''";
        String[] selectionArgs = null;
        final String sortOrder = "date ASC";

        // Create cursor
        Cursor cursor = context.getContentResolver().query(
                SMS_INBOX_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder);

        long messageId;
        long threadId;
        String address;
        long timestamp;
        String body;
        SmsMmsMessage message;

        if (cursor != null) {
            try {
                int count = cursor.getCount();
                if (count > 0) {
                    messages = new ArrayList<>(count);
                    while (cursor.moveToNext()) {
                        messageId = cursor.getLong(0);
                        threadId = cursor.getLong(1);
                        address = cursor.getString(2);
                        timestamp = cursor.getLong(3);
                        body = cursor.getString(4);

                        if (!TextUtils.isEmpty(address) && !TextUtils.isEmpty(body)
                                && timestamp > 0) {
                            message = new SmsMmsMessage(
                                    context, address, body, timestamp, threadId,
                                    count, messageId, SmsMmsMessage.MESSAGE_TYPE_SMS);
                            message.setNotify(false);
                            messages.add(message);
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return messages;
    }

    public static ArrayList<SmsMmsMessage> getUnreadMms(Context context) {
        ArrayList<SmsMmsMessage> messages = null;

        final String[] projection = new String[]{"_id", "thread_id", "date", "sub", "sub_cs"};
        String selection = UNREAD_CONDITION;
        String[] selectionArgs = null;
        final String sortOrder = "date ASC";
        int count = 0;

//        if (ignoreThreadId > 0) {
//            selection += " and thread_id != ?";
//            selectionArgs = new String[] { String.valueOf(ignoreThreadId) };
//        }

        Cursor cursor = context.getContentResolver().query(
                MMS_INBOX_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder);

        SmsMmsMessage message;
        if (cursor != null) {
            try {
                count = cursor.getCount();
                if (count > 0) {
                    messages = new ArrayList<>(count);
                    while (cursor.moveToNext()) {
                        long messageId = cursor.getLong(0);
                        long threadId = cursor.getLong(1);
                        long timestamp = cursor.getLong(2) * 1000;
                        String subject = cursor.getString(3);

                        message = new SmsMmsMessage(context, messageId, threadId, timestamp,
                                subject, count, SmsMmsMessage.MESSAGE_TYPE_MMS);
                        message.setNotify(false);
                        messages.add(message);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return messages;
    }


    /**
     * @return sms inbox intent
     * @param context the context
     */
    public static Intent getSmsInboxIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);

        int flags = Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP;
        intent.setFlags(flags);

        if (hasKitKat()) {
            // From KitKat onward start the default SMS app
            final String defaultSmsPackage = Sms.getDefaultSmsPackage(context);
            if (defaultSmsPackage != null) {
                intent.setPackage(defaultSmsPackage);
            }
        } else {
            // Pre-KitKat use mime type to start the SMS app
            intent.setType(SMS_MIME_TYPE);
        }

        return intent;
    }

    /**
     * Get system sms-to Intent (normally "compose message" activity)
     *
     * @param phoneNumber the phone number to compose the message to
     * @return the intent that can be started with startActivity()
     */
    public static Intent getSmsToIntent(Context context, String phoneNumber) {

        Intent popup = new Intent(Intent.ACTION_SENDTO);

        // Should *NOT* be using FLAG_ACTIVITY_MULTIPLE_TASK however something is broken on
        // a few popular devices that received recent Froyo upgrades that means this is required
        // in order to refresh the system compose message UI
        int flags =
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        // Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP;
        // Intent.FLAG_ACTIVITY_MULTIPLE_TASK;

        popup.setFlags(flags);

        if (!"".equals(phoneNumber)) {
            // Log.v("^^Found threadId (" + threadId + "), sending to Sms intent");
            popup.setData(Uri.parse(SMSTO_SCHEMA + Uri.encode(phoneNumber)));
        } else {
            return getSmsInboxIntent(context);
        }
        return popup;
    }

    /**
     * @param context context
     * @param ignoreThreadId thread id
     * @return sms mms message
     */
    public static SmsMmsMessage getMmsDetails(Context context, long ignoreThreadId) {

        final String[] projection = new String[]{"_id", "thread_id", "date", "sub", "sub_cs"};
        String selection = UNREAD_CONDITION;
        String[] selectionArgs = null;
        final String sortOrder = "date DESC";
        int count = 0;

        if (ignoreThreadId > 0) {
            selection += " and thread_id != ?";
            selectionArgs = new String[]{String.valueOf(ignoreThreadId)};
        }

        Cursor cursor = context.getContentResolver().query(
                MMS_INBOX_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder);

        if (cursor != null) {
            try {
                count = cursor.getCount();
                if (count > 0) {
                    cursor.moveToFirst();
                    long messageId = cursor.getLong(0);
                    long threadId = cursor.getLong(1);
                    long timestamp = cursor.getLong(2) * 1000;
                    String subject = cursor.getString(3);

                    return new SmsMmsMessage(context, messageId, threadId, timestamp,
                            subject, count, SmsMmsMessage.MESSAGE_TYPE_MMS);
                }

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return null;
    }

    public static SmsMmsMessage getMmsDetails(Context context) {
        return getMmsDetails(context, 0);
    }

    public static String getMmsAddress(Context context, long messageId) {
        final String[] projection = new String[]{"address", "contact_id", "charset", "type"};
//        final String selection = "type=137"; // "type="+ PduHeaders.FROM,
        final String selection = null;

        Uri.Builder builder = MMS_CONTENT_URI.buildUpon();
        builder.appendPath(String.valueOf(messageId)).appendPath("addr");

        Cursor cursor = context.getContentResolver().query(
                builder.build(),
                projection,
                selection,
                null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    // Apparently contact_id is always empty in this table so we can't get it from
                    // here

                    // Just return the address
                    return cursor.getString(0);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return context.getString(android.R.string.unknownName);
    }

    public static final Pattern NAME_ADDR_EMAIL_PATTERN =
            Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

    public static final Pattern QUOTED_STRING_PATTERN =
            Pattern.compile("\\s*\"([^\"]*)\"\\s*");

    public static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    );

    public static boolean isEmailAddress(String email) {
        if (email == null) {
            return false;
        }
        return EMAIL_ADDRESS_PATTERN.matcher(email).matches();
    }

    private static String extractAddrSpec(String address) {
        Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(address);

        if (match.matches()) {
            return match.group(2);
        }
        return address;
    }

    private static String getEmailDisplayName(String displayString) {
        Matcher match = QUOTED_STRING_PATTERN.matcher(displayString);
        if (match.matches()) {
            return match.group(1);
        }
        return displayString;
    }

    /**
     * Read the PDUs out of an SMS_RECEIVED_ACTION or DATA_SMS_RECEIVED_ACTION intent.
     *
     * @param intent the intent to read from
     * @return an array of SmsMessages for the PDUs
     */
    public static final SmsMessage[] getMessagesFromIntent(Intent intent) {
        Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
        if (messages == null) {
            return null;
        }
        if (messages.length == 0) {
            return null;
        }

        byte[][] pduObjs = new byte[messages.length][];

        for (int i = 0; i < messages.length; i++) {
            pduObjs[i] = (byte[]) messages[i];
        }
        byte[][] pdus = new byte[pduObjs.length][];
        int pduCount = pdus.length;
        SmsMessage[] msgs = new SmsMessage[pduCount];
        for (int i = 0; i < pduCount; i++) {
            pdus[i] = pduObjs[i];
            msgs[i] = SmsMessage.createFromPdu(pdus[i]);
        }
        return msgs;
    }

    public static String getFormattedCurrentDate(){
        //MM-dd-yyyy HH:mm:ss
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Date currentLocalTime = cal.getTime();
        DateFormat date = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        date.setTimeZone(TimeZone.getTimeZone("GMT"));
        return date.format(currentLocalTime);
    }


    public static File updateSMSLog(SmsMmsMessage message, Application application, String direction, Location location){
        String filePath = getFilePath(application);
        try
        {
            File root = new File(filePath);
            if (!root.exists()) {
                root.mkdirs();
            }
            File logFile = new File(root, SMSUtil.LOG_FILE_NAME_SMS);

            FileWriter writer = new FileWriter(logFile,true);
            writer.append("<message");
            writer.append(" datetime=\"" + SMSUtil.getFormattedCurrentDate() + "\"");
            writer.append(" type=\"" + application.getString(R.string.log_type_sms) + "\"");
            writer.append(" direction=\"" + direction + "\"");
            if ( message.getAddress() != null) {
                writer.append(" phoneNumber=\"" + message.getAddress().replace("+", "") + "\"");
            } else {writer.append(" phoneNumber=\"" +  "\"");}
            if ( message.getContactName() != null) {
                writer.append(" contactName=\"" + message.getContactName().replace("+", "") + "\"");
            } else {writer.append(" contactName=\"" +  "\"");}

            if (location != null) {
                writer.append(" bearing=\"" + location.getBearing() + "\"");
            }
            writer.append(">");
            writer.append(message.getMessageBody().trim());
            writer.append("</message>");
            writer.flush();
            writer.close();

            return logFile;

        }
        catch(IOException e)
        {
            Log.e(TAG, e.getLocalizedMessage());
            return null;
        }
    }

    public static String getFilePath(Application application) {
        String filePath="";
        if (application!=null && application.getAgentSettings() != null) {
            if (application.getAgentSettings().getExternalStorage() == 1 && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                filePath= application.getExternalFilesDir("logs") + "/";
            } else {
                filePath= application.getFilesDir().getAbsolutePath() + "/logs/";
            }
        }
        return filePath;
    }
}
