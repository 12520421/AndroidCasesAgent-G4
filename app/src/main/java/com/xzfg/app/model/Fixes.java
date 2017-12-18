package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.concurrent.ConcurrentLinkedQueue;

@Root
public class Fixes implements Parcelable {

    public static final Parcelable.Creator<Fixes> CREATOR = new Parcelable.Creator<Fixes>() {
        public Fixes createFromParcel(Parcel source) {
            return new Fixes(source);
        }

        public Fixes[] newArray(int size) {
            return new Fixes[size];
        }
    };
    @Attribute
    String serial;
    @Attribute
    String password;
    @ElementList(inline = true)
    ConcurrentLinkedQueue<Fix> fixes = new ConcurrentLinkedQueue<>();

    public Fixes() {
    }

    public Fixes(String serial, String password) {
        this.serial = serial;
        this.password = password;
    }

    protected Fixes(Parcel in) {
        this.serial = in.readString();
        this.password = in.readString();
        //noinspection unchecked
        this.fixes = (ConcurrentLinkedQueue<Fix>) in.readSerializable();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fixes fixes1 = (Fixes) o;

        if (getSerial() != null ? !getSerial().equals(fixes1.getSerial()) : fixes1.getSerial() != null)
            return false;
        if (getPassword() != null ? !getPassword().equals(fixes1.getPassword()) : fixes1.getPassword() != null)
            return false;
        return !(getFixes() != null ? !getFixes().equals(fixes1.getFixes()) : fixes1.getFixes() != null);

    }

    @Override
    public int hashCode() {
        int result = getSerial() != null ? getSerial().hashCode() : 0;
        result = 31 * result + (getPassword() != null ? getPassword().hashCode() : 0);
        result = 31 * result + (getFixes() != null ? getFixes().hashCode() : 0);
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

    public ConcurrentLinkedQueue<Fix> getFixes() {
        return fixes;
    }

    public void setFixes(ConcurrentLinkedQueue<Fix> fixes) {
        this.fixes = fixes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.serial);
        dest.writeString(this.password);
        dest.writeSerializable(this.fixes);
    }
}
