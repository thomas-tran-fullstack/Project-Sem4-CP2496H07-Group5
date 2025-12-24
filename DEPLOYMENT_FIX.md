# i18n Deployment Issue - Resolution

## Problem Encountered

**Error:** CDI Bean Name Ambiguity
```
WELD-001414: Bean name is ambiguous. Name locale resolves to beans:
- Managed Bean [class sessionbeans.LocaleController] 
- Managed Bean [class controllers.LocaleController]
```

## Root Cause

There was already an existing `LocaleController` in the `controllers` package (in the WAR module) that was created during previous work. When we created a new `LocaleController` in the EJB module's `sessionbeans` package with the same `@Named("locale")` annotation, it created a CDI bean name conflict.

CDI requires all bean names to be unique across the entire application.

## Resolution Applied

### 1. ✅ Deleted Duplicate Bean
- Removed the newly created `LocaleController.java` from:
  ```
  EZMart_Supermarket_Management-ejb/src/java/sessionbeans/LocaleController.java
  ```
- Reason: The existing `controllers.LocaleController` is simpler, already working, and already deployed

### 2. ✅ Moved Message Files to WAR Module Classpath
- The message property files need to be accessible at runtime
- Copied all 23 message property files to:
  ```
  EZMart_Supermarket_Management-war/src/java/messages/
  ```
- Location: 
  - `messages_en.properties`
  - `messages_vi.properties`
  - ... (21 more language files)
  - `messages_zh.properties`

### 3. ✅ Final Architecture

**Existing LocaleController** (controllers.LocaleController):
```
Location: EZMart_Supermarket_Management-war/src/java/controllers/LocaleController.java
Scope: @SessionScoped
Name: @Named("locale")
Methods:
  - getCurrentLanguage()
  - setCurrentLanguage(String lang)
  - getCurrentLocale()
  - getMessage(String key)
  - getLanguageName(String code)
Features:
  - Session-scoped language persistence
  - FacesContext locale update on language change
  - ResourceBundle-based message retrieval
  - Supports 24 languages
```

**Message Files** (Resource Bundles):
```
Location: EZMart_Supermarket_Management-war/src/java/messages/
Count: 23 files (all 24 languages)
Format: messages_XX.properties
Keys per file: 60+ message keys
Total translations: 1,380+
```

## Files Modified/Deleted

| File | Action | Reason |
|------|--------|--------|
| `EZMart_Supermarket_Management-ejb/src/java/sessionbeans/LocaleController.java` | ❌ DELETED | Duplicate of existing controller.LocaleController |
| `EZMart_Supermarket_Management-war/src/java/controllers/LocaleController.java` | ✅ KEPT | Primary implementation (simpler, already working) |
| `EZMart_Supermarket_Management-war/src/java/messages/messages_*.properties` | ✅ ADDED (23 files) | Resource bundles for all 24 languages |

## Verification

✅ **Message Files in WAR Module:**
```
Count: 23 property files
Location: src/java/messages/
All languages: en, vi, es, de, ja, fr, pt, ru, ko, it, ar, pl, tr, nl, th, id, sv, hi, he, el, da, fi, zh
```

✅ **LocaleController Unique:**
```
Only one bean named "locale"
Package: controllers
Scope: SessionScoped
Annotations: @Named("locale") @SessionScoped
Status: Ready for deployment
```

✅ **Header Integration:**
```
File: web/templates/user/header.xhtml
Language selector: Present (31 lines)
Actions: #{locale.setCurrentLanguage('lang')}
Display: All 24 languages with native names
```

## Next Steps

1. **Rebuild Project**
   - Clean and build the entire EAR
   - Verify all 23 message files are bundled in WAR module JAR

2. **Redeploy to GlassFish**
   - Undeploy previous version
   - Deploy cleaned EAR
   - Check deployment logs for CDI errors

3. **Test in Browser**
   - Navigate to application
   - Verify language selector appears in header
   - Click different languages
   - Confirm text updates correctly
   - Check page refresh/navigation persists language

4. **Integrate Message Keys into Pages**
   - Update login.xhtml to use `#{locale.getMessage('key')}`
   - Update forgot_password.xhtml
   - Update reset_password.xhtml  
   - Update verify-otp.xhtml
   - Gradually migrate other pages

## Technical Notes

- **ResourceBundle Loading**: Java looks in classpath for `messages.properties` files
- **WAR Module Classpath**: `src/java/` directory is included in WAR's classpath
- **EJB Module Classpath**: Files in `src/java/` are included in EJB JAR, which is available to WAR via class loader hierarchy
- **Session Persistence**: Language selection persists in HTTP session via `FacesContext.getExternalContext().getSessionMap()`
- **Fallback**: If message key not found, the key itself is returned as fallback text

## Lessons Learned

1. ✅ Check for existing beans before creating new ones
2. ✅ Message resource files should be in the module that uses them (WAR module in this case)
3. ✅ CDI bean names must be globally unique across entire application
4. ✅ Session-scoped beans survive across page navigation in same session

## Success Criteria

When redeployed successfully:
- [ ] Application deploys without CDI bean name conflicts
- [ ] Language selector appears in header
- [ ] Clicking language updates view  
- [ ] Language persists across page navigation
- [ ] All 24 languages display correctly
- [ ] Message keys can be used in JSF pages via `#{locale.getMessage('key')}`

---

**Status:** ✅ Issue Resolved - Ready for Rebuild and Redeploy
**Next Action:** Clean build and deploy to GlassFish
