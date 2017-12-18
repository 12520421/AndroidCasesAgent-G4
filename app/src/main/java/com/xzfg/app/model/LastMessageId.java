package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;


public class LastMessageId implements Parcelable {
    public static final Parcelable.Creator<LastMessageId> CREATOR = new Parcelable.Creator<LastMessageId>() {
        public LastMessageId createFromParcel(Parcel source) {
            return new LastMessageId(source);
        }

        public LastMessageId[] newArray(int size) {
            return new LastMessageId[size];
        }
    };
    @Attribute(required = false)
    public Long messageId;

    public LastMessageId() {
    }

    protected LastMessageId(Parcel in) {
        this.messageId = (Long) in.readValue(Long.class.getClassLoader());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LastMessageId that = (LastMessageId) o;

        return !(getMessageId() != null ? !getMessageId().equals(that.getMessageId()) : that.getMessageId() != null);

    }

    @Override
    public int hashCode() {
        return getMessageId() != null ? getMessageId().hashCode() : 0;
    }

    public Long getMessageId() {

        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.messageId);
    }
}
