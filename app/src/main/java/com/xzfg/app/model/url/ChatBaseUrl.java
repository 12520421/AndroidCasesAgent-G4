package com.xzfg.app.model.url;

import android.os.Parcel;

import com.xzfg.app.model.ScannedSettings;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;

public class ChatBaseUrl extends BaseUrl {
    protected String sessionId;

    public ChatBaseUrl() {
        super();
    }

    public ChatBaseUrl(ScannedSettings settings, String endpoint, String sessionId) {
        super(settings, endpoint);
        this.sessionId = sessionId;
    }

    @Override
    public String getParams() {

        try {
            StringBuilder sb = new StringBuilder(192);
            sb.append(super.getParams());
            appendParam(sb, "sessionId", sessionId);
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Couldn't build params.");
        }
        return null;

    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.sessionId);
    }

    protected ChatBaseUrl(Parcel in) {
        super(in);
        this.sessionId = in.readString();
    }


    public static final Creator<ChatBaseUrl> CREATOR = new Creator<ChatBaseUrl>() {
        public ChatBaseUrl createFromParcel(Parcel source) {
            return new ChatBaseUrl(source);
        }

        public ChatBaseUrl[] newArray(int size) {
            return new ChatBaseUrl[size];
        }
    };
}
