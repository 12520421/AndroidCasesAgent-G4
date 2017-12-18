package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.xzfg.app.services.GattClient;

/**
 * Created by VYLIEM on 6/28/2017.
 */

public class G4Settings implements Parcelable {
    GattClient gattClient;
    public  G4Settings(){}

    public GattClient getGattClient() {
        return gattClient;
    }

    public void setGattClient(GattClient gattClient) {
        this.gattClient = gattClient;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        //dest.writeParcelable(gattClient,flags);

    }
    protected G4Settings(Parcel in){
      //  gattClient = new GattClient();
        gattClient = in.readParcelable(getClass().getClassLoader());
    }
    public static final Parcelable.Creator<G4Settings> CREATOR = new Parcelable.Creator<G4Settings>(){
        @Override
        public G4Settings createFromParcel(Parcel source) {
            return new G4Settings(source);
        }

        @Override
        public G4Settings[] newArray(int size) {
            return new G4Settings[size];
        }
    };
}
