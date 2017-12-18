package com.xzfg.app.model.url;

import android.os.Parcel;

import com.xzfg.app.model.ScannedSettings;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;

public class AlertsUrl extends SessionUrl {


    String groupId;

    public AlertsUrl() {
        super();
    }

    public AlertsUrl(ScannedSettings settings, String endpoint, String sessionId) {
        super(settings, endpoint, sessionId);
    }

    @Override
    public String getParams() {
        try {
            StringBuilder sb = new StringBuilder(192);
            sb.append(super.getParams());
            if (groupId != null) {
                appendParam(sb, "groupId", groupId);
            } else {
                appendParam(sb, "groupId", null);
            }

            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Couldn't build params.");
        }
        return null;

    }


    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.groupId);
    }

    protected AlertsUrl(Parcel in) {
        super(in);
        this.groupId = in.readString();
    }

    public static final Creator<AlertsUrl> CREATOR = new Creator<AlertsUrl>() {
        public AlertsUrl createFromParcel(Parcel source) {
            return new AlertsUrl(source);
        }

        public AlertsUrl[] newArray(int size) {
            return new AlertsUrl[size];
        }
    };
}
