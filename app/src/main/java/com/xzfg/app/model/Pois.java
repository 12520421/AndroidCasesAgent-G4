package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Root(strict = false, name = "POIS")
public class Pois implements Parcelable {
    public static final Parcelable.Creator<Pois> CREATOR = new Parcelable.Creator<Pois>() {
        public Pois createFromParcel(Parcel source) {
            return new Pois(source);
        }

        public Pois[] newArray(int size) {
            return new Pois[size];
        }
    };
    @ElementList(inline = true, required = false)
    public List<Poi> poi = Collections.synchronizedList(new LinkedList<Poi>());
    @Element(required = false)
    public Error error;

    public Pois() {
    }

    protected Pois(Parcel in) {
        this.poi = in.createTypedArrayList(Poi.CREATOR);
        this.error = in.readParcelable(Error.class.getClassLoader());
    }

    public List<Poi> getPoi() {
        return poi;
    }

    public void setPoi(List<Poi> poi) {
        this.poi = poi;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(poi);
        dest.writeParcelable(this.error, 0);
    }
}
