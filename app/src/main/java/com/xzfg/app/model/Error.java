package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

@Root(strict = false)
public class Error implements Parcelable {
    public static final Parcelable.Creator<Error> CREATOR = new Parcelable.Creator<Error>() {
        public Error createFromParcel(Parcel source) {
            return new Error(source);
        }

        public Error[] newArray(int size) {
            return new Error[size];
        }
    };
    @Text(data = true, required = false)
    public String message;

    public Error() {
    }

    protected Error(Parcel in) {
        this.message = in.readString();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Error error = (Error) o;

        return !(getMessage() != null ? !getMessage().equals(error.getMessage()) : error.getMessage() != null);

    }

    @Override
    public int hashCode() {
        return getMessage() != null ? getMessage().hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.valueOf(getMessage());
    }
}
