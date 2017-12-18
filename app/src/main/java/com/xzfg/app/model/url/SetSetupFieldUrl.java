package com.xzfg.app.model.url;


import android.os.Parcel;

import com.xzfg.app.Application;
import com.xzfg.app.R;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class SetSetupFieldUrl extends SessionUrl {

  protected HashMap<String, String> pairs;
  protected String password;
  protected String serial;
  protected String userId;
  protected String orgId;

  @SuppressWarnings("unused")
  public SetSetupFieldUrl() {
    super();
  }


  public SetSetupFieldUrl(Application application) {
    this(application, null, null);
  }

  public SetSetupFieldUrl(Application application, String key, String value) {
    super(application.getScannedSettings(), application.getString(R.string.setsetupfield_endpoint), null);

    if (application.getScannedSettings() != null) {
      this.password = application.getScannedSettings().getPassword();
      this.userId = application.getScannedSettings().getUserId();
      this.orgId = String.valueOf(application.getScannedSettings().getOrganizationId());
      this.serial = application.getDeviceIdentifier();
    }

    if (key != null && value != null && key.length() > 0) {
      HashMap<String,String> params = new HashMap<>();
      params.put(key,value);
      this.pairs = params;
    }
  }

  public HashMap<String,String> getPairs() {
    return pairs;
  }

  public void setPairs(HashMap<String, String> pairs) {
    this.pairs = pairs;
  }

  @Override
  public String getParams() {
    try {
      StringBuilder sb = new StringBuilder(192);

      String baseParams = super.getParams();
      if (baseParams != null)
        sb.append(baseParams);

      appendParam(sb, "model", "CASESAgent");
      appendParam(sb, "password", password);
      appendParam(sb, "serial", orgId + serial);
      appendParam(sb, "userId", userId);


      if (pairs != null && !pairs.isEmpty()) {
        StringBuilder pairsBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : pairs.entrySet()) {
          if (pairsBuilder.length() > 0) {
            pairsBuilder.append("|");
          }
          pairsBuilder.append(entry.getKey());
          pairsBuilder.append("~");
          pairsBuilder.append(entry.getValue());
        }
        if (pairsBuilder.length() > 0) {
          appendParam(sb, "pairs", pairsBuilder.toString());
        }
      }
      return sb.toString();

    } catch (Exception e) {
      Timber.e(e, "An error occurred generating the url.");
    }
    return null;
  }


  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeSerializable(this.pairs);
    dest.writeString(this.password);
    dest.writeString(this.serial);
    dest.writeString(this.userId);
    dest.writeString(this.orgId);
  }

  protected SetSetupFieldUrl(Parcel in) {
    super(in);
    this.pairs = (HashMap<String, String>) in.readSerializable();
    this.password = in.readString();
    this.serial = in.readString();
    this.userId = in.readString();
    this.orgId = in.readString();
  }

  public static final Creator<SetSetupFieldUrl> CREATOR = new Creator<SetSetupFieldUrl>() {
    @Override
    public SetSetupFieldUrl createFromParcel(Parcel source) {
      return new SetSetupFieldUrl(source);
    }

    @Override
    public SetSetupFieldUrl[] newArray(int size) {
      return new SetSetupFieldUrl[size];
    }
  };
}
