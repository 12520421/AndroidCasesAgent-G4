package com.xzfg.app.model;

import android.os.Parcel;

import java.util.UUID;


public class SendableMessage extends Message {
    public static final Creator<SendableMessage> CREATOR = new Creator<SendableMessage>() {
        public SendableMessage createFromParcel(Parcel source) {
            return new SendableMessage(source);
        }

        public SendableMessage[] newArray(int size) {
            return new SendableMessage[size];
        }
    };
    public String identifier;
    public String toUserId;
    public String status;

    public SendableMessage() {
        super();
        identifier = UUID.randomUUID().toString();
    }

    public SendableMessage(String toUserId, String message) {
        super();
        this.toUserId = toUserId;
        this.message = message;
        identifier = UUID.randomUUID().toString();
    }

    public SendableMessage(User user, String message) {
        super();
        toUserId = user.getUserId();
        this.message = message;
        identifier = UUID.randomUUID().toString();
    }

    protected SendableMessage(Parcel in) {
        super(in);
        this.identifier = in.readString();
        this.toUserId = in.readString();
        this.status = in.readString();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SendableMessage that = (SendableMessage) o;

        return !(identifier != null ? !identifier.equals(that.identifier) : that.identifier != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + ", SendableMessage{" +
                "identifier='" + identifier + '\'' +
                ", toUserId='" + toUserId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.identifier);
        dest.writeString(this.toUserId);
        dest.writeString(this.status);
    }
}
