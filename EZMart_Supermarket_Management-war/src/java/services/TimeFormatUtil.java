package services;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to format time display for online status
 * @author Admin
 */
public class TimeFormatUtil {

    /**
     * Threshold (in seconds) to consider a user "online" for the admin UI.
     * Requirement: user is actively logged in (within 60 seconds of last activity)
     */
    private static final long ONLINE_THRESHOLD_SECONDS = 60;
    
    /**
     * Threshold (in seconds) for "Just Now" status after logout
     * If user logged out within 60 seconds, show "Just Now"
     */
    private static final long JUST_NOW_THRESHOLD_SECONDS = 60;

    /**
     * Format the online time to readable format
     * Example: "Just now", "5 minutes ago", "2 hours ago", "1 day ago", etc.
     */
    public static String formatOnlineTime(Date lastOnlineAt) {
        if (lastOnlineAt == null) {
            return "Offline"; // New user / never seen online
        }

        Date now = new Date();
        long diffInMillis = now.getTime() - lastOnlineAt.getTime();

        if (diffInMillis < 0) {
            return "Just Now";
        }

        long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis);
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
        long diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
        long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        long diffInWeeks = diffInDays / 7;
        long diffInMonths = diffInDays / 30;
        long diffInYears = diffInDays / 365;

        if (diffInSeconds < 60) {
            return "Just Now";
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
     * Check if user is currently online (last online within ONLINE_THRESHOLD_SECONDS)
     * Returns true only for actively logged-in users
     */
    public static boolean isOnline(Date lastOnlineAt) {
        if (lastOnlineAt == null) {
            return false;
        }
        
        Date now = new Date();
        long diffInMillis = now.getTime() - lastOnlineAt.getTime();
        long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis);
        return diffInSeconds >= 0 && diffInSeconds < ONLINE_THRESHOLD_SECONDS;
    }
    
    /**
     * Check if user recently went offline (within JUST_NOW_THRESHOLD_SECONDS)
     * Used to display "Just Now" status for recently logged-out users
     */
    public static boolean isJustNow(Date lastOnlineAt) {
        if (lastOnlineAt == null) {
            return false;
        }
        
        Date now = new Date();
        long diffInMillis = now.getTime() - lastOnlineAt.getTime();
        long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis);
        
        // Between 60-1800 seconds (1 minute to 30 minutes) show "Just Now"
        return diffInSeconds >= ONLINE_THRESHOLD_SECONDS && diffInSeconds < JUST_NOW_THRESHOLD_SECONDS;
    }
    
    /**
     * Get online status type for display
     * Returns: "Online" | "Just Now" | "Offline"
     */
    public static String getOnlineStatus(Date lastOnlineAt) {
        if (lastOnlineAt == null) {
            return "Offline";
        }
        
        if (isOnline(lastOnlineAt)) {
            return "Online";
        } else if (isJustNow(lastOnlineAt)) {
            return "Just Now";
        } else {
            return "Offline";
        }
    }
}

