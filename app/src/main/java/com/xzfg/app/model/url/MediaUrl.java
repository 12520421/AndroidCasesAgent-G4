package com.xzfg.app.model.url;

import android.os.Parcel;

import com.xzfg.app.model.ScannedSettings;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;


public class MediaUrl extends SessionUrl {
    Integer distance;
    String caseNumber;

    public MediaUrl() {
        super();
    }

    public MediaUrl(ScannedSettings settings, String endpoint, String sessionId) {
        super(settings, endpoint, sessionId);
    }


    @Override
    public String getParams() {
        try {
            StringBuilder sb = new StringBuilder(192);
            sb.append(super.getParams());

            appendParam(sb, "caseNumber", caseNumber);
            if (distance != null) {
                appendParam(sb, "distance", String.valueOf(distance));
            } else {
                appendParam(sb, "distance", null);
            }

            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "Couldn't build params.");
        }
        return null;

    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected MediaUrl(Parcel in) {
        super(in);
        this.distance = (Integer) in.readValue(Integer.class.getClassLoader());
        this.caseNumber = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeValue(this.distance);
        dest.writeString(this.caseNumber);
    }

    public static final Creator<MediaUrl> CREATOR = new Creator<MediaUrl>() {
        public MediaUrl createFromParcel(Parcel source) {
            return new MediaUrl(source);
        }

        public MediaUrl[] newArray(int size) {
            return new MediaUrl[size];
        }
    };

}
