package com.xzfg.app.util;

import org.simpleframework.xml.transform.Transform;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 */
public class DateTransformer implements Transform<Date> {
    private static final String dateFormat = "MM/dd/yyy h:mm:s a";

    @Override
    public Date read(String value) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date utcDate = sdf.parse(value);
        return utcDate;
    }

    @Override
    public String write(Date value) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(value);
    }
}
