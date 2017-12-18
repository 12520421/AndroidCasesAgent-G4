package com.xzfg.app.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;

public class RegistrationParams implements Parcelable {
    public static final Creator<RegistrationParams> CREATOR = new Creator<RegistrationParams>() {
        public RegistrationParams createFromParcel(Parcel source) {
            return new RegistrationParams(source);
        }
        public RegistrationParams[] newArray(int size) {
            return new RegistrationParams[size];
        }
    };

    @Attribute
    String url;
    @Attribute
    String port;
    @Attribute
    String organization;
    @Attribute
    String name;
    @Attribute
    String username;
    @Attribute
    String password;
    @Attribute
    String email;
    @Attribute
    String phone;
    @Attribute
    Uri avatar;

    public RegistrationParams() {
    }

    protected RegistrationParams(Parcel in) {
        this.url = in.readString();
        this.port = in.readString();
        this.organization = in.readString();
        this.name = in.readString();
        this.username = in.readString();
        this.password = in.readString();
        this.email = in.readString();
        this.phone = in.readString();
        this.avatar = Uri.parse(in.readString());
    }

    @Override
    public String toString() {
        return "RegistrationParams{" +
                "url='" + url + '\'' +
                ", port=" + port +
                ", organizarion=" + organization +
                ", name=" + name +
                ", username=" + username +
                ", password=" + password +
                ", email=" + email +
                ", phone=" + phone +
                ", avatar='" + avatar +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegistrationParams params = (RegistrationParams) o;

        if (getUrl() != null ? !getUrl().equals(params.getUrl()) : params.getUrl() != null)
            return false;
        if (getPort() != null ? !getPort().equals(params.getPort()) : params.getPort() != null)
            return false;
        if (getOrganization() != null ? !getOrganization().equals(params.getOrganization()) : params.getOrganization() != null)
            return false;
        if (getName() != null ? !getName().equals(params.getName()) : params.getName() != null)
            return false;
        if (getUsername() != null ? !getUsername().equals(params.getUsername()) : params.getUsername() != null)
            return false;
        if (getPassword() != null ? !getPassword().equals(params.getPassword()) : params.getPassword() != null)
            return false;
        if (getEmail() != null ? !getEmail().equals(params.getEmail()) : params.getEmail() != null)
            return false;
        if (getPhone() != null ? !getPhone().equals(params.getPhone()) : params.getPhone() != null)
            return false;
        return !(getAvatar() != null ? !getAvatar().equals(params.getAvatar()) : params.getAvatar() != null);

    }

    @Override
    public int hashCode() {
        int result = getUrl() != null ? getUrl().hashCode() : 0;
        result = 31 * result + (getPort() != null ? getPort().hashCode() : 0);
        result = 31 * result + (getOrganization() != null ? getOrganization().hashCode() : 0);
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getUsername() != null ? getUsername().hashCode() : 0);
        result = 31 * result + (getPassword() != null ? getPassword().hashCode() : 0);
        result = 31 * result + (getEmail() != null ? getEmail().hashCode() : 0);
        result = 31 * result + (getPhone() != null ? getPhone().hashCode() : 0);
        result = 31 * result + (getAvatar() != null ? getAvatar().hashCode() : 0);
        return result;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Uri getAvatar() {
        return avatar;
    }

    public void setAvatar(Uri avatar) {
        this.avatar = avatar;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.url);
        dest.writeString(this.port);
        dest.writeString(this.organization);
        dest.writeString(this.name);
        dest.writeString(this.username);
        dest.writeString(this.password);
        dest.writeString(this.email);
        dest.writeString(this.phone);
        dest.writeString(this.avatar.toString());
    }
}
