package controllers;

import entityclass.Customers;
import entityclass.Users;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.faces.context.FacesContext;
import sessionbeans.CustomersFacadeLocal;
import sessionbeans.UsersFacadeLocal;
import services.OtpService;
import services.EmailService;

@Named("auth")
@SessionScoped
public class AuthController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private UsersFacadeLocal usersFacade;
    @EJB
    private CustomersFacadeLocal customersFacade;
    @EJB
    private OtpService otpService;
    @EJB
    private EmailService emailService;

    private String identifier; // username or email for login
    private String password;

    private String regUsername;
    private String regEmail;
    private String regPassword;
    private String regConfirmPassword;
    private String firstName;
    private String middleName;
    private String lastName;
    
    // Google OAuth fields
    private String googleEmail;
    private String googleFullName;
    private String googleOtp;
    private String otpInputValue;
    private boolean googleRegistering = false;

    private Users currentUser;
    private Customers currentCustomer;
    private boolean loggedIn = false;

    public String login() {
        String hash = hashPassword(password);
        Users u = usersFacade.findByIdentifierAndPassword(identifier, hash);
        if (u != null) {
            currentUser = u;
            // Try to set customer profile if exists
            if (u.getCustomersList() != null && !u.getCustomersList().isEmpty()) {
                currentCustomer = u.getCustomersList().get(0);
            }
            loggedIn = true;
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                "Login successful!", null
            ));
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/pages/user/index.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        // stay on page on failure
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.addMessage(null, new jakarta.faces.application.FacesMessage(
            jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
            "Invalid email/username or password", null
        ));
        return null;
    }

    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        loggedIn = false;
        currentUser = null;
        currentCustomer = null;
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/pages/user/index.xhtml");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String register() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (regPassword == null || !regPassword.equals(regConfirmPassword)) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "Passwords do not match", null));
            return null;
        }
        // basic server-side checks
        if (regUsername == null || regUsername.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "Username is required", null));
            return null;
        }
        if (regEmail == null || regEmail.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "Email is required", null));
            return null;
        }
        // check username/email exist
        if (usersFacade.findByUsername(regUsername) != null) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "Username already exists", null));
            return null;
        }
        if (regEmail != null && usersFacade.findByEmail(regEmail) != null) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "Email already registered", null));
            return null;
        }

        try {
            Users u = new Users();
            u.setUsername(regUsername);
            u.setPasswordHash(hashPassword(regPassword));
            u.setEmail(regEmail);
            u.setRole("customer");
            u.setStatus("active");
            u.setCreatedAt(new Date());
            usersFacade.create(u);

            Customers c = new Customers();
            c.setFirstName(firstName);
            c.setMiddleName(middleName);
            c.setLastName(lastName);
            c.setCreatedAt(new Date());
            c.setUserID(u);
            customersFacade.create(c);

            currentUser = u;
            currentCustomer = c;
            loggedIn = true;

            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/pages/user/index.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        } catch (Exception ex) {
            // surface a readable message to the user and log exception
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "Registration failed: " + ex.getMessage(), null));
            ex.printStackTrace();
            return null;
        }
    }

    private String hashPassword(String pwd) {
        if (pwd == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(pwd.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return pwd;
        }
    }

    // Getters and setters
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRegUsername() { return regUsername; }
    public void setRegUsername(String regUsername) { this.regUsername = regUsername; }
    public String getRegEmail() { return regEmail; }
    public void setRegEmail(String regEmail) { this.regEmail = regEmail; }
    public String getRegPassword() { return regPassword; }
    public void setRegPassword(String regPassword) { this.regPassword = regPassword; }
    public String getRegConfirmPassword() { return regConfirmPassword; }
    public void setRegConfirmPassword(String regConfirmPassword) { this.regConfirmPassword = regConfirmPassword; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Users getCurrentUser() { return currentUser; }
    public Customers getCurrentCustomer() { return currentCustomer; }
    public boolean isLoggedIn() { return loggedIn; }

    public String getDisplayName() {
        if (currentCustomer != null) {
            StringBuilder sb = new StringBuilder();
            if (currentCustomer.getFirstName() != null && !currentCustomer.getFirstName().isEmpty()) sb.append(currentCustomer.getFirstName()).append(' ');
            if (currentCustomer.getMiddleName() != null && !currentCustomer.getMiddleName().isEmpty()) sb.append(currentCustomer.getMiddleName()).append(' ');
            if (currentCustomer.getLastName() != null && !currentCustomer.getLastName().isEmpty()) sb.append(currentCustomer.getLastName());
            String name = sb.toString().trim();
            if (!name.isEmpty()) return name;
        }
        if (currentUser != null && currentUser.getUsername() != null) return currentUser.getUsername();
        return "";
    }
    
    // Google OAuth methods
    /**
     * Handle Google OAuth callback.
     * If email exists, log in directly.
     * If email doesn't exist, generate OTP and show verification form.
     */
    public String handleGoogleAuth(String email, String fullName) {
        FacesContext fc = FacesContext.getCurrentInstance();
        
        // Check if user exists
        Users existingUser = usersFacade.findByEmail(email);
        if (existingUser != null) {
            // User exists, log them in
            currentUser = existingUser;
            if (existingUser.getCustomersList() != null && !existingUser.getCustomersList().isEmpty()) {
                currentCustomer = existingUser.getCustomersList().get(0);
            }
            loggedIn = true;
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                "Google login successful!", null
            ));
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/pages/user/index.xhtml"
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        
        // User doesn't exist, initiate registration flow
        googleEmail = email;
        googleFullName = fullName;
        googleRegistering = true;
        
        // Generate OTP
        String otp = otpService.generateOtp(email);
        googleOtp = otp;
        
        // Send OTP email
        try {
            emailService.sendOtpEmail(email, otp);
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                "Verification code sent to " + email + " (Check console in dev mode)", null
            ));
        } catch (Exception e) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Failed to send verification code: " + e.getMessage(), null
            ));
            return null;
        }
        
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(
                FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/pages/user/verify-otp.xhtml"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Verify OTP and complete Google registration.
     */
    public String verifyGoogleOtp() {
        FacesContext fc = FacesContext.getCurrentInstance();
        
        if (otpInputValue == null || otpInputValue.isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Please enter the verification code", null
            ));
            return null;
        }
        
        if (!otpService.verifyOtp(googleEmail, otpInputValue)) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Invalid or expired verification code", null
            ));
            return null;
        }
        
        try {
            // Create new user account
            Users u = new Users();
            u.setEmail(googleEmail);
            // Generate username from email
            String username = googleEmail.split("@")[0];
            int counter = 1;
            while (usersFacade.findByUsername(username + counter) != null) {
                counter++;
            }
            u.setUsername(username + counter);
            u.setPasswordHash(hashPassword(java.util.UUID.randomUUID().toString())); // Random password, not used for Google login
            u.setRole("customer");
            u.setStatus("active");
            u.setCreatedAt(new Date());
            usersFacade.create(u);
            
            // Create customer profile
            Customers c = new Customers();
            String[] nameParts = googleFullName.split(" ");
            if (nameParts.length >= 1) c.setFirstName(nameParts[0]);
            if (nameParts.length >= 2) {
                if (nameParts.length == 2) {
                    c.setLastName(nameParts[1]);
                } else {
                    StringBuilder middle = new StringBuilder();
                    for (int i = 1; i < nameParts.length - 1; i++) {
                        if (i > 1) middle.append(" ");
                        middle.append(nameParts[i]);
                    }
                    c.setMiddleName(middle.toString());
                    c.setLastName(nameParts[nameParts.length - 1]);
                }
            }
            c.setCreatedAt(new Date());
            c.setUserID(u);
            customersFacade.create(c);
            
            // Log user in
            currentUser = u;
            currentCustomer = c;
            loggedIn = true;
            
            // Send welcome email
            try {
                emailService.sendWelcomeEmail(googleEmail, googleFullName);
            } catch (Exception e) {
                System.err.println("Failed to send welcome email: " + e.getMessage());
            }
            
            // Redirect to index
            FacesContext.getCurrentInstance().getExternalContext().redirect(
                FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/pages/user/index.xhtml"
            );
        } catch (Exception ex) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Registration failed: " + ex.getMessage(), null
            ));
            ex.printStackTrace();
            return null;
        }
        
        return null;
    }
    
    /**
     * Resend OTP code.
     */
    public String resendOtp() {
        FacesContext fc = FacesContext.getCurrentInstance();
        
        try {
            // Generate new OTP
            String otp = otpService.generateOtp(googleEmail);
            googleOtp = otp;
            
            // Send OTP email
            emailService.sendOtpEmail(googleEmail, otp);
            
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                "Verification code resent to " + googleEmail, null
            ));
        } catch (Exception e) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Failed to resend verification code: " + e.getMessage(), null
            ));
        }
        
        return null;
    }
    
    // Getters and setters for Google OAuth
    public String getGoogleEmail() { return googleEmail; }
    public void setGoogleEmail(String googleEmail) { this.googleEmail = googleEmail; }
    public String getGoogleFullName() { return googleFullName; }
    public void setGoogleFullName(String googleFullName) { this.googleFullName = googleFullName; }
    public String getOtpInputValue() { return otpInputValue; }
    public void setOtpInputValue(String otpInputValue) { this.otpInputValue = otpInputValue; }
    public boolean isGoogleRegistering() { return googleRegistering; }
    public void setGoogleRegistering(boolean googleRegistering) { this.googleRegistering = googleRegistering; }
    
    /**
     * Redirect to Google OAuth consent screen
     */
    public String redirectToGoogleOAuth() {
        try {
            String authUrl = GoogleOAuthConfig.AUTHORIZATION_URL 
                + "?client_id=" + java.net.URLEncoder.encode(GoogleOAuthConfig.CLIENT_ID, "UTF-8")
                + "&redirect_uri=" + java.net.URLEncoder.encode(GoogleOAuthConfig.REDIRECT_URI, "UTF-8")
                + "&response_type=code"
                + "&scope=" + java.net.URLEncoder.encode(GoogleOAuthConfig.SCOPE, "UTF-8")
                + "&access_type=offline"
                + "&state=" + System.nanoTime();
            
            FacesContext.getCurrentInstance().getExternalContext().redirect(authUrl);
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, 
                new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    "Failed to redirect to Google: " + e.getMessage(), null
                ));
        }
        return null;
    }
    
    /**
     * Process Google OAuth callback from session
     */
    public String processGoogleOAuth() {
        FacesContext fc = FacesContext.getCurrentInstance();
        jakarta.servlet.http.HttpSession session = (jakarta.servlet.http.HttpSession) fc.getExternalContext().getSession(false);
        
        if (session == null) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Session expired", null
            ));
            try {
                fc.getExternalContext().redirect(fc.getExternalContext().getRequestContextPath() + "/pages/user/login.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        
        String email = (String) session.getAttribute("googleEmail");
        String name = (String) session.getAttribute("googleName");
        
        if (email == null || name == null) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Failed to retrieve Google account information", null
            ));
            try {
                fc.getExternalContext().redirect(fc.getExternalContext().getRequestContextPath() + "/pages/user/login.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        
        // Use the handleGoogleAuth method with real Google credentials
        return handleGoogleAuth(email, name);
    }
}

