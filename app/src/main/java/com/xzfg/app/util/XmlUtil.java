package com.xzfg.app.util;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 *
 */
public class XmlUtil {
    private XmlUtil() {
    }

    public static XmlPullParser createXmlParser(String xml) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));

        return parser;
    }

    public static String getXmlTagText(String xml, String tagName) {
        String result = null;

        try {
            XmlPullParser parser = createXmlParser(xml);
            int eventType = parser.getEventType();
            String currentTag = "";

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    currentTag = parser.getName();
                } else if (eventType == XmlPullParser.TEXT) {
                    if (currentTag.equalsIgnoreCase(tagName)) {
                        result = parser.getText();
                        break;
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

        return result;
    }
}
