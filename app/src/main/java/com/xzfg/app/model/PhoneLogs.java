package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.concurrent.ConcurrentLinkedQueue;

@Root
public class PhoneLogs implements Parcelable {

    public static final Creator<PhoneLogs> CREATOR = new Creator<PhoneLogs>() {
        public PhoneLogs createFromParcel(Parcel source) {
            return new PhoneLogs(source);
        }
        public PhoneLogs[] newArray(int size) {
            return new PhoneLogs[size];
        }
    };
    @Attribute
    String serial;
    @Attribute
    String password;
    @ElementList(inline = true)
    ConcurrentLinkedQueue<String> phoneLogs = new ConcurrentLinkedQueue<>();

    public PhoneLogs() {
    }

    public PhoneLogs(String serial, String password) {
        this.serial = serial;
        this.password = password;
    }

    protected PhoneLogs(Parcel in) {
        this.serial = in.readString();
        this.password = in.readString();
      //noinspection unchecked
      this.phoneLogs = (ConcurrentLinkedQueue<String>) in.readSerializable();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PhoneLogs logs1 = (PhoneLogs) o;

        if (getSerial() != null ? !getSerial().equals(logs1.getSerial()) : logs1.getSerial() != null)
            return false;
        if (getPassword() != null ? !getPassword().equals(logs1.getPassword()) : logs1.getPassword() != null)
            return false;
        return !(getPhoneLogs() != null ? !getPhoneLogs().equals(logs1.getPhoneLogs()) : logs1.getPhoneLogs() != null);

    }

    @Override
    public int hashCode() {
        int result = getSerial() != null ? getSerial().hashCode() : 0;
        result = 31 * result + (getPassword() != null ? getPassword().hashCode() : 0);
        result = 31 * result + (getPhoneLogs() != null ? getPhoneLogs().hashCode() : 0);
        return result;
    }

    public String getSerial() {

        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ConcurrentLinkedQueue<String> getPhoneLogs() {
        return phoneLogs;
    }

    public void setPhoneLogs(ConcurrentLinkedQueue<String> phoneLogs) {
        this.phoneLogs = phoneLogs;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.serial);
        dest.writeString(this.password);
        dest.writeSerializable(this.phoneLogs);
    }
}
