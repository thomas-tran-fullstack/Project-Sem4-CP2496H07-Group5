package services;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import jakarta.ejb.Singleton;

/**
 * Service to generate, store, and verify OTP codes.
 * OTP codes expire after 10 minutes.
 */
@Singleton
public class OtpService {
    
    private final Map<String, OtpRecord> otpStore = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private static final int OTP_LENGTH = 4;
    private static final long OTP_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes
    
    public static class OtpRecord {
        public String code;
        public long createdAt;
        public int attempts;
        
        public OtpRecord(String code) {
            this.code = code;
            this.createdAt = System.currentTimeMillis();
            this.attempts = 0;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > OTP_EXPIRY_MS;
        }
    }
    
    /**
     * Generate a 4-digit OTP and store it for the given email.
     */
    public String generateOtp(String email) {
        // Generate 4-digit OTP
        String otp = String.format("%04d", random.nextInt(10000));
        
        // Store OTP
        OtpRecord record = new OtpRecord(otp);
        otpStore.put(email, record);
        
        // Schedule auto-cleanup after expiry
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                otpStore.remove(email);
            }
        }, OTP_EXPIRY_MS);
        
        return otp;
    }
    
    /**
     * Verify OTP for the given email.
     * Returns true if OTP is valid and not expired.
     */
    public boolean verifyOtp(String email, String code) {
        OtpRecord record = otpStore.get(email);
        
        if (record == null) {
            return false;
        }
        
        if (record.isExpired()) {
            otpStore.remove(email);
            return false;
        }
        
        record.attempts++;
        
        // Max 5 attempts
        if (record.attempts > 5) {
            otpStore.remove(email);
            return false;
        }
        
        if (record.code.equals(code)) {
            otpStore.remove(email);
            return true;
        }
        
        return false;
    }
    
    /**
     * Clear OTP for the given email.
     */
    public void clearOtp(String email) {
        otpStore.remove(email);
    }
    
    /**
     * Check if OTP exists and is not expired.
     */
    public boolean hasValidOtp(String email) {
        OtpRecord record = otpStore.get(email);
        if (record == null) return false;
        if (record.isExpired()) {
            otpStore.remove(email);
            return false;
        }
        return true;
    }
}
