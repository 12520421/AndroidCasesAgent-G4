package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.Comparator;
import java.util.Date;

@Root(strict = false)
public class Message implements Comparator<Message>, Comparable<Message>, Parcelable {
    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
    @Attribute(required = true)
    public Long messageId;
    @Attribute(required = false)
    public Date created;
    @Attribute(required = false, empty = "")
    public String toUser;
    @Attribute(required = false)
    public String username;
    @Attribute(required = false)
    public String languageCode;
    @Attribute(required = false)
    public String type;
    @Text(data = true, required = false)
    public String message;

    public Message() {
    }

    protected Message(Parcel in) {
        this.messageId = (Long) in.readValue(Long.class.getClassLoader());
        long tmpCreated = in.readLong();
        this.created = tmpCreated == -1 ? null : new Date(tmpCreated);
        this.toUser = in.readString();
        this.username = in.readString();
        this.languageCode = in.readString();
        this.type = in.readString();
        this.message = in.readString();
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageId=" + messageId +
                ", created=" + created +
                ", toUser='" + toUser + '\'' +
                ", username='" + username + '\'' +
                ", languageCode='" + languageCode + '\'' +
                ", type='" + type + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message1 = (Message) o;

        if (messageId != null ? !messageId.equals(message1.messageId) : message1.messageId != null)
            return false;
        if (username != null ? !username.equals(message1.username) : message1.username != null)
            return false;
        return !(message != null ? !message.equals(message1.message) : message1.message != null);

    }

    @Override
    public int hashCode() {
        int result = messageId != null ? messageId.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public int compareTo(Message another) {
        return (getCreated().compareTo(another.getCreated()));
    }

    @Override
    public int compare(Message lhs, Message rhs) {
        return (lhs.getCreated().compareTo(rhs.getCreated()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.messageId);
        dest.writeLong(created != null ? created.getTime() : -1);
        dest.writeString(this.toUser);
        dest.writeString(this.username);
        dest.writeString(this.languageCode);
        dest.writeString(this.type);
        dest.writeString(this.message);
    }
}
