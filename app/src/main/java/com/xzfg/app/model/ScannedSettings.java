package com.xzfg.app.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.crashlytics.android.Crashlytics;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.R;
import com.xzfg.app.security.Crypto;

import java.util.Arrays;

import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import timber.log.Timber;

/**
 * Holds settings parsed from barcode scanning.
 */
public class ScannedSettings implements Parcelable {
    private static final String PREFIX = "https://";
    private static final String DELIMITER = "|";
    private static final String SPLITTER = "\\" + DELIMITER;
    private String ipAddress;
    private Long trackingPort;
    private Long uploadPort;
    private Long videoPort;
    private Long audioPort;
    private Long mapPort;
    private Long organizationId;
    private String userName;
    private String password;
    private String timestamp;
    private String userId;
    private String encryptionFormat;
    private byte[] encryptionKey;
    private int useMapPort = 1;
    private String mapUrl;

    private ScannedSettings(String ipAddress, Long trackingPort, Long uploadPort, Long audioPort, Long videoPort, Long mapPort, Long organizationId, String userName, String password, String timestamp, String userId, String encryptionFormat, byte[] encryptionKey, int useMapPort, String mapUrl) {
        this.ipAddress = ipAddress;
        this.organizationId = organizationId;
        this.userName = userName;
        this.password = password;
        this.timestamp = timestamp;
        this.userId = userId;
        this.encryptionFormat = encryptionFormat;
        this.encryptionKey = encryptionKey;
        this.trackingPort = trackingPort;
        this.uploadPort = uploadPort;
        this.videoPort = videoPort;
        this.audioPort = audioPort;
        this.mapPort = mapPort;
        this.useMapPort = useMapPort;
        this.mapUrl = mapUrl;
    }

    public ScannedSettings(String ipAddress, Long trackingPort) {
        this.ipAddress = ipAddress;
        this.trackingPort = trackingPort;
    }

    protected ScannedSettings(Parcel in) {
        this.ipAddress = in.readString();
        this.trackingPort = (Long) in.readValue(Long.class.getClassLoader());
        this.uploadPort = (Long) in.readValue(Long.class.getClassLoader());
        this.videoPort = (Long) in.readValue(Long.class.getClassLoader());
        this.audioPort = (Long) in.readValue(Long.class.getClassLoader());
        this.mapPort = (Long) in.readValue(Long.class.getClassLoader());
        this.organizationId = (Long) in.readValue(Long.class.getClassLoader());
        this.userName = in.readString();
        this.password = in.readString();
        this.timestamp = in.readString();
        this.userId = in.readString();
        this.encryptionFormat = in.readString();
        this.encryptionKey = in.createByteArray();
        this.useMapPort = in.readInt();
        this.mapUrl = in.readString();
    }

    public static ScannedSettings parse(Context context, Result result) throws InvalidScanException {

        String ipAddress = null;
        Long trackingPort = null;
        Long uploadPort = null;
        Long videoPort = null;
        Long audioPort = null;
        Long mapPort = null;
        Long organizationId = null;
        String userName = null;
        String password = null;
        String timestamp = null;
        String encryptionFormat = null;
        byte[] encryptionKey = null;
        String userId = null;
        int useMapPort = 0;
        String mapUrl = null;

        if (result == null || result.getBarcodeFormat() == null || result.getContents() == null) {
            Timber.e("Null result.");
            if (BuildConfig.DEBUG) {
                Crashlytics.setString("Barcode Format", result.getBarcodeFormat().getName());
                Crashlytics.setString("Barcode Contents", result.getContents());
            }
            throw new InvalidScanException(context.getString(R.string.qrcode_failed));
        }

        if (result.getBarcodeFormat() != BarcodeFormat.QRCODE) {
            Timber.e("Barcode format is not QRCODE.");
            if (BuildConfig.DEBUG) {
                Crashlytics.setString("Barcode Format", result.getBarcodeFormat().getName());
                Crashlytics.setString("Barcode Contents", result.getContents());
            }
            throw new InvalidScanException(context.getString(R.string.qrcode_failed));
        }

        //Timber.d("Barcode data: " + result.getContents());
        String rawSettings = result.getContents();

        if (!rawSettings.startsWith(PREFIX)) {
            Timber.w("Expected prefix not present.");
            if (BuildConfig.DEBUG) {
                Crashlytics.setString("Barcode Format", result.getBarcodeFormat().getName());
                Crashlytics.setString("Barcode Contents", result.getContents());
            }
            throw new InvalidScanException(context.getString(R.string.qrcode_failed));
        }

        if (!rawSettings.contains(DELIMITER)) {
            Timber.w("Expected delimiter not present.");
            if (BuildConfig.DEBUG) {
                Crashlytics.setString("Barcode Format", result.getBarcodeFormat().getName());
                Crashlytics.setString("Barcode Contents", result.getContents());
            }
            throw new InvalidScanException(context.getString(R.string.qrcode_failed));
        }

        Timber.d(rawSettings);

        // remove delimiter and split.
        rawSettings = rawSettings.substring(PREFIX.length(), rawSettings.length());
        String[] parts = rawSettings.split(SPLITTER);
        if (parts.length != 10) {
            if (BuildConfig.DEBUG) {
                Crashlytics.setString("Barcode Format", result.getBarcodeFormat().getName());
                Crashlytics.setString("Barcode Contents", result.getContents());
            }
            Timber.w("Incorrect number of parameters in scan, expected 10, got " + parts.length + ", Raw: " + result.getContents());
            throw new InvalidScanException(context.getString(R.string.qrcode_failed));
        }

        // handle ip and port seperately.
        ipAddress = parts[0];
        organizationId = Long.parseLong(parts[1]);
        userName = parts[2];
        password = parts[3];
        timestamp = parts[4];
        userId = parts[5];
        // New V2 format parameters
        trackingPort = Long.parseLong(parts[6]);
        uploadPort = Long.parseLong(parts[7]);
        videoPort = Long.parseLong(parts[8]);
        audioPort = Long.parseLong(parts[9]);
/*
        encryptionFormat = parts[6];
        if (encryptionFormat.length() < 1) {
            encryptionFormat = null;
        } else {
            try {
                encryptionKey = Hex.decodeHex(parts[7].toCharArray());
            } catch (Exception e) {
                Timber.e(e, "An error occurred attempting to decode the encryption key.");
            }
        }
        String portString = parts[8];
        String[] portPairs = portString.split("\\~");
        for (String pair : portPairs) {
            String[] pairParts = pair.split("\\@");

            String key = pairParts[0];
            String value = pairParts[1];
            //Timber.d("Key: " + key + ", Value: " + value);

            if (key.equals("T")) {
                trackingPort = Long.parseLong(value);
            }
            if (key.equals("V")) {
                videoPort = Long.parseLong(value);
            }
            if (key.equals("A")) {
                audioPort = Long.parseLong(value);
            }
            if (key.equals("U")) {
                uploadPort = Long.parseLong(value);
            }
            if (key.equals("C")) {
                mapPort = Long.parseLong(value);
            }
        }
        useMapPort = Integer.parseInt(parts[9]);
        mapUrl = parts[10];
*/

        ScannedSettings settings = new ScannedSettings(ipAddress, trackingPort, uploadPort, audioPort, videoPort, mapPort, organizationId, userName, password, timestamp, userId, encryptionFormat, encryptionKey, useMapPort, mapUrl);
        //Timber.d("Scanned settings: " + settings);
        return settings;

    }

    /**
     * Clears the encryption key from memory by overwriting all bits with 0.
     */
    public void clearKey() {
        Arrays.fill(this.encryptionKey, (byte) 0);
    }

    public void validateCrypto(Context context, Crypto crypto) throws InvalidScanException {
        String expectedFormat = crypto.getExpectedFormat();
        int expectedKeyLength = crypto.getExpectedKeyLength();
        String scannedFormat = getEncryptionFormat();
        int scannedLength = -1;
        if (getEncryptionKey() != null) {
            scannedLength = getEncryptionKey().length * 8;
        }

        // no encryption expected or supported.
        if ((expectedFormat == null || expectedKeyLength < 1) && (scannedFormat == null && scannedLength == -1)) {
            return;
        }

        // get the proper string values in anticipation of sending exceptions.
        String expectedString;
        if (expectedFormat != null) {
            switch (expectedFormat) {
                case "AES256":
                    expectedString = context.getString(R.string.aes_256);
                    break;
                case "AES128":
                    expectedString = context.getString(R.string.aes_128);
                    break;
                case "TripleDES":
                    expectedString = context.getString(R.string.des);
                    break;
                default:
                    expectedString = context.getString(R.string.no_encryption);
            }
        } else {
            expectedString = context.getString(R.string.no_encryption);
        }

        String scannedString;
        if (scannedFormat != null) {
            switch (scannedFormat) {
                case "AES256":
                    scannedString = context.getString(R.string.aes_256);
                    break;
                case "AES128":
                    scannedString = context.getString(R.string.aes_128);
                    break;
                case "TripleDES":
                    scannedString = context.getString(R.string.des);
                    break;
                default:
                    scannedString = context.getString(R.string.no_encryption);
            }
        } else {
            scannedString = context.getString(R.string.no_encryption);
        }

        //we didn't expect encyption, but found it.
        if ((expectedFormat == null || expectedKeyLength < 1) && (scannedFormat != null || scannedLength > 1)) {
            throw new InvalidScanException(context.getString(R.string.no_encryption_mismatch_error, expectedFormat, scannedFormat));
        }

        // we expected encryption, but didn't find it
        if ((expectedFormat != null && expectedKeyLength > 1) && (scannedFormat == null || scannedLength < 1)) {
            throw new InvalidScanException(context.getString(R.string.encryption_mismatch_error, expectedString, scannedString));
        }

        if (
                expectedFormat != null &&
                        scannedFormat != null &&
                        (
                                !expectedFormat.equals(scannedFormat) ||
                                        expectedKeyLength != scannedLength
                        )
                ) {
            throw new InvalidScanException(context.getString(R.string.encryption_mismatch_error, expectedString, scannedString));
        }
        // if we've gotten this far, we can set the encryption key.
        //Timber.d("Encryption key verified, setting into crypto instance.");
        crypto.setKey(getEncryptionKey());
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Long getTrackingPort() {
        return trackingPort;
    }

    public void setTrackingPort(Long trackingPort) {
        this.trackingPort = trackingPort;
    }

    public Long getUploadPort() {
        return uploadPort;
    }

    public void setUploadPort(Long uploadPort) {
        this.uploadPort = uploadPort;
    }

    public Long getVideoPort() {
        return videoPort;
    }

    public void setVideoPort(Long videoPort) {
        this.videoPort = videoPort;
    }

    public Long getAudioPort() {
        return audioPort;
    }

    public void setAudioPort(Long audioPort) {
        this.audioPort = audioPort;
    }

    public Long getMapPort() {
        return mapPort;
    }

    public void setMapPort(Long mapPort) {
        this.mapPort = mapPort;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEncryptionFormat() {
        return encryptionFormat;
    }

    public void setEncryptionFormat(String encryptionFormat) {
        this.encryptionFormat = encryptionFormat;
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public int getUseMapPort() {
        return useMapPort;
    }

    public void setUseMapPort(int useMapPort) {
        this.useMapPort = useMapPort;
    }

    public String getMapUrl() { return mapUrl; }
    public void setMapUrl(String mapUrl) { this.mapUrl = mapUrl; }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ScannedSettings> CREATOR = new Parcelable.Creator<ScannedSettings>() {
        public ScannedSettings createFromParcel(Parcel source) {
            return new ScannedSettings(source);
        }

        public ScannedSettings[] newArray(int size) {
            return new ScannedSettings[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.ipAddress);
        dest.writeValue(this.trackingPort);
        dest.writeValue(this.uploadPort);
        dest.writeValue(this.videoPort);
        dest.writeValue(this.audioPort);
        dest.writeValue(this.mapPort);
        dest.writeValue(this.organizationId);
        dest.writeString(this.userName);
        dest.writeString(this.password);
        dest.writeString(this.timestamp);
        dest.writeString(this.userId);
        dest.writeString(this.encryptionFormat);
        dest.writeByteArray(this.encryptionKey);
        dest.writeInt(this.useMapPort);
        dest.writeString(this.mapUrl);
    }

    public static class InvalidScanException extends Exception {
        public InvalidScanException(String message) {
            super(message);
        }
    }

    @Override
    public String toString() {
        return "ScannedSettings{" +
                "ipAddress='" + ipAddress + '\'' +
                ", trackingPort=" + trackingPort +
                ", uploadPort=" + uploadPort +
                ", videoPort=" + videoPort +
                ", audioPort=" + audioPort +
                ", mapPort=" + mapPort +
                ", organizationId=" + organizationId +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", userId='" + userId + '\'' +
                ", encryptionFormat='" + encryptionFormat + '\'' +
                ", encryptionKey=" + Arrays.toString(encryptionKey) +
                ", useMapPort=" + useMapPort +
                ", mapUrl='" + mapUrl + '\'' +
                '}';
    }
}
