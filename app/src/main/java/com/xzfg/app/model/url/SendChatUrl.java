package com.xzfg.app.model.url;

import android.os.Parcel;

import com.xzfg.app.model.ScannedSettings;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;


public class SendChatUrl extends ChatBaseUrl {

    private String toUserId;
    private String message;
    private String text;
    private String fromUsername;

    public SendChatUrl() {
        super();
    }


    public SendChatUrl(ScannedSettings settings, String endpoint, String sessionId) {
        super(settings, endpoint, sessionId);
    }


    @Override
    public String getParams() {
        try {
            StringBuilder sb = new StringBuilder(256);
            sb.append(super.getParams());
            appendParam(sb, "toUserId", toUserId);
            if (text != null)
                appendParam(sb, "text", text);
            if (message != null)
                appendParam(sb, "message", message);
            if (fromUsername != null)
                appendParam(sb, "fromUsername", fromUsername);
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected SendChatUrl(Parcel in) {
        super(in);
        this.toUserId = in.readString();
        this.message = in.readString();
        this.text = in.readString();
        this.fromUsername = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.toUserId);
        dest.writeString(this.message);
        dest.writeString(this.text);
        dest.writeString(this.fromUsername);
    }

    public static final Creator<SendChatUrl> CREATOR = new Creator<SendChatUrl>() {
        public SendChatUrl createFromParcel(Parcel source) {
            return new SendChatUrl(source);
        }

        public SendChatUrl[] newArray(int size) {
            return new SendChatUrl[size];
        }
    };

}
