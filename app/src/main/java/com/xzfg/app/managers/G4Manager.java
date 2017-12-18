package com.xzfg.app.managers;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;

import com.xzfg.app.Application;
import com.xzfg.app.DebugLog;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.G4Messages;
import com.xzfg.app.model.ListATCommand;
import com.xzfg.app.services.GattClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * Created by VYLIEM on 6/27/2017.
 */

public class G4Manager implements G4ManagerInterface,Parcelable {
    final String BLUEGIGA_SERVICE_UUID = ";1D5688DE-866D-3AA4-EC46-A1BDDB37ECF6";
    final String BLUEGIGA_CHAR_TX_UUID = ";AF20fBAC-2518-4998-9AF7-AF42540731B3";
    final String BLUEGIGA_CHAR_RX_UUID = ";AF20fBAC-2518-4998-9AF7-AF42540731B3";


    //variable to check number of command excute
    static boolean Excuted = true;
    boolean AlertClicked = false;
    boolean MarkClicked = false;
    boolean ConnectionState = false;
    boolean isCommandEntered = false;
    boolean chatReceive;
    boolean isEnter = false;
    boolean isStatusScreen = true;
    boolean isChatScreen = false;
    boolean isMarkAlertScreen = false;
    boolean isATCommandScreen = false;
    boolean isConfigurationScreen = false;
    boolean isFromConfigScreen = false;
    boolean isFromATScreen = false;
    boolean isMarkOn=false;
    boolean isPastAlertState = false;

    GattClient client;
    GattClient markClient;
    private static Date lastMarkedDate = new Date();
    private static Date lastAlertOffDate = new Date();
    private static Date lastAlertOnDate = new Date();
    StringBuilder stringBuider;
    final String AT_INFO_MODEL= "ATI";
    final String AT_INFO_REVISION ="ATI5";
    final String  AT_MODEM_SIGNAL_QUALITY ="AT$MCSQ";
    final String  AT_MODEM_SERIAL_NUMBER ="AT$MGSN";

    final String  AT_GPS_REPORT= "AT$GPSO";
    private final Object mediaLock = new Object();
    private boolean acceptExecute = true;

    final String  AT_REPORT_NORMAL_SET ="AT$GCSET=user_cfg.tx_time,";
    final String  AT_REPORT_NORMAL_GET ="AT$GCGET=user_cfg.tx_time";
    final String  AT_REPORT_ALERT_GET ="AT$GCGET=user_cfg.alert_tx_time";
    final String  AT_REPORT_ALERT_SET="AT$GCSET=user_cfg.alert_tx_time,";

    final String  AT_FFS_MARK= "AT$FFMARK";

    final String  AT_FFS_ALERT_SET_ON ="AT$FFALERT=1";
    final String  AT_FFS_ALERT_SET_OFF ="AT$FFALERT=0";
    final String  AT_FFS_ALERT_GET ="AT$FFALERT?";

    final String  AT_FFS_ALERT_RPT_PERIOD_SET ="AT$FFARP";
    final String  AT_FFS_ALERT_RPT_PERIOD_GET ="AT$FFARP?";

    final String  AT_LED_BRIGHTNESS_SET ="AT$LED=";
    final String  AT_LED_BRIGHTNESS_GET ="AT$LED?";


    final String  AT_SAVE_CONFIG_SETTINGS ="AT$GCSAVE";
    final String  AT_SAVE_SETTINGS ="AT&W";

    final String  AT_RESET ="AT$RST";

// Messaging

    final String AT_MSG_RECEIVE_DB_SET ="AT$GPMS=0";
    final String AT_MSG_SEND_DB_SET ="AT$GPMS=1";
    final String AT_MSG_DB_SET_RESULT ="$GPMS: ";
    final String AT_MSG_FORMAT_SET ="AT$GMGF=1";
    final String AT_MSG_SEND ="AT$GMGS=";
    final String AT_MSG_SEND_RESULT ="$GMGS: ";
    final String AT_MSG_READ ="AT$GMGR=";
    final String AT_MSG_DELETE ="AT$GMGD=";
    final String AT_MSG_NEW_MSG_INDICATION_SET ="AT$GNMI=1";
    final String AT_MSG_NEW_MSG_INDICATION_GET ="AT$GNMI?";
    final String AT_MSG_RECEIVED_NEW_MSG ="$GMTI: ";
    final String AT_MSG_LIST_MSGS_ALL ="AT$GMGL=ALL";
    final String AT_MSG_LIST_MSGS_UNREAD ="AT$GMGL=REC UNREAD";
    final String AT_MSG_LIST_MSGS_UNSENT ="AT$GMGL=STO UNSENT";
    final String AT_MSG_LIST_MSGS_RESULT ="$GMGL: ";
// GPS Data

    private String time;
    private String longtiltude;
    private String altiltude;
    private String heading;
    private String speed;
    private String HDOP;
    private String SATS;
    private String valid;
    private String mode;
    private int idMessageError;
    private boolean isChatRefreshRun = false;
    static int tempMesseageReceiver = 0;
    //Chat Data
    private Queue<String> failQueue = new LinkedList<String>();

    public boolean isFromATScreen() {
        return isFromATScreen;
    }

    public void setFromATScreen(boolean fromATScreen) {
        isFromATScreen = fromATScreen;
    }

    public boolean isATCommandScreen() {
        return isATCommandScreen;
    }

    public void setATCommandScreen(boolean ATCommandScreen) {
        isATCommandScreen = ATCommandScreen;
    }

    public boolean isConfigurationScreen() {
        return isConfigurationScreen;
    }

    public void setConfigurationScreen(boolean configurationScreen) {
        Log.d("ATCommand","" +  configurationScreen);
        isConfigurationScreen = configurationScreen;
    }

    public boolean isPastAlertState() {
        return isPastAlertState;
    }

    public void setPastAlertState(boolean pastAlertState) {
        isPastAlertState = pastAlertState;
    }

    public boolean isChatScreen() {
        return isChatScreen;
    }

    public boolean isChatRefreshRun() {
        return isChatRefreshRun;
    }



    public void setChatRefreshRun(boolean chatRefreshRun) {
        isChatRefreshRun = chatRefreshRun;
    }

    public void setChatATScreen(boolean chatScreen){
        this.isChatScreen = chatScreen;
    }

    synchronized public void DeleteAllArray()
    {

    }

    public boolean isFromConfigScreen() {
        return isFromConfigScreen;
    }

    public void setFromConfigScreen(boolean fromConfigScreen) {
        isFromConfigScreen = fromConfigScreen;
    }

    private Context context;
    public G4Manager(Context context){
        this.context = context;
    }
    public void setLed(String led) {
        this.Led = led;
    }
    public Date getlastMarkedDate() {
        return lastMarkedDate;
    }
    public void setlastMarkedDate(Date lastMarkedDate) {
        this.lastMarkedDate = lastMarkedDate;
    }
    public Date getlastAlertOffDate() {
        return lastAlertOffDate;
    }
    public void setlastAlertOffDate(Date lastAlertOffDate) {this.lastAlertOffDate = lastAlertOffDate; }
    public Date getlastAlertOnDate() {
        return lastAlertOnDate;
    }
    public void setlastAlertOnDate(Date lastAlertOnDate) {this.lastAlertOnDate = lastAlertOnDate; }
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
    private String address;

    //ID Chat
    private int firstIdReceiver;
    private int lastIdReceiver;
    private int firstIdSent;
    private int lastIdSent;
    //
    boolean hasUnsent = false;
    private ArrayList<G4Messages> listG4MessagesReceive = new ArrayList<>();
    private ArrayList<G4Messages> listG4MessagesSent = new ArrayList<>();
    private ArrayList<G4Messages> listG4MessagesAll = new ArrayList<>();
    private ArrayList<String>listMessageG4=new ArrayList<>();
    public List<String> getListMessage(){
        return listMessageG4;
    }
    public void add2List(String message){
        listMessageG4.add(message);
        Log.d("TestData",listMessageG4.size()+"");
    }
    public ArrayList<G4Messages> getListG4MessagesAll() {
        return listG4MessagesAll;
    }

    public void setListG4MessagesAll(ArrayList<G4Messages> listG4MessagesAll) {
        this.listG4MessagesAll = listG4MessagesAll;
    }
    public int getFirstIdReceiver() {
        return firstIdReceiver;
    }

    public void setFirstIdReceiver(int firstIdReceiver) {
        this.firstIdReceiver = firstIdReceiver;
    }

    public int getLastIdReceiver() {
        return lastIdReceiver;
    }

    public void setLastIdReceiver(int lastIdReceiver) {
        this.lastIdReceiver = lastIdReceiver;
    }

    public int getFirstIdSent() {
        return firstIdSent;
    }

    public void setFirstIdSent(int firstIdSent) {
        this.firstIdSent = firstIdSent;
    }

    public int getLastIdSent() {
        return lastIdSent;
    }

    public void setLastIdSent(int lastIdSent) {
        this.lastIdSent = lastIdSent;
    }

    public boolean isAlertState() {
        return alertState;
    }

    public void setAlertState(boolean alertState) {
        this.alertState = alertState;
    }

    public void setNormalReportingTime(String normalReportingTime) {
        this.normalReportingTime = normalReportingTime;
    }

    public void setAlertReportingTime(String alertReportingTime) {
        this.alertReportingTime = alertReportingTime;
    }

    public boolean isStatusScreen() {
        return isStatusScreen;
    }

    public void setStatusScreen(boolean chatScreen) {
        isStatusScreen = chatScreen;
    }

    public ArrayList<G4Messages> getListG4MessagesReceive() {
        return listG4MessagesReceive;
    }

    public void setListG4MessagesReceive(ArrayList<G4Messages> listG4MessagesReceive) {
        this.listG4MessagesReceive = listG4MessagesReceive;
    }

    public ArrayList<G4Messages> getListG4MessagesSent() {
        return listG4MessagesSent;
    }

    public void setListG4MessagesSent(ArrayList<G4Messages> listG4MessagesSent) {
        this.listG4MessagesSent = listG4MessagesSent;
    }

    public void empty2List(){
        this.listG4MessagesSent.clear();
        this.listG4MessagesReceive.clear();
    }

    //Check Alert State
    boolean alertState = false;
    public String getAddress() {
        return address;
    }

    public G4Manager() {

    }

    public void setAddress(String address) {
        this.address = address;
    }
    public void delete(){

    }
    private String data;


    ArrayList<ListATCommand> commandListEnter = new ArrayList<ListATCommand>();
    ArrayList<ListATCommand>  commandListAll=new ArrayList<ListATCommand>();
    LinkedList<String> commandList = new LinkedList<String>();

    public Queue<String> getFailQueue() {
        return failQueue;
    }

    public void setFailQueue(Queue<String> failQueue) {
        this.failQueue = failQueue;
    }

    public ArrayList<ListATCommand> getCommandListEnter() {
        return commandListEnter;
    }

    public void setCommandListEnter(ArrayList<ListATCommand> commandListEnter) {
        this.commandListEnter = commandListEnter;
    }

    public ArrayList<ListATCommand> getCommandListAll() {
        return commandListAll;
    }

    public void setCommandListAll(ArrayList<ListATCommand> commandListAll) {
        this.commandListAll = commandListAll;
    }

    final int MAX_CONNECT_ATTEMPTS =3;
    final double CONNECT_ATTEMP_TIME =5.0;
    @Inject
    Application application;
    public G4Manager(Application application,Context context){
        application.inject(this);
        this.context = context;
    }
    @Override
    public void setReportNormal(long reportNormal) {
        addCommand(AT_REPORT_NORMAL_SET+String.valueOf(reportNormal));
        addCommand(AT_SAVE_CONFIG_SETTINGS);
        addCommand(AT_RESET);
    }
//    public void addCommandListEnter(String command)
//    {
//        this.commandListEnter.add(command);
//    }
//    public void addCommandListAll(String command)
//    {
//        this.commandListAll.add(command);
//    }
    public boolean isChatReceive() {
        return chatReceive;
    }

    @Override
    public void setReportAlerting(long reportAlerting) {
        addCommand(AT_REPORT_ALERT_SET+String.valueOf(reportAlerting));
        addCommand(AT_SAVE_CONFIG_SETTINGS);
        addCommand(AT_RESET);
    }

    @Override
    public void setLedBrightness(long ledBrightness) {
        addCommand(AT_LED_BRIGHTNESS_SET+String.valueOf(ledBrightness));
        addCommand(AT_SAVE_SETTINGS);
    }

    @Override
    public void readRSSI() {

    }

    @Override
    public void prepareForMesaaging() {
        getIncommingMessages(true);
    }



    @Override
    public void setAlertOff() {
        addCommand(AT_FFS_ALERT_SET_OFF);
    }

    @Override
    public void getIncommingMessages(boolean allMessage) {
        addCommand(AT_MSG_RECEIVE_DB_SET);
        if(allMessage){
            addCommand(AT_MSG_LIST_MSGS_ALL);
        }
    }



    @Override
    public void getSentMessages() {
        addCommand(AT_MSG_SEND_DB_SET);
        addCommand(AT_MSG_LIST_MSGS_ALL);
    }

    @Override
    public void readMessageAtIndex(String index,boolean fromSent) {
        if(fromSent) {
            addCommand(AT_MSG_SEND_DB_SET);
        }
        else {
            addCommand(AT_MSG_RECEIVE_DB_SET);
        }
        addCommand(AT_MSG_READ);
    }

    @Override
    public void deleteMessageById(String id) {
        // addCommand(AT_MSG_SEND_DB_SET);
        addCommand(AT_MSG_DELETE+id);

    }



    @Override
    public void deleteMessage() {

    }

    @Override
    public void clearManagedObjectContext() {

    }

    @Override
    public void retrieveMessageCode() {

    }

    @Override
    public void messageCode() {

    }

    @Override
    public void clearMessage() {

    }



    @Override
    public void getSerialNumber() {
        addCommand(AT_MODEM_SERIAL_NUMBER);

    }

    @Override
    public void getSignalStrengths() {
        addCommand(AT_MODEM_SIGNAL_QUALITY);
        //readRSSI();
    }

    public boolean isEnter() {
        return isEnter;
    }

    public void setEnter(boolean enter) {
        isEnter = enter;
    }

    @Override
    synchronized public void getGPSFix() {
        addCommand(AT_GPS_REPORT);
    }

    @Override
    public void getAlert() {
        addCommand(AT_FFS_ALERT_GET);
    }

    @Override
    public void setAlertOn() {
        addCommand(AT_FFS_ALERT_SET_ON);
    }

    @Override
    public void getAlertGPSInfo() {

        getGPSFix();
    }

    public void resetCommandList(){
        Log.d("G4Chat","reset command");
        this.commandList.clear();
        Excuted = true;
    }


    public GattClient getMarkClient() {
        return markClient;
    }

    public void setMarkClient(GattClient markClient) {
        this.markClient = markClient;
    }

    public void deleteAllList(){
        this.commandListAll.clear();
        this.commandListEnter.clear();
    }

    public void setClient(GattClient Client){
        this.client = Client;
    }

    public GattClient getClient()
    {
        return this.client;
    }

    public void getName(){
        addCommand("ATI");
    }

    public void updateMessageScreen()
    {
        addCommand("AT$GPMS=0");
        addCommand("AT$GMGL=ALL");
        addCommand("AT$GPMS=1");
        addCommand("AT$GMGL=ALL");
    }

    public void getDeviceFirmware(){
        addCommand("ATI5");
    }
    public void getDeviceIMEI(){
        addCommand("AT$MGSN");
    }


    public boolean isMarkClicked() {
        return MarkClicked;
    }

    public void setMarkClicked(boolean markClicked) {
        MarkClicked = markClicked;
    }

    @Override
    public void addMark() {
        addCommand(AT_FFS_MARK);
    }

    @Override
    public void setupDevice() {
        addCommand(AT_MSG_FORMAT_SET);
        addCommand(AT_MSG_NEW_MSG_INDICATION_SET);
        addCommand(AT_INFO_MODEL);
        addCommand(AT_INFO_REVISION);
        addCommand(AT_MODEM_SERIAL_NUMBER);
        //addCommand(AT_REPORT_NORMAL_GET);
        addCommand(AT_FFS_ALERT_RPT_PERIOD_GET);
        addCommand(AT_LED_BRIGHTNESS_GET);
        addCommand(AT_GPS_REPORT);
    }

    public void getLedBrightness(){
        addCommand(AT_LED_BRIGHTNESS_GET);
    }

    public void getNormalReport(){
        addCommand(AT_REPORT_NORMAL_GET);
    }
    public void getAlertReport(){
        addCommand(AT_REPORT_ALERT_GET);
    }

    @Override
    public void notifyGPSInfo(){
    }

    @Override
    public void receiverCommand()
    {

    }

    @Override
    public void didreadRSSI() {

    }

    public void SendMessage(String message)
    {
        addCommand(AT_MSG_SEND+message);
    }

    public void DeleteMessageById(String ID){

    }

    public void updateListLocal(String msg){



    }


    @Override
    public void addCommand(String command) {
        synchronized (this)
        {

            if(command.equals("\n") || command.equals("\r")|| command.equals("\r\n") || command.equals("\n\r"))
                return;
            //commandListAll.add(command);
            //Log.d("ExcuteCommand", "Command: " + command);
            commandList.add(command);
        }
    }

    public boolean isAcceptExecute() {
        return acceptExecute;
    }

    public void setAcceptExecute(boolean acceptExecute) {
        this.acceptExecute = acceptExecute;
    }

    public boolean isExcuted() {
        return Excuted;
    }

    public void setExcuted(boolean excuted) {
        Excuted = excuted;
    }
    public void executeImmediateCommand(String command) {
        try {
            Log.d("G4Chat1", "@G4Manager:executeImmediateCommand:c: " + command);
            client.writeInteractor(command,false);
            SystemClock.sleep(500);
        }
        catch (Exception e) {
            Log.d("G4Chat1", "@G4Manager:executeImmediateCommand:Error: " + e.toString());
        }
    }

    public void executeImmediateATCommandScreen(String command)
    {
        try {
            Log.d("G4Chat1", "@G4Manager:executeImmediateCommand:c: " + command);
            client.writeInteractor(command,true);
            SystemClock.sleep(500);
        }
        catch (Exception e) {
            Log.d("G4Chat1", "@G4Manager:executeImmediateCommand:Error: " + e.toString());
        }
    }


    public  void  executeCommand(GattClient client){

        if (commandList.size() > 0) {

            if (commandList != null)
            {
                if (!commandList.getFirst().toString().equals("\n"))
                {
                    try {
                        if (Excuted == true) {
                            Excuted = false;
                            try {
                                Log.d("G4Chat", "execute check: " + String.valueOf(Excuted));
                                Log.d("G4Chat", "command: " + commandList.getFirst().toString());
                            }catch (Exception e){}
                            String command = commandList.getFirst();


//                                if (commandList.getFirst().contains("GMGR")) {
//                                    Log.d("G4Chat", "execute Command: " + commandList.getFirst().toString());
////                            for (String s : commandList){
////                                Log.d("G4Chat","ArrayList :"+ s);
////                            }
//                                    int value = Integer.parseInt(commandList.getFirst().replace("AT$GMGR=", "").toString());
//                                    if (idMessageError == value) {
//                                        Log.d("G4Chat", "Return: " + commandList.getFirst().toString());
//                                        return;
//                                    }
//
//                                }
                            client.writeInteractor(command,false);
                            if(command.contains("RST"))
                            {
                                //showDialog();
                                EventBus.getDefault().post(new Events.ShowProgressDialog());
                            }
//                                if (command.contains("GMGR")) {
//                                    idMessageError = Integer.parseInt(command.replace("AT$GMGR=", "").toString());
//                                }
                            SystemClock.sleep(500);


                        } else {
//                            String command = "AT$GPMS=0";
//                            client.writeInteractor(command);
//                            SystemClock.sleep(300);
//                            commandList.removeFirst();
//                            Excuted = true;
//                            executeCommand(client);
                        }
                    } catch (Exception e) {
                        Log.d("G4Chat", "Excuted error: " + e.toString());
                    }

                }

            }

        }

    }



    public void addBuilder(String value)
    {
        synchronized (this)
        {
            if(stringBuider == null)
            {
                this.stringBuider = new StringBuilder(200);
            }

            this.stringBuider.append(value);
        }

    }

    public StringBuilder getStringBuider() {

        synchronized (this) {
            if(stringBuider == null)
            {
                this.stringBuider = new StringBuilder(200);
            }
            return stringBuider;
        }
    }

    public void setStringBuider(StringBuilder stringBuider) {
        synchronized (this) {
            this.stringBuider = stringBuider;
        }
    }

    public void handleMessageScreeen(StringBuilder builder){

    }

    public void setChatReceive(boolean chatReceive) {
        this.chatReceive = chatReceive;
    }



    public Date toDateFormatRecycle(String time)
    {
        Date b =  new Date();
        DateFormat a =  new SimpleDateFormat("EEE MM dd HH:mm:ss Z YYYY",context.getResources().getConfiguration().locale);
        try{
            b = a.parse(time);
        }
        catch (Exception e)
        {

        }
        Log.d("dmm", b.toString());
        return b;

    }

    public Date  toDateFormat(String time){
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Date b = new Date();
        DateFormat a = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        a.setTimeZone(TimeZone.getTimeZone("UTC"));
        try{
            b = a.parse(time);
        }
        catch (Exception e)
        {
            String asd = e.toString();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTime(b);
        TimeZone tz = calendar.getTimeZone();
        int timezone = tz.getRawOffset();

        //calendar.add(Calendar.HOUR, timezone/3600/1000);
        b = calendar.getTime();
        //calendar.add(Calendar.HOUR, -7);

        Date now= new Date();
        calendar = Calendar.getInstance();
        calendar.setTime(now);

        if (b.after(now))
        {
            b = now;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MM dd HH:mm:ss Z YYYY");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));


        if(chatReceive)
        {
            Log.d("dmm","Receive: " + sdf.format(b));
        }
        else{
            Log.d("dmm","Send: " + sdf.format(b));
        }

        return b;
    }
//    public void addListAll(String data){
//        commandListAll.add(data);
//    }
    public void addCommand2List(String data){
//        Log.e("size of child",commandListAll.size()+"");
//        commandListAll.add(data);
//        commandListEnter.add(data);
//        Log.e("size of child",commandListAll.size()+"");
    }
    public void processingData(StringBuilder builder){
        try{

            EventBus.getDefault().post(new Events.SendDataCommand(builder.toString()));
            // listMessageG4.add(builder.toString());
            // Log.d("ExcuteCommand", "data = " +builder.toString());
//        if(commandListAll.size()>500){
//            commandListAll.clear();
//        }

            data = builder.toString();
//        if(data.contains("GPMS") || data.contains("GMGL") || data.contains("GMGR"))
//        {
//          //  handleMessageScreeen(builder);
//          //  return;
//        }
            if(commandList.size() > 0)
            {
                Log.d("G4Chat","Remove First 2");
                this.commandList.removeFirst();

            }
            DeviceName = "G4";
            String firstCharacter;
            try {
                firstCharacter = data.substring(0, 1);
            }
            catch (Exception e)
            {
                return;
            }
            while(firstCharacter.equals("\r") ||  firstCharacter.equals("\n"))
            {
                data = data.substring(1);
                firstCharacter = data.substring(0,1);
            }

            String[] part = data.split("\r\n");



            //check message enter on AT screen
            part[0] = part[0].replaceAll("(\\r|\\n)","");
            String commandSent;
//        if(commandListEnter.size() > 0)
//        {
//            try{
//                commandSent = commandListEnter.get(commandListEnter.size() - 1);
//                commandSent = commandSent.replaceAll("(\\r|\\n)","");
//                if(part[0].equals(commandSent))
//                {
//                    isCommandEntered = true;
//                }
//                if(isCommandEntered == true)
//                {
//                    try
//                    {
//                        isCommandEntered = false;
//                        /*
//                        for(int i = 0; i< part.length; i++)
//                        {
//                            commandListEnter.add(part[i]);
//                        }*/
//                        commandListEnter.add(part[0]);
//                        commandListEnter.add(part[1]);
//                        commandListEnter.add(part[2]);
//                        commandListEnter.add(part[3]);
//                        commandListEnter.add(part[4]);
//                        isEnter = true;
//
//                    }
//                    catch (Exception e)
//                    {
//
//                    }
//                }
//            }
//            catch (Exception e)
//            {
//
//            }
//
//        }
//
//        try{
//
//
//            commandListAll.add(builder.toString());
//            commandListAll.add("\n");
//
//        }
//        catch(ArrayIndexOutOfBoundsException e){
//
//        }

            if(isEnter != true)
            {

            }

            if(part[0].contains("ATI5"))
            {

                for(int i = 1; i< part.length; i++)
                {
                    if(!part[i].contains("ATI5"))
                    {
                        Log.d("IMEI",part[i]);
                        part[i] = part[i].replaceAll("(\\r|\\n)","");
                        part[i] = part[i].replaceAll("OK","");
                        part[i] = part[i].replaceAll(":","");
                        firmware=part[i];
                        Log.d("IMEI",firmware);
                        break;
                    }

                }

                Excuted = true;
                executeCommand(client);
                return;
            }
            try{
                if(part[1].equals("OK") != true && part[2].equals("\n") != true )
                {

                    //commandList.add("\n");
                    //get onlt second line -> b/c it contain the data we need
                    data = data.substring(data.indexOf('\n')+1);
                    part = data.split(":");
                    //get the command we sent
                    commandType = part[0];

                    //Delete the command at the head of the 2nd line
                    data = data.substring(data.indexOf(":") + 1);
                }
                else{
                    commandType = data;
                }
            }
            catch(ArrayIndexOutOfBoundsException e){

            }

            data=data.replaceAll("(\\r|\\n)","");
            data=data.replaceAll("OK","");
            try {
                commandType = commandType.replaceAll("(\\r|\\n)", "");
                commandType = commandType.replaceAll("OK", "");
            }catch (Exception e){}
            String[] tempPart = commandType.split("=");
            if(tempPart[0].equals("AT $LED")||tempPart[0].equals("AT$LED"))
            {
                Led = tempPart[1];
                Led = Led.replaceAll("OK|\r|\n","");
                return;
            }
            tempPart = commandType.split(",");
            tempPart[0] = tempPart[0].replaceAll("OK|\r|\n","");
            if(tempPart[0].equals("AT$GCSET=USER_CFG.TX_TIME") || tempPart[0].equals("AT $GCSET=USER_CFG.TX_TIME") )
            {
                normalReportingTime = tempPart[1];
                normalReportingTime = normalReportingTime.replaceAll("OK|\r|\n","");
                return;
            }

            else if (tempPart[0].equals("AT$GCSET=USER_CFG.ALERT_TX_TIME") || tempPart[0].equals("AT $GCSET=USER_CFG.ALERT_TX_TIME"))
            {
                alertReportingTime = tempPart[1];
                alertReportingTime = alertReportingTime.replaceAll("OK|\r|\n","");
                return;
            }
            String dataCommand = data;
            switch (commandType){

                case "$GPSO":
                    try{
                        part = dataCommand.split(",");
                        time = processDate(part[0]);
                        time = toDateFormat(time).toString();
                        longtiltude = part[1];
                        altiltude = part[2];
                        heading = part[3];
                        speed = part[4];
                        HDOP = part[5];
                        SATS = part[6];
                        valid = part[7];
                        mode = part[8];
                    }
                    catch (Exception e)
                    {
                        longtiltude = null;
                        altiltude = null;
                        time= null;
                        SATS = "0";
                    }

                    break;
                case "$MGSN":
                    Log.d("IMEI",part[1]);

                    part[1] = part[1].replaceAll("(\\r|\\n)","");
                    part[1] = part[1].replaceAll("OK","");
                    part[1] = part[1].replaceAll("$","");
                    part[1] = part[1].replaceAll("AT","");
                    if(part[1].length() >5)
                    {
                        if(part[1].matches("[0-9]+")){
                            IMEI = part[1];
                        }
                        else{
                            //remove all non-digit character
                            IMEI = part[1].replaceAll("[^\\d.]", "");
                        }
                    }



                    Log.d("IMEI",IMEI);
                    break;
                case "$LED":
                    part[1]=part[1].replaceAll(" ","");
                    Led = part[1];
                    Led = Led.replaceAll("OK|\r|\n","");
                    break;
                case "$GPMS":
                    part[1]=part[1].replaceAll(" ","");
                    GPMS=part[1];
                    break;
                case "G4":
                    //DeviceName=data;
                    break;
                case "$MCSQ":
                    try{
                        part[1]=part[1].replaceAll("\r","");
                        part[1]=part[1].replaceAll("\n","");
                        part[1]=part[1].replaceAll("OK","");
                        GPSStrength=part[1];
                    }
                    catch (Exception e)
                    {
                        GPSStrength="0";
                    }
                    break;
                case "ATI5":
                    firmware=data;
                    break;
                case "user_cfg.tx_time":
                    part = dataCommand.split(",");
                    normalReportingTime = part[1];
                    break;
                case "user_cfg.alert_tx_time":
                    part = dataCommand.split(",");
                    alertReportingTime = part[1];
                    break;
                case "$FFALERT":
                    if(data.contains("0"))
                    {
                        alertState = false;
                        Log.d("MarkAlert","Checked Alert, State Alert Off");
                    }
                    else{
                        alertState = true;
                        Log.d("MarkAlert","Checked Alert, State Alert On");
                    }
                    break;
                case "AT$FFALERT=1":
                    Log.d("MarkAlert","Checked Alert, State Alert On");
                    alertState = true;
                    break;
                case "AT$FFALERT=0":
                    alertState = false;
                    Log.d("MarkAlert","Checked Alert, State Alert Off");
                    break;
                case "$FFAEV":
                    if(data.equals(" 0AT$FFALERT=0") || data.equals(" 1AT$FFALERT=0"))
                    {
                        Log.d("MarkAlert","Checked Alert, State Alert Off");
                        alertState = false;
                    }
                    else if (data.equals(" 0AT$FFALERT=1") || data.equals(" 1AT$FFALERT=1"))
                    {
                        Log.d("MarkAlert","Checked Alert, State Alert On");
                        alertState = true;
                    }
                    break;
                default:


                    part = dataCommand.split("=");
                    if(part[0].equals("AT$LED"))
                    {
                        Led = part[1];
                        break;
                    }
                    part = dataCommand.split(",");
                    part[0] = part[0].replaceAll(" ","");

                    switch(part[0])
                    {

                        case "0AT$FFALERT=0":
                            Log.d("MarkAlert","Checked Alert, State Alert Off");
                            alertState = false;

                            break;
                        case "AT$FFALERT=0":
                            Log.d("MarkAlert","Checked Alert, State Alert Off");
                            alertState = false;
                            break;
                        case "1AT$FFALERT=0":
                            Log.d("MarkAlert","Checked Alert, State Alert Off");
                            alertState = false;
                            break;
                        case "0AT$FFALERT=1":
                            Log.d("MarkAlert","Checked Alert, State Alert On");
                            alertState = true;
                            break;
                        case "AT$FFALERT=1":
                            Log.d("MarkAlert","Checked Alert, State Alert On");
                            alertState = true;
                            break;
                        case "1AT$FFALERT=1":
                            Log.d("MarkAlert","Checked Alert, State Alert On");
                            alertState = true;
                            break;
                        case "user_cfg.tx_time":
                            normalReportingTime = part[1];
                            break;
                        case "user_cfg.alert_tx_time":
                            alertReportingTime = part[1];
                            break;
                        case "AT$SCSET=user_cfg.tx_time":
                            normalReportingTime = part[1];
                            break;
                        case "AT$SCSET=user_cfg.alert_tx_time":
                            alertReportingTime = part[1];
                            break;
                        default:
                            if(data.equals("AT&W") == false)
                            {
                                // firmware = data;
                            }
                            break;
                    }

                    break;


            }
            if(dataCommand.contains("DIFC"))
            {

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = sharedPref.edit();
                if(dataCommand.contains("1"))
                {
                    editor.putString("mark_alert_mode","Mark Only");
                    editor.apply();
                }
                else if(dataCommand.contains("2"))
                {
                    editor.putString("mark_alert_mode","Alert Only");
                    editor.apply();
                }
                else if(dataCommand.contains("3"))
                {
                    editor.putString("mark_alert_mode","Combined");
                    editor.apply();
                }
            }
            Excuted = true;
            executeCommand(client);
        }
        catch (Exception e)
        {

        }
    }

    public String processDate(String date)
    {
        String part[];
        date = date.replace("\"", "");
        part = date.split("T");

        String temp = part[0] + " " + part[1];
        part = temp.split("\\.");
        temp = part[0];
        /*
        Date b = new Date();
        DateFormat a = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
        try{
            b = a.parse(temp);
        }
        catch (Exception e)
        {

        }*/
        return temp;
    }

    public boolean isConnectionState() {
        return ConnectionState;
    }

    public void setConnectionState(boolean connectionState) {
        ConnectionState = connectionState;
    }

    public boolean isAlertClicked() {
        return AlertClicked;
    }

    public void setAlertClicked(boolean alertClicked) {
        AlertClicked = alertClicked;
    }
    private int sumMessegaReceive =0;
    private int sumMessegaSend =0;

    @Override
    public void sendMessage(String message) {
        addCommand(AT_MSG_SEND+"\r\n"+message+"\u001A");
    }
    private boolean newMessage = false;
    private boolean doneListReceiver = false;
    private boolean doneListSent = false;

    public boolean isDoneListSent() {
        return doneListSent;
    }

    public void setDoneListSent(boolean doneListSent) {
        this.doneListSent = doneListSent;
    }

    public boolean isDoneListReceiver() {
        return doneListReceiver;
    }

    public void setDoneListReceiver(boolean doneListReceiver) {
        this.doneListReceiver = doneListReceiver;
    }

    public void setIDMessendger(String value, boolean isReceiver)
    {
        DebugLog.d("execute message: "+value);
        //split String
        Log.d("G4Chat1", "@setIDMessendger: " +value);

        String firstCharacter = value.substring(0,1);
        String data = value;
        while(firstCharacter.equals("\r") ||  firstCharacter.equals("\n"))
        {
            data = data.substring(1);
            firstCharacter = data.substring(0,1);
        }
        String[] parts = data.split("\r\n");

        if(isReceiver)
        {
            try {
                if(parts[1].contains("0AT$GPMS")){
                    if(parts[2].contains("0AT$GPMS")) {
                        parts[3].replace("$GPMS:", "");
                        parts = parts[3].split(",");
                        firstIdReceiver = Integer.parseInt(parts[1].toString());
                        Log.d("G4Chat1", "first ID receiver: " + parts[1].toString());
                        lastIdReceiver = Integer.parseInt(parts[2].toString());
                    }else {
                        parts[2].replace("$GPMS:", "");
                        parts = parts[2].split(",");
                        firstIdReceiver = Integer.parseInt(parts[1].toString());
                        Log.d("G4Chat1", "first ID receiver: " + parts[1].toString());
                        lastIdReceiver = Integer.parseInt(parts[2].toString());
                    }
                }else {
                    parts[1].replace("$GPMS:", "");
                    parts = parts[1].split(",");
                    firstIdReceiver = Integer.parseInt(parts[1].toString());
                    Log.d("G4Chat1", "first ID receiver: " + parts[1].toString());
                    lastIdReceiver = Integer.parseInt(parts[2].toString());
                    Log.d("G4Chat1", "last ID receiver: " + parts[2].toString());
                }
                SharedPreferences getLastIDReceive = context.getSharedPreferences("G4Chat", 0);
                int getStoreLastIDReiceve = getLastIDReceive.getInt("lastIdReceiver", 0);
                //check new Message
                if (getStoreLastIDReiceve > 0 && lastIdReceiver > getStoreLastIDReiceve ) {
                    //show icon new Message
                    EventBus.getDefault().postSticky(new Events.ChatStatus(true));
                    Log.d("G4Chat1", "new Messenger :" + getStoreLastIDReiceve+1);
                    if (listG4MessagesReceive.size() >0 )
                    {
                        // if list receiver get done, get ID new Message
                        if(doneListReceiver) {
                          //  if (isChatScreen) {
                                Log.d("G4Chat1", "Execute new command :, list done");
                                Log.d("G4Chat1","list receive size :"+listG4MessagesReceive.size());
                                sumMessegaReceive = lastIdReceiver - firstIdReceiver +1;
                                newMessage = true;
                                DebugLog.d("execute message: ");
                                executeMessenger(client, listG4MessagesReceive.get(listG4MessagesReceive.size() - 1).getID()+1, true,null);
                         //   }
                        }else {
                            Log.d("G4Chat1", "Execute new command :, list not done");
                            Log.d("G4Chat1","list receive size :"+listG4MessagesReceive.size());
                            // if list receiver not done, get ID store message
                            if (listG4MessagesReceive.get(listG4MessagesReceive.size() - 1).getID() < lastIdReceiver &&isChatScreen) {
                                //listG4MessagesReceive.clear();
                                DebugLog.d("execute message: ");
                                newMessage = true;
                                sumMessegaReceive = lastIdReceiver - firstIdReceiver +1;
                                executeMessenger(client,listG4MessagesReceive.get(listG4MessagesReceive.size() - 1).getID()+1, true,null);
                            }
                        }
                    }else {
                        //if list = 0, execute first message
                        if (isChatScreen) {
                            sumMessegaReceive = lastIdReceiver - firstIdReceiver +1;
                            executeMessenger(client, firstIdReceiver, true,null);
                        }
                    }
                } else {
                    Log.d("G4Chat1", "store Last ID Receive: "+ getStoreLastIDReiceve);
                    // check list size receive > 0
                    if(listG4MessagesReceive.size()>0) {
                        //if list out of limit , refresh list
                        if(listG4MessagesReceive.size()>20){
                            Log.d("G4Chat1", "list size > 20 ");
                            if(listG4MessagesSent.size()>0) {
                              //  listG4MessagesReceive.clear();
                                for(int i = listG4MessagesReceive.get(0).getID() ; i < firstIdReceiver;i++)
                                {
                                    listG4MessagesReceive.remove(0);
                                }
                                getClient().writeInteractor("AT$GPMS=1",false);
                            }
//                            if(isChatScreen) {
//                                sumMessegaReceive = lastIdReceiver - firstIdReceiver +1;
//                                executeMessenger(client, firstIdReceiver, true,null);
//                            }
                        }
                        else {

                            if(acceptExecute) {
                                // if list has full, execute list send
                                if(doneListReceiver) {
                                  //  getClient().writeInteractor("AT$GMGL=STO UNSENT",false);
                                    if (isChatScreen)// && hasUnsent || listG4MessagesSent.size()==0) {
                                    {
                                        getClient().writeInteractor("AT$GPMS=1",false);
                                        Log.d("G4Chat1", "Execute send command :");
                                    }
                                }else {
                                    // if list not full, get last ID message
                                    if (listG4MessagesReceive.get(listG4MessagesReceive.size() - 1).getID() < lastIdReceiver &&isChatScreen) {
                                        Log.d("G4Chat1", "Execute first receive command :");
                                        //listG4MessagesReceive.clear();
                                        sumMessegaReceive = lastIdReceiver - firstIdReceiver +1;
                                        executeMessenger(client,listG4MessagesReceive.get(listG4MessagesReceive.size() - 1).getID()+1, true,null);
                                    }
                                }
                            }
                        }
                    }
                    //if list size = 0, get First ID message
                    else if(listG4MessagesReceive.size() == 0)
                    {
                        if(isChatScreen) {
                            Log.d("G4Chat1", "list size = 0 ");
                            Log.d("G4Chat1", "Execute first receive command :");
                            sumMessegaReceive = lastIdReceiver - firstIdReceiver +1;
                            executeMessenger(client, firstIdReceiver, true,null);
                        }
                    }
                }
                //store last ID receive
                SharedPreferences.Editor saveLastIDReiceve = context.getSharedPreferences("G4Chat", 0).edit();
                saveLastIDReiceve.putInt("lastIdReceiver", lastIdReceiver);
                saveLastIDReiceve.apply();
            }catch (Exception e){
                Log.d("G4Chat1", "set ID Receiver Error:" +e.toString() );
                if(isChatScreen())
                {
                    try{
                        //Receive all False message
                        //If not, Some Random OK will go in and break the code
                        //  EventBus.getDefault().post(new Events.RefreshChat());

                        //  EventBus.getDefault().post(new Events.RefreshChat());
                    }
                    catch (Exception a)
                    {
                    }

                }
            }
        }
        else {
            try {
                doneListSent = false;
                if (parts[1].contains("1AT$GPMS=1") || parts[1].contains("AT$GPMS=1") && parts[2].contains("$GPMS")) {
                    parts[2].replace("$GPMS:", "");
                    parts = parts[2].split(",");
                    firstIdSent = Integer.parseInt(parts[1].toString());
                    Log.d("G4Chat1", " first ID  : " + parts[1].toString());
                    lastIdSent = Integer.parseInt(parts[2].toString());
                    Log.d("G4Chat1", "last ID  : " + parts[2].toString());
                } else if (parts[2].toString().contains("1AT$GPMS=1")) {
                    parts[3].replace("$GPMS:", "");
                    parts = parts[3].split(",");
                    firstIdSent = Integer.parseInt(parts[1].toString());
                    Log.d("G4Chat1", "first ID sent  : " + parts[1].toString());
                    lastIdSent = Integer.parseInt(parts[2].toString());
                    Log.d("G4Chat1", "last ID sent  : " + parts[2].toString());
                } else {
                    parts[1].replace("$GPMS:", "");
                    parts = parts[1].split(",");
                    firstIdSent = Integer.parseInt(parts[1].toString());
                    Log.d("G4Chat1", "first ID sent: " + parts[1].toString());
                    lastIdSent = Integer.parseInt(parts[2].toString());
                    Log.d("G4Chat1", "last ID sent: " + parts[2].toString());
                }
                if(listG4MessagesSent.size() > 20){
                  //  listG4MessagesSent.clear();
                    for(int i = listG4MessagesSent.get(0).getID() ; i < firstIdSent;i++)
                    {
                        listG4MessagesSent.remove(0);
                    }
                    //EventBus.getDefault().post(new Events.UpdateListChat());
                    //EventBus.getDefault().post(new Events.UpdateList());
                    ////
                    executeMessenger(getClient(),firstIdSent, false, null);
                   // return;
                }
                if (listG4MessagesSent.size() > 0 && listG4MessagesSent.get(listG4MessagesSent.size() - 1).getID() < lastIdSent) {
                    executeMessenger(getClient(), (listG4MessagesSent.get(listG4MessagesSent.size() - 1).getID()) + 1, false, null);
                    return;
                }
                listG4MessagesSent.clear();
                sumMessegaSend = lastIdSent - firstIdSent + 1;
                Log.d("G4Chat1", "execute Send command: ");
                executeMessenger(client, firstIdSent, false, null);
            }catch (Exception e){

            }
        }
    }
    public void detectNewMessage(String value){
        Log.d("G4Chat1", "@detectNewMessage: Enter" +value);
        String firstCharacter = value.substring(0,1);
        String data = value;
        while(firstCharacter.equals("\r") ||  firstCharacter.equals("\n"))
        {
            data = data.substring(1);
            firstCharacter = data.substring(0,1);
        }
        String[] parts = data.split("\r\n");
        try {
            if(parts[1].contains("0AT$GPMS")){
                if(parts[2].contains("0AT$GPMS")) {
                    parts[3].replace("$GPMS:", "");
                    parts = parts[3].split(",");
                    firstIdReceiver = Integer.parseInt(parts[1].toString());
                    Log.d("G4Chat1", "@detecNewMessage first ID receiver: " + parts[1].toString());
                    lastIdReceiver = Integer.parseInt(parts[2].toString());
                    Log.d("G4Chat1", "@detecNewMessage last ID receiver: " + parts[2].toString());
                }else {
                    parts[2].replace("$GPMS:", "");
                    parts = parts[2].split(",");
                    firstIdReceiver = Integer.parseInt(parts[1].toString());
                    Log.d("G4Chat1", "@detecNewMessage first ID receiver: " + parts[1].toString());
                    lastIdReceiver = Integer.parseInt(parts[2].toString());
                    Log.d("G4Chat1", "@detecNewMessage last ID receiver: " + parts[2].toString());
                }
            }else {
                parts[1].replace("$GPMS:", "");
                parts = parts[1].split(",");
                firstIdReceiver = Integer.parseInt(parts[1].toString());
                Log.d("G4Chat1", "@detecNewMessage first ID receiver: " + parts[1].toString());
                lastIdReceiver = Integer.parseInt(parts[2].toString());
                Log.d("G4Chat1", "@detecNewMessage last ID receiver: " + parts[2].toString());
            }
            SharedPreferences getLastIDReceive = context.getSharedPreferences("G4Chat", 0);
            int getStoreLastIDReiceve = getLastIDReceive.getInt("lastIdReceiver", 0);
            if (getStoreLastIDReiceve > 0 && lastIdReceiver > getStoreLastIDReiceve ) {
                EventBus.getDefault().postSticky(new Events.ChatStatus(true));
                DebugLog.d("new message"+value);
            }
            else {
            }
        }catch (Exception e){

            Log.d("G4Chat1", "detect ID Receiver Error:" +e.toString() );
        }
        EventBus.getDefault().post(new Events.RefreshList());
    }

    public  void  executeMessenger(final GattClient client, final int idMessenger, final boolean isReiceiver, final String messageSend){
        if(messageSend==null) {
            Log.d("G4Chat1", "set accept: " + acceptExecute);
            if (acceptExecute) {
                client.writeInteractor("AT$GMGR=" + String.valueOf(idMessenger),false);
                Log.d("G4Chat1", "Send Command AT$GMGR=" + idMessenger);
            } else {
                Log.d("G4Chat1", "Stopped command");
                return;
            }
        }else {
            getClient().writeInteractor("AT$GMGS="+"\r\n"+messageSend+"\u001A",false);
           // setAcceptExecute(false);
        }
        final StringBuilder sb = new StringBuilder(192);
        client.setListener(new GattClient.OnCounterReadListener() {
            @Override
            public void onCounterRead(String value) {

                if (sb.toString().contains("ERROR")) {
                    Log.d("G4Chat1", "ERROR Message");
                    // check list for add error message
                    if (doneListReceiver) {
                        G4Messages g4Messages = new G4Messages();
                        g4Messages.setBody("Cannot read this message");
                       // listG4MessagesReceive.add(g4Messages);
                        //executeMessenger(getClient(), idMessenger + 1, false,null);
                    } else {
                        G4Messages g4Messages = new G4Messages();
                        g4Messages.setBody("Cannot read this message");
                      //  listG4MessagesSent.add(g4Messages);
                       // executeMessenger(getClient(), idMessenger + 1, true,null);
                    }
                }
                Log.d("G4Chat1", "@onCounterRead -G4Manager Data Receive "+sb.toString());

                if(sb.length() < 30)
                {
                    if(!value.contains("K\r\n"))
                    {
                        sb.append(value);
                    }
                    else{
                        if(sb.toString().contains("GMGS")){
                            sb.append(value);
                        }
                        //this is message with 2 OK
                        else if (value.toString().contains("OK")) {
                            Log.d("G4Chat1", "Continues.....");
                            sb.append(value);
                            //    executeMessenger(getClient(), idMessenger + 1, true, null);
                        }
                    }

                }
                else if(sb.length() == 0)
                {
                    if(value.contains("AT$GMGR"))
                    {
                        sb.append(value);
                    }
                }
                else{
                    if (!sb.toString().contains(value)) {
                        Log.d("G4Chat1", "Append: "+ value);
                        sb.append(value);

                    }
                }

//                if(value.contains("K\r\n"))
//                {
//                    if(sb.toString().contains("K\r\n"))
//                    {
//                        sb.append(value);
//                    }
//                }
                if (sb.toString().contains("OK")) {
                    if (isReiceiver) {
                        if (sb.toString().contains("GMGR")) {
                            DebugLog.d("Value return G4: "+ value);
                            Log.d("G4Chat1", "@onCounterRead -G4Manager Data with OK" + sb.toString());

                            String firstCharacter = sb.substring(0, 1);
                            String data = sb.toString();
                            sb.setLength(0);
                            while (firstCharacter.equals("\r") || firstCharacter.equals("\n")) {

                                data = data.substring(1);
                                firstCharacter = data.substring(0, 1);
                            }
                            String[] parts = data.split("\r\n");
                            if(parts[2].toString().contains("$GMGR")){
                                parts[1].replace("\r\n", "");
                                parts[2].replace("\r\n", "");
                                G4Messages g4Messages = new G4Messages();
                                g4Messages.setBody(parts[3]);
                                //for (int i = 1; i < parts.length - 2; i = i + 2) {
                                parts = parts[2].split(",");
                                //}
                                parts[0] = parts[0].replace("\n ", "");
                                parts[0] = parts[0].replace("\r: ", "");
                                //parts[0] = parts[0].replace("AT$GMGR: ","");
                                parts[0] = parts[0].replace("$GMGR: ", "");
                                g4Messages.setID(Integer.parseInt(parts[0]));
                                g4Messages.setDate(toDateFormat(processDate(parts[3])));
                                g4Messages.setStatus(parts[1]);
                                listG4MessagesReceive.add(g4Messages);
                                EventBus.getDefault().post(new Events.UpdateList());
                                DebugLog.d("update list: "+g4Messages.getBody().toString());

                            }else {
                                //receive message
                                parts[1].replace("\r\n", "");
                                parts[2].replace("\r\n", "");
                                G4Messages g4Messages = new G4Messages();
                                g4Messages.setBody(parts[2]);
                                //for (int i = 1; i < parts.length - 2; i = i + 2) {
                                parts = parts[1].split(",");
                                //}
                                parts[0] = parts[0].replace("\n ", "");
                                parts[0] = parts[0].replace("\r: ", "");
                                //parts[0] = parts[0].replace("AT$GMGR: ","");
                                parts[0] = parts[0].replace("$GMGR: ", "");
                                g4Messages.setID(Integer.parseInt(parts[0]));
                                g4Messages.setDate(toDateFormat(processDate(parts[3])));
                                g4Messages.setStatus(parts[1]);
                                listG4MessagesReceive.add(g4Messages);
                                EventBus.getDefault().post(new Events.UpdateList());
                                DebugLog.d("update list: "+g4Messages.getBody().toString());


                            }
                            float percent = (float) (listG4MessagesReceive.size()) / (sumMessegaReceive) * 50;
                            Log.d("G4Chat1", "size :" + listG4MessagesReceive.size() +" sum :" +sumMessegaReceive +" percent:" +percent );
                            EventBus.getDefault().post(new Events.UpdateLoading(String.valueOf(Math.round(percent)) + "%"));
                            //list incomplete , continues get ID
                            if (idMessenger < lastIdReceiver) {
                                executeMessenger(getClient(), idMessenger + 1, true,null);
                            }
                            //    Log.d("G4Chat1", "id messenger: " + idMessenger + lastIdReceiver);
                            //list comple
                            if (idMessenger == lastIdReceiver) {
//                                for (G4Messages g4 : listG4MessagesReceive) {
//                                    Log.d("G4Chat1", "Body Reiceive :"+g4.getID() + g4.getBody() + "\n");
//                                }
                                doneListReceiver = true;
                                if (isChatScreen) {
                                    //turn off icon message
                                    EventBus.getDefault().postSticky(new Events.ChatStatus(false));
                                }
                                if (newMessage && listG4MessagesSent.size()>0) {
                                    for (G4Messages g4 : listG4MessagesReceive) {
                                    Log.d("G4Chat1", "Body Reiceive :"+g4.getID() + g4.getBody() + "\n");
                                     }
                                    EventBus.getDefault().post(new Events.UpdateListChat());
                                    EventBus.getDefault().post(new Events.UpdateList());
                                    Log.d("G4Chat1", "update new messenger: " + idMessenger);
                                    newMessage = false;
                                }
                                if (acceptExecute) {
                                    //get list send
                                    client.writeInteractor("AT$GPMS=1",false);
                                }
                                // if detect new message, show UI

                            } else if (idMessenger > lastIdReceiver && listG4MessagesReceive.size() > 20) {
//                                        Log.d("G4Chat1", "get new Messenger :" );
//                                        listG4MessagesReceive.remove(0);
//                                        EventBus.getDefault().post(new Events.UpdateListChat());
                            }
                        }
                    } else {
                        if (sb.toString().contains("GMGR")) {

                            Log.d("G4Chat1", "Data with OK" + sb.toString());
                            //   Log.d("G4Chat1", "Refresh count");
                            //   count = 0;
                            String firstCharacter = sb.substring(0, 1);
                            String data = sb.toString();
                            sb.setLength(0);
                            while (firstCharacter.equals("\r") || firstCharacter.equals("\n")) {

                                data = data.substring(1);
                                firstCharacter = data.substring(0, 1);
                            }
                            String[] parts = data.split("\r\n");
                            if(parts[2].toString().contains("$GMGR"))
                            {
                                parts[1].replace("\r\n", "");
                                parts[2].replace("\r\n", "");
                                G4Messages g4Messages = new G4Messages();
                                g4Messages.setBody(parts[3]);
                                //for (int i = 1; i < parts.length - 2; i = i + 2) {
                                parts = parts[2].split(",");
                                //}
                                parts[0] = parts[0].replace("\n ", "");
                                parts[0] = parts[0].replace("\r: ", "");
                                //parts[0] = parts[0].replace("AT$GMGR: ","");
                                parts[0] = parts[0].replace("$GMGR: ", "");
                                g4Messages.setID(Integer.parseInt(parts[0]));
                                g4Messages.setDate(toDateFormat(processDate(parts[3])));
                                g4Messages.setStatus(parts[1]);
                                listG4MessagesSent.add(g4Messages);
                            }
                            else {
                                parts[1].replace("\r\n", "");
                                parts[2].replace("\r\n", "");
                                G4Messages g4Messages = new G4Messages();

                                g4Messages.setBody(parts[2]);
                                //for (int i = 1; i < parts.length - 2; i = i + 2) {
                                parts = parts[1].split(",");
                                //}
                                parts[0] = parts[0].replace("\n ", "");
                                parts[0] = parts[0].replace("\r: ", "");
                                //parts[0] = parts[0].replace("AT$GMGR: ","");
                                parts[0] = parts[0].replace("$GMGR: ", "");
                                g4Messages.setID(Integer.parseInt(parts[0]));
                                g4Messages.setDate(toDateFormat(processDate(parts[3])));
                                g4Messages.setStatus(parts[1]);
                                listG4MessagesSent.add(g4Messages);
                            }

                            float percent = (float) (listG4MessagesReceive.size() + listG4MessagesSent.size()) / (sumMessegaSend + sumMessegaReceive) * 100;
                            Log.d("G4Chat1", "percent :" + percent + "sizeSent: " + listG4MessagesSent.size() + "size: " + listG4MessagesReceive.size());
                            Log.d("G4Chat1", "sumReceive :" + sumMessegaReceive + " sumSent: " + sumMessegaSend);
                            EventBus.getDefault().post(new Events.UpdateLoading(String.valueOf(Math.round(percent)) + "%"));
                            //
                            if (idMessenger < lastIdSent) {
                                //get ID incomplete
                                executeMessenger(getClient(), idMessenger + 1, false,null);
                            } else if (idMessenger == lastIdSent) {
                                //get ID complete
                                EventBus.getDefault().post(new Events.UpdateListChat());
                                EventBus.getDefault().post(new Events.UpdateList());
                                doneListSent = true;
                            }
                        }
                    }
                    if (sb.toString().contains("GPMS=1")) {
                        // count = 0;
                        Log.d("G4Chat1", "GPMS=1 DATA:" + sb.toString());
                        setIDMessendger(sb.toString(), false);
                        sb.setLength(0);
                    }
                    if (sb.toString().contains("GPMS=0")) {
                        //   count = 0;
                        Log.d("G4Chat1", " GPMS=0 DATA :" + sb.toString());
                        setIDMessendger(sb.toString(), true);
                        sb.setLength(0);
                    }
                    if(sb.toString().contains("GMGS=")) {
                        setAcceptExecute(true);
                        Log.d("G4Chat1", "set accept: "+isAcceptExecute());
                        Log.d("G4Chat1", "add message send to list : "+sb.toString());
                        String[] parts = sb.toString().split("\r\n");
                        parts[2] = parts[2].replace("$GMGS: ","");
                        int idSent = Integer.parseInt(parts[2].toString());
                        Log.d("G4Chat1", "id Sent button : "+ idSent);
                        Log.d("G4Chat1", "get last ID sent : "+ getLastIdSent());
                        try {
                            if (idSent > getLastIdSent()) {
                                G4Messages g4Message = new G4Messages();
                                g4Message.setBody(messageSend);
                                g4Message.setID(idSent);
                                setLastIdSent(getLastIdSent()+1);
                                g4Message.setStatus("UNSENT");
                                Date now= new Date();
                                final Calendar calendar = Calendar.getInstance();
                                calendar.setTime(now);
                                g4Message.setDate(calendar.getTime());
                                listG4MessagesSent.add(g4Message);
                                //   EventBus.getDefault().post(new Events.UpdateListChat());
                            } else {
                                Log.d("G4Chat1", "Double messenger :" + idSent);
                            }
                        }catch (Exception e){
                           // Log.d("G4Chat1", "Double messenger error :" + idSent);
                            failQueue.add(messageSend);
                        }
                    }
                    else if(sb.toString().contains("$GMGL=ALL")){

                    }
                }
            }
            @Override
            public void onConnected(boolean success) {

            }

            @Override
            public void onRSSIChange(int rssi) {

            }
        });
    }
    public void addListMessage(String data){
        String[] parts = data.toString().split("$");
        for(String list:parts){
            if(list.contains("REC")){
                String[] partsList = list.split(",");
                G4Messages g4Message = new G4Messages();
//                                g4Message.setID();
//                                g4Message.setBody();
//                                g4Message.setDate();
//                                g4Message.setStatus();
            }else {
                String[] partsList = list.split(",");
                G4Messages g4Message = new G4Messages();
//                                g4Message.setID();
//                                g4Message.setBody();
//                                g4Message.setDate();
//                                g4Message.setStatus();
            }
        }
    }
    public void setMark(boolean mMark){
        isMarkOn=mMark;
    }
    public boolean getMark(){
        return isMarkOn;
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


        //
        dest.writeSerializable(listG4MessagesAll);
        dest.writeSerializable(listG4MessagesReceive);
        dest.writeSerializable(listG4MessagesSent);
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
        dest.writeString(address);
        dest.writeString(firmware);
        dest.writeByte((byte) (alertState ? 1 : 0));
        dest.writeByte((byte) (Excuted ? 1 : 0));
        dest.writeByte((byte) (AlertClicked ? 1 : 0));
        dest.writeByte((byte) (ConnectionState ? 1 : 0));
        dest.writeByte((byte) (isCommandEntered ? 1 : 0));
        dest.writeByte((byte) (chatReceive ? 1 : 0));
        dest.writeByte((byte) (isEnter ? 1 : 0));
        dest.writeByte((byte) (isStatusScreen ? 1 : 0));
        dest.writeByte((byte) (isChatScreen ? 1 : 0));
        dest.writeByte((byte) (isChatRefreshRun ? 1 : 0));
        dest.writeByte((byte) (isMarkAlertScreen ? 1 : 0));
        dest.writeByte((byte) (acceptExecute ? 1 : 0));
        dest.writeByte((byte) (isFromConfigScreen ? 1 : 0));
        dest.writeByte((byte) (isFromATScreen ? 1 : 0));
        dest.writeByte((byte) (isPastAlertState ? 1 : 0));
    }
    private G4Manager(Parcel in){
        data = in.readString();
        commandListEnter = (ArrayList<ListATCommand>) in.readSerializable();
        commandListAll = (ArrayList<ListATCommand>) in.readSerializable();
        commandList = (LinkedList<String>) in.readSerializable();

        //
        listG4MessagesAll = (ArrayList<G4Messages>) in.readSerializable();
        listG4MessagesReceive = (ArrayList<G4Messages>) in.readSerializable();
        listG4MessagesSent = (ArrayList<G4Messages>) in.readSerializable();
        isPastAlertState= in.readByte() != 0;
        isFromConfigScreen = in.readByte() != 0;
        AlertClicked = in.readByte() != 0;
        isChatRefreshRun = in.readByte() != 0;
        isFromATScreen = in.readByte() != 0;
        time = in.readString();
        isStatusScreen = in.readByte() != 0;
        isChatScreen = in.readByte() != 0;
        alertState = in.readByte() != 0;
        Excuted = in.readByte() != 0;
        ConnectionState = in.readByte() != 0;
        isCommandEntered = in.readByte() != 0;
        chatReceive = in.readByte() != 0;
        isEnter = in.readByte() != 0;
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
        //ssssssd
        firmware = in.readString();
        address = in.readString();
        isMarkAlertScreen = in.readByte() != 0;
        acceptExecute = in.readByte() != 0;

    }

    public String getTime() {
        try{
            String part[] = time.split(" ");
            time = part[3] + " " +part[4];
            return time;
        }
        catch (Exception e)
        {
            return  time;
        }

    }

    public String getLongtiltude() {
        return longtiltude;
    }

    public String getAltiltude() {
        return altiltude;
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

    public String getFirmware() {
        return firmware;
    }

//    public ArrayList<String> getCommandListEnter() {
//        return commandListEnter;
//    }

//    public void addCommandSentEnter(String command)
//    {
//        commandListEnter.add(command);
//    }
//    public void addCommandSentAll(String command)
//    {
//        commandListAll.add(command);
//    }
//
//    public ArrayList<String> getCommandListAll() {
//        return commandListAll;
//    }
public void updateCommandList(String data){
    commandListAll.remove(commandListAll.size()-2);
    commandListEnter.remove(commandListEnter.size()-2);
}
    public LinkedList<String> getCommandList() {
        return commandList;
    }

    public static final Parcelable.Creator<G4Manager> CREATOR = new Parcelable.Creator<G4Manager>(){
        @Override
        public G4Manager createFromParcel(Parcel source) {
            return new G4Manager(source);
        }

        @Override
        public G4Manager[] newArray(int size) {
            return new G4Manager[size];
        }
    };

    public boolean isMarkAlertScreen() {
        return isMarkAlertScreen;
    }

    public void setMarkAlertScreen(boolean markAlertScreen) {
        isMarkAlertScreen = markAlertScreen;
    }
}
