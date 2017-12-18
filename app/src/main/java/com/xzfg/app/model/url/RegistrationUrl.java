package com.xzfg.app.model.url;

import android.os.Parcel;

import com.xzfg.app.model.ScannedSettings;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;


public class RegistrationUrl extends BaseUrl {

    protected String password;
    protected String serial;
    protected String userId;
    protected String orgId;

    @SuppressWarnings("Unused")
    public RegistrationUrl() {
        super();
    }

    @SuppressWarnings("unused")
    public RegistrationUrl(String ipAddress, Long trackingPort, String endPoint, String password, String serial, String userId, String orgId) {
        super(ipAddress, trackingPort, endPoint);
        this.password = password;
        this.serial = serial;
        this.userId = userId;
        this.orgId = orgId;
    }

    public RegistrationUrl(ScannedSettings settings, String endPoint, String deviceId) {
        super(settings, endPoint);
        this.password = settings.getPassword();
        this.userId = settings.getUserId();
        this.endPoint = endPoint;
        this.serial = deviceId;
        this.orgId = String.valueOf(settings.getOrganizationId());
    }

    @Override
    public String getParams() {
        try {
            StringBuilder sb = new StringBuilder(192);
            sb.append(super.getParams());
            appendParam(sb, "model", "CASESAgent");
            appendParam(sb, "password", password);
            appendParam(sb, "serial", orgId + serial);
            appendParam(sb, "userId", userId);
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Error generrating parameters.");
        }
        return null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.password);
        dest.writeString(this.serial);
        dest.writeString(this.userId);
        dest.writeString(this.orgId);
    }

    protected RegistrationUrl(Parcel in) {
        super(in);
        this.password = in.readString();
        this.serial = in.readString();
        this.userId = in.readString();
        this.orgId = in.readString();
    }

    public static final Creator<RegistrationUrl> CREATOR = new Creator<RegistrationUrl>() {
        public RegistrationUrl createFromParcel(Parcel source) {
            return new RegistrationUrl(source);
        }

        public RegistrationUrl[] newArray(int size) {
            return new RegistrationUrl[size];
        }
    };
}
