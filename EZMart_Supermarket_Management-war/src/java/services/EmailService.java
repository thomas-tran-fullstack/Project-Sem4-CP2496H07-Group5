package services;

import jakarta.ejb.Stateless;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import javax.naming.InitialContext;

/**
 * Service to send emails via SMTP.
 */
@Stateless
public class EmailService {
    
    private static final boolean MAIL_ENABLED = false; // Disable mail for development
    
    /**
     * Send OTP verification email.
     */
    public void sendOtpEmail(String toEmail, String otp) throws Exception {
        if (!MAIL_ENABLED) {
            System.out.println("EMAIL DISABLED (Development Mode)");
            System.out.println("OTP for " + toEmail + ": " + otp);
            return;
        }
        
        try {
            InitialContext ctx = new InitialContext();
            Session session = (Session) ctx.lookup("java:comp/env/mail/ezmart");
            
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
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            System.err.println("Make sure mail resource 'java:comp/env/mail/ezmart' is configured in GlassFish");
            // Don't throw - allow app to continue in dev mode
        }
    }
    
    /**
     * Send welcome email after successful registration.
     */
    public void sendWelcomeEmail(String toEmail, String fullName) throws Exception {
        if (!MAIL_ENABLED) {
            System.out.println("Welcome email for " + fullName + " (" + toEmail + ") - disabled in dev mode");
            return;
        }
        
        try {
            InitialContext ctx = new InitialContext();
            Session session = (Session) ctx.lookup("java:comp/env/mail/ezmart");
            
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
        } catch (Exception e) {
            System.err.println("Failed to send welcome email to " + toEmail + ": " + e.getMessage());
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
