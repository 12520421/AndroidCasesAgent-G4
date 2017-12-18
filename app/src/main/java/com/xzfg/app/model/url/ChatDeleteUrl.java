package com.xzfg.app.model.url;

import android.os.Parcel;

import com.xzfg.app.model.ScannedSettings;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;

public class ChatDeleteUrl extends ChatBaseUrl {

    String toUserId;
    String messageId;
    String type;

    public ChatDeleteUrl() {
        super();
    }

    public ChatDeleteUrl(ScannedSettings scannedSettings, String endpoint, String sessionId) {
        super(scannedSettings, endpoint, sessionId);
    }


    @Override
    public String getParams() {
        try {
            StringBuilder sb = new StringBuilder(192);
            sb.append(super.getParams());

            if (messageId != null) {
                appendParam(sb, "messageId", messageId);
                if (type != null) {
                    appendParam(sb, "type", type);
                }
            }
            if (toUserId != null) {
                appendParam(sb, "toUserId", toUserId);
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Couldn't build params.");
        }
        return null;


    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.toUserId);
        dest.writeString(this.messageId);
        dest.writeString(this.type);
    }

    protected ChatDeleteUrl(Parcel in) {
        super(in);
        this.toUserId = in.readString();
        this.messageId = in.readString();
        this.type = in.readString();
    }

    public static final Creator<ChatDeleteUrl> CREATOR = new Creator<ChatDeleteUrl>() {
        public ChatDeleteUrl createFromParcel(Parcel source) {
            return new ChatDeleteUrl(source);
        }

        public ChatDeleteUrl[] newArray(int size) {
            return new ChatDeleteUrl[size];
        }
    };
}
