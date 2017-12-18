package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Root
@SuppressWarnings("unused")
public class MediaHeader implements Parcelable {

    public static final Parcelable.Creator<MediaHeader> CREATOR = new Parcelable.Creator<MediaHeader>() {
        public MediaHeader createFromParcel(Parcel source) {
            return new MediaHeader(source);
        }

        public MediaHeader[] newArray(int size) {
            return new MediaHeader[size];
        }
    };
    @ElementList(inline = true, required = false)
    public List<Media> media = Collections.synchronizedList(new LinkedList<Media>());
    @Element(required = false)
    public Error error;

    public MediaHeader() {
    }

    protected MediaHeader(Parcel in) {
        this.media = in.createTypedArrayList(Media.CREATOR);
        this.error = in.readParcelable(Error.class.getClassLoader());
    }

    public List<Media> getMedia() {
        return media;
    }

    public void setMedia(List<Media> media) {
        this.media = media;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(media);
        dest.writeParcelable(this.error, 0);
    }

    public static class Media implements android.os.Parcelable {
        public static final Creator<Media> CREATOR = new Creator<Media>() {
            public Media createFromParcel(Parcel source) {
                return new Media(source);
            }

            public Media[] newArray(int size) {
                return new Media[size];
            }
        };
        @Attribute(required = false)
        String type;
        @Attribute(required = false)
        Date lastUpload;
        @Attribute(required = false)
        String caseNumber;
        @Attribute(required = false)
        Integer video;
        @Attribute(required = false)
        Integer audio;
        @Attribute(required = false)
        Integer image;

        public Media() {
        }

        protected Media(Parcel in) {
            this.type = in.readString();
            long tmpLastUpload = in.readLong();
            this.lastUpload = tmpLastUpload == -1 ? null : new Date(tmpLastUpload);
            this.caseNumber = in.readString();
            this.video = (Integer) in.readValue(Integer.class.getClassLoader());
            this.audio = (Integer) in.readValue(Integer.class.getClassLoader());
            this.image = (Integer) in.readValue(Integer.class.getClassLoader());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Media media = (Media) o;

            return !(getCaseNumber() != null ? !getCaseNumber().equals(media.getCaseNumber()) : media.getCaseNumber() != null);

        }

        @Override
        public int hashCode() {
            return getCaseNumber() != null ? getCaseNumber().hashCode() : 0;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Date getLastUpload() {
            return lastUpload;
        }

        public void setLastUpload(Date lastUpload) {
            this.lastUpload = lastUpload;
        }

        public String getCaseNumber() {
            return caseNumber;
        }

        public void setCaseNumber(String caseNumber) {
            this.caseNumber = caseNumber;
        }

        public Integer getVideo() {
            return video;
        }

        public void setVideo(Integer video) {
            this.video = video;
        }

        public Integer getAudio() {
            return audio;
        }

        public void setAudio(Integer audio) {
            this.audio = audio;
        }

        public Integer getImage() {
            return image;
        }

        public void setImage(Integer image) {
            this.image = image;
        }

        @Override
        public String toString() {
            return "Media{" +
                    "type='" + type + '\'' +
                    ", lastUpload=" + lastUpload +
                    ", caseNumber='" + caseNumber + '\'' +
                    ", video=" + video +
                    ", audio=" + audio +
                    ", image=" + image +
                    '}';
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.type);
            dest.writeLong(lastUpload != null ? lastUpload.getTime() : -1);
            dest.writeString(this.caseNumber);
            dest.writeValue(this.video);
            dest.writeValue(this.audio);
            dest.writeValue(this.image);
        }
    }
}