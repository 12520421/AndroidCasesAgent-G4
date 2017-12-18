package com.xzfg.app.model.url;

import android.os.Parcel;

import com.xzfg.app.model.ScannedSettings;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;


public class PingUrl extends RegistrationUrl {

    private String message = "Android";

    public PingUrl() {
        super();
    }

    public PingUrl(ScannedSettings scannedSettings, String endPoint, String deviceId) {
        super(scannedSettings, endPoint, deviceId);
    }


    @Override
    public String getParams() {

        try {
            StringBuilder sb = new StringBuilder(192);
            sb.append(super.getParams());
            if (message != null) {
                appendParam(sb, "metaData", message);
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Error generating parameters.");
        }
        return null;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected PingUrl(Parcel in) {
        super(in);
        this.message = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.message);
    }

    public static final Creator<PingUrl> CREATOR = new Creator<PingUrl>() {
        public PingUrl createFromParcel(Parcel source) {
            return new PingUrl(source);
        }

        public PingUrl[] newArray(int size) {
            return new PingUrl[size];
        }
    };

}
