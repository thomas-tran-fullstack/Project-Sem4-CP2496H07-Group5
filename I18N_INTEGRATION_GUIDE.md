# EZMart i18n Integration Guide for JSF Pages

## Quick Start: Converting a Page to Use i18n

### Example: Converting login.xhtml

#### BEFORE (Hardcoded text):
```html
<div class="space-y-4">
    <h1 class="text-2xl font-bold">Sign into your account</h1>
    <p class="text-gray-600">Welcome to EZMart Supermarket</p>
    
    <h:form>
        <div class="space-y-3">
            <label class="block text-sm font-medium">Email address</label>
            <h:inputText id="email" value="#{auth.email}" />
            
            <label class="block text-sm font-medium">Password</label>
            <h:inputSecret id="password" value="#{auth.password}" />
            
            <h:commandButton value="Sign in" action="#{auth.login()}" />
        </div>
    </h:form>
    
    <p class="text-center">
        Don't have an account?
        <h:link value="Sign up" outcome="register" />
    </p>
</div>
```

#### AFTER (Using i18n):
```html
<div class="space-y-4">
    <h1 class="text-2xl font-bold">#{locale.getMessage('login.title')}</h1>
    <p class="text-gray-600">#{locale.getMessage('login.subtitle')}</p>
    
    <h:form>
        <div class="space-y-3">
            <label class="block text-sm font-medium">#{locale.getMessage('login.emailLabel')}</label>
            <h:inputText id="email" value="#{auth.email}" />
            
            <label class="block text-sm font-medium">#{locale.getMessage('login.passwordLabel')}</label>
            <h:inputSecret id="password" value="#{auth.password}" />
            
            <h:commandButton value="#{locale.getMessage('login.signIn')}" 
                              action="#{auth.login()}" />
        </div>
    </h:form>
    
    <p class="text-center">
        #{locale.getMessage('login.noAccount')}
        <h:link value="#{locale.getMessage('login.signUp')}" 
                 outcome="register" />
    </p>
</div>
```

## Page-by-Page Integration Plan

### 1. Login Page (login.xhtml)
**Message keys to use:**
- `login.title` - "Sign into your account"
- `login.subtitle` - "Welcome to EZMart Supermarket"
- `login.emailLabel` - "Email address"
- `login.passwordLabel` - "Password"
- `login.rememberMe` - "Remember me"
- `login.forgotPassword` - "Forgot password?"
- `login.signIn` - "Sign in"
- `login.continueWith` - "or continue with"
- `login.noAccount` - "Don't have an account?"
- `login.signUp` - "Sign up"

**Error messages to add (in AuthController):**
- `error.invalidCredentials` - "Invalid email or password"
- `error.emailRequired` - "Email field is required"
- `error.passwordRequired` - "Password field is required"

**Success messages:**
- `success.loginSuccess` - "Login successful!"
- `success.googleLoginSuccess` - "Google login successful!"

### 2. Register Page (register.xhtml)
**Message keys to use:**
- `register.title` - "Create account"
- `register.createAccount` - "Create a new account"
- `register.username` - "Username"
- `register.email` - "Email address"
- `register.password` - "Password"
- `register.confirmPassword` - "Confirm password"
- `register.firstName` - "First name"
- `register.lastName` - "Last name"
- `register.signUp` - "Sign up"

**Error messages:**
- `error.usernameRequired` - "Username field is required"
- `error.passwordMismatch` - "Passwords do not match"
- `error.emailAlreadyExists` - "This email is already registered"
- `error.usernameAlreadyExists` - "This username is already in use"
- `error.registrationFailed` - "Registration failed. Please try again"

**Success messages:**
- `success.registrationSuccess` - "Registration successful!"

### 3. Forgot Password Page (forgot_password.xhtml)
**Message keys:**
- `forgotpassword.title` - "Forgot password?"
- `forgotpassword.subtitle` - "Enter the email address associated with your account"
- `forgotpassword.email` - "Email address"
- `forgotpassword.send` - "Send verification code"
- `forgotpassword.backToLogin` - "Back to login"

**Error messages:**
- `error.emailNotFound` - "No account found with that email. Please create one!"

**Success messages:**
- `success.otpSent` - "Verification code sent to your email"

### 4. Verify OTP Page (verify-otp.xhtml)
**Message keys:**
- `verifyotp.title` - "Verify code"
- `verifyotp.subtitle` - "We've sent a 4-digit verification code"
- `verifyotp.codeLabel` - "Verification code"
- `verifyotp.verify` - "Verify"
- `verifyotp.didNotReceive` - "Didn't receive the code?"
- `verifyotp.resend` - "Resend"
- `verifyotp.codeExpiry` - "Code expires in 5 minutes"

**Error messages:**
- `error.invalidOtp` - "Invalid verification code"
- `error.otpEmpty` - "Please enter the verification code"
- `error.sessionExpired` - "Session has expired. Please try again"

**Success messages:**
- `success.otpSent` - "Verification code sent to your email"
- `success.otpResent` - "Verification code resent"

### 5. Reset Password Page (reset_password.xhtml)
**Message keys:**
- `resetpassword.title` - "Reset password"
- `resetpassword.subtitle` - "Enter a new password for your account"
- `resetpassword.newPassword` - "New password"
- `resetpassword.confirmPassword` - "Confirm password"
- `resetpassword.strength` - "Password strength"
- `resetpassword.reset` - "Reset password"

**Validation messages:**
- `validation.passwordWeak` - "Weak password"
- `validation.passwordMedium` - "Medium password"
- `validation.passwordStrong` - "Strong password"

**Error messages:**
- `error.passwordResetFailed` - "Failed to reset password. Please try again"

**Success messages:**
- `success.passwordResetSuccess` - "Password reset successfully!"

### 6. Header Navigation (header.xhtml)
**Message keys to add:**
- `header.allCategories` - "All categories"
- `header.aisles` - "Aisles"
- `header.deals` - "Deals"
- `header.orders` - "Orders"
- `header.signIn` - "Sign in"
- `header.account` - "Account"
- `header.userProfile` - "My profile"
- `header.logOut` - "Log out"
- `header.storeLocator` - "Store locator"
- `header.customerSupport` - "Customer support"
- `header.trackOrder` - "Track order"
- `header.welcome` - "Welcome"

## Step-by-Step Integration Instructions

### For Each Page:

1. **Identify all hardcoded text** that appears to users
2. **Map text to message keys** using existing keys or creating new ones
3. **Replace in XHTML with `#{locale.getMessage('key')}`**
4. **Update AuthController/other beans** to use message bundles for error/success messages
5. **Test in multiple languages** to ensure text fits properly

## Example: Adding a Message Key to a Java Bean

### Before (Hardcoded):
```java
@WebServlet(name = "AuthServlet", urlPatterns = {"/auth"})
public class AuthServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        // ... validation ...
        if (email == null || email.isEmpty()) {
            request.setAttribute("errorMessage", "Email field is required");
            // redirect to login
        }
    }
}
```

### After (Using i18n):
```java
@WebServlet(name = "AuthServlet", urlPatterns = {"/auth"})
public class AuthServlet extends HttpServlet {
    @Inject
    private LocaleController localeController;
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        // ... validation ...
        if (email == null || email.isEmpty()) {
            String errorMsg = localeController.getMessage("error.emailRequired");
            request.setAttribute("errorMessage", errorMsg);
            // redirect to login
        }
    }
}
```

### Or in JSF Managed Bean:
```java
@Named("auth")
@SessionScoped
public class AuthController {
    @Inject
    private LocaleController localeController;
    
    public String login() {
        // ... authentication logic ...
        if (!isValid) {
            String errorMsg = localeController.getMessage("error.invalidCredentials");
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, errorMsg, null));
            return null;
        }
        
        String successMsg = localeController.getMessage("success.loginSuccess");
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(successMsg));
        return "redirect:index";
    }
}
```

## Common Patterns

### Pattern 1: Simple Label
```html
<!-- Before -->
<label>Email address</label>

<!-- After -->
<label>#{locale.getMessage('login.emailLabel')}</label>
```

### Pattern 2: Button Text
```html
<!-- Before -->
<h:commandButton value="Sign in" action="#{auth.login()}" />

<!-- After -->
<h:commandButton value="#{locale.getMessage('login.signIn')}" 
                  action="#{auth.login()}" />
```

### Pattern 3: Link Text
```html
<!-- Before -->
<h:link value="Sign up" outcome="register" />

<!-- After -->
<h:link value="#{locale.getMessage('login.signUp')}" 
        outcome="register" />
```

### Pattern 4: Heading
```html
<!-- Before -->
<h1>Sign into your account</h1>

<!-- After -->
<h1>#{locale.getMessage('login.title')}</h1>
```

### Pattern 5: Error/Success Message in Bean
```java
// Before
FacesContext.getCurrentInstance().addMessage(null, 
    new FacesMessage("Login successful!"));

// After
String msg = localeController.getMessage("success.loginSuccess");
FacesContext.getCurrentInstance().addMessage(null, 
    new FacesMessage(msg));
```

## Testing Your Changes

After updating a page:

1. **Build the project** (NetBeans → Run Project)
2. **Navigate to the page** in your browser
3. **Test default language** (English) - text should appear correctly
4. **Click language selector** in header and choose a different language
5. **Verify page updates** with translated text
6. **Check text layout** - some languages (German, Russian) use longer words
7. **Test special characters** - ensure non-Latin scripts display correctly

## Troubleshooting

### Issue: Message key appears instead of translated text
**Solution:** 
- Verify the key exists in all language property files
- Check spelling matches exactly (case-sensitive)
- Ensure property files are in correct location: `EZMart_Supermarket_Management-ejb/src/java/messages/`

### Issue: Language selector not appearing
**Solution:**
- Verify LocaleController bean is created correctly
- Check header.xhtml is updated with language selector code
- Build and rebuild the project

### Issue: Clicking language selector does nothing
**Solution:**
- Verify `#{locale.setCurrentLanguage('lang')}` action is correct
- Check that FacesContext is available
- Review NetBeans/server logs for errors

### Issue: Text not updating after language selection
**Solution:**
- Check that page uses `#{locale.getMessage('key')}` not hardcoded text
- Verify JSF view lifecycle - may need full page refresh
- Check that message key exists in selected language file

## Performance Considerations

1. **ResourceBundle caching**: LocaleController loads bundles on demand - Java caches them
2. **Session storage**: Language preference stored in session (no database queries)
3. **No additional requests**: All translations done on same request

## Security Notes

1. **HTML escaping**: Use `#{locale.getMessage()}` for auto-escaping
2. **No user input in messages**: Message keys are fixed strings, not from user input
3. **Injection safe**: Resource bundles loaded from application classpath only

## Future Enhancements

1. **Persist language preference** to user profile (database)
2. **Auto-detect browser language** - set default based on Accept-Language header
3. **RTL support** - for Arabic, Hebrew (add CSS class to html tag)
4. **Translation management UI** - allow admins to update messages
5. **Missing translation fallback** - show key or English if translation missing
