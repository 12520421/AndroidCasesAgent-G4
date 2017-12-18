package com.xzfg.app.model.url;

import android.os.Parcel;

import com.xzfg.app.model.ScannedSettings;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;


public class PoiUrl extends SessionUrl {
    Integer distance;
    String groupId;

    public PoiUrl() {
        super();
    }

    public PoiUrl(ScannedSettings settings, String endpoint, String sessionId) {
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

    protected PoiUrl(Parcel in) {
        super(in);
        this.distance = (Integer) in.readValue(Integer.class.getClassLoader());
        this.groupId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeValue(this.distance);
        dest.writeString(this.groupId);
    }

    public static final Creator<PoiUrl> CREATOR = new Creator<PoiUrl>() {
        public PoiUrl createFromParcel(Parcel source) {
            return new PoiUrl(source);
        }

        public PoiUrl[] newArray(int size) {
            return new PoiUrl[size];
        }
    };

}
