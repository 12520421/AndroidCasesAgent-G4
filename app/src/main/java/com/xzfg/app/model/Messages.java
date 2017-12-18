package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Root(strict = false)
public class Messages implements Parcelable {

    public static final Parcelable.Creator<Messages> CREATOR = new Parcelable.Creator<Messages>() {
        public Messages createFromParcel(Parcel source) {
            return new Messages(source);
        }

        public Messages[] newArray(int size) {
            return new Messages[size];
        }
    };
    @ElementList(inline = true, required = false)
    public List<Message> messages = Collections.synchronizedList(new LinkedList<Message>());
    @Element(required = false)
    public LastMessageId lastMessageId;

    @SuppressWarnings("unused")
    public Messages() {
    }

    protected Messages(Parcel in) {
        this.messages = in.createTypedArrayList(Message.CREATOR);
        this.lastMessageId = in.readParcelable(LastMessageId.class.getClassLoader());
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public LastMessageId getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(LastMessageId lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(messages);
        dest.writeParcelable(this.lastMessageId, 0);
    }
}
