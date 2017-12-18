package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

public class PreviewSize implements Parcelable {
    public static final Parcelable.Creator<PreviewSize> CREATOR = new Parcelable.Creator<PreviewSize>() {
        public PreviewSize createFromParcel(Parcel source) {
            return new PreviewSize(source);
        }

        public PreviewSize[] newArray(int size) {
            return new PreviewSize[size];
        }
    };
    int width;
    int height;
    int y;


    public PreviewSize() {
    }

    protected PreviewSize(Parcel in) {
        this.width = in.readInt();
        this.height = in.readInt();
        this.y = in.readInt();
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PreviewSize that = (PreviewSize) o;

        if (getWidth() != that.getWidth()) return false;
        if (getHeight() != that.getHeight()) return false;
        return getY() == that.getY();

    }

    @Override
    public int hashCode() {
        int result = getWidth();
        result = 31 * result + getHeight();
        result = 31 * result + getY();
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeInt(this.y);
    }
}
