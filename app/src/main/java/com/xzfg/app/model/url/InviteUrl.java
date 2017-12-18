package com.xzfg.app.model.url;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.xzfg.app.codec.Hex;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.security.Crypto;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 */
public class InviteUrl implements Parcelable {
    protected boolean encode = true;
    protected String ipAddress;
    protected Long trackingPort;
    protected String endPoint;
    protected String invite;
    protected Map<String, String> paramData;
    protected Location location;


    @SuppressWarnings("unused")
    public InviteUrl() {
    }

    @SuppressWarnings("unused")
    public InviteUrl(String ipAddress, Long trackingPort, String endPoint, String invite) {
        this.ipAddress = ipAddress;
        this.trackingPort = trackingPort;
        this.endPoint = endPoint;
        this.invite = invite;
    }

    @SuppressWarnings("unused")
    public InviteUrl(ScannedSettings settings, String endPoint, String invite) {
        this(settings.getIpAddress(), settings.getTrackingPort(), endPoint, invite);
    }

    public boolean isEncode() {
        return encode;
    }

    public void setEncode(boolean encode) {
        this.encode = encode;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Long getTrackingPort() {
        return trackingPort;
    }

    public void setTrackingPort(Long trackingPort) {
        this.trackingPort = trackingPort;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public String getInvite() {
        return invite;
    }

    public void setInvite(String invite) {
        this.invite = invite;
    }

    public Map<String, String> getParamData() {
        return paramData;
    }

    public void setParamData(Map<String, String> paramData) {
        this.paramData = paramData;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getPrefix() {
        if (ipAddress.contains("://")) {
            return ipAddress + ":" + trackingPort + endPoint + "?";
        } else {
            return "https://" + ipAddress + ":" + trackingPort + endPoint + "?";
        }
    }

    protected void appendParam(StringBuilder stringBuilder, String param, String value) throws UnsupportedEncodingException {
        if (param == null) {
            return;
        }
        if (stringBuilder.length() > 0) {
            stringBuilder.append("&");
        }
        if (encode) {
            stringBuilder.append(URLEncoder.encode(param, "UTF-8"));
            stringBuilder.append("=");
            if (value != null)
                stringBuilder.append(URLEncoder.encode(value, "UTF-8"));
        } else {
            stringBuilder.append(param);
            stringBuilder.append("=");
            if (value != null)
                stringBuilder.append(value);
        }
    }

    public String getParams() {
        StringBuilder sb = new StringBuilder(192);
        try {

            if (invite != null) {
                appendParam(sb, "inviteCode", invite);
            }

            if (paramData != null && !paramData.isEmpty()) {
                for (Map.Entry<String, String> entry : paramData.entrySet()) {
                    appendParam(sb, entry.getKey(), entry.getValue());
                }
            }
            if (location != null) {
                appendParam(sb, "latitude", String.valueOf(location.getLatitude()));
                appendParam(sb, "longitude", String.valueOf(location.getLongitude()));
            }

            return sb.toString();
        } catch (Exception e) {
            Timber.e(e, "An error occurred building params.");
        }
        return "";
    }

    public String getEncryptedParams(Crypto crypto) {
        String paramString = getParams();
        if (paramString != null && paramString.length() > 0) {
            if (crypto.isEncrypting()) {
                return Hex.encodeHexString(crypto.encrypt(paramString));
            }
            return paramString;
        }
        return "";
    }

    public String toString(Crypto crypto) {
        return getPrefix() + getEncryptedParams(crypto);
    }

    public String toString() {
        return getPrefix() + getParams();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(encode ? (byte) 1 : (byte) 0);
        dest.writeString(this.ipAddress);
        dest.writeValue(this.trackingPort);
        dest.writeString(this.endPoint);
        dest.writeString(this.invite);
        dest.writeMap(this.paramData);
        dest.writeParcelable(this.location, 0);
    }

    protected InviteUrl(Parcel in) {
        this.encode = in.readByte() != 0;
        this.ipAddress = in.readString();
        this.trackingPort = (Long) in.readValue(Long.class.getClassLoader());
        this.endPoint = in.readString();
        this.invite = in.readString();
        if (this.paramData == null) {
            this.paramData = new HashMap<>();
        }
        in.readMap(this.paramData, Map.class.getClassLoader());
        this.location = in.readParcelable(Location.class.getClassLoader());
    }


    public static final Creator<InviteUrl> CREATOR = new Creator<InviteUrl>() {
        public InviteUrl createFromParcel(Parcel source) {
            return new InviteUrl(source);
        }

        public InviteUrl[] newArray(int size) {
            return new InviteUrl[size];
        }
    };
}
