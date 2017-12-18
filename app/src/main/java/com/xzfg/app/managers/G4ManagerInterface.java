package com.xzfg.app.managers;

/**
 * Created by VYLIEM on 6/27/2017.
 */

public interface G4ManagerInterface {
    void setReportNormal(long reportNormal);
    void setReportAlerting(long reportAlerting);
    void setLedBrightness(long ledBrightness);
    void readRSSI();
    void prepareForMesaaging();
    void sendMessage(String message);
    void getIncommingMessages(boolean allMessage);
    void getSentMessages();
    void readMessageAtIndex(String index,boolean fromSent);
    void deleteMessageById(String id);
    void deleteMessage();
    void clearManagedObjectContext();
    void retrieveMessageCode();
    void messageCode();
    void clearMessage();
    void addCommand(String command);
    void getSerialNumber();
    void getSignalStrengths();
    void getGPSFix();
    void getAlert();
    void setAlertOn();
    void setAlertOff();
    void getAlertGPSInfo();
    void addMark();
    void setupDevice();
    void notifyGPSInfo();
    void receiverCommand();
    void didreadRSSI();


}
