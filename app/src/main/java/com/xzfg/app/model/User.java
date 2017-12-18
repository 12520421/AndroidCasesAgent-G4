package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;

import java.util.Comparator;


public class User implements Comparator<User>, Comparable<User>, Parcelable {
    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        public User createFromParcel(Parcel source) {
            return new User(source);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };
    @Attribute
    public String userId;
    @Attribute
    public String username;
    @Attribute
    public String online;
    @Attribute
    public String hasPhone;
    @Attribute
    public String hasEmail;
    public boolean system = false;
    public String sortName;
    @Attribute(required = false)
    public Double distance;
    @Attribute(required = false)
    public Integer direction;

    @SuppressWarnings("unused")
    public User() {
    }

    public User(String userId, String username, boolean onlineStatus) {
        this.userId = userId;
        this.username = username;
        if (onlineStatus) {
            online = "YES";
        } else {
            online = "NO";
        }
    }

    protected User(Parcel in) {
        this.userId = in.readString();
        this.username = in.readString();
        this.online = in.readString();
        this.hasPhone = in.readString();
        this.hasEmail = in.readString();
        this.system = in.readByte() != 0;
        this.sortName = in.readString();
        this.distance = (Double) in.readValue(Double.class.getClassLoader());
        this.direction = (Integer) in.readValue(Integer.class.getClassLoader());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (system != user.system) return false;
        if (userId != null ? !userId.equals(user.userId) : user.userId != null) return false;
        if (hasPhone != null ? !hasPhone.equals(user.hasPhone) : user.hasPhone != null)
            return false;
        if (hasEmail != null ? !hasEmail.equals(user.hasEmail) : user.hasEmail != null)
            return false;
        if (distance != null ? !distance.equals(user.distance) : user.distance != null)
            return false;
        return !(direction != null ? !direction.equals(user.direction) : user.direction != null);

    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", online='" + online + '\'' +
                ", hasPhone='" + hasPhone + '\'' +
                ", hasEmail='" + hasEmail + '\'' +
                ", system=" + system +
                ", distance=" + distance +
                ", direction=" + direction +
                '}';
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (hasPhone != null ? hasPhone.hashCode() : 0);
        result = 31 * result + (hasEmail != null ? hasEmail.hashCode() : 0);
        result = 31 * result + (system ? 1 : 0);
        result = 31 * result + (distance != null ? distance.hashCode() : 0);
        result = 31 * result + (direction != null ? direction.hashCode() : 0);
        return result;
    }

    public String getSortName() {
        if (sortName == null) {
            synchronized (this) {
                sortName =
                        (isOnline() ? String.valueOf(0) : String.valueOf(1)) + "-" +
                                (isSystem() ? String.valueOf(0) : String.valueOf(1)) + "-" +
                                getUsername().toUpperCase();
            }
        }
        return sortName;
    }

    public void setSortName(String sortName) {
        this.sortName = sortName;
    }

    public boolean isOnline() {
        return online.equalsIgnoreCase("YES");
    }

    @Override
    public int compareTo(User another) {
        return getSortName().compareTo(another.getSortName());
    }

    @Override
    public int compare(User lhs, User rhs) {
        return lhs.getSortName().compareTo(rhs.getSortName());
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOnline() {
        return online;
    }

    public void setOnline(String online) {
        this.online = online;
    }

    public String getHasPhone() {
        return hasPhone;
    }

    public void setHasPhone(String hasPhone) {
        this.hasPhone = hasPhone;
    }

    public String getHasEmail() {
        return hasEmail;
    }

    public void setHasEmail(String hasEmail) {
        this.hasEmail = hasEmail;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Integer getDirection() {
        return direction;
    }

    public void setDirection(Integer direction) {
        this.direction = direction;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.userId);
        dest.writeString(this.username);
        dest.writeString(this.online);
        dest.writeString(this.hasPhone);
        dest.writeString(this.hasEmail);
        dest.writeByte(system ? (byte) 1 : (byte) 0);
        dest.writeString(this.sortName);
        dest.writeValue(this.distance);
        dest.writeValue(this.direction);
    }

}
