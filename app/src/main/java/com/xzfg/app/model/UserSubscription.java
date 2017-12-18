package com.xzfg.app.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.transform.Transform;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Root(strict = false)
@SuppressWarnings("unused")

public class UserSubscription {

    @Element(required = false)
    String emailAddress;
    @Element(required = false)
    String name;
    @Element(required = false)
    Date subscriptionExpiryDate;
    @Element(required = false)
    Boolean subscriptionValid;
    @Element(required = false)
    Integer userType;
    @Element(required = false)
    String subscriptionProfileId;

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getSubscriptionExpiryDate() {
        return subscriptionExpiryDate;
    }

    public void set(Date subscriptionExpiryDate) {
        this.subscriptionExpiryDate = subscriptionExpiryDate;
    }

    public boolean getSubscriptionValid() {
        return subscriptionValid;
    }

    public void setSubscriptionValid(boolean subscriptionValid) {
        this.subscriptionValid = subscriptionValid;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public String getSubscriptionProfileId() {
        return subscriptionProfileId;
    }

    public void setSubscriptionProfileId(String subscriptionProfileId) {
        this.subscriptionProfileId = subscriptionProfileId;
    }

    public static class DateFormatTransformer implements Transform<Date> {
        private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public DateFormatTransformer() {
        }

        @Override
        public Date read(String value) throws Exception {
            return dateFormat.parse(value);
        }

        @Override
        public String write(Date value) throws Exception {
            return dateFormat.format(value);
        }
    }
}
