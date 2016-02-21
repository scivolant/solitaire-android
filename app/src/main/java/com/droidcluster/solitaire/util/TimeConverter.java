package com.droidcluster.solitaire.util;

public class TimeConverter {

    public static String timeToString(long millisec) {
        if (millisec < 1000) {
            return "0:00";
        }

        long seconds = millisec / 1000;
        long sec = seconds % 60;
        long minutes = seconds / 60;

        long hours = minutes / 60;
        if (hours == 0) {
            return String.valueOf(minutes % 60) + ':' + twoDigits(sec);
        }

        long days = hours / 24;
        if (days == 0) {
            return String.valueOf(hours) + ':' + twoDigits(minutes % 60) + ':' + twoDigits(sec);
        }

        return String.valueOf(days) + ':' + twoDigits(hours % 24) + ':' + twoDigits(minutes % 60) + ':'
                + twoDigits(sec);
    }

    private static String twoDigits(long number) {
        if (number < 10) {
            return '0' + String.valueOf(number);
        } else {
            return String.valueOf(number);
        }
    }

}
