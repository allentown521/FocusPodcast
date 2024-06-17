package allen.town.podcast.core.util;

import android.content.Context;
import android.text.format.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import allen.town.focus_common.util.EntityDateUtils;
import allen.town.podcast.core.R;

/**
 * Formats dates.
 */
public class DateFormatter {
    private DateFormatter() {

    }

    public static String formatRfc822Date(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yy HH:mm:ss Z", Locale.US);
        return format.format(date);
    }

    public static String formatAbbrev(final Context context, final Date date) {
        if (date == null) {
            return "";
        }
        long time = date.getTime();
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        boolean withinLastYear = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR);
        int format = android.text.format.DateUtils.FORMAT_ABBREV_ALL;
        if (withinLastYear) {
            format |= android.text.format.DateUtils.FORMAT_NO_YEAR;
        }

        if (DateUtils.isToday(time)) {
            //今天
            return context.getString(R.string.today);

        } else if (EntityDateUtils.isYesterday(time)) {
            //昨天
            return context.getString(R.string.yesterday);
        }

        return android.text.format.DateUtils.formatDateTime(context, time, format);
    }


    public static String formatForAccessibility(final Date date) {
        if (date == null) {
            return "";
        }
        return DateFormat.getDateInstance(DateFormat.LONG).format(date);
    }
}
