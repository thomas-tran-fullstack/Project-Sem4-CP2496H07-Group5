package controllers;

import entityclass.Customers;
import entityclass.Users;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.text.Normalizer;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.inject.Inject;
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
    
    @Inject
    private LocaleController localeController;

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

    // Password reset fields
    private String forgotEmail; // email entered in 'forgot password' form
    private String passwordResetEmail; // email for which password reset OTP was sent (session-backed)
    private boolean passwordResetVerified = false; // set after successful OTP verification
    private String resetNewPassword;
    private String resetConfirmPassword;

    private Users currentUser;
    private Customers currentCustomer;
    private boolean loggedIn = false;

    
    public String login() {
        String hash = hashPassword(password);
        Users u = usersFacade.findByIdentifierAndPassword(identifier, hash);
        if (u != null && "ACTIVE".equals(u.getStatus())) {
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
            // Role-based redirect
           String redirectUrl;

if ("admin".equalsIgnoreCase(u.getRole())) {
    redirectUrl = "/pages/admin/dashboard.xhtml";
} else if ("customer".equalsIgnoreCase(u.getRole())) {
    redirectUrl = "/pages/user/index.xhtml";
} else {
    redirectUrl = "/pages/user/profile.xhtml";
}

try {
    FacesContext.getCurrentInstance()
        .getExternalContext()
        .redirect(FacesContext.getCurrentInstance()
        .getExternalContext()
        .getRequestContextPath() + redirectUrl);
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
        String redirectUrl;
        if (currentUser != null && "ADMIN".equals(currentUser.getRole())) {
            redirectUrl = "/pages/user/login.xhtml";
        } else {
            redirectUrl = "/pages/user/index.xhtml";
        }
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        loggedIn = false;
        currentUser = null;
        currentCustomer = null;
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + redirectUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String register() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (regPassword == null || !regPassword.equals(regConfirmPassword)) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.passwordMismatch"), null));
            return null;
        }
        // basic server-side checks
        if (regUsername == null || regUsername.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.usernameRequired"), null));
            return null;
        }
        if (regEmail == null || regEmail.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.emailRequired"), null));
            return null;
        }
        // check username/email exist
        if (usersFacade.findByUsername(regUsername) != null) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.usernameAlreadyExists"), null));
            return null;
        }
        if (regEmail != null && usersFacade.findByEmail(regEmail) != null) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.emailAlreadyExists"), null));
            return null;
        }

        try {
            Users u = new Users();
            u.setUsername(regUsername);
            u.setPasswordHash(hashPassword(regPassword));
            u.setEmail(regEmail);
            u.setRole("customer");
            u.setStatus("ACTIVE");
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
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.registrationFailed"), null));
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
    public Users getCurrentUserLazy() {
        if (currentUser == null) {
            try {
                FacesContext fc = FacesContext.getCurrentInstance();
                Object idObj = fc.getExternalContext().getSessionMap().get("currentUserId");
                if (idObj != null) {
                    Integer id = null;
                    if (idObj instanceof Integer) id = (Integer) idObj;
                    else if (idObj instanceof Long) id = ((Long) idObj).intValue();
                    else if (idObj instanceof String) id = Integer.valueOf((String) idObj);
                    if (id != null) {
                        Users u = usersFacade.find(id);
                        if (u != null) {
                            currentUser = u;
                            if (u.getCustomersList() != null && !u.getCustomersList().isEmpty()) {
                                currentCustomer = u.getCustomersList().get(0);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // ignore lookup errors
            }
        }
        return currentUser;
    }

    public boolean isLoggedIn() {
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            Object val = fc.getExternalContext().getSessionMap().get("loggedIn");
            if (val instanceof Boolean) return (Boolean) val;
        } catch (Exception e) {
            // ignore
        }
        return loggedIn;
    }

    // Normalize name: remove diacritics (e.g., "Trần Bảo Toàn" -> "Tran Bao Toan")
    private String normalize(String s) {
        if (s == null) return null;
        String tmp = Normalizer.normalize(s, Normalizer.Form.NFD);
        // remove diacritic marks
        tmp = tmp.replaceAll("\\p{M}", "");
        // handle vietnamese đ/Đ
        tmp = tmp.replace('đ', 'd').replace('Đ', 'D');
        // remove any remaining non-ASCII characters
        tmp = tmp.replaceAll("[^\\p{ASCII}]", "");
        return tmp.trim();
    }

    public String getGoogleFullName() { return normalize(googleFullName); }

    // Setters to allow external code (servlets) to update session-scoped bean state
    public void setCurrentUser(Users user) { this.currentUser = user; }
    public void setCurrentCustomer(Customers customer) { this.currentCustomer = customer; }
    public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }
    public String getDisplayName() {
        // Ensure currentUser/currentCustomer are loaded from session if not present
        getCurrentUserLazy();

        if (currentCustomer != null) {
            StringBuilder sb = new StringBuilder();
            if (currentCustomer.getFirstName() != null && !currentCustomer.getFirstName().isEmpty()) sb.append(currentCustomer.getFirstName()).append(' ');
            if (currentCustomer.getMiddleName() != null && !currentCustomer.getMiddleName().isEmpty()) sb.append(currentCustomer.getMiddleName()).append(' ');
            if (currentCustomer.getLastName() != null && !currentCustomer.getLastName().isEmpty()) sb.append(currentCustomer.getLastName());
            String name = sb.toString().trim();
            if (!name.isEmpty()) return name;
        }
        if (currentUser != null && currentUser.getUsername() != null) return normalize(currentUser.getUsername());
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
                localeController.getMessage("success.googleLoginSuccess"), null
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
        
        // Store email and name in session for reliable retrieval (e.g., during resendOtp)
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("googleEmail", email);
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("googleFullName", fullName);
        
        // Generate OTP
        String otp = otpService.generateOtp(email);
        googleOtp = otp;
        
        // Send OTP email
        try {
            emailService.sendOtpEmail(email, otp);
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                localeController.getMessage("success.otpSent") + " " + email, null
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
     * Unified OTP verification: handles Google registration flow and password-reset flow.
     */
    public String verifyOtp() {
        FacesContext fc = FacesContext.getCurrentInstance();

        if (otpInputValue == null || otpInputValue.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                localeController.getMessage("error.otpEmpty"), null
            ));
            return null;
        }

        // Determine flow: password reset or google registration
        String sessionPasswordResetEmail = (String) fc.getExternalContext().getSessionMap().get("passwordResetEmail");
        boolean isReset = sessionPasswordResetEmail != null && !sessionPasswordResetEmail.isEmpty();

        String email = null;
        String fullName = null;

        if (isReset) {
            email = sessionPasswordResetEmail;
        } else {
            email = (String) fc.getExternalContext().getSessionMap().get("googleEmail");
            if (email == null || email.isEmpty()) email = googleEmail;
            fullName = (String) fc.getExternalContext().getSessionMap().get("googleFullName");
            if (fullName == null || fullName.isEmpty()) fullName = googleFullName;
        }

        if (email == null || email.isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Email not found. Please restart verification.", null
            ));
            return null;
        }

        if (!otpService.verifyOtp(email, otpInputValue.trim())) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Invalid or expired verification code", null
            ));
            return null;
        }

        // If password reset flow, mark verified and redirect to reset page
        if (isReset) {
            fc.getExternalContext().getSessionMap().put("passwordResetVerifiedEmail", email);
            try {
                fc.getExternalContext().redirect(fc.getExternalContext().getRequestContextPath() + "/pages/user/reset_password.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        // Otherwise proceed with Google registration (existing logic)
        if (fullName == null || fullName.trim().isEmpty()) {
            try {
                if (email != null && email.contains("@")) {
                    fullName = email.split("@")[0];
                } else {
                    fullName = "";
                }
            } catch (Exception e) {
                fullName = "";
            }
        }

        try {
            Users u = new Users();
            u.setEmail(email);
            String username = email.split("@")[0];
            int counter = 1;
            while (usersFacade.findByUsername(username + counter) != null) counter++;
            u.setUsername(username + counter);
            u.setPasswordHash(hashPassword(java.util.UUID.randomUUID().toString()));
            u.setRole("customer");
            u.setStatus("ACTIVE");
            u.setCreatedAt(new Date());
            usersFacade.create(u);

            Customers c = new Customers();
            String[] nameParts = fullName.split(" ");
            if (nameParts.length >= 1) c.setFirstName(nameParts[0]);
            if (nameParts.length >= 2) {
                if (nameParts.length == 2) c.setLastName(nameParts[1]);
                else {
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

            currentUser = u;
            currentCustomer = c;
            loggedIn = true;

            try {
                emailService.sendWelcomeEmail(email, fullName);
            } catch (Exception e) {
                System.err.println("Failed to send welcome email: " + e.getMessage());
            }

            fc.getExternalContext().getSessionMap().remove("googleEmail");
            fc.getExternalContext().getSessionMap().remove("googleFullName");

            try {
                fc.getExternalContext().redirect(fc.getExternalContext().getRequestContextPath() + "/pages/user/index.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception ex) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "Registration failed: " + ex.getMessage(), null));
            ex.printStackTrace();
            return null;
        }

        return null;
    }
    
    /**
     * Resend OTP code for either Google registration or password reset.
     */
    public String resendOtp() {
        FacesContext fc = FacesContext.getCurrentInstance();
        try {
            String email = (String) fc.getExternalContext().getSessionMap().get("passwordResetEmail");
            boolean isReset = email != null && !email.isEmpty();
            if (!isReset) {
                email = (String) fc.getExternalContext().getSessionMap().get("googleEmail");
                if (email == null || email.isEmpty()) email = googleEmail;
            }

            if (email == null || email.isEmpty()) {
                fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    "Email not found. Please restart verification.", null
                ));
                return null;
            }

            String otp = otpService.generateOtp(email);
            if (isReset) {
                // nothing to store locally
            } else {
                googleOtp = otp;
            }
            emailService.sendOtpEmail(email, otp);
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                "Verification code resent to " + email, null
            ));
        } catch (Exception e) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Failed to resend verification code: " + e.getMessage(), null
            ));
        }
        return null;
    }
    
    // Getters and setters for Google OAuth and password reset
    public String getGoogleEmail() { return googleEmail; }
    public void setGoogleEmail(String googleEmail) { this.googleEmail = googleEmail; }
    public void setGoogleFullName(String googleFullName) { this.googleFullName = googleFullName; }
    public String getOtpInputValue() { return otpInputValue; }
    public void setOtpInputValue(String otpInputValue) { this.otpInputValue = otpInputValue; }
    public boolean isGoogleRegistering() { return googleRegistering; }
    public void setGoogleRegistering(boolean googleRegistering) { this.googleRegistering = googleRegistering; }

    public String getForgotEmail() { return forgotEmail; }
    public void setForgotEmail(String forgotEmail) { this.forgotEmail = forgotEmail; }
    public String getPasswordResetEmail() { return passwordResetEmail; }
    public void setPasswordResetEmail(String passwordResetEmail) { this.passwordResetEmail = passwordResetEmail; }
    public boolean isPasswordResetVerified() { return passwordResetVerified; }
    public void setPasswordResetVerified(boolean passwordResetVerified) { this.passwordResetVerified = passwordResetVerified; }
    public String getResetNewPassword() { return resetNewPassword; }
    public void setResetNewPassword(String resetNewPassword) { this.resetNewPassword = resetNewPassword; }
    public String getResetConfirmPassword() { return resetConfirmPassword; }
    public void setResetConfirmPassword(String resetConfirmPassword) { this.resetConfirmPassword = resetConfirmPassword; }
    
    /**
     * Request password reset: validate email exists, generate OTP and send.
     */
    public String requestPasswordReset() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (forgotEmail == null || forgotEmail.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.emailRequired"), null));
            return null;
        }
        Users u = usersFacade.findByEmail(forgotEmail.trim());
        if (u == null) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.emailNotFound"), null));
            return null;
        }
        // Generate OTP and store session key
        String otp = otpService.generateOtp(forgotEmail.trim());
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("passwordResetEmail", forgotEmail.trim());
        this.passwordResetEmail = forgotEmail.trim();
        try {
            emailService.sendOtpEmail(forgotEmail.trim(), otp);
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_INFO, localeController.getMessage("success.otpSent") + " " + forgotEmail.trim(), null));
            FacesContext.getCurrentInstance().getExternalContext().redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/pages/user/verify-otp.xhtml");
        } catch (Exception e) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "Failed to send verification code: " + e.getMessage(), null));
        }
        return null;
    }

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
                + "&prompt=select_account"
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
     * After OTP verification for password-reset, change the user's password.
     */
    public String resetPassword() {
        FacesContext fc = FacesContext.getCurrentInstance();
        // check session verified email
        String email = (String) fc.getExternalContext().getSessionMap().get("passwordResetVerifiedEmail");
        if (email == null || email.isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.sessionExpired"), null));
            return null;
        }
        if (resetNewPassword == null || resetNewPassword.trim().isEmpty() || !resetNewPassword.equals(resetConfirmPassword)) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.passwordMismatch"), null));
            return null;
        }
        try {
            Users u = usersFacade.findByEmail(email);
            if (u == null) {
                fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "No account found for the verified email.", null));
                return null;
            }
            u.setPasswordHash(hashPassword(resetNewPassword));
            usersFacade.edit(u);
            // clear session keys
            fc.getExternalContext().getSessionMap().remove("passwordResetEmail");
            fc.getExternalContext().getSessionMap().remove("passwordResetVerifiedEmail");
            // feedback and redirect to login
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_INFO, "Password reset successful. You can now log in.", null));
            FacesContext.getCurrentInstance().getExternalContext().redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/pages/user/login.xhtml");
        } catch (Exception e) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.passwordResetFailed"), null));
            e.printStackTrace();
            return null;
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
        String name = (String) session.getAttribute("googleFullName");

        System.out.println("AuthController.processGoogleOAuth: session googleEmail=" + email + ", googleFullName=" + name);
        
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
        System.out.println("AuthController.processGoogleOAuth: calling handleGoogleAuth for " + email);
        return handleGoogleAuth(email, name);
    }
}

