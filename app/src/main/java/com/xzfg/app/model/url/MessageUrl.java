package com.xzfg.app.model.url;


import android.os.Parcel;

import com.xzfg.app.model.ScannedSettings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import timber.log.Timber;


public class MessageUrl extends RegistrationUrl {

    public static final String MESSAGE_SOS = "SOS";
    public static final String MESSAGE_SOS_DURESS = "SOSDuress";
    public static final String MESSAGE_SOS_CANCEL = "SOSCancel";
    public static final String MESSAGE_LOW_BATTERY = "Low Battery";
    public static final String MESSAGE_APP_OPENED = "Cases App Opened";
    public static final String MESSAGE_APP_CLOSED = "Cases App Closed";
    public static final String MESSAGE_APP_HIDDEN = "Hide App Detected";
    public static final String MESSAGE_AUDIO_SUBMITTED = "Audio Submitted";
    public static final String MESSAGE_VIDEO_SUBMITTED = "Video Submitted";
    public static final String MESSAGE_PICTURE_SUBMITTED = "Picture Submitted";
    public static final String MESSAGE_SMS_LOG_SUBMITTED = "SMS Log Submitted";
    public static final String MESSAGE_PHONE_LOG_SUBMITTED = "Phone Log Submitted";
    public static final String MESSAGE_LIVE_VIDEO_START = "Live Video Start";
    public static final String MESSAGE_LIVE_AUDIO_START = "Live Audio Start";
    public static final String MESSAGE_STOPPED_TRACKING = "Stopped Tracking";
    public static final String MESSAGE_STARTED_TRACKING = "Started Tracking";
    public static final String MESSAGE_LIVE_VIDEO_STOP = "Live Video Stop";
    public static final String MESSAGE_LIVE_AUDIO_STOP = "Live Audio Stop";
    public static final String MESSAGE_GET_EULA = "GetEULA";
    public static final String MESSAGE_SET_EULA = "SetEULA";
    public static final String MESSAGE_GET_HELP = "GetHelp";

    protected ConcurrentHashMap<String, String> metaData;
    protected String message;
    protected String sessionId;


    protected String ackMessage;

    @SuppressWarnings("unused")
    public MessageUrl() {
        super();
    }

    public MessageUrl(ScannedSettings settings, String endPoint, String deviceId) {
        super(settings, endPoint, deviceId);
    }

    @Override
    public String getParams() {
        try {
            StringBuilder sb = new StringBuilder(192);

            String baseParams = super.getParams();
            if (baseParams != null)
                sb.append(baseParams);

            if (message != null)
                appendParam(sb, "message", message);

            if (sessionId != null)
                appendParam(sb, "sessionId", sessionId);

            if (metaData != null && !metaData.isEmpty()) {
                StringBuilder metaDataBuilder = new StringBuilder();
                for (Map.Entry<String, String> entry : metaData.entrySet()) {
                    if (metaDataBuilder.length() > 0) {
                        metaDataBuilder.append("|");
                    }
                    metaDataBuilder.append(entry.getKey());
                    metaDataBuilder.append("~");
                    metaDataBuilder.append(entry.getValue());
                }
                if (metaDataBuilder.length() > 0) {
                    appendParam(sb, "metaData", metaDataBuilder.toString());
                }
            }
            return sb.toString();

        } catch (Exception e) {
            Timber.e(e, "An error occurred generating the url.");
        }
        return null;
    }

    public ConcurrentHashMap<String, String> getMetaData() {
        return metaData;
    }

    public void setMetaData(ConcurrentHashMap<String, String> metaData) {
        this.metaData = metaData;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAckMessage() {
        return ackMessage;
    }

    public void setAckMessage(String ackMessage) {
        this.ackMessage = ackMessage;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeSerializable(this.metaData);
        dest.writeString(this.message);
        dest.writeString(this.ackMessage);
    }

    protected MessageUrl(Parcel in) {
        super(in);
      //noinspection unchecked
      this.metaData = (ConcurrentHashMap<String, String>) in.readSerializable();
        this.message = in.readString();
        this.ackMessage = in.readString();
    }

    public static final Creator<MessageUrl> CREATOR = new Creator<MessageUrl>() {
        public MessageUrl createFromParcel(Parcel source) {
            return new MessageUrl(source);
        }

        public MessageUrl[] newArray(int size) {
            return new MessageUrl[size];
        }
    };
}
