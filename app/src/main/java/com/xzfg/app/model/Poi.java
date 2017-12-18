package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(name = "POI", strict = false)
public class Poi implements Parcelable {
    public static final Parcelable.Creator<Poi> CREATOR = new Parcelable.Creator<Poi>() {
        public Poi createFromParcel(Parcel source) {
            return new Poi(source);
        }

        public Poi[] newArray(int size) {
            return new Poi[size];
        }
    };
    @Attribute
    Long id;
    @Attribute(required = false)
    String categoryId;
    @Attribute(required = false)
    String categoryName;
    @Attribute
    String name;
    @Attribute(required = false)
    String address;
    @Attribute
    String latitude;
    @Attribute
    String longitude;
    @Attribute(required = false)
    String distance;
    @Attribute(required = false)
    String direction;
    @Attribute(required = false)
    String groupName;

    public Poi() {
    }

    protected Poi(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.categoryId = in.readString();
        this.categoryName = in.readString();
        this.name = in.readString();
        this.address = in.readString();
        this.latitude = in.readString();
        this.longitude = in.readString();
        this.distance = in.readString();
        this.direction = in.readString();
        this.groupName = in.readString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return "Poi{" +
                "id=" + id +
                ", categoryId='" + categoryId + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", distance='" + distance + '\'' +
                ", direction='" + direction + '\'' +
                ", groupName='" + groupName + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.id);
        dest.writeString(this.categoryId);
        dest.writeString(this.categoryName);
        dest.writeString(this.name);
        dest.writeString(this.address);
        dest.writeString(this.latitude);
        dest.writeString(this.longitude);
        dest.writeString(this.distance);
        dest.writeString(this.direction);
        dest.writeString(this.groupName);
    }
}
