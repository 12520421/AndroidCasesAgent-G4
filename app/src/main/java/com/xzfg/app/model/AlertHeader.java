package com.xzfg.app.model;

import android.os.Parcel;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

@Root(name = "AlertHeader")
@SuppressWarnings("unused")
public class AlertHeader {

    @ElementList(inline = true, required = false)
    public List<AlertHeader.Record> records = Collections.synchronizedList(new LinkedList<Record>());

    @Element(required = false)
    public Error error;

    public AlertHeader() {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlertHeader that = (AlertHeader) o;

        if (getRecords() != null ? !getRecords().equals(that.getRecords()) : that.getRecords() != null)
            return false;
        return !(getError() != null ? !getError().equals(that.getError()) : that.getError() != null);

    }

    @Override
    public int hashCode() {
        int result = getRecords() != null ? getRecords().hashCode() : 0;
        result = 31 * result + (getError() != null ? getError().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AlertHeader{" +
                "records=" + records +
                ", error=" + error +
                '}';
    }

    public static class Record implements android.os.Parcelable, Comparator<Record>, Comparable<Record> {
        public static final Creator<Record> CREATOR = new Creator<Record>() {
            public Record createFromParcel(Parcel source) {
                return new Record(source);
            }

            public Record[] newArray(int size) {
                return new Record[size];
            }
        };
        @Attribute(required = false)
        public String groupId;
        @Attribute(required = false)
        public String groupName;
        @Attribute(required = false)
        public String count;

        public Record() {
        }

        protected Record(Parcel in) {
            this.groupId = in.readString();
            this.groupName = in.readString();
            this.count = in.readString();
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public String getCount() {
            return count;
        }

        public void setCount(String count) {
            this.count = count;
        }

        @Override
        public int compareTo(Record another) {
            return -(getGroupName().compareTo(another.getGroupName()));
        }

        @Override
        public int compare(Record lhs, Record rhs) {
            return -(lhs.getGroupName().compareTo(rhs.getGroupName()));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.groupId);
            dest.writeString(this.groupName);
            dest.writeString(this.count);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Record record = (Record) o;

            return !(getGroupId() != null ? !getGroupId().equals(record.getGroupId()) : record.getGroupId() != null);

        }

        @Override
        public int hashCode() {
            return getGroupId() != null ? getGroupId().hashCode() : 0;
        }

        @Override
        public String toString() {
            return "Record{" +
                    "groupId='" + groupId + '\'' +
                    ", groupName='" + groupName + '\'' +
                    ", count='" + count + '\'' +
                    '}';
        }
    }
}
