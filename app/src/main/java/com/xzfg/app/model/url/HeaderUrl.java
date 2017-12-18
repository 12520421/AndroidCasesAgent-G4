package com.xzfg.app.model.url;

import android.os.Parcel;

import com.xzfg.app.model.ScannedSettings;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;


public class HeaderUrl extends SessionUrl {
    Integer distance;

    public HeaderUrl() {
        super();
    }

    public HeaderUrl(ScannedSettings settings, String endpoint, String sessionId) {
        super(settings, endpoint, sessionId);
    }


    @Override
    public String getParams() {
        try {
            StringBuilder sb = new StringBuilder(192);
            sb.append(super.getParams());
            if (distance != null) {
                appendParam(sb, "distance", String.valueOf(distance));
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

    @Override
    public int describeContents() {
        return 0;
    }

    protected HeaderUrl(Parcel in) {
        super(in);
        this.distance = (Integer) in.readValue(Integer.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeValue(this.distance);
    }

    public static final Creator<HeaderUrl> CREATOR = new Creator<HeaderUrl>() {
        public HeaderUrl createFromParcel(Parcel source) {
            return new HeaderUrl(source);
        }

        public HeaderUrl[] newArray(int size) {
            return new HeaderUrl[size];
        }
    };

}
