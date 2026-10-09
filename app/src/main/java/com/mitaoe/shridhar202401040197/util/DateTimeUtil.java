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
        String lower = text.toLowerCase(Locale.ROOT).trim();
        Calendar now = Calendar.getInstance();
        Calendar cal = Calendar.getInstance();

        // 1. Relative offsets: "in 2 hours", "in 30 mins", "in 15 minutes"
        Pattern relativePattern = Pattern.compile("\\bin\\s+(\\d+)\\s*(hour|hr|minute|min)s?\\b");
        Matcher relMatcher = relativePattern.matcher(lower);
        if (relMatcher.find()) {
            try {
                int amount = Integer.parseInt(relMatcher.group(1));
                String unit = relMatcher.group(2);
                if (unit != null && unit.startsWith("h")) {
                    cal.add(Calendar.HOUR_OF_DAY, amount);
                } else {
                    cal.add(Calendar.MINUTE, amount);
                }
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return cal.getTimeInMillis();
            } catch (Exception ignored) {}
        }

        boolean dateSet = false;
        int defaultHour = 9;

        // 2. Relative Days
        if (lower.contains("tomorrow")) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            dateSet = true;
        } else if (lower.contains("tonight")) {
            dateSet = true;
            defaultHour = 20; // 8 PM
        } else if (lower.contains("today")) {
            dateSet = true;
        }

        // 3. Weekday matching ("next monday", "on friday", etc.)
        String[] days = {"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
        for (int i = 0; i < days.length; i++) {
            Pattern dayPattern = Pattern.compile("\\b(?:next\\s+|on\\s+)?(" + days[i] + ")\\b");
            Matcher dayMatcher = dayPattern.matcher(lower);
            if (dayMatcher.find()) {
                int targetDayOfWeek = i + 1; // Calendar.SUNDAY = 1, MONDAY = 2...
                int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
                int daysToAdd = (targetDayOfWeek - currentDayOfWeek + 7) % 7;
                if (daysToAdd == 0 || lower.contains("next " + days[i])) {
                    daysToAdd += 7;
                }
                cal.setTimeInMillis(now.getTimeInMillis());
                cal.add(Calendar.DAY_OF_YEAR, daysToAdd);
                dateSet = true;
                break;
            }
        }

        // 4. Month date matching: "on 15 oct", "15 october", "oct 15"
        String[] months = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
        for (int m = 0; m < months.length; m++) {
            // "15 oct" or "15th october"
            Pattern mPattern1 = Pattern.compile("\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+" + months[m] + "[a-z]*\\b");
            Matcher mMatch1 = mPattern1.matcher(lower);
            if (mMatch1.find()) {
                try {
                    int day = Integer.parseInt(mMatch1.group(1));
                    cal.set(Calendar.MONTH, m);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    if (cal.before(now)) {
                        cal.add(Calendar.YEAR, 1);
                    }
                    dateSet = true;
                    break;
                } catch (Exception ignored) {}
            }

            // "oct 15" or "october 15th"
            Pattern mPattern2 = Pattern.compile("\\b" + months[m] + "[a-z]*\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b");
            Matcher mMatch2 = mPattern2.matcher(lower);
            if (mMatch2.find()) {
                try {
                    int day = Integer.parseInt(mMatch2.group(1));
                    cal.set(Calendar.MONTH, m);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    if (cal.before(now)) {
                        cal.add(Calendar.YEAR, 1);
                    }
                    dateSet = true;
                    break;
                } catch (Exception ignored) {}
            }
        }

        // 5. Explicit time search:
        // (1) with am/pm (e.g. 5pm, 6:30 am)
        // (2) with colon (e.g. 14:30, 5:00)
        // (3) preceded by 'at' or '@' (e.g. at 5, @ 6)
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
            cal.set(Calendar.HOUR_OF_DAY, defaultHour);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }

        return 0;
    }
}
