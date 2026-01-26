package utils;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineUserRegistry {
    private static final Set<Integer> ONLINE = ConcurrentHashMap.newKeySet();

    // Track active HttpSession(s) per userId so admin actions can force logout.
    private static final ConcurrentMap<Integer, Set<HttpSession>> SESSIONS = new ConcurrentHashMap<>();

    public static void markOnline(Integer userId) {
        if (userId != null) ONLINE.add(userId);
    }

    public static void markOffline(Integer userId) {
        if (userId != null) ONLINE.remove(userId);
    }

    public static boolean isOnline(Integer userId) {
        return userId != null && ONLINE.contains(userId);
    }

    public static void registerSession(Integer userId, HttpSession session) {
        if (userId == null || session == null) {
            return;
        }
        markOnline(userId);
        SESSIONS.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public static void unregisterSession(Integer userId, HttpSession session) {
        if (userId == null || session == null) {
            return;
        }
        Set<HttpSession> set = SESSIONS.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                SESSIONS.remove(userId);
                markOffline(userId);
            }
        }
    }

    /**
     * Force logout (kick) a user by invalidating all known sessions.
     * Note: The UI will reflect logout at latest on the user's next request.
     */
    public static void forceLogout(Integer userId) {
        if (userId == null) {
            return;
        }
        Set<HttpSession> set = SESSIONS.remove(userId);
        if (set != null) {
            for (HttpSession s : set) {
                try {
                    s.invalidate();
                } catch (Exception ignored) {
                    // Session may already be invalidated
                }
            }
        }
        markOffline(userId);
    }

    public static Map<Integer, Set<HttpSession>> getSessionsSnapshot() {
        // Useful for debugging; do not mutate.
        return java.util.Collections.unmodifiableMap(SESSIONS);
    }
}
