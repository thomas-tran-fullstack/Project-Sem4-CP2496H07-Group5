package services;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to format time display for online status
 * @author Admin
 */
public class TimeFormatUtil {

    /**
     * Format the online time to readable format
     * Example: "Just now", "5 minutes ago", "2 hours ago", "1 day ago", etc.
     */
    public static String formatOnlineTime(Date lastOnlineAt) {
        if (lastOnlineAt == null) {
            return "Just now"; // New user
        }

        Date now = new Date();
        long diffInMillis = now.getTime() - lastOnlineAt.getTime();

        if (diffInMillis < 0) {
            return "Just now";
        }

        long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis);
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
        long diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
        long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        long diffInWeeks = diffInDays / 7;
        long diffInMonths = diffInDays / 30;
        long diffInYears = diffInDays / 365;

        if (diffInSeconds < 60) {
            return "Just now";
        } else if (diffInMinutes == 1) {
            return "1 minute ago";
        } else if (diffInMinutes < 60) {
            return diffInMinutes + " minutes ago";
        } else if (diffInHours == 1) {
            return "1 hour ago";
        } else if (diffInHours < 24) {
            return diffInHours + " hours ago";
        } else if (diffInDays == 1) {
            return "1 day ago";
        } else if (diffInDays < 7) {
            return diffInDays + " days ago";
        } else if (diffInWeeks == 1) {
            return "1 week ago";
        } else if (diffInWeeks < 4) {
            return diffInWeeks + " weeks ago";
        } else if (diffInMonths == 1) {
            return "1 month ago";
        } else if (diffInMonths < 12) {
            return diffInMonths + " months ago";
        } else if (diffInYears == 1) {
            return "1 year ago";
        } else {
            return diffInYears + " years ago";
        }
    }

    /**
     * Check if user is currently online (last online within 5 minutes)
     */
    public static boolean isOnline(Date lastOnlineAt) {
        if (lastOnlineAt == null) {
            return false;
        }
        
        Date now = new Date();
        long diffInMillis = now.getTime() - lastOnlineAt.getTime();
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
        
        return diffInMinutes <= 5;
    }

    /**
     * Check if the last online time was "just now" (within 60 seconds)
     */
    public static boolean isJustNow(Date lastOnlineAt) {
        if (lastOnlineAt == null) {
            return true; // New user is considered "just now"
        }
        
        Date now = new Date();
        long diffInMillis = now.getTime() - lastOnlineAt.getTime();
        
        if (diffInMillis < 0) {
            return true;
        }
        
        long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis);
        return diffInSeconds < 60;
    }
}
