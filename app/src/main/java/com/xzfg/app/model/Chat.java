package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Chat implements Comparator<Chat>, Comparable<Chat>, Parcelable {

    public static final Parcelable.Creator<Chat> CREATOR = new Parcelable.Creator<Chat>() {
        public Chat createFromParcel(Parcel source) {
            return new Chat(source);
        }

        public Chat[] newArray(int size) {
            return new Chat[size];
        }
    };
    private volatile User user;
    private List<Message> messages = Collections.synchronizedList(new LinkedList<Message>());
    private Date initDate = new Date();

    @SuppressWarnings("unused")
    public Chat() {

    }

    public Chat(User user, Message... messages) {
        this.user = user;
        for (Message m : messages) {
            addMessage(m);
        }
    }

    protected Chat(Parcel in) {
        this.user = in.readParcelable(User.class.getClassLoader());
        this.messages = in.createTypedArrayList(Message.CREATOR);
        long tmpInitDate = in.readLong();
        this.initDate = tmpInitDate == -1 ? null : new Date(tmpInitDate);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Date getInitDate() {
        return initDate;
    }

    public void setInitDate(Date initDate) {
        this.initDate = initDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Chat) {
            return user.equals(((Chat) o).user);
        }
        return false;
    }

    public void addMessage(Message newMessage) {
        this.messages.add(newMessage);
    }

    public void addMessages(List<Message> newMessages) {
        this.messages.addAll(newMessages);
    }

    public String[] getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        Message m = messages.get(messages.size() - 1);
        return new String[]{m.getUsername(), m.getMessage()};
    }

    public Date getLastMessageDate() {
        if (getMessages() != null && getMessages().size() > 0) {
            return getMessages().get(getMessages().size() - 1).getCreated();
        }
        return initDate;
    }

    @Override
    public int compareTo(Chat another) {
        return -getLastMessageDate().compareTo(another.getLastMessageDate());
    }

    @Override
    public int compare(Chat lhs, Chat rhs) {
        return -lhs.getLastMessageDate().compareTo(rhs.getLastMessageDate());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.user, 0);
        dest.writeTypedList(messages);
        dest.writeLong(initDate != null ? initDate.getTime() : -1);
    }

    @Override
    public String toString() {
        return "Chat{" +
                "user=" + user +
                ", messages=" + messages +
                ", initDate=" + initDate +
                '}';
    }
}
