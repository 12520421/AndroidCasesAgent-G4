package com.xzfg.app.managers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;


import com.xzfg.app.Application;
import com.xzfg.app.services.GattClient;
//import com.xzfg.app.services.BluetoothService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

/**
 * Created by APP-PC on 6/12/2017.
 */

public class BluetoothManager implements Parcelable {
    //Receive data
    private String data;

    ArrayList<String>  commandListEnter = new ArrayList<String>();
    ArrayList<String>  commandListAll=new ArrayList<String>();

    public ArrayList<String> getCommandListEnter() {
        return commandListEnter;
    }

    public ArrayList<String> getCommandListAll() {
        return commandListAll;
    }

    LinkedList<String> commandList = new LinkedList<String>();
    //GPS data
    private String time;
    private String longtiltude;
    private String altiltude;
    private String heading;
    private String speed;
    private String HDOP;
    private String SATS;
    private String valid;
    private String mode;
    //other data
    private String commandType;
    private String IMEI;
    private String Led;
    private String AlertState;
    private String GPMS;
    private String DeviceName;
    private String GPSStrength;
    private String normalReportingTime;
    private String alertReportingTime;
    private String firmware;

    //boolean to check Status
    private boolean GPSStatus = false;;
    private boolean AlertOnStatus = false;;
    private boolean AlertOffStatus = false;;
    private boolean StatusUpdate = false;
    private boolean StatusIMEIUpdate = false;

    public boolean isGPSStatus() {
        return GPSStatus;
    }

    public void setGPSStatus(boolean GPSStatus) {
        this.GPSStatus = GPSStatus;
    }

    public boolean isAlertOnStatus() {
        return AlertOnStatus;
    }

    public void setAlertOnStatus(boolean alertOnStatus) {
        AlertOnStatus = alertOnStatus;
    }

    public boolean isAlertOffStatus() {
        return AlertOffStatus;
    }

    public void setAlertOffStatus(boolean alertOffStatus) {
        AlertOffStatus = alertOffStatus;
    }

    public boolean isStatusUpdate() {
        return StatusUpdate;
    }

    public void setStatusUpdate(boolean statusUpdate) {
        StatusUpdate = statusUpdate;
    }

    public boolean isStatusIMEIUpdate() {
        return StatusIMEIUpdate;
    }

    public void setStatusIMEIUpdate(boolean statusIMEIUpdate) {
        StatusIMEIUpdate = statusIMEIUpdate;
    }

    public boolean isStatusNameUpdate() {
        return StatusNameUpdate;
    }

    public void setStatusNameUpdate(boolean statusNameUpdate) {
        StatusNameUpdate = statusNameUpdate;
    }

    public boolean isStatusFirmwareUpdate() {
        return StatusFirmwareUpdate;
    }

    public void setStatusFirmwareUpdate(boolean statusFirmwareUpdate) {
        StatusFirmwareUpdate = statusFirmwareUpdate;
    }

    public boolean isMarkStatus() {
        return MarkStatus;
    }

    public void setMarkStatus(boolean markStatus) {
        MarkStatus = markStatus;
    }

    private boolean StatusNameUpdate = false;
    private boolean StatusFirmwareUpdate = false;
    private boolean MarkStatus = false;



    public LinkedList<String> getCommandList() {
        return commandList;
    }

    public void setCommandList(LinkedList<String> commandList) {
        this.commandList = commandList;
    }

    public void processingData(StringBuilder builder){
        if(commandList.size() > 0)
        {
            this.commandList.removeFirst();
        }


        data = builder.toString();
        commandListAll.add(data);
        //get onlt second line -> b/c it contain the data we need
        data = data.substring(data.indexOf('\n')+1);
        data=data.replaceAll("(\\r|\\n)","");
        data=data.replaceAll("OK","");
        String[] part = data.split(":");
        //get the command we sent
        commandType = part[0];

        //Delete the command at the head of the 2nd line
        data = data.substring(data.indexOf(":") + 1);
        String dataCommand = data;
        switch (commandType){
            case "$GPSO":
                part = dataCommand.split(",");
                time = part[0];
                dataCommand = dataCommand.substring(data.indexOf(",") + 1);
                part = dataCommand.split(",");
                longtiltude = part[0];
                dataCommand = dataCommand.substring(data.indexOf(",") + 1);
                part = dataCommand.split(",");
                altiltude = part[0];
                dataCommand = dataCommand.substring(data.indexOf(",") + 1);
                part = dataCommand.split(",");
                heading = part[0];
                dataCommand = dataCommand.substring(data.indexOf(",") + 1);
                part = dataCommand.split(",");
                speed = part[0];
                dataCommand = dataCommand.substring(data.indexOf(",") + 1);
                part = dataCommand.split(",");
                HDOP = part[0];
                dataCommand = dataCommand.substring(data.indexOf(",") + 1);
                part = dataCommand.split(",");
                SATS = part[0];
                dataCommand = dataCommand.substring(data.indexOf(",") + 1);
                part = dataCommand.split(",");
                valid = part[0];
                mode = part[1];


                break;
            case "$MGSN":
                IMEI = part[1];
                break;
            case "$LED":
                Led = part[1];
                break;
            case "$FFALERT":
                AlertState = part[1];
                break;
            case "$GPMS":
                GPMS=part[1];
                break;
            case "G4":
                DeviceName=data;
                break;
            case "$MCSQ":
                GPSStrength=part[1];
                break;
            case "ATI5":
                firmware=data;
                break;
            case "AT$GCGET=user_cfg.tx_time":
                part = dataCommand.split(",");
                normalReportingTime = part[1];
                break;
            case "AT$GCGET=user_cfg.alert_tx_time":
                part = dataCommand.split(",");
                alertReportingTime = part[1];
                break;
            default:
                firmware=data;
                break;


        }
    }

    public void executeCommand(GattClient client){
        if(commandList.size() > 0) {
            String command = commandList.getFirst();
            client.writeInteractor(command,false);
        }
    }

    public String getHeading() {
        return heading;
    }

    public String getSpeed() {
        return speed;
    }

    public String getHDOP() {
        return HDOP;
    }

    public String getSATS() {
        return SATS;
    }

    public String getValid() {
        return valid;
    }

    public String getMode() {
        return mode;
    }

    public String getFirmware() {
        return firmware;
    }

    public String getTime() {
        return time;
    }

    public String getLongtiltude() {
        return longtiltude;
    }

    public String getAltiltude() {
        return altiltude;
    }

    public String getData() {
        return data;
    }

    public String getCommandType() {
        return commandType;
    }

    public String getIMEI() {
        return IMEI;
    }

    public String getLed() {
        return Led;
    }

    public String getAlertState() {
        return AlertState;
    }

    public String getGPMS() {
        return GPMS;
    }

    public String getDeviceName() {
        return DeviceName;
    }

    public String getGPSStrength() {
        return GPSStrength;
    }

    public String getNormalReportingTime() {
        return normalReportingTime;
    }

    public String getAlertReportingTime() {
        return alertReportingTime;
    }



    public void SendCommand(GattClient client, String command){
        switch (command)
        {
            case "GPS_cor":
                //client.writeInteractor("AT$GPSO");
                commandListAll.add("AT$GPSO");
                commandList.add("AT$GPSO");
                break;
            case "device_name":
                //client.writeInteractor("ATI");
                commandListAll.add("ATI");
                commandList.add("ATI");
                break;
            case "Alert_on":
                //client.writeInteractor("AT$FFALERT=1");
                commandListAll.add("AT$FFALERT=1");
                commandList.add("AT$FFALERT=1");
                break;
            case "Alert_off":
                //client.writeInteractor("AT$FFALERT=0");
                commandListAll.add("AT$FFALERT=0");
                commandList.add("AT$FFALERT=0");
                break;
            case "GPS_str":
                //client.writeInteractor("AT$MCSQ");
                commandListAll.add("AT$MCSQ");
                commandList.add("AT$MCSQ");
                break;
            case "IMEI":
                //client.writeInteractor("AT$MGSN");
                commandListAll.add("AT$MGSN");
                commandList.add("AT$MGSN");
                break;
            case "Firmware":
                //client.writeInteractor("ATI5");
                commandListAll.add("ATI5");
                commandList.add("ATI5");
                break;
            case "NRT":
                //client.writeInteractor("AT$GCGET=user_cfg.tx_time");
                commandListAll.add("AT$GCGET=user_cfg.tx_time");
                commandList.add("AT$GCGET=user_cfg.tx_time");
                break;
            case "ART":
                //client.writeInteractor("AT$GCGET=user_cfg.alert_tx_time");
                commandListAll.add("AT$GCGET=user_cfg.alert_tx_time");
                commandList.add("AT$GCGET=user_cfg.alert_tx_time");
                break;
            case "getLed":
                //client.writeInteractor("AT$LED?");
                commandListAll.add("AT$LED?");
                commandList.add("AT$LED?");
                break;
            case "getAlert":
                //client.writeInteractor("AT$FFALERT?");
                commandListAll.add("AT$FFALERT?");
                commandList.add("AT$FFALERT?");
                break;
            default:
                commandListAll.add(command);
                commandList.add(command);
                break;
        }

            executeCommand(client);


    }

    public void SendCommandWithParameter(GattClient client, String command, String parameter)
    {
        //It seem that only message using additional parameter
        //just leave it here for another time
        switch (command)
        {


        }

        //client.writeInteractor("");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(data);

        dest.writeSerializable(commandListEnter);
        dest.writeSerializable(commandListAll);

        dest.writeSerializable(commandList);
        //GPS data
        dest.writeString(time);
        dest.writeString(longtiltude);
        dest.writeString(altiltude);
        dest.writeString(heading);
        dest.writeString(speed);
        dest.writeString(HDOP);
        dest.writeString(SATS);
        dest.writeString(valid);
        dest.writeString(mode);
        //other data
        dest.writeString(commandType);
        dest.writeString(IMEI);
        dest.writeString(Led);
        dest.writeString(AlertState);
        dest.writeString(GPMS);
        dest.writeString(DeviceName);
        dest.writeString(GPSStrength);
        dest.writeString(normalReportingTime);
        dest.writeString(alertReportingTime);

        dest.writeString(firmware);


    }
    private BluetoothManager(Parcel in){
        data = in.readString();
        commandListEnter = (ArrayList<String>) in.readSerializable();
        commandListAll = (ArrayList<String>) in.readSerializable();
        commandList = (LinkedList<String>) in.readSerializable();

        time = in.readString();
        longtiltude = in.readString();
        altiltude = in.readString();
        heading = in.readString();
        speed = in.readString();
        HDOP = in.readString();
        speed = in.readString();
        HDOP = in.readString();
        SATS = in.readString();
        valid = in.readString();
        mode = in.readString();
        commandType = in.readString();
        IMEI = in.readString();
        Led = in.readString();
        AlertState = in.readString();
        GPMS = in.readString();
        DeviceName = in.readString();
        GPSStrength = in.readString();
        normalReportingTime = in.readString();
        alertReportingTime = in.readString();
        //ssssssdsss
        firmware = in.readString();

    }
    public static final Parcelable.Creator<BluetoothManager> CREATOR = new Parcelable.Creator<BluetoothManager>(){
        @Override
        public BluetoothManager createFromParcel(Parcel source) {
            return new BluetoothManager(source);
        }

        @Override
        public BluetoothManager[] newArray(int size) {
            return new BluetoothManager[size];
        }
    };
}
