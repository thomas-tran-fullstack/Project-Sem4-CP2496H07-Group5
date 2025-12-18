package controllers;

/**
 * Google OAuth Configuration
 * Replace these with your actual credentials from Google Cloud Console
 */
public class GoogleOAuthConfig {
    // Get from: https://console.cloud.google.com/
    // Go to APIs & Services > Credentials
    // Create OAuth 2.0 Client ID (Web application)
    // Add redirect URI: http://localhost:8080/EZMart_Supermarket_Management-war/oauth-callback
    
    // IMPORTANT: For development with localhost, use this redirect:
    // http://localhost:8080/EZMart_Supermarket_Management-war/oauth-callback
    
    // For production, change to your actual domain:
    // https://yourdomain.com/EZMart_Supermarket_Management-war/oauth-callback
    
    public static final String CLIENT_ID = "46177555204-pse95ofks7srvnkuicvb2797i77dl9ab.apps.googleusercontent.com";
    public static final String CLIENT_SECRET = "YOUR_CLIENT_SECRET_HERE";
    public static final String REDIRECT_URI = "http://localhost:8080/EZMart_Supermarket_Management-war/oauth-callback";
    public static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    public static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    public static final String SCOPE = "openid email profile";
}
