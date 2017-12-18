package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.xzfg.app.Application;
import com.xzfg.app.util.XmlUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("unused")
public class AgentContacts implements Parcelable {
    private AgentContactsList contacts = new AgentContactsList();

    public AgentContacts() {
    }

    public AgentContacts(AgentContacts contacts) {
        // Do deep copy
        for (AgentContact c : contacts.getContacts()) {
            this.contacts.add(new AgentContact(c));
        }
        //this.setContacts(contacts.getContacts());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        AgentContacts that = (AgentContacts) o;

        if (getContacts() != null ? !getContacts().equals(that.getContacts()) : that.getContacts() != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (getContacts() != null ? getContacts().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AgentContacts{" + getContacts().toString() + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AgentContactsList.writeToParcel(dest, this.getContacts());
    }

    protected AgentContacts(Parcel in) {
        this.setContacts(AgentContactsList.readFromParcel(in));
    }

    public static final Creator<AgentContacts> CREATOR = new Creator<AgentContacts>() {
        public AgentContacts createFromParcel(Parcel source) {
            return new AgentContacts(source);
        }

        public AgentContacts[] newArray(int size) {
            return new AgentContacts[size];
        }
    };

    public AgentContactsList getContacts() {
        return contacts;
    }

    public void setContacts(AgentContactsList contacts) {
        this.contacts = contacts;
    }

    public static AgentContacts parse(Application application, String input) throws Exception {
        final String contactTag = "contact";
        AgentContactsList contacts = new AgentContactsList();

        AgentContacts agentContacts = application.getAgentContacts();
        if (agentContacts == null) {
            agentContacts = new AgentContacts();
        }

        // Parse from XML
        try {
            XmlPullParser parser = XmlUtil.createXmlParser(input);
            int eventType = parser.getEventType();
            int prevDepth = 0;
            String currentTag = "", parentTag = "";

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    try {
                        currentTag = parser.getName().toLowerCase();
                        if (parser.getDepth() < 2) {
                            parentTag = currentTag;
                        }

                        switch (currentTag) {
                            case contactTag:
                                if (parentTag.equals(contactTag + "s")) {
                                    contacts.add(new AgentContact(parser));
                                }
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                eventType = parser.next();
            }
            agentContacts.setContacts(contacts);

        } catch (XmlPullParserException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return agentContacts;
    }

    public String toXml(String serial, String password) {
        String result = String.format("<?xml version='1.0' encoding='utf-8' ?><contacts serial=\"%s\" password=\"%s\">", serial, password);

        for (AgentContact contact : getContacts()) {
            result += String.format("<contact id=\"%s\" name=\"%s\" emailAddress=\"%s\" phoneNumber=\"%s\" isFamilyMember=\"%s\" />",
                    contact.getId(), contact.getName(), contact.getEmailAddress(), contact.getPhoneNumber(), contact.getFamilyMember());
        }

        result += "</contacts>";

        return result;
    }


    // classes
    public static class AgentContactsList extends ArrayList<AgentContact> {

        // default constructor
        public AgentContactsList() {
        }

        // copy constructor
        public AgentContactsList(AgentContactsList that) {
            for (AgentContact item : that) {
                add(new AgentContact(item));
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            AgentContactsList that = (AgentContactsList) o;

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
            String result = "AgentContactsList{";
            for (AgentContact item : this) {
                result += item.toString();
            }
            result += '}';
            return result;
        }

        @Override
        public int hashCode() {
            int result = 0;
            for (AgentContact item : this) {
                result += item.hashCode();
            }

            return result;
        }

        public static void writeToParcel(Parcel dest, AgentContactsList list) {
            if (list == null) {
                dest.writeInt(0);
            } else {
                dest.writeInt(list.size());
                for (AgentContact item : list) {
                    AgentContact.writeToParcel(dest, item);
                }
            }
        }

        public static AgentContactsList readFromParcel(Parcel in) {
            AgentContactsList result = new AgentContactsList();
            int count = in.readInt();
            while ((count--) > 0) {
                result.add(AgentContact.readFromParcel(in));
            }
            return result;
        }
    }


    //
    public static class AgentContact {
        private String id;
        private String name;
        private String emailAddress;
        private String phoneNumber;
        private String familyMember;

        // default constructor
        public AgentContact() {
        }

        // copy constructor
        public AgentContact(AgentContact that) {
            this(that.id, that.name, that.emailAddress, that.phoneNumber, that.familyMember);
        }

        public AgentContact(XmlPullParser parser) {
            this(parser.getAttributeValue(null, "id"),
                    parser.getAttributeValue(null, "name"),
                    parser.getAttributeValue(null, "emailAddress"),
                    parser.getAttributeValue(null, "phoneNumber"),
                    parser.getAttributeValue(null, "isFamilyMember")
            );
        }

        public AgentContact(String id, String name, String emailAddress,
                            String phoneNumber, String familyMember) {
            this.id = id;
            this.name = name;
            this.emailAddress = emailAddress;
            this.phoneNumber = phoneNumber;
            this.familyMember = familyMember;
        }

        public AgentContact(String id, String name, String emailAddress,
                            String phoneNumber, boolean familyMember) {
            this(id, name, emailAddress, phoneNumber, familyMember ? "1" : "0");
        }

        public void set(AgentContact that) {
            this.id = that.id;
            this.name = that.name;
            this.emailAddress = that.emailAddress;
            this.phoneNumber = that.phoneNumber;
            this.familyMember = that.familyMember;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            AgentContact that = (AgentContact) o;

            if (id != null ? !id.equals(that.id) : that.id != null)
                return false;

            if (name != null ? !name.equals(that.name) : that.name != null)
                return false;

            if (emailAddress != null ? !emailAddress.equals(that.emailAddress) : that.emailAddress != null)
                return false;

            if (phoneNumber != null ? !phoneNumber.equals(that.phoneNumber) : that.phoneNumber != null)
                return false;

            if (familyMember != null ? !familyMember.equals(that.familyMember) : that.familyMember != null)
                return false;

            return true;
        }

        @Override
        public String toString() {
            return "AgentContact{" +
                    "id=" + id +
                    ", name=" + name +
                    ", emailAddress=" + emailAddress +
                    ", phoneNumber=" + phoneNumber +
                    ", isFamilyMember=" + familyMember +
                    '}';
        }

        @Override
        public int hashCode() {
            int result = (id != null ? id.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (emailAddress != null ? emailAddress.hashCode() : 0);
            result = 31 * result + (phoneNumber != null ? phoneNumber.hashCode() : 0);
            result = 31 * result + (familyMember != null ? familyMember.hashCode() : 0);

            return result;
        }

        public static void writeToParcel(Parcel dest, AgentContact item) {
            // Write 0 if null item
            if (item == null) {
                dest.writeInt(0);
            } else {
                dest.writeInt(1);
                dest.writeString(item.id);
                dest.writeString(item.name);
                dest.writeString(item.emailAddress);
                dest.writeString(item.phoneNumber);
                dest.writeString(item.familyMember);
            }
        }

        public static AgentContact readFromParcel(Parcel in) {
            if (in.readInt() != 0) {
                // Read the rest of data if got non zero int
                return new AgentContact(in.readString(), in.readString(),
                        in.readString(), in.readString(), in.readString());
            }
            return null;
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

        public String getFamilyMember() {
            return familyMember;
        }

        public void setFamilyMember(String familyMember) {
            this.familyMember = familyMember;
        }

        public boolean isFamilyMember() {
            return ("1".equals(familyMember));
        }
    }

}
