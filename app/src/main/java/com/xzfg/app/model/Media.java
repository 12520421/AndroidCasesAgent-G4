package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


@Root
@SuppressWarnings("unused")

public class Media implements Parcelable {

    public static final Parcelable.Creator<Media> CREATOR = new Parcelable.Creator<Media>() {
        public Media createFromParcel(Parcel source) {
            return new Media(source);
        }

        public Media[] newArray(int size) {
            return new Media[size];
        }
    };
    @ElementList(inline = true, required = false)
    public List<Record> records = Collections.synchronizedList(new LinkedList<Media.Record>());
    @Element(required = false)
    public Error error;

    public Media() {
    }

    protected Media(Parcel in) {
        this.records = in.createTypedArrayList(Record.CREATOR);
        this.error = in.readParcelable(Error.class.getClassLoader());
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
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
        dest.writeTypedList(records);
        dest.writeParcelable(this.error, 0);
    }

    public static class Record implements android.os.Parcelable {

        public static final Creator<Record> CREATOR = new Creator<Record>() {
            public Record createFromParcel(Parcel source) {
                return new Record(source);
            }

            public Record[] newArray(int size) {
                return new Record[size];
            }
        };
        @Attribute(required = true)
        Integer id;
        @Attribute(required = false)
        Date created;
        @Attribute(required = false)
        String caseNumber;
        @Attribute(required = true)
        String type;
        @Attribute(required = false)
        String name;
        @Attribute(required = false, empty = "")
        String latitude;
        @Attribute(required = false)
        String longitude;
        @Attribute(required = false)
        String mediaUrl;
        @Attribute(required = false)
        String distance;
        @Attribute(required = false)
        Integer direction;
        @Text(data = true, required = false)
        String description;
        @Attribute(required = false)
        String address;

        public Record() {
        }

        protected Record(Parcel in) {
            this.id = (Integer) in.readValue(Integer.class.getClassLoader());
            long tmpCreated = in.readLong();
            this.created = tmpCreated == -1 ? null : new Date(tmpCreated);
            this.caseNumber = in.readString();
            this.type = in.readString();
            this.name = in.readString();
            this.latitude = in.readString();
            this.longitude = in.readString();
            this.mediaUrl = in.readString();
            this.distance = in.readString();
            this.direction = (Integer) in.readValue(Integer.class.getClassLoader());
            this.description = in.readString();
            this.address = in.readString();
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public String getCaseNumber() {
            return caseNumber;
        }

        public void setCaseNumber(String caseNumber) {
            this.caseNumber = caseNumber;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public String getMediaUrl() {
            return mediaUrl;
        }

        public void setMediaUrl(String mediaUrl) {
            this.mediaUrl = mediaUrl;
        }

        public String getDistance() {
            return distance;
        }

        public void setDistance(String distance) {
            this.distance = distance;
        }

        public Integer getDirection() {
            return direction;
        }

        public void setDirection(Integer direction) {
            this.direction = direction;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAddress() {
            return address;
        }

        public void setAdress(String address) {
            this.address = address;
        }

        @Override
        public String toString() {
            return "Record{" +
                    "id=" + id +
                    ", created=" + created +
                    ", caseNumber='" + caseNumber + '\'' +
                    ", type='" + type + '\'' +
                    ", name='" + name + '\'' +
                    ", latitude='" + latitude + '\'' +
                    ", longitude='" + longitude + '\'' +
                    ", address='" + address + '\'' +
                    ", mediaUrl='" + mediaUrl + '\'' +
                    ", distance='" + distance + '\'' +
                    ", direction=" + direction +
                    ", description='" + description + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Record record = (Record) o;

            return !(getId() != null ? !getId().equals(record.getId()) : record.getId() != null);

        }

        @Override
        public int hashCode() {
            return getId() != null ? getId().hashCode() : 0;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeValue(this.id);
            dest.writeLong(created != null ? created.getTime() : -1);
            dest.writeString(this.caseNumber);
            dest.writeString(this.type);
            dest.writeString(this.name);
            dest.writeString(this.latitude);
            dest.writeString(this.longitude);
            dest.writeString(this.mediaUrl);
            dest.writeString(this.distance);
            dest.writeValue(this.direction);
            dest.writeString(this.description);
            dest.writeString(this.address);
        }
    }
}
