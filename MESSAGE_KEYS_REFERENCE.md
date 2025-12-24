# EZMart i18n Message Keys Reference

## Complete List of Available Message Keys

All message keys follow a consistent `prefix.key` naming convention and are available in all 24 languages.

### Login Page Keys (`login.*`)
```
login.title                = "Sign into your account" / equivalent translation
login.subtitle             = "Welcome to EZMart Supermarket" / equivalent
login.emailLabel           = "Email address" / equivalent
login.passwordLabel        = "Password" / equivalent
login.rememberMe           = "Remember me" / equivalent
login.forgotPassword       = "Forgot password?" / equivalent
login.signIn               = "Sign in" / equivalent
login.continueWith         = "or continue with" / equivalent
login.noAccount            = "Don't have an account?" / equivalent
login.signUp               = "Sign up" / equivalent
```

### Registration Page Keys (`register.*`)
```
register.title             = "Create account" / equivalent
register.createAccount     = "Create a new account" / equivalent
register.username          = "Username" / equivalent
register.email             = "Email address" / equivalent
register.password          = "Password" / equivalent
register.confirmPassword   = "Confirm password" / equivalent
register.firstName         = "First name" / equivalent
register.lastName          = "Last name" / equivalent
register.signUp            = "Sign up" / equivalent
```

### Forgot Password Page Keys (`forgotpassword.*`)
```
forgotpassword.title       = "Forgot password?" / equivalent
forgotpassword.subtitle    = "Enter the email address associated with your account" / equivalent
forgotpassword.email       = "Email address" / equivalent
forgotpassword.send        = "Send verification code" / equivalent
forgotpassword.backToLogin = "Back to login" / equivalent
```

### Reset Password Page Keys (`resetpassword.*`)
```
resetpassword.title        = "Reset password" / equivalent
resetpassword.subtitle     = "Enter a new password for your account" / equivalent
resetpassword.newPassword  = "New password" / equivalent
resetpassword.confirmPassword = "Confirm password" / equivalent
resetpassword.strength     = "Password strength" / equivalent
resetpassword.reset        = "Reset password" / equivalent
```

### OTP Verification Page Keys (`verifyotp.*`)
```
verifyotp.title            = "Verify code" / equivalent
verifyotp.subtitle         = "We've sent a 4-digit verification code" / equivalent
verifyotp.codeLabel        = "Verification code" / equivalent
verifyotp.verify           = "Verify" / equivalent
verifyotp.didNotReceive    = "Didn't receive the code?" / equivalent
verifyotp.resend           = "Resend" / equivalent
verifyotp.codeExpiry       = "Code expires in 5 minutes" / equivalent
```

### Error Messages (`error.*`)
```
error.emailNotFound        = "No account found with that email. Please create one!"
error.emailRequired        = "Email field is required"
error.passwordRequired     = "Password field is required"
error.usernameRequired     = "Username field is required"
error.passwordMismatch     = "Passwords do not match"
error.invalidCredentials   = "Invalid email or password"
error.emailAlreadyExists   = "This email is already registered"
error.usernameAlreadyExists = "This username is already in use"
error.registrationFailed   = "Registration failed. Please try again"
error.invalidOtp           = "Invalid verification code"
error.otpEmpty             = "Please enter the verification code"
error.sessionExpired       = "Session has expired. Please try again"
error.passwordResetFailed  = "Failed to reset password. Please try again"
```

### Success Messages (`success.*`)
```
success.loginSuccess       = "Login successful!"
success.googleLoginSuccess = "Google login successful!"
success.registrationSuccess = "Registration successful!"
success.otpSent            = "Verification code sent to your email"
success.otpResent          = "Verification code resent"
success.passwordResetSuccess = "Password reset successfully!"
```

### Validation Messages (`validation.*`)
```
validation.passwordWeak    = "Weak password"
validation.passwordMedium  = "Medium password"
validation.passwordStrong  = "Strong password"
```

### Header Navigation Keys (`header.*`)
```
header.search              = "Search"
header.allCategories       = "All categories"
header.aisles              = "Aisles"
header.deals               = "Deals"
header.orders              = "Orders"
header.signIn              = "Sign in"
header.account             = "Account"
header.userProfile         = "My profile"
header.logOut              = "Log out"
header.storeLocator        = "Store locator"
header.customerSupport     = "Customer support"
header.trackOrder          = "Track order"
header.welcome             = "Welcome"
```

## Usage Examples

### In XHTML (JSF) Pages
```html
<!-- Simple message retrieval -->
<h:outputText value="#{locale.getMessage('login.title')}" />

<!-- With variables -->
<h1>#{locale.getMessage('header.welcome')}</h1>

<!-- In form labels -->
<h:outputLabel for="emailInput" value="#{locale.getMessage('login.emailLabel')}" />
<h:inputText id="emailInput" value="#{someBean.email}" />

<!-- In buttons -->
<h:commandButton value="#{locale.getMessage('login.signIn')}" 
                  action="#{authController.login()}" />

<!-- In messages/errors -->
<h:message for="emailInput" rendered="#{not empty emailInput.value and not valid}">
    #{locale.getMessage('error.emailRequired')}
</h:message>
```

### In Java Code (AuthController)
```java
// Inject LocaleController
@Inject
private LocaleController localeController;

// Get translated message
String message = localeController.getMessage("error.emailNotFound");

// Use in FacesMessage
FacesContext.getCurrentInstance().addMessage(null, 
    new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));

// Or for validation errors
String passwordError = localeController.getMessage("error.passwordMismatch");
```

## Language Codes Supported

| Code | Language | Native Name |
|------|----------|-------------|
| en | English | English |
| vi | Vietnamese | Tiếng Việt |
| es | Spanish | Español |
| de | German | Deutsch |
| ja | Japanese | 日本語 |
| fr | French | Français |
| pt | Portuguese | Português |
| ru | Russian | Русский |
| ko | Korean | 한국어 |
| it | Italian | Italiano |
| ar | Arabic | العربية |
| pl | Polish | Polski |
| tr | Turkish | Türkçe |
| nl | Dutch | Nederlands |
| th | Thai | ไทย |
| id | Indonesian | Bahasa Indonesia |
| sv | Swedish | Svenska |
| hi | Hindi | हिन्दी |
| he | Hebrew | עברית |
| el | Greek | Ελληνικά |
| da | Danish | Dansk |
| fi | Finnish | Suomi |
| zh | Chinese (Simplified) | 中文 |
| sec | Secondary | Secundary |

## Adding New Message Keys

To add a new message key:

1. **Choose appropriate prefix** based on page:
   - `login.` for login page
   - `register.` for registration
   - `error.` for error messages
   - `success.` for success messages
   - `validation.` for validation messages
   - `header.` for header/navigation text
   - Create new prefix for other pages (e.g., `product.`, `cart.`, `profile.`, etc.)

2. **Add to all language files**:
   ```properties
   # In messages_en.properties:
   product.title=Product Details
   
   # In messages_vi.properties:
   product.title=Chi tiết sản phẩm
   
   # In messages_es.properties:
   product.title=Detalles del producto
   # ... etc for all 24 languages
   ```

3. **Use in XHTML**:
   ```html
   <h1>#{locale.getMessage('product.title')}</h1>
   ```

## Tips for Developers

1. **Keep message keys short and descriptive**: Use dot notation for hierarchy
   - Good: `login.title`, `error.emailNotFound`
   - Bad: `page1Title`, `err_em_notfound`

2. **Group related messages**: Use common prefixes
   - All login-related: `login.*`
   - All errors: `error.*`
   - All validation: `validation.*`

3. **Use consistent naming**: Similar concepts should use similar keys
   - `error.emailRequired`, `error.passwordRequired`
   - `success.loginSuccess`, `success.registrationSuccess`

4. **Always update all 24 languages**: When adding new keys, translate them in all language files

5. **Test with different languages**: Ensure text fits UI/layout in all languages
   - German text tends to be longer
   - Arabic uses RTL layout
   - Japanese/Chinese don't use spaces between words

6. **Use getMessage() for dynamic messages**:
   ```java
   // In AuthController or other beans
   String errorMsg = localeController.getMessage("error.invalidCredentials");
   ```

## Current Implementation Status

✅ **Completed:**
- LocaleController CDI bean
- 23 language property files (all keys)
- Header language selector UI
- Session-scoped locale management

📝 **TODO:**
- Update JSF pages to use message keys
- Update AuthController to use message bundles for FacesMessages
- Add more message keys for product, cart, profile, etc. pages
- Test language switching across all pages
- Consider adding language preference to user profile (persist across sessions)
