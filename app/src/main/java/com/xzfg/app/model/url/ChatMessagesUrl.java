package com.xzfg.app.model.url;

import android.os.Parcel;

import com.xzfg.app.model.ScannedSettings;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;


public class ChatMessagesUrl extends ChatBaseUrl {

    private Long lastMessageId = 0L;

    public ChatMessagesUrl() {
        super();
    }

    public ChatMessagesUrl(ScannedSettings settings, String endpoint, String sessionId) {
        super(settings, endpoint, sessionId);
    }


    @Override
    public String getParams() {
        try {
            StringBuilder sb = new StringBuilder(192);
            sb.append(super.getParams());
            appendParam(sb, "lastMessagId", String.valueOf(lastMessageId));
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Couldn't build params.");
        }
        return null;
    }

    public Long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(Long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected ChatMessagesUrl(Parcel in) {
        super(in);
        this.lastMessageId = (Long) in.readValue(Long.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeValue(this.lastMessageId);
    }

    public static final Creator<ChatMessagesUrl> CREATOR = new Creator<ChatMessagesUrl>() {
        public ChatMessagesUrl createFromParcel(Parcel source) {
            return new ChatMessagesUrl(source);
        }

        public ChatMessagesUrl[] newArray(int size) {
            return new ChatMessagesUrl[size];
        }
    };

}
