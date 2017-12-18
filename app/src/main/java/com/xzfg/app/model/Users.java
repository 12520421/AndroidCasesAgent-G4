package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.ElementList;

import java.util.LinkedList;


public class Users implements Parcelable {

    public static final Parcelable.Creator<Users> CREATOR = new Parcelable.Creator<Users>() {
        public Users createFromParcel(Parcel source) {
            return new Users(source);
        }

        public Users[] newArray(int size) {
            return new Users[size];
        }
    };
    @ElementList(inline = true, required = false)
    public LinkedList<User> users;

    public Users() {
    }

    protected Users(Parcel in) {
      //noinspection unchecked
      this.users = (LinkedList<User>) in.readSerializable();
    }

    public LinkedList<User> getUsers() {
        return users;
    }

    public void setUsers(LinkedList<User> users) {
        this.users = users;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Users users1 = (Users) o;

        return !(getUsers() != null ? !getUsers().equals(users1.getUsers()) : users1.getUsers() != null);

    }

    @Override
    public int hashCode() {
        return getUsers() != null ? getUsers().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Users{" +
                "users=" + users +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(users);
    }
}
