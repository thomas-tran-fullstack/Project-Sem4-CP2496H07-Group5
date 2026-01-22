package utils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineUserRegistry {
    private static final Set<Integer> ONLINE = ConcurrentHashMap.newKeySet();

    public static void markOnline(Integer userId) {
        if (userId != null) ONLINE.add(userId);
    }

    public static void markOffline(Integer userId) {
        if (userId != null) ONLINE.remove(userId);
    }

    public static boolean isOnline(Integer userId) {
        return userId != null && ONLINE.contains(userId);
    }
}
