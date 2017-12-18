package com.xzfg.app;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import com.xzfg.app.model.AgentContacts;
import com.xzfg.app.model.AgentProfile;
import com.xzfg.app.model.AgentRoles;
import com.xzfg.app.model.AgentSettings;
import com.xzfg.app.model.AlertContent;
import com.xzfg.app.model.AlertHeader;
import com.xzfg.app.model.CannedMessages;
import com.xzfg.app.model.Media;
import com.xzfg.app.model.Message;
import com.xzfg.app.model.PoiGroup;
import com.xzfg.app.model.ScannedSettings;
import com.xzfg.app.model.SendableMessage;
import com.xzfg.app.model.User;
import com.xzfg.app.model.weather.WeatherData;

import java.util.Calendar;
import java.util.List;
import java.util.Map;


/**
 * A container for event classes, for use with EventBus.
 */
@SuppressWarnings("unused")
public final class Events {

    private Events() {
    }

    /**
     * Used to pass audio recording status.
     */
    public static final class AudioStatus {
        private final boolean recording;
        private final boolean streaming;

        public AudioStatus(boolean recording, boolean streaming) {
            this.recording = recording;
            this.streaming = streaming;
        }

        public boolean isRecording() {
            return recording;
        }

        public boolean isStreaming() {
            return streaming;
        }
    }

    /**
     * Used to pass video recording status.
     */
    public static final class VideoStatus {
        private final boolean recording;
        private final boolean streaming;

        public VideoStatus(boolean recording, boolean streaming) {
            this.recording = recording;
            this.streaming = streaming;
        }

        public boolean isRecording() {
            return recording;
        }

        public boolean isStreaming() {
            return streaming;
        }
    }


    /**
     * Used to pass live tracking status.
     */
    public static final class TrackingStatus {
        private final boolean tracking;

        public TrackingStatus(boolean tracking) {
            this.tracking = tracking;
        }

        public boolean isTracking() {
            return this.tracking;
        }
    }
    public static final class TrackingConnect {
    }
    public static final class SendData {
        private final String data;

        public String getData() {
            return data;
        }

        public SendData(String data) {

            this.data = data;
        }
    }
    public static final class SendRssi {
        private final int rssi;

        public int getRssi() {
            return rssi;
        }

        public SendRssi(int rssi) {
            this.rssi = rssi;
        }
    }
    public static final class SendConnectionState {
        private final boolean isConnected;

        public boolean isConnected() {
            return isConnected;
        }

        public SendConnectionState(boolean isConnected) {

            this.isConnected = isConnected;
        }



    }
    /**
     * Used to pass chat status.
     */
    public static final class ChatStatus {
        private final boolean chats;

        public ChatStatus(boolean chats) {
            this.chats = chats;
        }

        public boolean hasChats() {
            return chats;
        }
    }
    public static final class AutoConnect {
        private final boolean connect;


        public AutoConnect(boolean connect) {
            this.connect = connect;
        }

        public boolean isConnect() {
            return connect;
        }
    }
    /**
     * check status receive message.
     */
    public static final class ReceiveMessage {
        private final boolean isReceive;

        public ReceiveMessage(boolean isReceive) {
            this.isReceive = isReceive;
        }

        public boolean isReceive() {
            return isReceive;
        }
    }

    /**
     * Used to pass file status
     */
    public static final class FileStatus {
        private final int count;

        public FileStatus(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * Used to pass boss mode status.
     */
    public static final class BossModeStatus {
        private final String status;

        public BossModeStatus(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }

    public static final class NetworkAvailable {
        public NetworkAvailable() {
        }
    }
    public static final class RefreshList {
        public RefreshList() {
        }
    }

    /**
     * Used to pass registration status.
     */
    public static final class Registration {
        private final boolean status;
        private final String message;
        private final AgentSettings agentSettings;
        private final ScannedSettings scannedSettings;

        public Registration(boolean status, String message) {
            this.status = status;
            this.message = message;
            this.agentSettings = null;
            this.scannedSettings = null;
        }

        public Registration(
                boolean status,
                String message,
                AgentSettings agentSettings,
                ScannedSettings scannedSettings
        ) {
            this.status = status;
            this.message = message;
            this.agentSettings = agentSettings;
            this.scannedSettings = scannedSettings;
        }

        public boolean getStatus() {
            return this.status;
        }

        public String getMessage() {
            return message;
        }

        public AgentSettings getAgentSettings() {
            return agentSettings;
        }

        public ScannedSettings getScannedSettings() {
            return scannedSettings;
        }
    }

    /**
     * Used to pass SSL error message.
     */
    public static final class SSLHandshakeError {
        private final String message;

        public SSLHandshakeError(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class NetworkStatus {
        private final boolean up;

        public NetworkStatus(boolean networkStatus) {
            this.up = networkStatus;
        }

        public boolean isUp() {
            return up;
        }
    }

    public static final class ScanSuccess {
        private final ScannedSettings scannedSettings;

        public ScanSuccess(ScannedSettings scannedSettings) {
            this.scannedSettings = scannedSettings;
        }

        public ScannedSettings getScannedSettings() {
            return scannedSettings;
        }
    }

    public static final class AgentSettingsAcquired {
        private final AgentSettings agentSettings;

        public AgentSettingsAcquired(AgentSettings agentSettings) {
            this.agentSettings = agentSettings;
        }

        public AgentSettings getAgentSettings() {
            return agentSettings;
        }
    }

    public static final class AgentProfileAcquired {
        private final AgentProfile agentProfile;

        public AgentProfileAcquired(AgentProfile agentProfile) {
            this.agentProfile = agentProfile;
        }

        public AgentProfile getAgentProfile() {
            return agentProfile;
        }
    }

    public static final class AgentContactsAcquired {
        private final AgentContacts agentContacts;

        public AgentContactsAcquired(AgentContacts agentContacts) {
            this.agentContacts = agentContacts;
        }

        public AgentContacts getAgentContacts() {
            return agentContacts;
        }
    }

    public static final class CannedMessagesAcquired {
        private final CannedMessages messages;

        public CannedMessagesAcquired(CannedMessages messages) {
            this.messages = messages;
        }

        public CannedMessages getMessages() {
            return messages;
        }
    }

    public static final class DisplayChanged {
        private final int id;
        private final String name;

        public DisplayChanged(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static final class MenuItemSelected {
        public final String name;
        public final int id;

        public MenuItemSelected(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }
    }

    public static final class ScreenOff {
        public ScreenOff() {
        }
    }

    public static final class Session {
        private final String sessionId;

        public Session(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getSessionId() {
            return sessionId;
        }
    }

    public static final class SessionAcquired {
        private final String sessionId;

        public SessionAcquired(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getSessionId() {
            return sessionId;
        }
    }

    public static final class ContactsLoaded {
        private final List<User> contacts;

        public ContactsLoaded(List<User> contacts) {
            this.contacts = contacts;
        }

        public List<User> getContacts() {
            return contacts;
        }
    }

    public static final class ChatDataUpdated {
        public ChatDataUpdated() {
        }
    }

    public static final class StartChat {
        private final User contact;

        public StartChat(User selectedContact) {
            this.contact = selectedContact;
        }

        public User getContact() {
            return contact;
        }
    }

    public static final class NewChatMessages {
        private final Map<String, List<Message>> newMap;

        public NewChatMessages(Map<String, List<Message>> newMap) {
            this.newMap = newMap;
        }

        public List<Message> getMessages(String username) {
            return newMap.get(username);
        }
    }

    public static final class ChatMessageSent {
        private final SendableMessage sendableMessage;

        public ChatMessageSent(SendableMessage sendableMessage) {
            this.sendableMessage = sendableMessage;
        }

        public SendableMessage getSendableMessage() {
            return sendableMessage;
        }
    }

    public static final class KeyBoardShown {
        private final boolean keyboardShown;

        public KeyBoardShown(boolean keyboardShown) {
            this.keyboardShown = keyboardShown;
        }

        public boolean isKeyboardShown() {
            return keyboardShown;
        }
    }

    public static final class ChatsCleared {
        private final String userId;

        public ChatsCleared(String id) {
            this.userId = id;
        }

        public String getUserId() {
            return userId;
        }
    }

    public static final class VideoServiceAvailable {
        private final boolean available;

        public VideoServiceAvailable(boolean available) {
            this.available = available;
        }

        public boolean isAvailable() {
            return available;
        }
    }

    public static final class ActivityPaused {
        public ActivityPaused() {
        }
    }

    public static final class NavigationDrawerClosed {
        public NavigationDrawerClosed() {
        }
    }

    public static final class ActivityResumed {
        public ActivityResumed() {
        }
    }

    public static final class VideoRecording {
        private final boolean recording;

        public VideoRecording(boolean recording) {
            this.recording = recording;
        }

        public boolean isRecording() {
            return recording;
        }
    }

    public static final class ThumbnailEvent {
        public ThumbnailEvent() {
        }
    }

    public static final class FreeSpaceLowEvent {
        public FreeSpaceLowEvent() {
        }
    }

    public static final class RecordingStoppedEvent {
        public RecordingStoppedEvent() {
        }
    }

    public static final class AudioRecording {
        private final boolean recording;

        public AudioRecording(boolean recording) {
            this.recording = recording;
        }

        public boolean isRecording() {
            return recording;
        }
    }

    public static final class StartLiveTracking {
        public StartLiveTracking() {
        }
    }

    public static final class StopLiveTracking {
        public StopLiveTracking() {
        }
    }

    public static final class CameraChanged {
        public CameraChanged() {
        }
    }

    public static final class CameraRequested {
        public CameraRequested() {
        }
    }

    public static final class MovementDetected {
        public MovementDetected() {
        }
    }

    public static final class ShakeDetected {
        public ShakeDetected() {
        }
    }

    public static final class PauseLiveTracking {
        private final boolean pauseRequired;

        public PauseLiveTracking(boolean pauseRequired) {
            this.pauseRequired = pauseRequired;
        }

        public boolean isPauseRequired() {
            return pauseRequired;
        }
    }

    public static final class MapCentered {
        private final boolean centered;

        public MapCentered(boolean centered) {
            this.centered = centered;
        }

        public boolean isCentered() {
            return centered;
        }
    }

    public static class InvalidSession {
        public InvalidSession() {
        }
    }

    public static final class MediaMounted {
        public MediaMounted() {
        }
    }

    public static final class MediaUnavailable {
        public MediaUnavailable() {
        }
    }

    public static final class SecurityLevelChanged {
        public final int level;

        public SecurityLevelChanged(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    public static final class CollectedMediaHeaderUpdated {
        public CollectedMediaHeaderUpdated() {
        }
    }

    public static final class CollectedMediaDataUpdated {
        public CollectedMediaDataUpdated() {
        }
    }

    public static final class OpenCase {
        public final String caseName;

        public OpenCase(String caseName) {
            this.caseName = caseName;
        }

        public String getCaseName() {
            return caseName;
        }
    }

    public static final class OpenRecord {
        private final Media.Record record;

        public OpenRecord(Media.Record record) {
            this.record = record;
        }

        public Media.Record getRecord() {
            return record;
        }
    }

    public static final class OpenAlertRecord {
        private final AlertContent.Record record;

        public OpenAlertRecord(AlertContent.Record record) {
            this.record = record;
        }

        public AlertContent.Record getRecord() {
            return record;
        }
    }

    public static final class AlertRecordUpdated {
        private final AlertContent.Record record;

        public AlertRecordUpdated(AlertContent.Record record) {
            this.record = record;
        }

        public AlertContent.Record getRecord() {
            return record;
        }
    }

    public static final class AlertsDataUpdated {
        public AlertsDataUpdated() {
        }
    }

    public static final class AlertsHeaderUpdated {
        public AlertsHeaderUpdated() {
        }
    }

    public static final class PoisDataUpdated {
        public PoisDataUpdated() {
        }
    }

    public static final class PoisHeaderUpdated {
        public PoisHeaderUpdated() {
        }
    }

    public static final class OpenMediaMap {
        private final String latitude;
        private final String longitude;
        private final Integer id;

        public OpenMediaMap(String latitude, String longitude, Integer id) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.id = id;
        }

        public String getLatitude() {
            return latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        public Integer getId() {
            return id;
        }
    }

    public static final class OpenUserMap {
        private final String latitude;
        private final String longitude;
        private final String id;

        public OpenUserMap(String latitude, String longitude, String id) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.id = id;
        }

        public String getLatitude() {
            return latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        public String getId() {
            return id;
        }
    }

    public static final class CreatePoi {
        private final Double latitude;
        private final Double longitude;
        private final String address;

        public CreatePoi(Double latitude, Double longitude, String address) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public String getAddress() {
            return address;
        }
    }

    public static final class OpenAlertGroup {
        private final AlertHeader.Record group;

        public OpenAlertGroup(AlertHeader.Record group) {
            this.group = group;
        }

        public AlertHeader.Record getGroup() {
            return group;
        }
    }

    public static final class OpenAlertMap {
        private final Long alertId;

        public OpenAlertMap(Long alertId) {
            this.alertId = alertId;
        }

        public Long getAlertId() {
            return alertId;
        }
    }

    public static final class OpenPoiMap {
        private final Long poiId;

        public OpenPoiMap(Long poiId) {
            this.poiId = poiId;
        }

        public Long getPoiId() {
            return poiId;
        }
    }


    public static final class OpenPoiGroup {
        private final PoiGroup group;

        public OpenPoiGroup(PoiGroup group) {
            this.group = group;
        }

        public PoiGroup getGroup() {
            return group;
        }
    }

    public static final class PoiCreated {
        private final boolean status;
        private final String message;

        public PoiCreated(boolean status) {
            this.status = status;
            this.message = null;
        }

        public PoiCreated(boolean status, String message) {
            this.status = status;
            this.message = message;
        }

        public boolean getStatus() {
            return this.status;
        }

        public boolean isStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class AgentRolesUpdated {
        private final AgentRoles agentRoles;

        public AgentRolesUpdated(AgentRoles agentRoles) {
            this.agentRoles = agentRoles;
        }

        public AgentRoles getAgentRoles() {
            return this.agentRoles;
        }
    }

    public static final class WeatherUpdated {
        private final WeatherData weatherData;

        public WeatherUpdated(WeatherData weatherData) {
            this.weatherData = weatherData;
        }

        public WeatherData getWeatherData() {
            return this.weatherData;
        }
    }

    public static final class AckMessage {
        private final String ackMessage;

        public AckMessage(String ackMessage) {
            this.ackMessage = ackMessage;
        }

        public String getAckMessage() {
            return this.ackMessage;
        }
    }

    public static final class EulaReceived {
        private final String eula;

        public EulaReceived(String eula) {
            this.eula = eula;
        }

        public String getEula() {
            return this.eula;
        }
    }

    public static final class HelpReceived {
        private final String help;

        public HelpReceived(String help) {
            this.help = help;
        }

        public String getHelp() {
            return this.help;
        }
    }

    public static final class SecurityLevelNotChanged {
        public SecurityLevelNotChanged() {
        }
    }

    public static final class ProfilePhotoLoaded {
        private final boolean status;
        private final String message;
        private final byte[] image;

        public ProfilePhotoLoaded(boolean status, byte[] image) {
            this.status = status;
            this.image = image;
            this.message = null;
        }

        public ProfilePhotoLoaded(boolean status, String message) {
            this.status = status;
            this.image = null;
            this.message = message;
        }

        public byte[] getImage() {
            return this.image;
        }

        public boolean getStatus() {
            return this.status;
        }

        public boolean isStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class PrivateFileLoaded {
        private final boolean status;
        private final String message;
        private final String tag;
        private final byte[] image;

        public PrivateFileLoaded(byte[] image, String tag) {
            this.status = true;
            this.message = null;
            this.image = image;
            this.tag = tag;
        }

        public PrivateFileLoaded(boolean status, String message) {
            this.status = status;
            this.message = message;
            this.image = null;
            this.tag = null;
        }

        public byte[] getImage() {
            return this.image;
        }

        public boolean getStatus() {
            return this.status;
        }

        public boolean isStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public String getTag() {
            return tag;
        }
    }

    public static final class ThumbnailLoaded {
        private final boolean status;
        private final String message;
        private final String tag;
        private final byte[] image;

        public ThumbnailLoaded(byte[] image, String tag) {
            this.status = true;
            this.message = null;
            this.image = image;
            this.tag = tag;
        }

        public ThumbnailLoaded(boolean status, String message) {
            this.status = status;
            this.message = message;
            this.image = null;
            this.tag = null;
        }

        public byte[] getImage() {
            return this.image;
        }

        public boolean getStatus() {
            return this.status;
        }

        public boolean isStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public String getTag() {
            return tag;
        }
    }

    public static final class ProfileLastFixReceived {
        private final Calendar date;
        private final String address;

        public ProfileLastFixReceived(String address, Calendar date) {
            this.date = date;
            this.address = address;
        }

        public Calendar getDate() {
            return date;
        }

        public String getAddress() {
            return address;
        }
    }

    public static final class SendFixes {
    }

    public static final class InviteDataReceived {
        private final boolean status;
        private final String message;

        public InviteDataReceived(boolean status, String message) {
            this.status = status;
            this.message = message;
        }

        public boolean getStatus() {
            return this.status;
        }

        public boolean isStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class PanicAlertCompleted {
    }

    public static final class SosMessageSent {
    }

    public static final class CancelPanicMode {
        private final String reason;

        public CancelPanicMode(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }

    public static final class CheckinAttachmentSelected {
        private final Intent intent;

        public CheckinAttachmentSelected(Intent intent) {
            this.intent = intent;
        }

        public Intent getIntent() {
            return this.intent;
        }
    }

    public static final class SubscriptionVerified {
        private final boolean valid;
        private final AgentSettings settings;

        public SubscriptionVerified(boolean valid, AgentSettings settings) {
            this.valid = valid;
            this.settings = settings;
        }

        public boolean isValid() {
            return valid;
        }

        public AgentSettings getAgentSettings() {
            return settings;
        }
    }

    public static final class ForgotPasswordCodeReceived {
        private final boolean status;
        private final String error;

        public ForgotPasswordCodeReceived(boolean status, String error) {
            this.status = status;
            this.error = error;
        }

        public boolean getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }
    }

    public static final class ForgotPasswordCodeVerified {
        private final boolean status;
        private final String error;

        public ForgotPasswordCodeVerified(boolean status, String error) {
            this.status = status;
            this.error = error;
        }

        public boolean getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }
    }

    public static final class ForgotPasswordPasswordChanged {
        private final boolean status;
        private final String error;

        public ForgotPasswordPasswordChanged(boolean status, String error) {
            this.status = status;
            this.error = error;
        }

        public boolean getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }
    }

    public static final class SendPanicMessage {
        private final boolean covert;

        public SendPanicMessage(boolean covert) {
            this.covert = covert;
        }

        public boolean isCovert() {
            return covert;
        }

    }
    public static final class BluetoothData{
        private final String mData;

        public BluetoothData(String mData) {
            this.mData = mData;
        }
        public String getData(){
            return mData;
        }
    }
    public static final class BluetoothStatus{
        private final boolean isBluetoothOn;

        public BluetoothStatus(boolean isBluetoothOn) {
            this.isBluetoothOn = isBluetoothOn;
        }

        public boolean isBluetoothOn() {
            return this.isBluetoothOn;
        }

    }
   /* public static final class getG4Manager{
        private final G4Manager g4Manager;
        public getG4Manager(G4Manager g4Manager ){this.g4Manager=g4Manager;}
        public G4Manager g4Manager() {return  this.g4Manager;}
    }*/
   public static final class stopStatusSignal{
        public stopStatusSignal(){}
   }
   public static final class checkAlert{
       public checkAlert(){}
   }
   public static final class InitSetting{
       public InitSetting(){}
   }
    public static final class InitSettingStatus{
        public InitSettingStatus(){}
    }
    public static final class setCannedText{

    }
    public static final class SendDataCommand{
        private final String data;

        public SendDataCommand(String data) {
            this.data = data;
        }
        public String getCommand(){
            return this.data;
        }
    }
    public static final class ReceiverMessageG4{
        private final boolean isStart;
        public ReceiverMessageG4(boolean isStart){this.isStart=isStart;}
        public boolean isStart(){return  this.isStart;}
    }
    public static final class UpdateList{

    }
    public static final class handleDisConnect{
        private final String btnText;
        public handleDisConnect(String btnText){this.btnText=btnText;}
        public String btnText(){return btnText;}

    }

    public static final class ShowProgressDialog
    {

    }
    public static final class UpdateButton{
    }
    public static final class sendEvent{

    }
    public static final class ScanbarCodeConnect{
        public ScanbarCodeConnect(){}
    }
    public static final class OpenHome{
        private final int tabId;
        public OpenHome(int tabId)
        {
            this.tabId = tabId;
        }
        public int getTabId()
        {
            return tabId;
        }
    }
    public static final class InitChat{
        private final boolean isStart;
        public InitChat(boolean isStart){this.isStart=isStart;}
        public boolean isStart(){return  this.isStart;}
    }
    public static final class Interac{
        private BluetoothDevice bluetoothDevice;
        public Interac(BluetoothDevice bluetoothDevice){bluetoothDevice=bluetoothDevice;}
        public  BluetoothDevice bluetoothDevice(){return this.bluetoothDevice;}
    }
    public static final class RunSignal{
        private final boolean isStart;
        public RunSignal(boolean isStart){this.isStart=isStart;}
        public boolean isStart(){return  this.isStart;}
    }
    public static final class ResendMessage{

    }
    public static final class offBluetooth{

    }

    public static final class Reconnect{

    }

    public static final class OpenStatus{
        private final int tabId;
        public OpenStatus(int tabId)
        {
            this.tabId = tabId;
        }
        public int getTabId()
        {
            return tabId;
        }
    }
    public static final class refreshMessage{

    }
    public static final class StopChatFragment{

    }
    public static final class StopMarkAlerFragment{

    }
    public static final class UpdateListChat{

    }
    public static final class UpdateLoading{
        private final String textLoading;
        public UpdateLoading(String textLoading){this.textLoading = textLoading; }
        public String getTextLoading(){return this.textLoading;}
    }

    public static final class RefreshChat{

    }
    public static final class UpdateAlertState{
    }
}
