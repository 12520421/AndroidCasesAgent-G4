package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Root(strict = false, name = "PoiCategories")
public class PoiCategories implements Parcelable {
    public static final Parcelable.Creator<PoiCategories> CREATOR = new Parcelable.Creator<PoiCategories>() {
        public PoiCategories createFromParcel(Parcel source) {
            return new PoiCategories(source);
        }

        public PoiCategories[] newArray(int size) {
            return new PoiCategories[size];
        }
    };
    @ElementList(inline = true, required = false)
    public List<PoiCategory> poiCategories = Collections.synchronizedList(new LinkedList<PoiCategory>());
    @Element(required = false)
    public Error error;

    public PoiCategories() {
    }

    protected PoiCategories(Parcel in) {
        this.poiCategories = in.createTypedArrayList(PoiCategory.CREATOR);
        this.error = in.readParcelable(Error.class.getClassLoader());
    }

    public List<PoiCategory> getPoiCategories() {
        return poiCategories;
    }

    public void setPoiCategories(List<PoiCategory> poiCategories) {
        this.poiCategories = poiCategories;
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
        dest.writeTypedList(poiCategories);
        dest.writeParcelable(this.error, 0);
    }
}
