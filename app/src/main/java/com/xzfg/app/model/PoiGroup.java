package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(strict = false)
public class PoiGroup implements Parcelable {
    public static final Parcelable.Creator<PoiGroup> CREATOR = new Parcelable.Creator<PoiGroup>() {
        public PoiGroup createFromParcel(Parcel source) {
            return new PoiGroup(source);
        }

        public PoiGroup[] newArray(int size) {
            return new PoiGroup[size];
        }
    };
    @Attribute
    String id;
    @Attribute
    String name;
    @Attribute
    String records;

    public PoiGroup() {
    }

    protected PoiGroup(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.records = in.readString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PoiGroup poiGroup = (PoiGroup) o;

        return !(getId() != null ? !getId().equals(poiGroup.getId()) : poiGroup.getId() != null);

    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
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
    public String toString() {
        return "PoiGroup{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", records='" + records + '\'' +
                '}';
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
