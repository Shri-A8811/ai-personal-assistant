package com.mitaoe.shridhar202401040197.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeUtil {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

    public static String formatTime(long millis) {
        if (millis <= 0) return "";
        return TIME_FORMAT.format(new Date(millis));
    }

    public static String formatDate(long millis) {
        if (millis <= 0) return "";
        return DATE_FORMAT.format(new Date(millis));
    }

    public static String formatDateTime(long millis) {
        if (millis <= 0) return "";
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);

        if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            return "Today at " + TIME_FORMAT.format(new Date(millis));
        }

        now.add(Calendar.DAY_OF_YEAR, 1);
        if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            return "Tomorrow at " + TIME_FORMAT.format(new Date(millis));
        }

        return DATE_TIME_FORMAT.format(new Date(millis));
    }

    public static long getEndOfTodayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    public static long parseNaturalDate(String text) {
        if (text == null) return 0;
        String lower = text.toLowerCase(Locale.ROOT);
        Calendar cal = Calendar.getInstance();

        boolean dateSet = false;
        if (lower.contains("tomorrow")) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            dateSet = true;
        } else if (lower.contains("today") || lower.contains("tonight")) {
            dateSet = true;
        }

        // Search for time pattern with explicit time indicators:
        // (1) with am/pm (e.g. 5pm, 6:30 am)
        // (2) with colon (e.g. 14:30, 5:00)
        // (3) preceded by 'at' or '@' (e.g. at 5, @ 6)
        // (4) followed by o'clock
        Pattern timePattern = Pattern.compile("(?i)(?:\\b(at|@)\\s*)?(\\b\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|o'clock)?\\b");
        Matcher matcher = timePattern.matcher(lower);

        int hour = -1;
        int minute = 0;

        while (matcher.find()) {
            try {
                String prefix = matcher.group(1);
                String hStr = matcher.group(2);
                String mStr = matcher.group(3);
                String suffix = matcher.group(4);

                boolean hasPrefix = prefix != null && !prefix.isEmpty();
                boolean hasColon = mStr != null && !mStr.isEmpty();
                boolean hasSuffix = suffix != null && !suffix.isEmpty();

                // If it's a naked number with no time signals, skip it (e.g. "2" apples, chapter "4")
                if (!hasPrefix && !hasColon && !hasSuffix) {
                    continue;
                }

                if (hStr != null) {
                    int parsedHour = Integer.parseInt(hStr);
                    if (parsedHour >= 0 && parsedHour <= 24) {
                        hour = parsedHour;
                        if (hasColon) {
                            minute = Integer.parseInt(mStr);
                        }
                        if (suffix != null) {
                            if (suffix.equalsIgnoreCase("pm") && hour < 12) hour += 12;
                            if (suffix.equalsIgnoreCase("am") && hour == 12) hour = 0;
                        } else if (hour < 8 && !lower.contains("morning") && !hasColon) {
                            hour += 12;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (hour != -1) {
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }

        if (dateSet) {
            // Default 9:00 AM
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            return cal.getTimeInMillis();
        }

        return 0;
    }
}
