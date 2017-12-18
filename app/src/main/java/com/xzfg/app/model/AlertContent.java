package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


@Root(name = "AlertContent")
@SuppressWarnings("unused")
public class AlertContent implements Parcelable {
    public static final Creator<AlertContent> CREATOR = new Creator<AlertContent>() {
        public AlertContent createFromParcel(Parcel source) {
            return new AlertContent(source);
        }

        public AlertContent[] newArray(int size) {
            return new AlertContent[size];
        }
    };
    @ElementList(inline = true, required = false)
    public List<AlertContent.Record> records = Collections.synchronizedList(new LinkedList<Record>());
    @Element(required = false)
    public Error error;

    public AlertContent() {
    }

    protected AlertContent(Parcel in) {
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

    public static class Record implements Parcelable {
        public static final Creator<Record> CREATOR = new Creator<Record>() {
            public Record createFromParcel(Parcel source) {
                return new Record(source);
            }

            public Record[] newArray(int size) {
                return new Record[size];
            }
        };
        @Attribute
        Long alertId;
        @Attribute
        String targetName;
        @Attribute
        String action;
        @Attribute
        String latitude;
        @Attribute
        String longitude;
        @Attribute(required = false)
        String distance;
        @Attribute(required = false)
        String direction;
        @Attribute(required = false)
        Date created;
        @Attribute(required = false)
        String address;
        @Attribute(required = false)
        int userAccepted;
        @Attribute(required = false)
        int otherAccepted;

        public Record() {
        }

        protected Record(Parcel in) {
            this.alertId = (Long) in.readValue(Long.class.getClassLoader());
            this.targetName = in.readString();
            this.action = in.readString();
            this.latitude = in.readString();
            this.longitude = in.readString();
            this.distance = in.readString();
            this.direction = in.readString();
            long tmpCreated = in.readLong();
            this.created = tmpCreated == -1 ? null : new Date(tmpCreated);
            this.address = in.readString();
            this.userAccepted = in.readInt();
            this.otherAccepted = in.readInt();
        }

        public Long getAlertId() {
            return alertId;
        }

        public void setAlertId(Long alertId) {
            this.alertId = alertId;
        }

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
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

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getUserAccepted() {
            return userAccepted;
        }

        public void setUserAccepted(int userAccepted) {
            this.userAccepted = userAccepted;
        }

        public int getOtherAccepted() {
            return otherAccepted;
        }

        public void setOtherAccepted(int otherAccepted) {
            this.otherAccepted = otherAccepted;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Record record = (Record) o;

            return !(getAlertId() != null ? !getAlertId().equals(record.getAlertId()) : record.getAlertId() != null);

        }

        @Override
        public int hashCode() {
            return getAlertId() != null ? getAlertId().hashCode() : 0;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeValue(this.alertId);
            dest.writeString(this.targetName);
            dest.writeString(this.action);
            dest.writeString(this.latitude);
            dest.writeString(this.longitude);
            dest.writeString(this.distance);
            dest.writeString(this.direction);
            dest.writeLong(created != null ? created.getTime() : -1);
            dest.writeString(this.address);
            dest.writeInt(this.userAccepted);
            dest.writeInt(this.otherAccepted);
        }
    }
}
