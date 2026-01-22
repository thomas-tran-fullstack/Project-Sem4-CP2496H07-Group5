package controllers;

import entityclass.Customers;
import entityclass.Users;
import entityclass.Addresses;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpSession;
import sessionbeans.CustomersFacadeLocal;
import sessionbeans.UsersFacadeLocal;
import sessionbeans.AddressesFacadeLocal;
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
    private AddressesFacadeLocal addressesFacade;
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
    private String mobilePhone;
    private String street;
    private String city;
    private String state;
    private String country;
    private Double latitude;
    private Double longitude;
    private String addressType;
    private String addressLabel;

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
    private boolean rememberMe = false;
    private boolean editingProfile = false;

    public String login() {
        System.out.println("AuthController.login: identifier=" + identifier + ", password provided=" + (password != null ? "yes" : "no"));
        String hash = hashPassword(password);
        System.out.println("AuthController.login: hashed password=" + hash);
        Users u = usersFacade.findByIdentifierAndPassword(identifier, hash);
        System.out.println("AuthController.login: user found=" + (u != null));
        if (u != null) {
            System.out.println("AuthController.login: user status=" + u.getStatus() + ", role=" + u.getRole());
        }
        // Block INACTIVE accounts from logging in
        if (u != null && "INACTIVE".equalsIgnoreCase(u.getStatus())) {
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    "Your account had been Inactived.", null
            ));
            return null;
        }

        // Block BANNED accounts from logging in (BanUntil)
        if (u != null) {
            try {
                Date now = new Date();
                Date banUntil = u.getBanUntil();
                if (banUntil != null) {
                    if (banUntil.after(now)) {
                        FacesContext fc = FacesContext.getCurrentInstance();
                        String timeStr = new SimpleDateFormat("HH:mm").format(banUntil);
                        String dateStr = new SimpleDateFormat("dd/MM/yyyy").format(banUntil);
                        fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                                "Your account had been banned. It will end at '" + timeStr + "' '" + dateStr + "'", null
                        ));
                        return null;
                    }

                    // Ban expired -> clear
                    u.setBanUntil(null);
                    if ("BANNED".equalsIgnoreCase(u.getStatus())) {
                        u.setStatus("ACTIVE");
                    }
                    usersFacade.edit(u);
                }
            } catch (Exception e) {
                // ignore ban normalization errors
            }
        }
        if (u != null) {
            currentUser = u;
            
            // Load customer profile - try both relationship and direct query
            if (u.getCustomersList() != null && !u.getCustomersList().isEmpty()) {
                currentCustomer = u.getCustomersList().get(0);
                System.out.println("AuthController.login: Got currentCustomer from relationship");
            } else if ("CUSTOMER".equalsIgnoreCase(u.getRole())) {
                // Fallback: query directly from database
                try {
                    currentCustomer = customersFacade.findByUserID(u.getUserID());
                    System.out.println("AuthController.login: Got currentCustomer from direct query");
                } catch (Exception e) {
                    System.out.println("AuthController.login: Failed to load customer from direct query: " + e.getMessage());
                }
            }
            
            loggedIn = true;
            
            // Mark user as online in registry (for "Online" status determination)
            utils.OnlineUserRegistry.markOnline(u.getUserID());
            
            // Update lastOnlineAt to mark user as currently online
            try {
                u.setLastOnlineAt(new Date());
                usersFacade.edit(u);
                System.out.println("AuthController.login: Updated lastOnlineAt for user " + u.getUsername());
            } catch (Exception e) {
                System.out.println("AuthController.login: Warning - could not update lastOnlineAt: " + e.getMessage());
            }
            
            // Store in session for filter access (JSF session map)
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentUser", currentUser);
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentCustomer", currentCustomer);
            if (currentCustomer != null) {
                FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentCustomerId", currentCustomer.getCustomerID());
            }
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("loggedIn", loggedIn);

            // Also store in HttpSession for servlet access (critical for REST APIs)
            try {
                Object sessionObj = FacesContext.getCurrentInstance().getExternalContext().getSession(false);
                if (sessionObj instanceof jakarta.servlet.http.HttpSession) {
                    jakarta.servlet.http.HttpSession httpSession = (jakarta.servlet.http.HttpSession) sessionObj;
                    httpSession.setAttribute("currentUser", currentUser);
                    httpSession.setAttribute("currentCustomer", currentCustomer);
                    if (currentCustomer != null) {
                        httpSession.setAttribute("currentCustomerId", currentCustomer.getCustomerID());
                    }
                    httpSession.setAttribute("loggedIn", loggedIn);
                    System.out.println("AuthController.login: Stored user in HttpSession, sessionId=" + httpSession.getId());
                }
            } catch (Exception e) {
                System.out.println("AuthController.login: Warning - could not store in HttpSession: " + e.getMessage());
            }

            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                    "Login successful!", null
            ));
            // Role-based redirect
            String redirectUrl;
            String role = (u.getRole() != null) ? u.getRole().toUpperCase() : "CUSTOMER";

            if ("ADMIN".equals(role)) {
                redirectUrl = "/pages/admin/dashboard.xhtml";
            } else if ("STAFF".equals(role)) {
                redirectUrl = "/pages/staff/dashboard.xhtml";
            } else if ("CUSTOMER".equals(role)) {
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

    /**
     * Ensures session attributes are synced to HttpSession for servlet access.
     * Call this method on page load to ensure REST APIs can access session
     * data.
     */
    public void ensureHttpSessionSync() {
        try {
            Object sessionObj = FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            if (!(sessionObj instanceof jakarta.servlet.http.HttpSession)) {
                return;
            }

            jakarta.servlet.http.HttpSession httpSession = (jakarta.servlet.http.HttpSession) sessionObj;

            // Hydrate từ session nếu bean chưa có
            getCurrentUserLazy();

            // Ưu tiên trạng thái loggedIn trong HttpSession nếu đã có (vd GoogleOAuthProcessor set)
            Object sessLoggedIn = httpSession.getAttribute("loggedIn");
            if (sessLoggedIn instanceof Boolean) {
                this.loggedIn = (Boolean) sessLoggedIn;
            } else if (this.currentUser != null) {
                this.loggedIn = true;
            }

            if (currentUser != null) {
                httpSession.setAttribute("currentUser", currentUser);
            }
            if (currentCustomer != null) {
                httpSession.setAttribute("currentCustomer", currentCustomer);
            }
            if (currentCustomer != null && currentCustomer.getCustomerID() != null) {
                httpSession.setAttribute("currentCustomerId", currentCustomer.getCustomerID());
            }

            // Chỉ set loggedIn sau khi đã resolve ở trên
            httpSession.setAttribute("loggedIn", this.loggedIn);

        } catch (Exception e) {
            System.out.println("AuthController.ensureHttpSessionSync: Warning - could not sync: " + e.getMessage());
        }
    }

    public String logout() {
        String redirectUrl = "/pages/user/index.xhtml";
        
        // Persist last activity timestamp and mark offline before destroying session
        try {
            if (currentUser != null && currentUser.getUserID() != null) {
                utils.OnlineUserRegistry.markOffline(currentUser.getUserID());
                Users u = usersFacade.find(currentUser.getUserID());
                if (u != null) {
                    u.setLastOnlineAt(new Date());
                    usersFacade.edit(u);
                }
            }
        } catch (Exception e) {
            System.out.println("AuthController.logout: Warning - could not update LastOnlineAt: " + e.getMessage());
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

        // Validate username
        if (regUsername == null || regUsername.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.usernameRequired"), null));
            return null;
        }

        // Validate first name
        if (firstName == null || firstName.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.firstNameRequired"), null));
            return null;
        }

        // Validate last name
        if (lastName == null || lastName.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.lastNameRequired"), null));
            return null;
        }

        // Validate password
        if (regPassword == null || regPassword.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.passwordRequired"), null));
            return null;
        }

        // Validate confirm password
        if (regConfirmPassword == null || regConfirmPassword.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.confirmPasswordRequired"), null));
            return null;
        }

        // Check passwords match
        if (!regPassword.equals(regConfirmPassword)) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.passwordMismatch"), null));
            return null;
        }

        // Validate email
        if (regEmail == null || regEmail.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.emailRequired"), null));
            return null;
        }

        // Validate mobile phone
        if (mobilePhone == null || mobilePhone.trim().isEmpty()) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.mobilePhoneRequired"), null));
            return null;
        }
        // Phone must be digits only
        if (!mobilePhone.matches("\\d+")) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.mobilePhoneInvalid"), null));
            return null;
        }
        // Phone must be 10-11 digits
        if (mobilePhone.length() < 10 || mobilePhone.length() > 11) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.mobilePhoneLength"), null));
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

        // Check if mobile phone already exists
        if (customersFacade.findByMobilePhone(mobilePhone) != null) {
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, localeController.getMessage("error.mobilePhoneAlreadyExists"), null));
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
            c.setMobilePhone(mobilePhone);
            if (street != null && !street.trim().isEmpty()) {
                c.setStreet(street);
            }
            if (city != null && !city.trim().isEmpty()) {
                c.setCity(city);
            }
            if (state != null && !state.trim().isEmpty()) {
                c.setState(state);
            }
            if (country != null && !country.trim().isEmpty()) {
                c.setCountry(country);
            }
            if (latitude != null) {
                c.setLatitude(latitude);
            }
            if (longitude != null) {
                c.setLongitude(longitude);
            }
            c.setCreatedAt(new Date());
            c.setUserID(u);
            customersFacade.create(c);

            // Create Address record if address data was provided during registration
            if (country != null && !country.trim().isEmpty()
                    && city != null && !city.trim().isEmpty()
                    && street != null && !street.trim().isEmpty()) {

                Addresses addr = new Addresses();
                addr.setCustomerID(c);
                addr.setType(addressType != null && !addressType.isEmpty() ? addressType : "Home");
                addr.setStreet(street);
                addr.setCity(city);
                addr.setRegion(state);
                addr.setState(state);
                addr.setCountry(country);
                addr.setLatitude(latitude);
                addr.setLongitude(longitude);
                addr.setHouse(addressLabel);
                addr.setCreatedAt(new Date());
                addr.setIsDefault(true); // First address is default
                addressesFacade.create(addr);
            }

            currentUser = u;
            currentCustomer = c;
            loggedIn = true;

            // Clear registration form data
            regUsername = null;
            regEmail = null;
            regPassword = null;
            regConfirmPassword = null;
            firstName = null;
            middleName = null;
            lastName = null;
            mobilePhone = null;
            street = null;
            city = null;
            state = null;
            country = null;
            latitude = null;
            longitude = null;
            addressType = null;
            addressLabel = null;

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
        if (pwd == null) {
            return null;
        }
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
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public String getRegUsername() {
        return regUsername;
    }

    public void setRegUsername(String regUsername) {
        this.regUsername = regUsername;
    }

    public String getRegEmail() {
        return regEmail;
    }

    public void setRegEmail(String regEmail) {
        this.regEmail = regEmail;
    }

    public String getRegPassword() {
        return regPassword;
    }

    public void setRegPassword(String regPassword) {
        this.regPassword = regPassword;
    }

    public String getRegConfirmPassword() {
        return regConfirmPassword;
    }

    public void setRegConfirmPassword(String regConfirmPassword) {
        this.regConfirmPassword = regConfirmPassword;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public String getAddressLabel() {
        return addressLabel;
    }

    public void setAddressLabel(String addressLabel) {
        this.addressLabel = addressLabel;
    }

    public Users getCurrentUser() {
        return currentUser;
    }

    public Customers getCurrentCustomer() {
        return currentCustomer;
    }

    public Users getCurrentUserLazy() {
        if (currentUser == null) {
            try {
                FacesContext fc = FacesContext.getCurrentInstance();
                Object idObj = fc.getExternalContext().getSessionMap().get("currentUserId");
                if (idObj != null) {
                    Integer id = null;
                    if (idObj instanceof Integer) {
                        id = (Integer) idObj;
                    } else if (idObj instanceof Long) {
                        id = ((Long) idObj).intValue();
                    } else if (idObj instanceof String) {
                        id = Integer.valueOf((String) idObj);
                    }
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
            if (val instanceof Boolean) {
                return (Boolean) val;
            }
        } catch (Exception e) {
            // ignore
        }
        return loggedIn;
    }

    // Normalize name: remove diacritics (e.g., "Trần Bảo Toàn" -> "Tran Bao Toan")
    private String normalize(String s) {
        if (s == null) {
            return null;
        }
        String tmp = Normalizer.normalize(s, Normalizer.Form.NFD);
        // remove diacritic marks
        tmp = tmp.replaceAll("\\p{M}", "");
        // handle vietnamese đ/Đ
        tmp = tmp.replace('đ', 'd').replace('Đ', 'D');
        // remove any remaining non-ASCII characters
        tmp = tmp.replaceAll("[^\\p{ASCII}]", "");
        return tmp.trim();
    }

    public String getGoogleFullName() {
        if (googleFullName == null) {
            return null;
        }
        return googleFullName;
    }

    // Setters to allow external code (servlets) to update session-scoped bean state
    public void setCurrentUser(Users user) {
        this.currentUser = user;
    }

    public void setCurrentCustomer(Customers customer) {
        this.currentCustomer = customer;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public String getDisplayName() {
        // Ensure currentUser/currentCustomer are loaded from session if not present
        getCurrentUserLazy();

        if (currentCustomer != null) {
            StringBuilder sb = new StringBuilder();
            if (currentCustomer.getFirstName() != null && !currentCustomer.getFirstName().isEmpty()) {
                sb.append(currentCustomer.getFirstName()).append(' ');
            }
            if (currentCustomer.getMiddleName() != null && !currentCustomer.getMiddleName().isEmpty()) {
                sb.append(currentCustomer.getMiddleName()).append(' ');
            }
            if (currentCustomer.getLastName() != null && !currentCustomer.getLastName().isEmpty()) {
                sb.append(currentCustomer.getLastName());
            }
            String name = sb.toString().trim();
            if (!name.isEmpty()) {
                return name;
            }
        }
        if (currentUser != null && currentUser.getUsername() != null) {
            return normalize(currentUser.getUsername());
        }
        return "";
    }

    // Google OAuth methods
    /**
     * Handle Google OAuth callback. If email exists, log in directly. If email
     * doesn't exist, generate OTP and show verification form.
     */
    public String handleGoogleAuth(String email, String fullName) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (email != null) {
            email = email.trim();
        }

        // Check if user exists
        Users existingUser = usersFacade.findByEmail(email);
        if (existingUser != null) {
            // Check if user account is inactive
            if ("INACTIVE".equalsIgnoreCase(existingUser.getStatus())) {
                fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                        jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                        "Your account had been Inactived.", null
                ));
                try {
                    FacesContext.getCurrentInstance().getExternalContext().redirect(
                            FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/pages/user/login.xhtml"
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            // Check if user is banned
            try {
                Date now = new Date();
                Date banUntil = existingUser.getBanUntil();
                if (banUntil != null) {
                    if (banUntil.after(now)) {
                        String timeStr = new SimpleDateFormat("HH:mm").format(banUntil);
                        String dateStr = new SimpleDateFormat("dd/MM/yyyy").format(banUntil);
                        fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                                "Your account had been banned. It will end at '" + timeStr + "' '" + dateStr + "'", null
                        ));
                        try {
                            FacesContext.getCurrentInstance().getExternalContext().redirect(
                                    FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/pages/user/login.xhtml"
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    // Ban expired -> clear
                    existingUser.setBanUntil(null);
                    if ("BANNED".equalsIgnoreCase(existingUser.getStatus())) {
                        existingUser.setStatus("ACTIVE");
                    }
                    usersFacade.edit(existingUser);
                }
            } catch (Exception e) {
                // ignore
            }

            // User exists, log them in
            currentUser = existingUser;
            if (existingUser.getCustomersList() != null && !existingUser.getCustomersList().isEmpty()) {
                currentCustomer = existingUser.getCustomersList().get(0);
            }

            // Update LastOnlineAt (used for Online indicator in admin user management)
            try {
                existingUser.setLastOnlineAt(new Date());
                usersFacade.edit(existingUser);
            } catch (Exception e) {
                System.out.println("AuthController.handleGoogleAuth: Warning - could not update LastOnlineAt: " + e.getMessage());
            }

            loggedIn = true;
            // Store in session for filter access
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentUser", currentUser);
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentCustomer", currentCustomer);
            if (currentCustomer != null) {
                FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentCustomerId", currentCustomer.getCustomerID());
            }
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("loggedIn", loggedIn);

            // Also store in HttpSession for servlet access (match normal login())
            try {
                Object sessionObj = FacesContext.getCurrentInstance().getExternalContext().getSession(false);
                if (sessionObj instanceof jakarta.servlet.http.HttpSession) {
                    jakarta.servlet.http.HttpSession httpSession = (jakarta.servlet.http.HttpSession) sessionObj;
                    httpSession.setAttribute("currentUser", currentUser);
                    httpSession.setAttribute("currentCustomer", currentCustomer);
                    if (currentCustomer != null) {
                        httpSession.setAttribute("currentCustomerId", currentCustomer.getCustomerID());
                    }
                    httpSession.setAttribute("loggedIn", loggedIn);
                }
            } catch (Exception e) {
                System.out.println("AuthController.handleGoogleAuth: Warning - could not store in HttpSession: " + e.getMessage());
            }
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                    localeController.getMessage("success.googleLoginSuccess"), null
            ));
            // Role-based redirect (match normal login())
            String redirectUrl;
            String role = (existingUser.getRole() != null) ? existingUser.getRole().toUpperCase() : "CUSTOMER";
            
            if ("ADMIN".equals(role)) {
                redirectUrl = "/pages/admin/dashboard.xhtml";
            } else if ("STAFF".equals(role)) {
                redirectUrl = "/pages/staff/dashboard.xhtml";
            } else if ("CUSTOMER".equals(role)) {
                redirectUrl = "/pages/user/index.xhtml";
            } else {
                redirectUrl = "/pages/user/profile.xhtml";
            }

            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect(
                        FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + redirectUrl
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
     * Unified OTP verification: handles Google registration flow and
     * password-reset flow.
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
            if (email == null || email.isEmpty()) {
                email = googleEmail;
            }
            fullName = (String) fc.getExternalContext().getSessionMap().get("googleFullName");
            if (fullName == null || fullName.isEmpty()) {
                fullName = googleFullName;
            }
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
            while (usersFacade.findByUsername(username + counter) != null) {
                counter++;
            }
            u.setUsername(username + counter);
            u.setPasswordHash(hashPassword(java.util.UUID.randomUUID().toString()));
            u.setRole("customer");
            u.setStatus("ACTIVE");
            u.setCreatedAt(new Date());
            // New Google-created account must start with default avatar (user.png)
            // Keep AvatarUrl null/empty and let UI fall back to /images/user.png
            u.setAvatarUrl(null);
            u.setLastOnlineAt(new Date());
            usersFacade.create(u);

            Customers c = new Customers();
            String[] nameParts = fullName.split(" ");
            if (nameParts.length >= 1) {
                c.setFirstName(nameParts[0]);
            }
            if (nameParts.length >= 2) {
                if (nameParts.length == 2) {
                    c.setLastName(nameParts[1]);
                } else {
                    StringBuilder middle = new StringBuilder();
                    for (int i = 1; i < nameParts.length - 1; i++) {
                        if (i > 1) {
                            middle.append(" ");
                        }
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
            // Store in session for filter access
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentUser", currentUser);
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentCustomer", currentCustomer);
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentCustomerId", currentCustomer.getCustomerID());
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("loggedIn", loggedIn);

            // Also store in HttpSession for servlet access (match normal login())
            try {
                Object sessionObj = FacesContext.getCurrentInstance().getExternalContext().getSession(false);
                if (sessionObj instanceof jakarta.servlet.http.HttpSession) {
                    jakarta.servlet.http.HttpSession httpSession = (jakarta.servlet.http.HttpSession) sessionObj;
                    httpSession.setAttribute("currentUser", currentUser);
                    httpSession.setAttribute("currentCustomer", currentCustomer);
                    httpSession.setAttribute("currentCustomerId", currentCustomer.getCustomerID());
                    httpSession.setAttribute("loggedIn", loggedIn);
                }
            } catch (Exception e) {
                System.out.println("AuthController.verifyOtp: Warning - could not store in HttpSession: " + e.getMessage());
            }

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
                if (email == null || email.isEmpty()) {
                    email = googleEmail;
                }
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
    public String getGoogleEmail() {
        return googleEmail;
    }

    public void setGoogleEmail(String googleEmail) {
        this.googleEmail = googleEmail;
    }

    public void setGoogleFullName(String googleFullName) {
        this.googleFullName = googleFullName;
    }

    public String getOtpInputValue() {
        return otpInputValue;
    }

    public void setOtpInputValue(String otpInputValue) {
        this.otpInputValue = otpInputValue;
    }

    public boolean isGoogleRegistering() {
        return googleRegistering;
    }

    public void setGoogleRegistering(boolean googleRegistering) {
        this.googleRegistering = googleRegistering;
    }

    public String getForgotEmail() {
        return forgotEmail;
    }

    public void setForgotEmail(String forgotEmail) {
        this.forgotEmail = forgotEmail;
    }

    public String getPasswordResetEmail() {
        return passwordResetEmail;
    }

    public void setPasswordResetEmail(String passwordResetEmail) {
        this.passwordResetEmail = passwordResetEmail;
    }

    public boolean isPasswordResetVerified() {
        return passwordResetVerified;
    }

    public void setPasswordResetVerified(boolean passwordResetVerified) {
        this.passwordResetVerified = passwordResetVerified;
    }

    public String getResetNewPassword() {
        return resetNewPassword;
    }

    public void setResetNewPassword(String resetNewPassword) {
        this.resetNewPassword = resetNewPassword;
    }

    public String getResetConfirmPassword() {
        return resetConfirmPassword;
    }

    public void setResetConfirmPassword(String resetConfirmPassword) {
        this.resetConfirmPassword = resetConfirmPassword;
    }

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

    // ============= Profile Page Properties =============
    // Getters to provide user info to profile.xhtml
    public String getUserFullName() {
        getCurrentUserLazy();
        if (currentCustomer != null) {
            StringBuilder sb = new StringBuilder();
            if (currentCustomer.getFirstName() != null && !currentCustomer.getFirstName().isEmpty()) {
                sb.append(currentCustomer.getFirstName()).append(' ');
            }
            if (currentCustomer.getMiddleName() != null && !currentCustomer.getMiddleName().isEmpty()) {
                sb.append(currentCustomer.getMiddleName()).append(' ');
            }
            if (currentCustomer.getLastName() != null && !currentCustomer.getLastName().isEmpty()) {
                sb.append(currentCustomer.getLastName());
            }
            String result = sb.toString().trim();
            if (!result.isEmpty()) {
                return result;
            }
        }
        if (currentUser != null && currentUser.getUsername() != null) {
            return normalize(currentUser.getUsername());
        }
        return "User";
    }

    public String getUserFirstName() {
        getCurrentUserLazy();
        if (currentCustomer != null && currentCustomer.getFirstName() != null) {
            return currentCustomer.getFirstName();
        }
        return "";
    }

    public String getUserLastName() {
        getCurrentUserLazy();
        if (currentCustomer != null && currentCustomer.getLastName() != null) {
            return currentCustomer.getLastName();
        }
        return "";
    }

    public String getUserEmail() {
        getCurrentUserLazy();
        if (currentUser != null && currentUser.getEmail() != null) {
            return currentUser.getEmail();
        }
        return "";
    }

    public String getUserPhoneNumber() {
        getCurrentUserLazy();
        if (currentCustomer != null && currentCustomer.getMobilePhone() != null) {
            return currentCustomer.getMobilePhone();
        }
        return "";
    }

    public String getUserProfileImageUrl() {
        getCurrentUserLazy();
        try {
            String ctx = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
            if (currentUser != null && currentUser.getUserID() != null) {
                // Query fresh from DB to get latest avatar URL
                Users freshUser = null;
                try {
                    freshUser = usersFacade.find(currentUser.getUserID());
                } catch (Exception e) {
                    System.out.println("getUserProfileImageUrl: Could not refresh user from DB: " + e.getMessage());
                    freshUser = currentUser;
                }
                
                // Use fresh user data for avatar URL
                String avatarUrl = freshUser != null ? freshUser.getAvatarUrl() : currentUser.getAvatarUrl();
                
                // Always use current timestamp for cache-busting to prevent stale cache
                long t = System.currentTimeMillis();
                
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    if (avatarUrl.contains("?")) {
                        return avatarUrl + "&t=" + t;
                    } else {
                        return avatarUrl + "?t=" + t;
                    }
                }
                // No avatar set -> use bundled default
                return ctx + "/resources/images/user.png";
            }
            // fallback to bundled default image
            return ctx + "/resources/images/user.png";
        } catch (Exception e) {
            return "/resources/images/user.png";
        }
    }

    public String getUserMemberSince() {
        getCurrentUserLazy();
        if (currentUser != null && currentUser.getCreatedAt() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy");
            return sdf.format(currentUser.getCreatedAt());
        }
        return "";
    }

    public String getUserMembershipLevel() {
        // Default membership level; can be extended to read from a separate column or badge
        return "Member";
    }

    // Profile editing bindings and actions
    public Integer getUserId() {
        getCurrentUserLazy();
        if (currentUser != null) {
            return currentUser.getUserID();
        }
        return null;
    }

    public String getUserMiddleName() {
        getCurrentUserLazy();
        if (currentCustomer != null && currentCustomer.getMiddleName() != null) {
            return currentCustomer.getMiddleName();
        }
        return "";
    }

    public void setUserMiddleName(String middleName) {
        getCurrentUserLazy();
        if (currentCustomer == null) {
            if (currentUser != null) {
                currentCustomer = customersFacade.findByUserID(currentUser.getUserID());
            }
            if (currentCustomer == null && currentUser != null) {
                currentCustomer = new Customers();
                currentCustomer.setUserID(currentUser);
                currentCustomer.setCreatedAt(new Date());
            }
        }
        if (currentCustomer != null) {
            currentCustomer.setMiddleName(middleName);
        }
    }

    public String getUserHomePhone() {
        getCurrentUserLazy();
        if (currentCustomer != null && currentCustomer.getHomePhone() != null) {
            return currentCustomer.getHomePhone();
        }
        return "";
    }

    public void setUserHomePhone(String homePhone) {
        getCurrentUserLazy();
        if (currentCustomer == null) {
            if (currentUser != null) {
                currentCustomer = customersFacade.findByUserID(currentUser.getUserID());
            }
            if (currentCustomer == null && currentUser != null) {
                currentCustomer = new Customers();
                currentCustomer.setUserID(currentUser);
                currentCustomer.setCreatedAt(new Date());
            }
        }
        if (currentCustomer != null) {
            currentCustomer.setHomePhone(homePhone);
        }
    }

    public void setUserFirstName(String firstName) {
        getCurrentUserLazy();
        if (currentCustomer == null) {
            if (currentUser != null) {
                currentCustomer = customersFacade.findByUserID(currentUser.getUserID());
            }
            if (currentCustomer == null && currentUser != null) {
                currentCustomer = new Customers();
                currentCustomer.setUserID(currentUser);
                currentCustomer.setCreatedAt(new Date());
            }
        }
        if (currentCustomer != null) {
            currentCustomer.setFirstName(firstName);
        }
    }

    public void setUserLastName(String lastName) {
        getCurrentUserLazy();
        if (currentCustomer == null) {
            if (currentUser != null) {
                currentCustomer = customersFacade.findByUserID(currentUser.getUserID());
            }
            if (currentCustomer == null && currentUser != null) {
                currentCustomer = new Customers();
                currentCustomer.setUserID(currentUser);
                currentCustomer.setCreatedAt(new Date());
            }
        }
        if (currentCustomer != null) {
            currentCustomer.setLastName(lastName);
        }
    }

    public void setUserPhoneNumber(String phone) {
        getCurrentUserLazy();
        if (currentCustomer == null) {
            if (currentUser != null) {
                currentCustomer = customersFacade.findByUserID(currentUser.getUserID());
            }
            if (currentCustomer == null && currentUser != null) {
                currentCustomer = new Customers();
                currentCustomer.setUserID(currentUser);
                currentCustomer.setCreatedAt(new Date());
            }
        }
        if (currentCustomer != null) {
            currentCustomer.setMobilePhone(phone);
        }
    }

    public boolean isEditingProfile() {
        return editingProfile;
    }

    public String startEdit() {
        editingProfile = true;
        getCurrentUserLazy();
        if (currentCustomer == null && currentUser != null) {
            currentCustomer = customersFacade.findByUserID(currentUser.getUserID());
        }
        return null;
    }

    public String cancelEdit() {
        editingProfile = false;
        // reload from DB to discard unsaved changes
        getCurrentUserLazy();
        if (currentUser != null) {
            currentCustomer = customersFacade.findByUserID(currentUser.getUserID());
        }
        return null;
    }

    public String saveProfile() {
        FacesContext fc = FacesContext.getCurrentInstance();
        try {
            getCurrentUserLazy();
            if (currentUser == null) {
                fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, "No logged-in user", null));
                fc.getExternalContext().getRequestMap().put("profileSaveStatus", "error");
                fc.getExternalContext().getRequestMap().put("profileSaveMessage", "No logged-in user");
                return null;
            }
            if (currentCustomer == null) {
                currentCustomer = customersFacade.findByUserID(currentUser.getUserID());
            }
            if (currentCustomer == null) {
                currentCustomer = new Customers();
                currentCustomer.setUserID(currentUser);
                currentCustomer.setCreatedAt(new Date());
                customersFacade.create(currentCustomer);
            } else {
                customersFacade.edit(currentCustomer);
            }
            // reload from DB to ensure latest persisted values (fix immediate UI updates)
            try {
                currentCustomer = customersFacade.findByUserID(currentUser.getUserID());
                setCurrentCustomer(currentCustomer);
            } catch (Exception ignore) {
            }
            editingProfile = false;
            String successMsg = localeController.getMessage("success.profileSaved");
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_INFO, successMsg, null));
            fc.getExternalContext().getRequestMap().put("profileSaveStatus", "success");
            fc.getExternalContext().getRequestMap().put("profileSaveMessage", successMsg);
        } catch (Exception e) {
            String err = localeController.getMessage("error.saveProfileFailed");
            fc.addMessage(null, new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_ERROR, err, null));
            fc.getExternalContext().getRequestMap().put("profileSaveStatus", "error");
            fc.getExternalContext().getRequestMap().put("profileSaveMessage", err + (e.getMessage() != null ? ": " + e.getMessage() : ""));
            e.printStackTrace();
        }
        return null;
    }
}
