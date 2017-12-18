package com.xzfg.app.util;

import android.content.Context;
import android.widget.TextView;

import com.xzfg.app.R;

import net.danlew.android.joda.DateUtils;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 */
public class DateUtil {
    private DateUtil() {
    }

    public static void formatDate(TextView textView, Date date) {
        if (textView != null) {
            if (date != null) {
                int days = Math.abs(Days.daysBetween(new DateTime(date).toLocalDate(), new DateTime().toLocalDate()).getDays());
                if (days < 7) {
                    if (days == 0) {
                        textView.setText(DateUtils.formatDateTime(textView.getContext(), new DateTime(date), DateUtils.FORMAT_SHOW_TIME));
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", textView.getContext().getResources().getConfiguration().locale);
                        sdf.setTimeZone(TimeZone.getDefault());
                        textView.setText(sdf.format(date));
                    }
                } else {
                    textView.setText(DateUtils.formatDateTime(textView.getContext(), new DateTime(date), DateUtils.FORMAT_SHOW_DATE));
                }
            } else {
                textView.setText("");
            }
        }
    }

    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }


    public static String formatLastAddressDate(Context context, Calendar date) {
        String result = context.getString(R.string.unknown);
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();

        yesterday.setTimeInMillis(today.getTimeInMillis());
        yesterday.add(Calendar.HOUR, -24);

        if (date != null) {
            if (isSameDay(date, today)) {
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a");
                result = sdf.format(date.getTime());
            }
            else if (isSameDay(date, yesterday)) {
                result = "Yesterday";
            }
            else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");
                result = sdf.format(date.getTime());
            }
        }

        return result;
    }


}
