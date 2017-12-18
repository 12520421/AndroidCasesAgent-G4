package com.xzfg.app.model;

import android.net.Uri;

import java.io.File;
import java.util.Arrays;


public class Submission {
    public UploadPackage uploadPackage;
    public String file;
    public byte[] bytes;
    public Uri uri;
    public byte[] thumbnailData;

    public Submission() {
    }

    public Submission(UploadPackage uploadPackage, byte[] bytes) {
        this.uploadPackage = uploadPackage;
        this.bytes = bytes;
    }

    public Submission(UploadPackage uploadPackage, File file) {
        this.uploadPackage = uploadPackage;
        this.file = file.getAbsolutePath();
    }

    public Submission(UploadPackage uploadPackage, Uri uri) {
        this.uploadPackage = uploadPackage;
        this.uri = uri;
    }

    public UploadPackage getUploadPackage() {
        return uploadPackage;
    }

    public void setUploadPackage(UploadPackage uploadPackage) {
        this.uploadPackage = uploadPackage;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public byte[] getThumbnailData() {
        return thumbnailData;
    }

    public void setThumbnailData(byte[] thumbnailData) {
        this.thumbnailData = thumbnailData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Submission that = (Submission) o;

        if (getUploadPackage() != null ? !getUploadPackage().equals(that.getUploadPackage()) : that.getUploadPackage() != null)
            return false;
        if (getFile() != null ? !getFile().equals(that.getFile()) : that.getFile() != null)
            return false;
        if (!Arrays.equals(getBytes(), that.getBytes())) return false;
        if (getUri() != null ? !getUri().equals(that.getUri()) : that.getUri() != null)
            return false;
        return Arrays.equals(getThumbnailData(), that.getThumbnailData());

    }

    @Override
    public int hashCode() {
        int result = getUploadPackage() != null ? getUploadPackage().hashCode() : 0;
        result = 31 * result + (getFile() != null ? getFile().hashCode() : 0);
        result = 31 * result + (getBytes() != null ? Arrays.hashCode(getBytes()) : 0);
        result = 31 * result + (getUri() != null ? getUri().hashCode() : 0);
        result = 31 * result + (getThumbnailData() != null ? Arrays.hashCode(getThumbnailData()) : 0);
        return result;
    }

}
