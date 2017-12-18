package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(name = "CHECKIN", strict = false)
public class Checkin implements Parcelable {
    public static final Creator<Checkin> CREATOR = new Creator<Checkin>() {
        public Checkin createFromParcel(Parcel source) {
            return new Checkin(source);
        }

        public Checkin[] newArray(int size) {
            return new Checkin[size];
        }
    };

    @Attribute
    String message;
    @Attribute
    String contacts;
    @Attribute
    String date;
    @Attribute
    String latitude;
    @Attribute
    String longitude;

    public Checkin() {
    }

    protected Checkin(Checkin that) {
        this.message = that.message;
        this.contacts = that.contacts;
        this.date = that.date;
        this.latitude = that.latitude;
        this.longitude = that.longitude;
    }

    protected Checkin(Parcel in) {
        this.message = in.readString();
        this.contacts = in.readString();
        this.date = in.readString();
        this.latitude = in.readString();
        this.longitude = in.readString();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContacts() {
        return contacts;
    }

    public void setContacts(String contacts) {
        this.contacts = contacts;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "Checkin{" +
                "message=" + message +
                ", contacts='" + contacts + '\'' +
                ", date='" + date + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int hashCode() {
        int result = getMessage() != null ? getMessage().hashCode() : 0;
        result = 31 * result + (getContacts() != null ? getContacts().hashCode() : 0);
        result = 31 * result + (getDate() != null ? getDate().hashCode() : 0);
        result = 31 * result + (getLatitude() != null ? getLatitude().hashCode() : 0);
        result = 31 * result + (getLongitude() != null ? getLongitude().hashCode() : 0);

        return result;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.message);
        dest.writeString(this.contacts);
        dest.writeString(this.date);
        dest.writeString(this.latitude);
        dest.writeString(this.longitude);
    }
}
