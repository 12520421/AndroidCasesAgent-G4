package com.xzfg.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.xzfg.app.Application;
import com.xzfg.app.util.XmlUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class CannedMessages implements Parcelable {
    private List<String> messages = new ArrayList<>();

    public CannedMessages() {
    }

    public CannedMessages(CannedMessages cannedMessages) {
        this.setMessages(cannedMessages.getMessages());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        CannedMessages that = (CannedMessages) o;

        if (getMessages() != null ? !getMessages().equals(that.getMessages()) : that.getMessages() != null)
            return false;

        return true;
    }

    public boolean isEmpty() {
        return (messages == null || messages.isEmpty());
    }

    @Override
    public int hashCode() {
        int result = (getMessages() != null ? getMessages().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CannedMessages{" +
                "messages=" + getMessages() +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(this.getMessages());
    }

    protected CannedMessages(Parcel in) {
        in.readStringList(this.messages);
    }

    public static final Creator<CannedMessages> CREATOR = new Creator<CannedMessages>() {
        public CannedMessages createFromParcel(Parcel source) {
            return new CannedMessages(source);
        }

        public CannedMessages[] newArray(int size) {
            return new CannedMessages[size];
        }
    };

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = new ArrayList<>();
        for (String message : messages) {
            this.messages.add(message);
        }
    }

    public static CannedMessages parse(Application application, String input) throws Exception {
        final String cannedMessageTag = "cannedmessage";
        List<String> messages = new ArrayList<>();

        CannedMessages cannedMessages = application.getCannedMessages();
        if (cannedMessages == null) {
            cannedMessages = new CannedMessages();
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
                            case cannedMessageTag:
                                if (parentTag.equals(cannedMessageTag + "s")) {
                                    messages.add(parser.getAttributeValue(null, "text"));
                                }
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                eventType = parser.next();
            }
            cannedMessages.setMessages(messages);

        } catch (XmlPullParserException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cannedMessages;
    }

}
