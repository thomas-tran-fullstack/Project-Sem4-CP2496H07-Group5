package controllers;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.util.Locale;
import java.util.ResourceBundle;

@Named("locale")
@SessionScoped
public class LocaleController implements Serializable {
    private static final long serialVersionUID = 1L;

    private String currentLanguage = "en"; // default English

    public LocaleController() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null) {
            Locale userLocale = fc.getViewRoot().getLocale();
            if (userLocale != null) {
                currentLanguage = userLocale.getLanguage();
            }
        }
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void setCurrentLanguage(String lang) {
        this.currentLanguage = lang;
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null) {
            Locale locale = new Locale(lang);
            fc.getViewRoot().setLocale(locale);
            fc.getExternalContext().getSessionMap().put("locale", locale);
        }
    }

    public Locale getCurrentLocale() {
        return new Locale(currentLanguage);
    }

    /**
     * Get message from bundle
     */
    public String getMessage(String key) {
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc == null) return key;
            ResourceBundle bundle = ResourceBundle.getBundle("messages.messages", new Locale(currentLanguage));
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            } else {
                System.out.println("[LocaleController] Missing key '" + key + "' for locale " + currentLanguage);
                return key;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return key;
        }
    }

    public String getLanguageName(String code) {
        switch (code) {
            case "en": return "English";
            case "vi": return "Tiếng Việt";
            case "es": return "Español";
            case "de": return "Deutsch";
            case "ja": return "日本語";
            case "fr": return "Français";
            case "pt": return "Português";
            case "ru": return "Русский";
            case "ko": return "한국어";
            case "it": return "Italiano";
            case "ar": return "العربية";
            case "pl": return "Polski";
            case "tr": return "Türkçe";
            case "nl": return "Nederlands";
            case "th": return "ไทย";
            case "id": return "Bahasa Indonesia";
            case "sv": return "Svenska";
            case "hi": return "हिन्दी";
            case "he": return "עברית";
            case "el": return "Ελληνικά";
            case "da": return "Dansk";
            case "fi": return "Suomi";
            case "zh": return "中文";
            default: return code;
        }
    }
}
