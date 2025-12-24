package services;

import jakarta.ejb.Stateless;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Service to send emails via SMTP.
 * Uses a custom SSLContext with trust-all TrustManager to bypass TLS proxy interception.
 * (Development-only; not recommended for production.)
 */
@Stateless
public class EmailService {
    
    private static final boolean MAIL_ENABLED = true; // Disable mail for development; OTP prints to console
    
    /**
     * Create a mail Session programmatically with custom TrustAllSocketFactory.
     * This bypasses JVM certificate validation for development with TLS proxies.
     */
    private Session createMailSession() throws Exception {
        // Create mail session properties for Gmail SMTP (port 465 SMTPS - immediate SSL)
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "services.TrustAllSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        
        // Disable hostname verification for development (TLS proxy bypass)
        props.put("mail.smtp.ssl.checkserveridentity", "false");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.debug", "true");
        
        // Create Authenticator for Gmail App Password
        jakarta.mail.Authenticator auth = new jakarta.mail.Authenticator() {
            @Override
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(
                    "thomastran.fullstack@gmail.com",
                    "knrw tcer kcjo bqej"  // Gmail App Password (16-char)
                );
            }
        };
        
        // Create session with custom authenticator and custom socket factory
        Session session = Session.getInstance(props, auth);
        return session;
    }
    
    /**
     * Send OTP verification email.
     */
    public void sendOtpEmail(String toEmail, String otp) throws Exception {
        // Always print OTP to console for debugging purposes
        System.out.println("=== EZMart OTP Code (Development Debug) ===");
        System.out.println("Email: " + toEmail);
        System.out.println("OTP Code: " + otp);
        System.out.println("==========================================");

        if (!MAIL_ENABLED) {
            System.out.println("EMAIL DISABLED (Development Mode)");
            System.out.println("--- Simulated email from EZMart ---");
            System.out.println("From: EZMart <noreply@ezmart.com>");
            System.out.println("To: " + toEmail);
            System.out.println("Subject: EZMart - Email Verification Code");
            System.out.println("");
            System.out.println("Hello,");
            System.out.println("");
            System.out.println("Thank you for signing in with Google. Your EZMart verification code is:");
            System.out.println("");
            System.out.println("    " + otp + "");
            System.out.println("");
            System.out.println("This code will expire in 10 minutes. If you did not request this, please ignore this email.");
            System.out.println("");
            System.out.println("Best regards,");
            System.out.println("EZMart Team");
            System.out.println("--- End simulated email ---");
            return;
        }
        
        try {
            // Use programmatically created session with trust-all SSLContext (dev-only)
            Session session = createMailSession();

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("noreply@ezmart.com"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("EZMart - Email Verification Code");
            
            String htmlBody = "<html><body>" +
                "<h2>Welcome to EZMart!</h2>" +
                "<p>Your verification code is: <strong style='font-size: 24px; color: #13a844;'>" + otp + "</strong></p>" +
                "<p>This code will expire in 10 minutes.</p>" +
                "<p>If you did not request this code, please ignore this email.</p>" +
                "</body></html>";
            
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            
            Transport.send(message);
            System.out.println("EmailService: OTP email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace(System.err);
            // Don't throw - allow app to continue in dev mode
        }
    }
    
    /**
     * Send welcome email after successful registration.
     */
    public void sendWelcomeEmail(String toEmail, String fullName) throws Exception {
        if (!MAIL_ENABLED) {
            System.out.println("--- Simulated welcome email from EZMart ---");
            System.out.println("From: EZMart <noreply@ezmart.com>");
            System.out.println("To: " + toEmail);
            System.out.println("Subject: Welcome to EZMart!");
            System.out.println("");
            System.out.println("Hello " + fullName + ",");
            System.out.println("");
            System.out.println("Welcome to EZMart! Your account has been successfully created.");
            System.out.println("");
            System.out.println("Best regards,");
            System.out.println("EZMart Team");
            System.out.println("--- End simulated email ---");
            return;
        }
        
        try {
            // Use programmatically created session with trust-all SSLContext (dev-only)
            Session session = createMailSession();

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("noreply@ezmart.com"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Welcome to EZMart!");
            
            String htmlBody = "<html><body>" +
                "<h2>Welcome, " + escapeHtml(fullName) + "!</h2>" +
                "<p>Your account has been successfully created on EZMart.</p>" +
                "<p>You can now start shopping fresh groceries with us.</p>" +
                "<p><a href='https://ezmart.com'>Visit EZMart</a></p>" +
                "</body></html>";
            
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            
            Transport.send(message);
            System.out.println("EmailService: Welcome email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send welcome email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace(System.err);
            // Don't throw - welcome email is not critical
        }
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
