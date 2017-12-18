package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Root(name = "PoiGroups", strict = false)
public class PoiGroups implements Parcelable {
    public static final Parcelable.Creator<PoiGroups> CREATOR = new Parcelable.Creator<PoiGroups>() {
        public PoiGroups createFromParcel(Parcel source) {
            return new PoiGroups(source);
        }

        public PoiGroups[] newArray(int size) {
            return new PoiGroups[size];
        }
    };
    @ElementList(inline = true, required = false)
    public List<PoiGroup> poiGroups = Collections.synchronizedList(new LinkedList<PoiGroup>());
    @Element(required = false)
    public Error error;

    public PoiGroups() {
    }

    protected PoiGroups(Parcel in) {
        this.poiGroups = in.createTypedArrayList(PoiGroup.CREATOR);
        this.error = in.readParcelable(Error.class.getClassLoader());
    }

    public List<PoiGroup> getPoiGroups() {
        return poiGroups;
    }

    public void setPoiGroups(List<PoiGroup> poiGroups) {
        this.poiGroups = poiGroups;
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
        dest.writeTypedList(poiGroups);
        dest.writeParcelable(this.error, 0);
    }
}
