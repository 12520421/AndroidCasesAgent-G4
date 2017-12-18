package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Location implements Parcelable {

    @Attribute
    String dateUTC;
    @Attribute
    Double latitude;
    @Attribute
    Double longitude;
    @Attribute(required = false)
    Float accuracy;
    @Attribute(required = false)
    Double altitude;
    @Attribute(required = false, empty = "")
    Float speed;
    @Attribute
    String provider;
    @Attribute(required = false)
    Integer battery;
    @Attribute(required = false, empty = "")
    String metaData;
    @Attribute(required = false)
    Float bearing;

    public Location() {
    }

    public Location(android.location.Location inLocation) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        setDateUTC(f.format(new Date()));
        setLatitude(inLocation.getLatitude());
        setLongitude(inLocation.getLongitude());
        setAccuracy(inLocation.getAccuracy());
        setProvider(inLocation.getProvider());
        if (inLocation.hasAltitude()) {
            setAltitude(inLocation.getAltitude());
        }
        if (inLocation.hasBearing()) {
            setBearing(inLocation.getBearing());
        }
        if (inLocation.hasSpeed()) {
            setSpeed(inLocation.getSpeed());
        }
    }

    public String getDateUTC() {

        return dateUTC;
    }

    public void setDateUTC(String dateUTC) {

        this.dateUTC = dateUTC;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Float getSpeed() {
        return speed;
    }

    public void setSpeed(Float speed) {
        this.speed = speed;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Integer getBattery() {
        return battery;
    }

    public void setBattery(Integer battery) {
        this.battery = battery;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public Float getBearing() {
        return bearing;
    }

    public void setBearing(Float bearing) {
        this.bearing = bearing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Location location = (Location) o;

        if (getDateUTC() != null ? !getDateUTC().equals(location.getDateUTC())
            : location.getDateUTC() != null) {
            return false;
        }
        if (getLatitude() != null ? !getLatitude().equals(location.getLatitude())
            : location.getLatitude() != null) {
            return false;
        }
        if (getLongitude() != null ? !getLongitude().equals(location.getLongitude())
            : location.getLongitude() != null) {
            return false;
        }
        if (getAccuracy() != null ? !getAccuracy().equals(location.getAccuracy())
            : location.getAccuracy() != null) {
            return false;
        }
        if (getAltitude() != null ? !getAltitude().equals(location.getAltitude())
            : location.getAltitude() != null) {
            return false;
        }
        if (getSpeed() != null ? !getSpeed().equals(location.getSpeed())
            : location.getSpeed() != null) {
            return false;
        }
        if (getProvider() != null ? !getProvider().equals(location.getProvider())
            : location.getProvider() != null) {
            return false;
        }
        if (getBattery() != null ? !getBattery().equals(location.getBattery())
            : location.getBattery() != null) {
            return false;
        }
        if (getMetaData() != null ? !getMetaData().equals(location.getMetaData())
            : location.getMetaData() != null) {
            return false;
        }
        return getBearing() != null ? getBearing().equals(location.getBearing())
            : location.getBearing() == null;

    }

    @Override
    public int hashCode() {
        int result = getDateUTC() != null ? getDateUTC().hashCode() : 0;
        result = 31 * result + (getLatitude() != null ? getLatitude().hashCode() : 0);
        result = 31 * result + (getLongitude() != null ? getLongitude().hashCode() : 0);
        result = 31 * result + (getAccuracy() != null ? getAccuracy().hashCode() : 0);
        result = 31 * result + (getAltitude() != null ? getAltitude().hashCode() : 0);
        result = 31 * result + (getSpeed() != null ? getSpeed().hashCode() : 0);
        result = 31 * result + (getProvider() != null ? getProvider().hashCode() : 0);
        result = 31 * result + (getBattery() != null ? getBattery().hashCode() : 0);
        result = 31 * result + (getMetaData() != null ? getMetaData().hashCode() : 0);
        result = 31 * result + (getBearing() != null ? getBearing().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Location{" +
            "dateUTC='" + dateUTC + '\'' +
            ", latitude=" + latitude +
            ", longitude=" + longitude +
            ", accuracy=" + accuracy +
            ", altitude=" + altitude +
            ", speed=" + speed +
            ", provider='" + provider + '\'' +
            ", battery=" + battery +
            ", metaData='" + metaData + '\'' +
            ", bearing=" + bearing +
            '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.dateUTC);
        dest.writeValue(this.latitude);
        dest.writeValue(this.longitude);
        dest.writeValue(this.accuracy);
        dest.writeValue(this.altitude);
        dest.writeValue(this.speed);
        dest.writeString(this.provider);
        dest.writeValue(this.battery);
        dest.writeString(this.metaData);
        dest.writeValue(this.bearing);
    }

    protected Location(Parcel in) {
        this.dateUTC = in.readString();
        this.latitude = (Double) in.readValue(Double.class.getClassLoader());
        this.longitude = (Double) in.readValue(Double.class.getClassLoader());
        this.accuracy = (Float) in.readValue(Float.class.getClassLoader());
        this.altitude = (Double) in.readValue(Double.class.getClassLoader());
        this.speed = (Float) in.readValue(Float.class.getClassLoader());
        this.provider = in.readString();
        this.battery = (Integer) in.readValue(Integer.class.getClassLoader());
        this.metaData = in.readString();
        this.bearing = (Float) in.readValue(Float.class.getClassLoader());
    }

    public static final Creator<Location> CREATOR = new Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel source) {
            return new Location(source);
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };
}
