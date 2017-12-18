package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.xzfg.app.Application;
import com.xzfg.app.util.XmlUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("unused")
public class AgentProfile implements Parcelable {
    private String durationDaytime;
    private String name;
    private String emailAddress;
    private String phoneNumber;
    private ProfileSelectorList defaultMaps;
    private ProfileSelectorList generalTrackers;
    private ProfileSelectorList trackerVisibleRanges;
    private ProfileSelectorList alertRadiuses;
    private ProfileSelectorList aoiVisibleRanges;
    private ProfileSelectorList teams;
    private ProfileSelectorList dayOfWeeks;
    private ProfileSelectorList startDaytimes;
    private ProfileSelectorList endDaytimes;


    public AgentProfile() {
    }

    public AgentProfile(AgentProfile agentProfile) {
        this.setDurationDaytime(agentProfile.getDurationDaytime());
        this.setName(agentProfile.getName());
        this.setEmailAddress(agentProfile.getEmailAddress());
        this.setPhoneNumber(agentProfile.getPhoneNumber());
        this.setDefaultMaps(new ProfileSelectorList(agentProfile.getDefaultMaps()));
        this.setGeneralTrackers(new ProfileSelectorList(agentProfile.getGeneralTrackers()));
        this.setTrackerVisibleRanges(new ProfileSelectorList(agentProfile.getTrackerVisibleRanges()));
        this.setAlertRadiuses(new ProfileSelectorList(agentProfile.getAlertRadiuses()));
        this.setAoiVisibleRanges(new ProfileSelectorList(agentProfile.getAoiVisibleRanges()));
        this.setTeams(new ProfileSelectorList(agentProfile.getTeams()));
        this.setDayOfWeeks(new ProfileSelectorList(agentProfile.getDayOfWeeks()));
        this.setStartDaytimes(new ProfileSelectorList(agentProfile.getStartDaytimes()));
        this.setEndDaytimes(new ProfileSelectorList(agentProfile.getEndDaytimes()));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        AgentProfile that = (AgentProfile) o;

        if (getDurationDaytime() != null ? !getDurationDaytime().equals(that.getDurationDaytime()) : that.getDurationDaytime() != null)
            return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null)
            return false;
        if (getEmailAddress() != null ? !getEmailAddress().equals(that.getEmailAddress()) : that.getEmailAddress() != null)
            return false;
        if (getPhoneNumber() != null ? !getPhoneNumber().equals(that.getPhoneNumber()) : that.getPhoneNumber() != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = (getDurationDaytime() != null ? getDurationDaytime().hashCode() : 0);
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getEmailAddress() != null ? getEmailAddress().hashCode() : 0);
        result = 31 * result + (getPhoneNumber() != null ? getPhoneNumber().hashCode() : 0);
        result = 31 * result + getDefaultMaps().hashCode();
        result = 31 * result + getGeneralTrackers().hashCode();
        result = 31 * result + getTrackerVisibleRanges().hashCode();
        result = 31 * result + getAlertRadiuses().hashCode();
        result = 31 * result + getAoiVisibleRanges().hashCode();
        result = 31 * result + getTeams().hashCode();
        result = 31 * result + getDayOfWeeks().hashCode();
        result = 31 * result + getStartDaytimes().hashCode();
        result = 31 * result + getEndDaytimes().hashCode();

        return result;
    }

    @Override
    public String toString() {
        return "AgentProfile{" +
                "durationDaytime=" + getDurationDaytime() +
                "name=" + getName() +
                "emailAddress=" + getEmailAddress() +
                "phoneNumber=" + getPhoneNumber() +
                "defaultMaps=" + getDefaultMaps().toString() +
                "generalTrackers=" + getGeneralTrackers().toString() +
                "trackerVisibleRanges=" + getTrackerVisibleRanges().toString() +
                "alertRadiuses=" + getAlertRadiuses().toString() +
                "aoiVisibleRanges=" + getAoiVisibleRanges().toString() +
                "teams=" + getTeams().toString() +
                "dayOfWeeks=" + getDayOfWeeks().toString() +
                "startDaytimes=" + getStartDaytimes().toString() +
                "endDaytimes=" + getEndDaytimes().toString() +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.getDurationDaytime());
        dest.writeString(this.getName());
        dest.writeString(this.getEmailAddress());
        dest.writeString(this.getPhoneNumber());
        ProfileSelectorList.writeToParcel(dest, this.getDefaultMaps());
        ProfileSelectorList.writeToParcel(dest, this.getGeneralTrackers());
        ProfileSelectorList.writeToParcel(dest, this.getTrackerVisibleRanges());
        ProfileSelectorList.writeToParcel(dest, this.getAlertRadiuses());
        ProfileSelectorList.writeToParcel(dest, this.getAoiVisibleRanges());
        ProfileSelectorList.writeToParcel(dest, this.getTeams());
        ProfileSelectorList.writeToParcel(dest, this.getDayOfWeeks());
        ProfileSelectorList.writeToParcel(dest, this.getStartDaytimes());
        ProfileSelectorList.writeToParcel(dest, this.getEndDaytimes());
    }

    protected AgentProfile(Parcel in) {
        this.setDurationDaytime(in.readString());
        this.setName(in.readString());
        this.setEmailAddress(in.readString());
        this.setPhoneNumber(in.readString());
        this.setDefaultMaps(ProfileSelectorList.readFromParcel(in));
        this.setGeneralTrackers(ProfileSelectorList.readFromParcel(in));
        this.setTrackerVisibleRanges(ProfileSelectorList.readFromParcel(in));
        this.setAlertRadiuses(ProfileSelectorList.readFromParcel(in));
        this.setAoiVisibleRanges(ProfileSelectorList.readFromParcel(in));
        this.setTeams(ProfileSelectorList.readFromParcel(in));
        this.setDayOfWeeks(ProfileSelectorList.readFromParcel(in));
        this.setStartDaytimes(ProfileSelectorList.readFromParcel(in));
        this.setEndDaytimes(ProfileSelectorList.readFromParcel(in));
    }

    public static final Creator<AgentProfile> CREATOR = new Creator<AgentProfile>() {
        public AgentProfile createFromParcel(Parcel source) {
            return new AgentProfile(source);
        }

        public AgentProfile[] newArray(int size) {
            return new AgentProfile[size];
        }
    };

    public String getDurationDaytime() {
        return durationDaytime;
    }

    public void setDurationDaytime(String durationDaytime) {
        this.durationDaytime = durationDaytime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public ProfileSelectorList getDefaultMaps() {
        return defaultMaps;
    }

    public void setDefaultMaps(ProfileSelectorList defaultMaps) {
        this.defaultMaps = defaultMaps;
    }

    public ProfileSelectorList getGeneralTrackers() {
        return generalTrackers;
    }

    public void setGeneralTrackers(ProfileSelectorList generalTrackers) {
        this.generalTrackers = generalTrackers;
    }

    public ProfileSelectorList getTrackerVisibleRanges() {
        return trackerVisibleRanges;
    }

    public void setTrackerVisibleRanges(ProfileSelectorList trackerVisibleRanges) {
        this.trackerVisibleRanges = trackerVisibleRanges;
    }

    public ProfileSelectorList getAlertRadiuses() {
        return alertRadiuses;
    }

    public void setAlertRadiuses(ProfileSelectorList alertRadiuses) {
        this.alertRadiuses = alertRadiuses;
    }

    public ProfileSelectorList getAoiVisibleRanges() {
        return aoiVisibleRanges;
    }

    public void setAoiVisibleRanges(ProfileSelectorList aoiVisibleRanges) {
        this.aoiVisibleRanges = aoiVisibleRanges;
    }

    public ProfileSelectorList getTeams() {
        return teams;
    }

    public void setTeams(ProfileSelectorList teams) {
        this.teams = teams;
    }

    public ProfileSelectorList getDayOfWeeks() {
        return dayOfWeeks;
    }

    public void setDayOfWeeks(ProfileSelectorList dayOfWeeks) {
        this.dayOfWeeks = dayOfWeeks;
    }

    public ProfileSelectorList getStartDaytimes() {
        return startDaytimes;
    }

    public void setStartDaytimes(ProfileSelectorList startDaytimes) {
        this.startDaytimes = startDaytimes;
    }

    public ProfileSelectorList getEndDaytimes() {
        return endDaytimes;
    }

    public void setEndDaytimes(ProfileSelectorList endDaytimes) {
        this.endDaytimes = endDaytimes;
    }


    public static AgentProfile parse(Application application, String input) throws Exception {
        final String defaultMap = "defaultmap";
        final String defaultMaps = defaultMap + "s";
        final String generalTracker = "generaltracker";
        final String generalTrackers = generalTracker + "s";
        final String trackerVisibleRange = "trackervisiblerange";
        final String trackerVisibleRanges = trackerVisibleRange + "s";
        final String alertRadius = "alertradius";
        final String alertRadiuses = alertRadius + "es";
        final String aoiVisibleRange = "aoivisiblerange";
        final String aoiVisibleRanges = aoiVisibleRange + "s";
        final String team = "team";
        final String teams = team + "s";
        final String dayOfWeek = "dayofweek";
        final String dayOfWeeks = dayOfWeek + "s";
        final String startDaytime = "startdaytime";
        final String startDaytimes = startDaytime + "s";
        final String endDaytime = "enddaytime";
        final String endDaytimes = endDaytime + "s";
        final String durationDaytime = "durationdaytime";
        final String name = "name";
        final String emailAddress = "emailaddress";
        final String phoneNumber = "phonenumber";
        final String pushToken = "pushtoken";


        AgentProfile profile = application.getAgentProfile();
        if (profile == null) {
            profile = new AgentProfile();
        }

        // Parse from XML
        try {
            XmlPullParser parser = XmlUtil.createXmlParser(input);
            int eventType = parser.getEventType();
            int prevDepth = 0;
            String currentTag = "", parentTag = "";

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    currentTag = parser.getName().toLowerCase();
                    if (parser.getDepth() < 3) {
                        parentTag = currentTag;
                    }

                    try {
                        switch (currentTag) {
                            case defaultMaps:
                                profile.setDefaultMaps(new ProfileSelectorList());
                                break;
                            case defaultMap:
                                if (parentTag.equals(defaultMaps)) {
                                    ProfileSelector item = new ProfileSelector(parser);
                                    profile.getDefaultMaps().add(item);
                                }
                                break;
                            case generalTrackers:
                                profile.setGeneralTrackers(new ProfileSelectorList());
                                break;
                            case generalTracker:
                                if (parentTag.equals(generalTrackers)) {
                                    ProfileSelector item = new ProfileSelector(parser);
                                    profile.getGeneralTrackers().add(item);
                                }
                                break;
                            case trackerVisibleRanges:
                                profile.setTrackerVisibleRanges(new ProfileSelectorList());
                                break;
                            case trackerVisibleRange:
                                if (parentTag.equals(trackerVisibleRanges)) {
                                    ProfileSelector item = new ProfileSelector(parser);
                                    profile.getTrackerVisibleRanges().add(item);
                                }
                                break;
                            case alertRadiuses:
                                profile.setAlertRadiuses(new ProfileSelectorList());
                                break;
                            case alertRadius:
                                if (parentTag.equals(alertRadiuses)) {
                                    ProfileSelector item = new ProfileSelector(parser);
                                    profile.getAlertRadiuses().add(item);
                                }
                                break;
                            case aoiVisibleRanges:
                                profile.setAoiVisibleRanges(new ProfileSelectorList());
                                break;
                            case aoiVisibleRange:
                                if (parentTag.equals(aoiVisibleRanges)) {
                                    ProfileSelector item = new ProfileSelector(parser);
                                    profile.getAoiVisibleRanges().add(item);
                                }
                                break;
                            case teams:
                                profile.setTeams(new ProfileSelectorList());
                                break;
                            case team:
                                if (parentTag.equals(teams)) {
                                    ProfileSelector item = new ProfileSelector(parser);
                                    profile.getTeams().add(item);
                                }
                                break;
                            case dayOfWeeks:
                                profile.setDayOfWeeks(new ProfileSelectorList());
                                break;
                            case dayOfWeek:
                                if (parentTag.equals(dayOfWeeks)) {
                                    ProfileSelector item = new ProfileSelector(parser);
                                    profile.getDayOfWeeks().add(item);
                                }
                                break;
                            case startDaytimes:
                                profile.setStartDaytimes(new ProfileSelectorList());
                                break;
                            case startDaytime:
                                if (parentTag.equals(startDaytimes)) {
                                    ProfileSelector item = new ProfileSelector(parser);
                                    profile.getStartDaytimes().add(item);
                                }
                                break;
                            case endDaytimes:
                                profile.setEndDaytimes(new ProfileSelectorList());
                                break;
                            case endDaytime:
                                if (parentTag.equals(endDaytimes)) {
                                    ProfileSelector item = new ProfileSelector(parser);
                                    profile.getEndDaytimes().add(item);
                                }
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (eventType == XmlPullParser.TEXT) {

                    try {
                        switch (currentTag) {
                            case durationDaytime:
                                profile.setDurationDaytime(parser.getText());
                                break;
                            case name:
                                profile.setName(parser.getText());
                                break;
                            case emailAddress:
                                profile.setEmailAddress(parser.getText());
                                break;
                            case phoneNumber:
                                profile.setPhoneNumber(parser.getText());
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    currentTag = "";
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return profile;
    }


    // Parse to Url params
    public HashMap<String, String> getUrlParams() {
        HashMap<String, String> result = new HashMap<>();

        result.put("defaultMapId", (defaultMaps == null || defaultMaps.getSelected() == null) ? "" : defaultMaps.getSelected().getId());
        result.put("generalTrackerId", (generalTrackers == null || generalTrackers.getSelected() == null) ? "" : generalTrackers.getSelected().getId());
        result.put("trackerVisibleRangeId", (trackerVisibleRanges == null || trackerVisibleRanges.getSelected() == null) ? "" : trackerVisibleRanges.getSelected().getId());
        result.put("alertRadiusId", (alertRadiuses == null || alertRadiuses.getSelected() == null) ? "" : alertRadiuses.getSelected().getId());
        result.put("aoiVisibleRangeId", (aoiVisibleRanges == null || aoiVisibleRanges.getSelected() == null) ? "" : aoiVisibleRanges.getSelected().getId());
        result.put("teamId", (teams == null || teams.getSelected() == null) ? "" : teams.getSelected().getId());
        result.put("dayOfWeek", (dayOfWeeks == null || dayOfWeeks.getSelected() == null) ? "" : dayOfWeeks.getSelected().getId());
        result.put("startDaytime", (startDaytimes == null || startDaytimes.getSelected() == null) ? "" : startDaytimes.getSelected().getId());
        result.put("endDaytime", (endDaytimes == null || endDaytimes.getSelected() == null) ? "" : endDaytimes.getSelected().getId());
        result.put("durationDaytime", durationDaytime == null ? "" : durationDaytime);
        result.put("name", name == null ? "" : name);
        result.put("emailAddress", emailAddress == null ? "" : emailAddress);
        result.put("phoneNumber", phoneNumber == null ? "" : phoneNumber);

        return result;
    }


    // classes
    public static class ProfileSelectorList extends ArrayList<ProfileSelector> {

        // default constructor
        public ProfileSelectorList() {
        }

        // copy constructor
        public ProfileSelectorList(ProfileSelectorList that) {
            for (ProfileSelector item : that) {
                add(new ProfileSelector(item));
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            ProfileSelectorList that = (ProfileSelectorList) o;

            if (size() != that.size()) return false;

            for (int i = 0; i < size(); i++) {
                if (!this.get(i).equals(that.get(i))) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public String toString() {
            String result = "ProfileSelectorList{";
            for (ProfileSelector item : this) {
                result += item.toString();
            }
            result += '}';
            return result;
        }

        @Override
        public int hashCode() {
            int result = 0;
            for (ProfileSelector item : this) {
                result += item.hashCode();
            }

            return result;
        }

        public static void writeToParcel(Parcel dest, ProfileSelectorList list) {
            if (list == null) {
                dest.writeInt(0);
            } else {
                dest.writeInt(list.size());
                for (ProfileSelector item : list) {
                    ProfileSelector.writeToParcel(dest, item);
                }
            }
        }

        public static ProfileSelectorList readFromParcel(Parcel in) {
            ProfileSelectorList result = new ProfileSelectorList();
            int count = in.readInt();
            while ((count--) > 0) {
                result.add(ProfileSelector.readFromParcel(in));
            }
            return result;
        }

        // Returns 0-based index of "selected" item
        public int getSelectedPosition() {
            for (int i = 0; i < this.size(); i++) {
                ProfileSelector item = this.get(i);
                if (item != null && item.isSelected()) {
                    return i;
                }
            }

            // HACK: Return 1st item if noting is selected or server will crash
            if (this.size() > 0) {
                return 0;
            }

            return -1;
        }

        // Returns "selected" item
        public ProfileSelector getSelected() {
            int pos = getSelectedPosition();
            return (pos < 0 ? null : this.get(pos));
        }

        // Set "seleted" item
        public void setSelected(int position) {
            for (int i = 0; i < this.size(); i++) {
                ProfileSelector item = this.get(i);
                if (item != null) {
                    item.setSelected(i == position ? "1" : "0");
                }
            }
        }

        public ArrayList<String> getNames() {
            ArrayList<String> result = new ArrayList<>(this.size());
            for (ProfileSelector item : this) {
                result.add(item == null ? "" : item.getName());
            }
            return result;
        }
    }


    //
    public static class ProfileSelector {
        private String id;
        private String name;
        private String selected;

        // default constructor
        public ProfileSelector() {
        }

        // copy constructor
        public ProfileSelector(ProfileSelector that) {
            this(that.id, that.name, that.selected);
        }

        public ProfileSelector(XmlPullParser parser) {
            this(parser.getAttributeValue(null, "id"),
                    parser.getAttributeValue(null, "name"),
                    parser.getAttributeValue(null, "selected"));
        }

        public ProfileSelector(String id, String name, String selected) {
            this.id = id;
            this.name = name;
            this.selected = selected;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            ProfileSelector that = (ProfileSelector) o;

            if (id != null ? !id.equals(that.id) : that.id != null)
                return false;

            if (name != null ? !name.equals(that.name) : that.name != null)
                return false;

            return (selected == that.selected);
        }

        @Override
        public String toString() {
            return "ProfileSelector{" +
                    "id=" + id +
                    ", name=" + name +
                    ", selected=" + selected +
                    '}';
        }

        @Override
        public int hashCode() {
            int result = (id != null ? id.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (selected != null ? selected.hashCode() : 0);

            return result;
        }

        public static void writeToParcel(Parcel dest, ProfileSelector item) {
            // Write 0 if null item
            if (item == null) {
                dest.writeInt(0);
            } else {
                dest.writeInt(1);
                dest.writeString(item.id);
                dest.writeString(item.name);
                dest.writeString(item.selected);
            }
        }

        public static ProfileSelector readFromParcel(Parcel in) {
            if (in.readInt() != 0) {
                // Read the rest of data if got non zero int
                return new ProfileSelector(in.readString(), in.readString(), in.readString());
            }
            return null;
        }

        public boolean isSelected() {
            return ("1".equals(selected));
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSelected() {
            return selected;
        }

        public void setSelected(String selected) {
            this.selected = selected;
        }
    }

}
