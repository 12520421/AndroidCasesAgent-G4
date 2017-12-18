package com.xzfg.app.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Date;

public class UploadPackage implements Parcelable {

    public static final Parcelable.Creator<UploadPackage> CREATOR = new Parcelable.Creator<UploadPackage>() {
        public UploadPackage createFromParcel(Parcel source) {
            return new UploadPackage(source);
        }

        public UploadPackage[] newArray(int size) {
            return new UploadPackage[size];
        }
    };
    private String type;
    private String format;
    private Integer orientation;
    private float[] orientationMatrix;
    private Date date;
    private Double latitude;
    private Double longitude;
    private Float accuracy;
    private Long length;
    private Uri uri;
    private String caseNumber;
    private String caseDescription;
    private String batch;
    // Negative value means last segment
    private Integer segment;
    private Boolean isAvatar = false;
    private Checkin checkin = null;

    @SuppressWarnings("unused")
    public UploadPackage() {
    }

    protected UploadPackage(Parcel in) {
        this.type = in.readString();
        this.format = in.readString();
        this.orientation = (Integer) in.readValue(Integer.class.getClassLoader());
        this.orientationMatrix = in.createFloatArray();
        long tmpDate = in.readLong();
        this.date = tmpDate == -1 ? null : new Date(tmpDate);
        this.latitude = (Double) in.readValue(Double.class.getClassLoader());
        this.longitude = (Double) in.readValue(Double.class.getClassLoader());
        this.accuracy = (Float) in.readValue(Float.class.getClassLoader());
        this.length = (Long) in.readValue(Long.class.getClassLoader());
        this.uri = in.readParcelable(Uri.class.getClassLoader());
        this.caseNumber = in.readString();
        this.caseDescription = in.readString();
        this.batch = in.readString();
        this.segment = in.readInt();
        this.isAvatar = in.readInt() != 0;
        // Read checkin, check for null
        int validCheckin = in.readInt();
        if (validCheckin > 0)
            this.checkin = in.readParcelable(Checkin.class.getClassLoader());
        else
            this.checkin = null;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getOrientation() {
        return orientation;
    }

    public void setOrientation(Integer orientation) {
        this.orientation = orientation;
    }

    public float[] getOrientationMatrix() {
        return orientationMatrix;
    }

    public void setOrientationMatrix(float[] orientationMatrix) {
        this.orientationMatrix = orientationMatrix;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getCaseDescription() {
        return caseDescription;
    }

    public void setCaseDescription(String caseDescription) {
        this.caseDescription = caseDescription;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public Integer getSegment() {
        return segment;
    }

    public void setSegment(Integer segment) {
        this.segment = segment;
    }

    public Boolean getIsAvatar() {
        return isAvatar;
    }

    public void setIsAvatar(Boolean isAvatar) {
        this.isAvatar = isAvatar;
    }

    public Checkin getCheckin() {
        return checkin;
    }

    public void setCheckin(Checkin checkin) {
        this.checkin = checkin;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UploadPackage that = (UploadPackage) o;

        if (getType() != null ? !getType().equals(that.getType()) : that.getType() != null)
            return false;
        if (getFormat() != null ? !getFormat().equals(that.getFormat()) : that.getFormat() != null)
            return false;
        if (getOrientation() != null ? !getOrientation().equals(that.getOrientation()) : that.getOrientation() != null)
            return false;
        if (!Arrays.equals(getOrientationMatrix(), that.getOrientationMatrix())) return false;
        if (getDate() != null ? !getDate().equals(that.getDate()) : that.getDate() != null)
            return false;
        if (getLatitude() != null ? !getLatitude().equals(that.getLatitude()) : that.getLatitude() != null)
            return false;
        if (getLongitude() != null ? !getLongitude().equals(that.getLongitude()) : that.getLongitude() != null)
            return false;
        if (getAccuracy() != null ? !getAccuracy().equals(that.getAccuracy()) : that.getAccuracy() != null)
            return false;
        if (getLength() != null ? !getLength().equals(that.getLength()) : that.getLength() != null)
            return false;
        if (getUri() != null ? !getUri().equals(that.getUri()) : that.getUri() != null)
            return false;
        if (getCaseNumber() != null ? !getCaseNumber().equals(that.getCaseNumber()) : that.getCaseNumber() != null)
            return false;
        if (getBatch() != null ? !getBatch().equals(that.getBatch()) : that.getBatch() != null)
            return false;
        if (getSegment() != null ? !getSegment().equals(that.getSegment()) : that.getSegment() != null)
            return false;
        if (getIsAvatar() != null ? !getIsAvatar().equals(that.getIsAvatar()) : that.getIsAvatar() != null)
            return false;
        if (getCheckin() != that.getCheckin())
            return false;

        return !(getCaseDescription() != null ? !getCaseDescription().equals(that.getCaseDescription()) : that.getCaseDescription() != null);
    }

    @Override
    public int hashCode() {
        int result = getType() != null ? getType().hashCode() : 0;
        result = 31 * result + (getFormat() != null ? getFormat().hashCode() : 0);
        result = 31 * result + (getOrientation() != null ? getOrientation().hashCode() : 0);
        result = 31 * result + (getOrientationMatrix() != null ? Arrays.hashCode(getOrientationMatrix()) : 0);
        result = 31 * result + (getDate() != null ? getDate().hashCode() : 0);
        result = 31 * result + (getLatitude() != null ? getLatitude().hashCode() : 0);
        result = 31 * result + (getLongitude() != null ? getLongitude().hashCode() : 0);
        result = 31 * result + (getAccuracy() != null ? getAccuracy().hashCode() : 0);
        result = 31 * result + (getLength() != null ? getLength().hashCode() : 0);
        result = 31 * result + (getUri() != null ? getUri().hashCode() : 0);
        result = 31 * result + (getCaseNumber() != null ? getCaseNumber().hashCode() : 0);
        result = 31 * result + (getCaseDescription() != null ? getCaseDescription().hashCode() : 0);
        result = 31 * result + (getBatch() != null ? getBatch().hashCode() : 0);
        result = 31 * result + (getSegment() != null ? getSegment().hashCode() : 0);
        result = 31 * result + (getIsAvatar() != null ? getIsAvatar().hashCode() : 0);
        result = 31 * result + (getCheckin() != null ? getCheckin().hashCode() : 0);

        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.type);
        dest.writeString(this.format);
        dest.writeValue(this.orientation);
        dest.writeFloatArray(this.orientationMatrix);
        dest.writeLong(date != null ? date.getTime() : -1);
        dest.writeValue(this.latitude);
        dest.writeValue(this.longitude);
        dest.writeValue(this.accuracy);
        dest.writeValue(this.length);
        dest.writeParcelable(this.uri, 0);
        dest.writeString(this.caseNumber);
        dest.writeString(this.caseDescription);
        dest.writeString(this.batch);
        dest.writeValue(this.segment);
        dest.writeValue((Integer) (this.isAvatar ? 1 : 0));
        // Write checkin, check for null
        if (getCheckin() == null) {
            dest.writeInt(0);
        }
        else {
            dest.writeInt(1);
            dest.writeParcelable(getCheckin(), 0);
        }
    }

    @Override
    public String toString() {
        return "UploadPackage{" +
                "type='" + type + '\'' +
                ", format='" + format + '\'' +
                ", orientation=" + orientation +
                ", orientationMatrix=" + Arrays.toString(orientationMatrix) +
                ", date=" + date +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracy=" + accuracy +
                ", length=" + length +
                ", uri=" + uri +
                ", caseNumber='" + caseNumber + '\'' +
                ", caseDescription='" + caseDescription + '\'' +
                ", batch='" + batch + '\'' +
                ", segment='" + segment + '\'' +
                ", isAvatar='" + isAvatar + '\'' +
                ", checkin='" + (checkin == null ? "null" : checkin.toString()) + '\'' +
                '}';
    }
}
