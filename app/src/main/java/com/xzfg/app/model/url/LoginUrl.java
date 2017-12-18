package com.xzfg.app.model.url;

import android.os.Parcel;

import com.xzfg.app.Application;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;


public class LoginUrl extends BaseUrl {

    private String encKey;
    private String uid;

    @SuppressWarnings("unused")
    public LoginUrl() {
        super();
    }

    @SuppressWarnings("unused")
    public LoginUrl(String ipAddress, Long trackingPort, String endPoint, String encKey, String uid) {
        super(ipAddress, trackingPort, endPoint);
        this.encKey = encKey;
        this.uid = uid;
    }

    @SuppressWarnings("unused")
    public LoginUrl(Application application, String endPoint) {
        super(application.getScannedSettings(), endPoint);
        this.encKey = application.getScannedSettings().getUserId();
        this.uid = application.getDeviceIdentifier();
    }


    @Override
    public String getParams() {
        try {
            StringBuilder sb = new StringBuilder(192);
            sb.append(super.getParams());

            appendParam(sb, "encKey", encKey);
            appendParam(sb, "uid", uid);
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Couldn't build params.");
        }
        return null;
    }

    public String getEncKey() {
        return encKey;
    }

    public void setEncKey(String encKey) {
        this.encKey = encKey;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    protected LoginUrl(Parcel in) {
        super(in);
        this.encKey = in.readString();
        this.uid = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.encKey);
        dest.writeString(this.uid);
    }

    public static final Creator<LoginUrl> CREATOR = new Creator<LoginUrl>() {
        public LoginUrl createFromParcel(Parcel source) {
            return new LoginUrl(source);
        }

        public LoginUrl[] newArray(int size) {
            return new LoginUrl[size];
        }
    };

}
