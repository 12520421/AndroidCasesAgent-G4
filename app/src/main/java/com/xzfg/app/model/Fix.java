package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.Contract;
import org.simpleframework.xml.Element;


public class Fix implements Parcelable {

    @Element
    Location location;

    public Fix() {
    }

    public Fix(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Fix fix = (Fix) o;

        return getLocation() != null ? getLocation().equals(fix.getLocation())
            : fix.getLocation() == null;

    }

    @Override
    public int hashCode() {
        return getLocation() != null ? getLocation().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Fix{" +
            "location=" + location +
            '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.location, flags);
    }

    protected Fix(Parcel in) {
        this.location = in.readParcelable(Location.class.getClassLoader());
    }

    public static final Creator<Fix> CREATOR = new Creator<Fix>() {
        @org.jetbrains.annotations.Contract("_ -> !null")
        @Override
        public Fix createFromParcel(Parcel source) {
            return new Fix(source);
        }

        @Contract(value = "_ -> !null", pure = true)
        @Override
        public Fix[] newArray(int size) {
            return new Fix[size];
        }
    };
}
