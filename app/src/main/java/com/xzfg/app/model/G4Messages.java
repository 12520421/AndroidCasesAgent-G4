package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Comparator;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class G4Messages implements Comparator<G4Messages>, Comparable<G4Messages>, Parcelable {
public static final Parcelable.Creator<G4Messages> CREATOR = new Parcelable.Creator<G4Messages>() {
	public G4Messages createFromParcel(Parcel in) {
		return new G4Messages(in);
	}

	public G4Messages[] newArray(int size) {
		return new G4Messages[size];
	}
};
int id;
String body;
Date mesgDate;
String status;
Boolean isSent;

public G4Messages() {
	super();
}

private G4Messages(Parcel in) {
	this.id = in.readInt();
	this.body = in.readString();
	long tmpCreated = in.readLong();
	this.mesgDate = tmpCreated == -1 ? null : new Date(tmpCreated);
	this.status = in.readString();
	this.isSent = in.readByte() != 0;
}

public int getID() {
	return id;
}
public void setID(int id) {
	this.id = id;
}
public String getBody() {
	return body;
}
public void setBody(String body) {
	this.body = body;
}

public Date getDate() {
	return mesgDate;
}

public void setDate(Date mesgDate) {
	this.mesgDate = mesgDate;
}

public String getLocalDate() {
	SimpleDateFormat f = new SimpleDateFormat("MMM d, HH:mm:ss");
	f.setTimeZone(TimeZone.getDefault());
	return (f.format(new Date(mesgDate.getTime())));
}

public String getUtcDate() {
	SimpleDateFormat f = new SimpleDateFormat("MMM d, HH:mm:ss");
	f.setTimeZone(TimeZone.getTimeZone("UTC"));
	return (f.format(new Date(mesgDate.getTime())));
}

public String getStatus() {
	return status;
}
public void setStatus(String status) {
	this.status = status;
}
public Boolean getSent() {
	return isSent;
}
public void setSent(Boolean sent) {
	isSent = sent;
}

@Override
public int describeContents() {
	return 0;
}

@Override
public void writeToParcel(Parcel dest, int flags) {
}

@Override
public int compareTo(@NonNull G4Messages o) {
	return 0;
}

@Override
public int compare(G4Messages o1, G4Messages o2) {
	return 0;
}

}

