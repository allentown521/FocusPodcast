package allen.town.podcast.core.util;

import android.content.Context;
import android.util.Pair;

import java.util.Locale;

import allen.town.podcast.core.R;

/**
 * Provides methods for converting various units.
 */
public final class Converter {
    /**
     * Class shall not be instantiated.
     */
    private Converter() {
    }

    private static final int HOURS_MIL = 3600000;
    private static final int MINUTES_MIL = 60000;
    private static final int SECONDS_MIL = 1000;

    /**
     * Converts milliseconds to a string containing hours, minutes and seconds.
     */
    public static String getDurationStringLong(int duration) {
        if (duration <= 0) {
            return "00:00:00";
        } else {
            int[] hms = millisecondsToHms(duration);
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hms[0], hms[1], hms[2]);
        }
    }

    private static int[] millisecondsToHms(long duration) {
        int h = (int) (duration / HOURS_MIL);
        long rest = duration - h * HOURS_MIL;
        int m = (int) (rest / MINUTES_MIL);
        rest -= m * MINUTES_MIL;
        int s = (int) (rest / SECONDS_MIL);
        return new int[]{h, m, s};
    }

    /**
     * Converts milliseconds to a string containing hours and minutes or minutes and seconds.
     */
    public static String getDurationStringShort(int duration, boolean durationIsInHours) {
        int firstPartBase = durationIsInHours ? HOURS_MIL : MINUTES_MIL;
        int firstPart = duration / firstPartBase;
        int leftoverFromFirstPart = duration - firstPart * firstPartBase;
        int secondPart = leftoverFromFirstPart / (durationIsInHours ? MINUTES_MIL : SECONDS_MIL);

        return String.format(Locale.getDefault(), "%02d:%02d", firstPart, secondPart);
    }

    /**
     * Converts long duration string (HH:MM:SS) to milliseconds.
     */
    public static int durationStringLongToMs(String input) {
        String[] parts = input.split(":");
        if (parts.length != 3) {
            return 0;
        }
        return Integer.parseInt(parts[0]) * 3600 * 1000
                + Integer.parseInt(parts[1]) * 60 * 1000
                + Integer.parseInt(parts[2]) * 1000;
    }

    /**
     * Converts short duration string (XX:YY) to milliseconds. If durationIsInHours is true then the
     * format is HH:MM, otherwise it's MM:SS.
     */
    public static int durationStringShortToMs(String input, boolean durationIsInHours) {
        String[] parts = input.split(":");
        if (parts.length != 2) {
            return 0;
        }

        int modifier = durationIsInHours ? 60 : 1;

        return Integer.parseInt(parts[0]) * 60 * 1000 * modifier
                + Integer.parseInt(parts[1]) * 1000 * modifier;
    }

    /**
     * Converts milliseconds to a localized string containing hours and minutes.
     */
    public static String getDurationStringLocalized(Context context, long duration) {
        int h = (int) (duration / HOURS_MIL);
        int rest = (int) (duration - h * HOURS_MIL);
        int m = rest / MINUTES_MIL;

        String result = "";
        if (h > 0) {
            String hours = context.getResources().getQuantityString(R.plurals.time_hours_quantified, h, h);
            result += hours + " ";
        }
        String minutes = context.getResources().getQuantityString(R.plurals.time_minutes_quantified, m, m);
        result += minutes;
        return result;
    }

    /**
     * Converts seconds to a localized representation.
     *
     * @param time The time in seconds
     * @return "HH:MM hours"
     */
    public static Pair<String, String> shortLocalizedDuration(Context context, long time) {
        float hours = (float) time / 3600f;
        //小于1个小时
        if (hours < 1) {
            if (time < 60) {
                //显示秒
                return new Pair(String.format(Locale.getDefault(), "%d ", time) + context.getString(R.string.time_seconds), "");
            } else {
                //显示分钟
                return new Pair(String.format(Locale.getDefault(), "%d ", time / 60) + context.getString(R.string.time_minutes), "");
            }
        } else {
            if (hours < 24) {
                //显示小时和分
                int hour = (int) (time / 3600);
                String hourStr = String.format(Locale.getDefault(), "%d ", hour) + context.getString(R.string.time_hours);
                String minStr = String.format(Locale.getDefault(), "%d ", (time - hour * 3600L) / 60) + context.getString(R.string.time_minutes);
                return new Pair(hourStr, minStr);
            } else if (hours < 24 * 365) {
                //显示天和小时
                int days = (int) (time / (3600 * 24));
                String dayStr = String.format(Locale.getDefault(), "%d ", days) + context.getString(R.string.time_days);
                String hourStr = String.format(Locale.getDefault(), "%d ", (time - days * 24 * 3600L) / 3600) + context.getString(R.string.time_hours);
                return new Pair(dayStr, hourStr);
            } else {
                //显示年和天
                int years = (int) (time / (3600 * 24 * 365));
                String yearStr = String.format(Locale.getDefault(), "%d ", years) + context.getString(R.string.time_year);
                String dayStr = String.format(Locale.getDefault(), "%d ", (time - years * 24 * 365 * 3600L) / (3600 * 24)) + context.getString(R.string.time_days);
                return new Pair(yearStr, dayStr);
            }
        }
    }
}
