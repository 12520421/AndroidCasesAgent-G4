package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;


@Root(strict = false)
public class PoiCategory implements Parcelable {
    public static final Parcelable.Creator<PoiCategory> CREATOR = new Parcelable.Creator<PoiCategory>() {
        public PoiCategory createFromParcel(Parcel source) {
            return new PoiCategory(source);
        }

        public PoiCategory[] newArray(int size) {
            return new PoiCategory[size];
        }
    };
    @Attribute(required = false)
    public String id;
    @Attribute(required = false)
    public String name;
    @Attribute(required = false)
    public String records;

    public PoiCategory() {
    }

    protected PoiCategory(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.records = in.readString();
    }

    @Override
    public String toString() {
        return "PoiCategory{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", records='" + records + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRecords() {
        return records;
    }

    public void setRecords(String records) {
        this.records = records;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeString(this.records);
    }
}
