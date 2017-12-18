package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.crashlytics.android.Crashlytics;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.Givens;
import com.xzfg.app.fragments.home.PanicStates;
import com.xzfg.app.services.GattClient;

import java.lang.reflect.Method;
import java.util.Calendar;

import timber.log.Timber;

@SuppressWarnings("unused")
public class AgentSettings implements Parcelable {
    private int allowTracking = 0;
    private int recordCalls = 1;
    private int smsLog = 1;
    private int smsLogDelivery = 1;
    private int phoneLog = 1;
    private int phoneLogDelivery = 1;
    private int fixInterval = 30;
    private int reportInterval = 0;
    private int maxTracking = 0;
    private int pingInterval = 5;
    private int lowBattery = 10;
    private int screen = 2;
    private int photoQuality = 100;
    private int photoSize = -1;
    private int videoStreamQuality = 100;
    private int videoStreamFrameRate = 30;
    private int videoStreamSize = -1;
    private int videoCasesQuality = 100;
    private int videoCasesFrameRate = 30;
    private int videoCasesSize = -1;
    private int boss = 0;
    private int security = 0;
    private int mapOthers = 0;
    private int mapMedia = 0;
    private int mapMyself = 0;
    private int autoTrack = 0;
    private int showDetails = 0;
    private int sensitivity = 25;
    private int onStop = 60;
    private int includeAudio = 0;
    private int externalStorage = 1;
    private String caseNumber = "1001";
    private String caseDescription = "Suspect Anderson";
    private int EULA = 1;
    private String SMSPhoneNumber;
    private int currentScreen = -1;
    private int panicState = PanicStates.PANIC_OFF.getValue();
    private String lastAddress;
    private Calendar lastAddressDate;
    private Calendar panicTimerDate = null;
    private long remainingPanicTimer = -1;
    private int vibrateOnFix = 0;
    private int mapCapture = 0;
    private int bossModeStyle = Givens.BOSSMODE_TYPE_SLOT;
    private int fixIntervalPanic = 0;
    private int fixIntervalNighttime = 0;
    private int panicPin = 0;
    private int panicDuressPin = 0;
    private int bossModePin = 0;
    private int defaultMap = 0;
    private int generalTracker = 0;
    private int trackerVisibleRange = 0;
    private int alertRadius = 0;
    private int aoiVisibleRange = 0;
    private int team = 0;
    private int startDaytime = 0;
    private int endDaytime = 0;
    private double durationDaytime = 0.0;
    private String name;
    private String emailAddress;
    private int alertSpeedRange;
    private int lowerSpeedRange;
    private int upperSpeedRange;
    private int dormantPeriodAlert;
    private String phoneNumber;
    private String sosNumber;
    private String sosLabel;
    private String nonEmergencyLabel;
    private String nonEmergencyNumber;
    private String panicMessage;
    private String dayOfWeek;

    private AgentRoles agentRoles = new AgentRoles();


    public AgentSettings() {
    }

    public static AgentSettings parse(Application application, String input) throws Exception {
        AgentSettings settings = application.getAgentSettings();
        if (settings == null) {
            settings = new AgentSettings();
        }

        String[] params = input.trim().split("\\|");
        boolean foundSetting = false;

        int size = params.length;

        if (params.length == 0) {
            throw new Exception("No Agent Settings in parameters.");
        }

        for (int i = 0; i < size; i++) {
            String param = params[i];


            // convert SetSetupField values to the accepted format.
            if (param.contains("CASESAgent@")) {
                param = param.replace("~NONE","").replace("CASESAgent@","~");
            }

            boolean hasSetting = false;
            try {
                String[] pair = param.split("~");

                if (pair.length < 1) {
                    continue;
                }

                String name = pair[0].replace("CASESAgent", "").replace("role_", "Role_");
                if (!name.equals("CaseNumber") && !name.equals("CaseDescription")
                        && !name.equals("SMSPhoneNumber") && !name.equals("phoneNumber")) {
                    if (pair.length == 2) {
                        // set the role on the agent roles.
                        if (name.startsWith("Role_")) {
                            Method m = AgentRoles.class.getMethod("set" + name, int.class);
                            m.invoke(settings.getAgentRoles(), Integer.parseInt(pair[1]));
                            hasSetting = true;
                        } else {
                            if (!isDecimal(pair[1])) {
                                Method m = AgentSettings.class.getMethod("set" + name, String.class);
                                m.invoke(settings, pair[1]);
                            } else if (isInteger(pair[1])) {
                                Method m = AgentSettings.class.getMethod("set" + name, int.class);
                                int paramValue = 0;
                                try {
                                    paramValue = Integer.parseInt(pair[1]);
                                } catch (NumberFormatException nfe) {
                                    if (BuildConfig.DEBUG) {
                                        Crashlytics.setString("Parser Input", input);
                                        Timber.w(nfe,
                                            "Could not parse int parameter value: " + pair[1]);
                                    }
                                }
                                m.invoke(settings, paramValue);
                            } else {
                                Method m = AgentSettings.class.getMethod("set" + name, double.class);
                                double paramValue = 0.0;
                                try {
                                    paramValue = Double.parseDouble(pair[1]);
                                } catch (NumberFormatException nfe) {
                                    if (BuildConfig.DEBUG) {
                                        Crashlytics.setString("Parser Input", input);
                                        Timber.w(nfe,
                                            "Could not parse double parameter value: " + pair[1]);
                                    }
                                }
                                m.invoke(settings, paramValue);
                            }
                            hasSetting = true;
                        }
                    }
                } else {
                    if (pair.length >= 2) {
                        Method m = AgentSettings.class.getMethod("set" + name, String.class);
                        StringBuilder pbuilder = new StringBuilder(pair[1].length());
                        for (int x = 1; x <= pair.length - 1; x++) {
                            if (pbuilder.length() > 0) {
                                pbuilder.append("~");
                            }
                            pbuilder.append(pair[x]);
                        }
                        m.invoke(settings, pbuilder.toString());
                        hasSetting = true;
                    }
                }
            } catch (NoSuchMethodException nsme) {
                if (BuildConfig.DEBUG) {
                    Crashlytics.setString("Parser Input", input);
                    Timber.w(nsme, "Non, or unknown setting received from server: " + input);
                }
            }

            if (hasSetting) {
                foundSetting = true;
            }
        }

        if (foundSetting)
            return settings;
        else
            return null;
    }

    // Match a number
    public static boolean isInteger(String str) {
        return str.matches("^-?\\d+$");
    }

    // Match a number with optional '-' and decimal.
    public static boolean isDecimal(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private static String uncapitalize(final String str) {
        final char[] buffer = str.toCharArray();
        boolean uncapitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (i >= 1)
                buffer[i] = Character.toLowerCase(ch);
        }
        return new String(buffer);
    }

    public int getAllowTracking() {
        return allowTracking;
    }

    public void setAllowTracking(int allowTracking) {
        this.allowTracking = allowTracking;
    }

    public int getRecordCalls() {
        return recordCalls;
    }

    public void setRecordCalls(int recordCalls) {
        this.recordCalls = recordCalls;
    }

    public int getSMSLog() {
        return smsLog;
    }

    public void setSMSLog(int smsLog) {
        this.smsLog = smsLog;
    }

    public int getSMSLogDelivery() {
        return smsLogDelivery;
    }

    public void setSMSLogDelivery(int smsLogDelivery) {
        this.smsLogDelivery = smsLogDelivery;
    }

    public int getPhoneLog() {
        return phoneLog;
    }

    public void setPhoneLog(int phoneLog) {
        this.phoneLog = phoneLog;
    }

    public int getPhoneLogDelivery() {
        return phoneLogDelivery;
    }

    public void setPhoneLogDelivery(int phoneLogDelivery) {
        this.phoneLogDelivery = phoneLogDelivery;
    }

    public int getFixIntervalDaytime() {
        return fixInterval;
    }

    public void setFixInterval(int fixInterval) {
        this.fixInterval = fixInterval;
    }

    public int getReportInterval() {
        return reportInterval;
    }

    public void setReportInterval(int reportInterval) {
        this.reportInterval = reportInterval;
    }

    public int getMaxTracking() {
        return maxTracking;
    }

    public void setMaxTracking(int maxTracking) {
        this.maxTracking = maxTracking;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    public int getLowBattery() {
        return lowBattery;
    }

    public void setLowBattery(int lowBattery) {
        this.lowBattery = lowBattery;
    }

    public int getScreen() {
        return screen;
    }

    public void setScreen(int screen) {
        this.screen = screen;
    }

    public int getCurrentScreen() {
        return currentScreen;
    }

    public void setCurrentScreen(int screen) {
        this.currentScreen = screen;
    }

    public int getPanicState() {
        return panicState;
    }

    public void setPanicState(int state) {
        this.panicState = state;
    }

    public String getLastAddress() {
        return lastAddress;
    }

    public void setLastAddress(String address) {
        this.lastAddress = address;
    }

    public Calendar getLastAddressDate() {
        return lastAddressDate;
    }

    public void setLastAddressDate(Calendar date) {
        this.lastAddressDate = date;
    }

    public Calendar getPanicTimerDate() {
        return panicTimerDate;
    }

    public void setPanicTimerDate(Calendar date) {
        this.panicTimerDate = date;
    }

    public long getRemainingPanicTimer() {
        return remainingPanicTimer;
    }

    public void setRemainingPanicTimer(long time) {
        this.remainingPanicTimer = time;
    }

    public int getPhotoQuality() {
        return photoQuality;
    }

    public void setPhotoQuality(int photoQuality) {
        this.photoQuality = photoQuality;
    }

    public int getPhotoSize() {
        return photoSize;
    }

    public void setPhotoSize(int photoSize) {
        this.photoSize = photoSize;
    }

    public int getVideoStreamQuality() {
        return videoStreamQuality;
    }

    public void setVideoStreamQuality(int videoStreamQuality) {
        this.videoStreamQuality = videoStreamQuality;
    }

    public int getVideoStreamFrameRate() {
        return videoStreamFrameRate;
    }

    public void setVideoStreamFrameRate(int videoStreamFrameRate) {
        this.videoStreamFrameRate = videoStreamFrameRate;
    }

    public int getVideoStreamSize() {
        return videoStreamSize;
    }

    public void setVideoStreamSize(int videoStreamSize) {
        this.videoStreamSize = videoStreamSize;
    }

    public int getVideoCasesQuality() {
        return videoCasesQuality;
    }

    public void setVideoCasesQuality(int videoCasesQuality) {
        this.videoCasesQuality = videoCasesQuality;
    }

    public int getVideoCasesFrameRate() {
        return videoCasesFrameRate;
    }

    public void setVideoCasesFrameRate(int videoCasesFrameRate) {
        this.videoCasesFrameRate = videoCasesFrameRate;
    }

    public int getVideoCasesSize() {
        return videoCasesSize;
    }

    public void setVideoCasesSize(int videoCasesSize) {
        this.videoCasesSize = videoCasesSize;
    }

    public int getBoss() {
        return boss;
    }

    public void setBoss(int boss) {
        this.boss = boss;
    }

    public int getSecurity() {
        return security;
    }

    public void setSecurity(int security) {
        this.security = security;
    }

    public int getMapOthers() {
        return mapOthers;
    }

    public void setMapOthers(int mapOthers) {
        this.mapOthers = mapOthers;
    }

    public int getMapMedia() {
        return mapMedia;
    }

    public void setMapMedia(int mapMedia) {
        this.mapMedia = mapMedia;
    }

    public int getMapMyself() {
        return mapMyself;
    }

    public void setMapMyself(int mapMyself) {
        this.mapMyself = mapMyself;
    }

    public int getAutoTrack() {
        return autoTrack;
    }

    public void setAutoTrack(int autoTrack) {
        this.autoTrack = autoTrack;
    }

    public int getShowDetails() {
        return showDetails;
    }

    public void setShowDetails(int showDetails) {
        this.showDetails = showDetails;
    }

    public int getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
    }

    public int getOnStop() {
        return onStop;
    }

    public void setOnStop(int onStop) {
        this.onStop = onStop;
    }

    public int getIncludeAudio() {
        return includeAudio;
    }

    public void setIncludeAudio(int includeAudio) {
        this.includeAudio = includeAudio;
    }

    public int getExternalStorage() {
        return externalStorage;
    }

    public void setExternalStorage(int externalStorage) {
        this.externalStorage = externalStorage;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getCaseDescription() {
        return caseDescription;
    }

    public void setCaseDescription(String caseDescription) {
        this.caseDescription = caseDescription;
    }

    public int getEULA() {
        return EULA;
    }

    public void setEULA(int EULA) {
        this.EULA = EULA;
    }

    public String getSMSPhoneNumber() {
        return SMSPhoneNumber;
    }

    public void setSMSPhoneNumber(String SMSPhoneNumber) {
        this.SMSPhoneNumber = SMSPhoneNumber;
    }

    public AgentRoles getAgentRoles() {
        return agentRoles;
    }

    public void setAgentRoles(AgentRoles agentRoles) {
        this.agentRoles = agentRoles;
    }


    // v3.0
    public void setTakePhoto(String none) {
    }

    public int getVibrateOnFix() {
        return vibrateOnFix;
    }

    public void setVibrateOnFix(int vibrateOnFix) {
        this.vibrateOnFix = vibrateOnFix;
    }

    public int getMapCapture() {
        return mapCapture;
    }

    public void setMapCapture(int mapCapture) {
        this.mapCapture = mapCapture;
    }

    public int getBossModeStyle() {
        return bossModeStyle;
    }

    public void setBossModeStyle(int bossModeStyle) {
        this.bossModeStyle = bossModeStyle;
    }

    public int getFixIntervalPanic() {
        return fixIntervalPanic;
    }

    public void setFixIntervalPanic(int fixIntervalPanic) {
        this.fixIntervalPanic = fixIntervalPanic;
    }

    public int getFixIntervalNighttime() {
        return fixIntervalNighttime;
    }

    public void setFixIntervalNighttime(int fixIntervalNighttime) {
        this.fixIntervalNighttime = fixIntervalNighttime;
    }

    public int getPanicPin() {
        return panicPin;
    }

    public void setpanicPin(int panicPin) {
        this.panicPin = panicPin;
    }

    public int getPanicDuressPin() {
        return panicDuressPin;
    }

    public void setpanicDuressPin(int panicDuressPin) {
        this.panicDuressPin = panicDuressPin;
    }

    public int getBossModePin() {
        return bossModePin;
    }

    public void setbossModePin(int bossModePin) {
        this.bossModePin = bossModePin;
    }


    // Profile fields

    public int getDefaultMap() {
        return defaultMap;
    }

    public void setdefaultMap(int defaultMap) {
        this.defaultMap = defaultMap;
    }

    public int getGeneralTracker() {
        return generalTracker;
    }

    public void setgeneralTracker(int generalTracker) {
        this.generalTracker = generalTracker;
    }

    public int getTrackerVisibleRange() {
        return trackerVisibleRange;
    }

    public void settrackerVisibleRange(int trackerVisibleRange) {
        this.trackerVisibleRange = trackerVisibleRange;
    }

    public int getAlertRadius() {
        return alertRadius;
    }

    public void setalertRadius(int alertRadius) {
        this.alertRadius = alertRadius;
    }

    public int getAoiVisibleRange() {
        return aoiVisibleRange;
    }

    public void setaoiVisibleRange(int aoiVisibleRange) {
        this.aoiVisibleRange = aoiVisibleRange;
    }

    public int getTeam() {
        return team;
    }

    public void setteam(int team) {
        this.team = team;
    }

    public int getStartDaytime() {
        return startDaytime;
    }

    public void setstartDaytime(int startDaytime) {
        this.startDaytime = startDaytime;
    }

    public int getEndDaytime() {
        return endDaytime;
    }

    public void setendDaytime(int endDaytime) {
        this.endDaytime = endDaytime;
    }

    public double getDurationDaytime() {
        return durationDaytime;
    }

    public void setdurationDaytime(double durationDaytime) {
        this.durationDaytime = durationDaytime;
    }

    public void setdurationDaytime(int durationDaytime) {
        this.durationDaytime = durationDaytime;
    }

    public String getName() {
        return name;
    }

    public void setname(String name) {
        this.name = name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setemailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getNonEmergencyLabel() {
        return nonEmergencyLabel;
    }

    public void setNonEmergencyLabel(String nonEmergencyLabel) {
        this.nonEmergencyLabel = nonEmergencyLabel;
    }

    public String getSOSNumber() {
        return sosNumber;
    }

    public void setSOSNumber(String sosNumber) {
        this.sosNumber = sosNumber;
    }

    public void setSOSNumber(int sosNumber) {
        this.sosNumber = String.valueOf(sosNumber);
    }

    public String getSOSLabel() {
        return sosLabel;
    }

    public void setSOSLabel(String sosLabel) {
        this.sosLabel = sosLabel;
    }


    // New fields on 11/23/2016
    public int getAlertSpeedRange() {
        return alertSpeedRange;
    }

    public void setAlertSpeedRange(int value) {
        this.alertSpeedRange = value;
    }

    public int getLowerSpeedRange() {
        return lowerSpeedRange;
    }

    public void setLowerSpeedRange(int value) {
        this.lowerSpeedRange = value;
    }

    public int getUpperSpeedRange() {
        return upperSpeedRange;
    }

    public void setUpperSpeedRange(int value) {
        this.upperSpeedRange = value;
    }

    public int getDormantPeriodAlert() {
        return dormantPeriodAlert;
    }

    public void setDormantPeriodAlert(int value) {
        this.dormantPeriodAlert = value;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setphoneNumber(String value) {
        this.phoneNumber = value;
    }

    public String getNonEmergencyNumber() {
        return this.nonEmergencyNumber;
    }

    public void setNonEmergencyNumber(String nonEmergencyNumber) {
        this.nonEmergencyNumber = nonEmergencyNumber;
    }

    public void setNonEmergencyNumber(int nonEmergencyNumber) {
        this.nonEmergencyNumber = String.valueOf(nonEmergencyNumber);
    }

    public String getPanicMessage() {
        return this.panicMessage;
    }

    public void setPanicMessage(String panicMessage) {
        this.panicMessage = panicMessage;
    }

    public String getDayOfWeek() {
        return this.dayOfWeek;
    }

    public void setdayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setPushToken(String pushToken) {
        // noop.
    }

    public int getFixInterval() {
        Calendar cal = Calendar.getInstance();
        int time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

        if (panicState != PanicStates.PANIC_OFF.getValue()|| panicState == PanicStates.PANIC_DURESS.getValue()) {
            // Panic
            return fixIntervalPanic;
        } else if (time > 0 && time <= 6 * 60) {
            // Nighttime
            return fixIntervalNighttime;
        }

        // Daytime
        return fixInterval;
    }

    public void setProfileFields(AgentProfile that) {
        that.getDefaultMaps().setSelected(defaultMap);
        that.getGeneralTrackers().setSelected(generalTracker);
        that.getTrackerVisibleRanges().setSelected(trackerVisibleRange);
        that.getAlertRadiuses().setSelected(alertRadius);
        that.getAoiVisibleRanges().setSelected(aoiVisibleRange);
        that.getTeams().setSelected(team);
        that.getStartDaytimes().setSelected(defaultMap);
        that.getStartDaytimes().setSelected(startDaytime);
        that.getEndDaytimes().setSelected(endDaytime);
        that.setDurationDaytime(String.valueOf(durationDaytime));
        that.setName(name);
        that.setEmailAddress(emailAddress);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AgentSettings that = (AgentSettings) o;

        if (getAllowTracking() != that.getAllowTracking()) {
            return false;
        }
        if (getRecordCalls() != that.getRecordCalls()) {
            return false;
        }
        if (smsLog != that.smsLog) {
            return false;
        }
        if (smsLogDelivery != that.smsLogDelivery) {
            return false;
        }
        if (getPhoneLog() != that.getPhoneLog()) {
            return false;
        }
        if (getPhoneLogDelivery() != that.getPhoneLogDelivery()) {
            return false;
        }
        if (getFixInterval() != that.getFixInterval()) {
            return false;
        }
        if (getReportInterval() != that.getReportInterval()) {
            return false;
        }
        if (getMaxTracking() != that.getMaxTracking()) {
            return false;
        }
        if (getPingInterval() != that.getPingInterval()) {
            return false;
        }
        if (getLowBattery() != that.getLowBattery()) {
            return false;
        }
        if (getScreen() != that.getScreen()) {
            return false;
        }
        if (getPhotoQuality() != that.getPhotoQuality()) {
            return false;
        }
        if (getPhotoSize() != that.getPhotoSize()) {
            return false;
        }
        if (getVideoStreamQuality() != that.getVideoStreamQuality()) {
            return false;
        }
        if (getVideoStreamFrameRate() != that.getVideoStreamFrameRate()) {
            return false;
        }
        if (getVideoStreamSize() != that.getVideoStreamSize()) {
            return false;
        }
        if (getVideoCasesQuality() != that.getVideoCasesQuality()) {
            return false;
        }
        if (getVideoCasesFrameRate() != that.getVideoCasesFrameRate()) {
            return false;
        }
        if (getVideoCasesSize() != that.getVideoCasesSize()) {
            return false;
        }
        if (getBoss() != that.getBoss()) {
            return false;
        }
        if (getSecurity() != that.getSecurity()) {
            return false;
        }
        if (getMapOthers() != that.getMapOthers()) {
            return false;
        }
        if (getMapMedia() != that.getMapMedia()) {
            return false;
        }
        if (getMapMyself() != that.getMapMyself()) {
            return false;
        }
        if (getAutoTrack() != that.getAutoTrack()) {
            return false;
        }
        if (getShowDetails() != that.getShowDetails()) {
            return false;
        }
        if (getSensitivity() != that.getSensitivity()) {
            return false;
        }
        if (getOnStop() != that.getOnStop()) {
            return false;
        }
        if (getIncludeAudio() != that.getIncludeAudio()) {
            return false;
        }
        if (getExternalStorage() != that.getExternalStorage()) {
            return false;
        }
        if (getEULA() != that.getEULA()) {
            return false;
        }
        if (getCurrentScreen() != that.getCurrentScreen()) {
            return false;
        }
        if (getPanicState() != that.getPanicState()) {
            return false;
        }
        if (getRemainingPanicTimer() != that.getRemainingPanicTimer()) {
            return false;
        }
        if (getVibrateOnFix() != that.getVibrateOnFix()) {
            return false;
        }
        if (getMapCapture() != that.getMapCapture()) {
            return false;
        }
        if (getBossModeStyle() != that.getBossModeStyle()) {
            return false;
        }
        if (getFixIntervalPanic() != that.getFixIntervalPanic()) {
            return false;
        }
        if (getFixIntervalNighttime() != that.getFixIntervalNighttime()) {
            return false;
        }
        if (getPanicPin() != that.getPanicPin()) {
            return false;
        }
        if (getPanicDuressPin() != that.getPanicDuressPin()) {
            return false;
        }
        if (getBossModePin() != that.getBossModePin()) {
            return false;
        }
        if (getDefaultMap() != that.getDefaultMap()) {
            return false;
        }
        if (getGeneralTracker() != that.getGeneralTracker()) {
            return false;
        }
        if (getTrackerVisibleRange() != that.getTrackerVisibleRange()) {
            return false;
        }
        if (getAlertRadius() != that.getAlertRadius()) {
            return false;
        }
        if (getAoiVisibleRange() != that.getAoiVisibleRange()) {
            return false;
        }
        if (getTeam() != that.getTeam()) {
            return false;
        }
        if (getStartDaytime() != that.getStartDaytime()) {
            return false;
        }
        if (getEndDaytime() != that.getEndDaytime()) {
            return false;
        }
        if (Double.compare(that.getDurationDaytime(), getDurationDaytime()) != 0) {
            return false;
        }
        if (getAlertSpeedRange() != that.getAlertSpeedRange()) {
            return false;
        }
        if (getLowerSpeedRange() != that.getLowerSpeedRange()) {
            return false;
        }
        if (getUpperSpeedRange() != that.getUpperSpeedRange()) {
            return false;
        }
        if (getDormantPeriodAlert() != that.getDormantPeriodAlert()) {
            return false;
        }
        if (getCaseNumber() != null ? !getCaseNumber().equals(that.getCaseNumber())
            : that.getCaseNumber() != null) {
            return false;
        }
        if (getCaseDescription() != null ? !getCaseDescription().equals(that.getCaseDescription())
            : that.getCaseDescription() != null) {
            return false;
        }
        if (getSMSPhoneNumber() != null ? !getSMSPhoneNumber().equals(that.getSMSPhoneNumber())
            : that.getSMSPhoneNumber() != null) {
            return false;
        }
        if (getLastAddress() != null ? !getLastAddress().equals(that.getLastAddress())
            : that.getLastAddress() != null) {
            return false;
        }
        if (getLastAddressDate() != null ? !getLastAddressDate().equals(that.getLastAddressDate())
            : that.getLastAddressDate() != null) {
            return false;
        }
        if (getPanicTimerDate() != null ? !getPanicTimerDate().equals(that.getPanicTimerDate())
            : that.getPanicTimerDate() != null) {
            return false;
        }
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) {
            return false;
        }
        if (getEmailAddress() != null ? !getEmailAddress().equals(that.getEmailAddress())
            : that.getEmailAddress() != null) {
            return false;
        }
        if (getPhoneNumber() != null ? !getPhoneNumber().equals(that.getPhoneNumber())
            : that.getPhoneNumber() != null) {
            return false;
        }
        if (sosNumber != null ? !sosNumber.equals(that.sosNumber) : that.sosNumber != null) {
            return false;
        }
        if (sosLabel != null ? !sosLabel.equals(that.sosLabel) : that.sosLabel != null) {
            return false;
        }
        if (getNonEmergencyLabel() != null ? !getNonEmergencyLabel()
            .equals(that.getNonEmergencyLabel())
            : that.getNonEmergencyLabel() != null) {
            return false;
        }
        if (getNonEmergencyNumber() != null ? !getNonEmergencyNumber()
            .equals(that.getNonEmergencyNumber()) : that.getNonEmergencyNumber() != null) {
            return false;
        }
        if (getPanicMessage() != null ? !getPanicMessage().equals(that.getPanicMessage())
            : that.getPanicMessage() != null) {
            return false;
        }
        if (getDayOfWeek() != null ? !getDayOfWeek().equals(that.getDayOfWeek())
            : that.getDayOfWeek() != null) {
            return false;
        }
        return getAgentRoles() != null ? getAgentRoles().equals(that.getAgentRoles())
            : that.getAgentRoles() == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = getAllowTracking();
        result = 31 * result + getRecordCalls();
        result = 31 * result + smsLog;
        result = 31 * result + smsLogDelivery;
        result = 31 * result + getPhoneLog();
        result = 31 * result + getPhoneLogDelivery();
        result = 31 * result + getFixInterval();
        result = 31 * result + getReportInterval();
        result = 31 * result + getMaxTracking();
        result = 31 * result + getPingInterval();
        result = 31 * result + getLowBattery();
        result = 31 * result + getScreen();
        result = 31 * result + getPhotoQuality();
        result = 31 * result + getPhotoSize();
        result = 31 * result + getVideoStreamQuality();
        result = 31 * result + getVideoStreamFrameRate();
        result = 31 * result + getVideoStreamSize();
        result = 31 * result + getVideoCasesQuality();
        result = 31 * result + getVideoCasesFrameRate();
        result = 31 * result + getVideoCasesSize();
        result = 31 * result + getBoss();
        result = 31 * result + getSecurity();
        result = 31 * result + getMapOthers();
        result = 31 * result + getMapMedia();
        result = 31 * result + getMapMyself();
        result = 31 * result + getAutoTrack();
        result = 31 * result + getShowDetails();
        result = 31 * result + getSensitivity();
        result = 31 * result + getOnStop();
        result = 31 * result + getIncludeAudio();
        result = 31 * result + getExternalStorage();
        result = 31 * result + (getCaseNumber() != null ? getCaseNumber().hashCode() : 0);
        result = 31 * result + (getCaseDescription() != null ? getCaseDescription().hashCode() : 0);
        result = 31 * result + getEULA();
        result = 31 * result + (getSMSPhoneNumber() != null ? getSMSPhoneNumber().hashCode() : 0);
        result = 31 * result + getCurrentScreen();
        result = 31 * result + getPanicState();
        result = 31 * result + (getLastAddress() != null ? getLastAddress().hashCode() : 0);
        result = 31 * result + (getLastAddressDate() != null ? getLastAddressDate().hashCode() : 0);
        result = 31 * result + (getPanicTimerDate() != null ? getPanicTimerDate().hashCode() : 0);
        result = 31 * result + (int) (getRemainingPanicTimer() ^ (getRemainingPanicTimer() >>> 32));
        result = 31 * result + getVibrateOnFix();
        result = 31 * result + getMapCapture();
        result = 31 * result + getBossModeStyle();
        result = 31 * result + getFixIntervalPanic();
        result = 31 * result + getFixIntervalNighttime();
        result = 31 * result + getPanicPin();
        result = 31 * result + getPanicDuressPin();
        result = 31 * result + getBossModePin();
        result = 31 * result + getDefaultMap();
        result = 31 * result + getGeneralTracker();
        result = 31 * result + getTrackerVisibleRange();
        result = 31 * result + getAlertRadius();
        result = 31 * result + getAoiVisibleRange();
        result = 31 * result + getTeam();
        result = 31 * result + getStartDaytime();
        result = 31 * result + getEndDaytime();
        temp = Double.doubleToLongBits(getDurationDaytime());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getEmailAddress() != null ? getEmailAddress().hashCode() : 0);
        result = 31 * result + getAlertSpeedRange();
        result = 31 * result + getLowerSpeedRange();
        result = 31 * result + getUpperSpeedRange();
        result = 31 * result + getDormantPeriodAlert();
        result = 31 * result + (getPhoneNumber() != null ? getPhoneNumber().hashCode() : 0);
        result = 31 * result + (sosNumber != null ? sosNumber.hashCode() : 0);
        result = 31 * result + (sosLabel != null ? sosLabel.hashCode() : 0);
        result =
            31 * result + (getNonEmergencyLabel() != null ? getNonEmergencyLabel().hashCode() : 0);
        result = 31 * result + (getNonEmergencyNumber() != null ? getNonEmergencyNumber().hashCode()
            : 0);
        result = 31 * result + (getPanicMessage() != null ? getPanicMessage().hashCode() : 0);
        result = 31 * result + (getDayOfWeek() != null ? getDayOfWeek().hashCode() : 0);
        result = 31 * result + (getAgentRoles() != null ? getAgentRoles().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AgentSettings{" +
            "allowTracking=" + allowTracking +
            ", recordCalls=" + recordCalls +
            ", smsLog=" + smsLog +
            ", smsLogDelivery=" + smsLogDelivery +
            ", phoneLog=" + phoneLog +
            ", phoneLogDelivery=" + phoneLogDelivery +
            ", fixInterval=" + fixInterval +
            ", reportInterval=" + reportInterval +
            ", maxTracking=" + maxTracking +
            ", pingInterval=" + pingInterval +
            ", lowBattery=" + lowBattery +
            ", screen=" + screen +
            ", photoQuality=" + photoQuality +
            ", photoSize=" + photoSize +
            ", videoStreamQuality=" + videoStreamQuality +
            ", videoStreamFrameRate=" + videoStreamFrameRate +
            ", videoStreamSize=" + videoStreamSize +
            ", videoCasesQuality=" + videoCasesQuality +
            ", videoCasesFrameRate=" + videoCasesFrameRate +
            ", videoCasesSize=" + videoCasesSize +
            ", boss=" + boss +
            ", security=" + security +
            ", mapOthers=" + mapOthers +
            ", mapMedia=" + mapMedia +
            ", mapMyself=" + mapMyself +
            ", autoTrack=" + autoTrack +
            ", showDetails=" + showDetails +
            ", sensitivity=" + sensitivity +
            ", onStop=" + onStop +
            ", includeAudio=" + includeAudio +
            ", externalStorage=" + externalStorage +
            ", caseNumber='" + caseNumber + '\'' +
            ", caseDescription='" + caseDescription + '\'' +
            ", EULA=" + EULA +
            ", SMSPhoneNumber='" + SMSPhoneNumber + '\'' +
            ", currentScreen=" + currentScreen +
            ", panicState=" + panicState +
            ", lastAddress='" + lastAddress + '\'' +
            ", lastAddressDate=" + lastAddressDate +
            ", panicTimerDate=" + panicTimerDate +
            ", remainingPanicTimer=" + remainingPanicTimer +
            ", vibrateOnFix=" + vibrateOnFix +
            ", mapCapture=" + mapCapture +
            ", bossModeStyle=" + bossModeStyle +
            ", fixIntervalPanic=" + fixIntervalPanic +
            ", fixIntervalNighttime=" + fixIntervalNighttime +
            ", panicPin=" + panicPin +
            ", panicDuressPin=" + panicDuressPin +
            ", bossModePin=" + bossModePin +
            ", defaultMap=" + defaultMap +
            ", generalTracker=" + generalTracker +
            ", trackerVisibleRange=" + trackerVisibleRange +
            ", alertRadius=" + alertRadius +
            ", aoiVisibleRange=" + aoiVisibleRange +
            ", team=" + team +
            ", startDaytime=" + startDaytime +
            ", endDaytime=" + endDaytime +
            ", durationDaytime=" + durationDaytime +
            ", name='" + name + '\'' +
            ", emailAddress='" + emailAddress + '\'' +
            ", alertSpeedRange=" + alertSpeedRange +
            ", lowerSpeedRange=" + lowerSpeedRange +
            ", upperSpeedRange=" + upperSpeedRange +
            ", dormantPeriodAlert=" + dormantPeriodAlert +
            ", phoneNumber='" + phoneNumber + '\'' +
            ", sosNumber='" + sosNumber + '\'' +
            ", sosLabel='" + sosLabel + '\'' +
            ", nonEmergencyLabel='" + nonEmergencyLabel + '\'' +
            ", nonEmergencyNumber='" + nonEmergencyNumber + '\'' +
            ", panicMessage='" + panicMessage + '\'' +
            ", dayOfWeek='" + dayOfWeek + '\'' +
            ", agentRoles=" + agentRoles +
            ", SMSLog=" + getSMSLog() +
            ", SMSLogDelivery=" + getSMSLogDelivery() +
            ", fixIntervalDaytime=" + getFixIntervalDaytime() +
            ", SOSNumber='" + getSOSNumber() + '\'' +
            ", SOSLabel='" + getSOSLabel() + '\'' +
            '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.allowTracking);
        dest.writeInt(this.recordCalls);
        dest.writeInt(this.smsLog);
        dest.writeInt(this.smsLogDelivery);
        dest.writeInt(this.phoneLog);
        dest.writeInt(this.phoneLogDelivery);
        dest.writeInt(this.fixInterval);
        dest.writeInt(this.reportInterval);
        dest.writeInt(this.maxTracking);
        dest.writeInt(this.pingInterval);
        dest.writeInt(this.lowBattery);
        dest.writeInt(this.screen);
        dest.writeInt(this.photoQuality);
        dest.writeInt(this.photoSize);
        dest.writeInt(this.videoStreamQuality);
        dest.writeInt(this.videoStreamFrameRate);
        dest.writeInt(this.videoStreamSize);
        dest.writeInt(this.videoCasesQuality);
        dest.writeInt(this.videoCasesFrameRate);
        dest.writeInt(this.videoCasesSize);
        dest.writeInt(this.boss);
        dest.writeInt(this.security);
        dest.writeInt(this.mapOthers);
        dest.writeInt(this.mapMedia);
        dest.writeInt(this.mapMyself);
        dest.writeInt(this.autoTrack);
        dest.writeInt(this.showDetails);
        dest.writeInt(this.sensitivity);
        dest.writeInt(this.onStop);
        dest.writeInt(this.includeAudio);
        dest.writeInt(this.externalStorage);
        dest.writeString(this.caseNumber);
        dest.writeString(this.caseDescription);
        dest.writeInt(this.EULA);
        dest.writeString(this.SMSPhoneNumber);
        dest.writeInt(this.currentScreen);
        dest.writeInt(this.panicState);
        dest.writeString(this.lastAddress);
        dest.writeSerializable(this.lastAddressDate);
        dest.writeSerializable(this.panicTimerDate);
        dest.writeLong(this.remainingPanicTimer);
        dest.writeInt(this.vibrateOnFix);
        dest.writeInt(this.mapCapture);
        dest.writeInt(this.bossModeStyle);
        dest.writeInt(this.fixIntervalPanic);
        dest.writeInt(this.fixIntervalNighttime);
        dest.writeInt(this.panicPin);
        dest.writeInt(this.panicDuressPin);
        dest.writeInt(this.bossModePin);
        dest.writeInt(this.defaultMap);
        dest.writeInt(this.generalTracker);
        dest.writeInt(this.trackerVisibleRange);
        dest.writeInt(this.alertRadius);
        dest.writeInt(this.aoiVisibleRange);
        dest.writeInt(this.team);
        dest.writeInt(this.startDaytime);
        dest.writeInt(this.endDaytime);
        dest.writeDouble(this.durationDaytime);
        dest.writeString(this.name);
        dest.writeString(this.emailAddress);
        dest.writeInt(this.alertSpeedRange);
        dest.writeInt(this.lowerSpeedRange);
        dest.writeInt(this.upperSpeedRange);
        dest.writeInt(this.dormantPeriodAlert);
        dest.writeString(this.phoneNumber);
        dest.writeString(this.sosNumber);
        dest.writeString(this.sosLabel);
        dest.writeString(this.nonEmergencyLabel);
        dest.writeString(this.nonEmergencyNumber);
        dest.writeString(this.panicMessage);
        dest.writeString(this.dayOfWeek);
        dest.writeParcelable(this.agentRoles, flags);
    }

    protected AgentSettings(Parcel in) {
        this.allowTracking = in.readInt();
        this.recordCalls = in.readInt();
        this.smsLog = in.readInt();
        this.smsLogDelivery = in.readInt();
        this.phoneLog = in.readInt();
        this.phoneLogDelivery = in.readInt();
        this.fixInterval = in.readInt();
        this.reportInterval = in.readInt();
        this.maxTracking = in.readInt();
        this.pingInterval = in.readInt();
        this.lowBattery = in.readInt();
        this.screen = in.readInt();
        this.photoQuality = in.readInt();
        this.photoSize = in.readInt();
        this.videoStreamQuality = in.readInt();
        this.videoStreamFrameRate = in.readInt();
        this.videoStreamSize = in.readInt();
        this.videoCasesQuality = in.readInt();
        this.videoCasesFrameRate = in.readInt();
        this.videoCasesSize = in.readInt();
        this.boss = in.readInt();
        this.security = in.readInt();
        this.mapOthers = in.readInt();
        this.mapMedia = in.readInt();
        this.mapMyself = in.readInt();
        this.autoTrack = in.readInt();
        this.showDetails = in.readInt();
        this.sensitivity = in.readInt();
        this.onStop = in.readInt();
        this.includeAudio = in.readInt();
        this.externalStorage = in.readInt();
        this.caseNumber = in.readString();
        this.caseDescription = in.readString();
        this.EULA = in.readInt();
        this.SMSPhoneNumber = in.readString();
        this.currentScreen = in.readInt();
        this.panicState = in.readInt();
        this.lastAddress = in.readString();
        this.lastAddressDate = (Calendar) in.readSerializable();
        this.panicTimerDate = (Calendar) in.readSerializable();
        this.remainingPanicTimer = in.readLong();
        this.vibrateOnFix = in.readInt();
        this.mapCapture = in.readInt();
        this.bossModeStyle = in.readInt();
        this.fixIntervalPanic = in.readInt();
        this.fixIntervalNighttime = in.readInt();
        this.panicPin = in.readInt();
        this.panicDuressPin = in.readInt();
        this.bossModePin = in.readInt();
        this.defaultMap = in.readInt();
        this.generalTracker = in.readInt();
        this.trackerVisibleRange = in.readInt();
        this.alertRadius = in.readInt();
        this.aoiVisibleRange = in.readInt();
        this.team = in.readInt();
        this.startDaytime = in.readInt();
        this.endDaytime = in.readInt();
        this.durationDaytime = in.readDouble();
        this.name = in.readString();
        this.emailAddress = in.readString();
        this.alertSpeedRange = in.readInt();
        this.lowerSpeedRange = in.readInt();
        this.upperSpeedRange = in.readInt();
        this.dormantPeriodAlert = in.readInt();
        this.phoneNumber = in.readString();
        this.sosNumber = in.readString();
        this.sosLabel = in.readString();
        this.nonEmergencyLabel = in.readString();
        this.nonEmergencyNumber = in.readString();
        this.panicMessage = in.readString();
        this.dayOfWeek = in.readString();
        this.agentRoles = in.readParcelable(AgentRoles.class.getClassLoader());
    }

    public static final Creator<AgentSettings> CREATOR = new Creator<AgentSettings>() {
        @Override
        public AgentSettings createFromParcel(Parcel source) {
            return new AgentSettings(source);
        }

        @Override
        public AgentSettings[] newArray(int size) {
            return new AgentSettings[size];
        }
    };
}
