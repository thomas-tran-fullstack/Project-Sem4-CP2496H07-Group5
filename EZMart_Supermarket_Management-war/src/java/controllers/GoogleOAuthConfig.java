package controllers;

/**
 * Google OAuth Configuration
 * Replace these with your actual credentials from Google Cloud Console
 */
public class GoogleOAuthConfig {
    // Get from: https://console.cloud.google.com/
    // Go to APIs & Services > Credentials
    // Create OAuth 2.0 Client ID (Web application)
    
    public static final String CLIENT_ID = "46177555204-pse95ofks7srvnkuicvb2797i77dl9ab.apps.googleusercontent.com";
    public static final String CLIENT_SECRET = "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";
    public static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    public static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    public static final String SCOPE = "openid email profile";
    
    // ===== IMPORTANT: Set this to your server's public URL =====
    // For ngrok: https://disepalous-emiliano-oppositely.ngrok-free.dev
    // For production: https://yourdomain.com
    // LEAVE EMPTY to auto-detect from request
    public static final String BASE_URL = "https://disepalous-emiliano-oppositely.ngrok-free.dev";
    
    /**
     * Get the redirect URI
     * If BASE_URL is set, use it; otherwise, detect from request
     */
    public static String getRedirectUri(Object request) {
        // If BASE_URL is explicitly set, use it
        if (BASE_URL != null && !BASE_URL.isEmpty() && !BASE_URL.equals("")) {
            String redirectUri = BASE_URL + "/EZMart_Supermarket_Management-war/oauth-callback";
            System.out.println("GoogleOAuthConfig: Using BASE_URL - " + redirectUri);
            return redirectUri;
        }
        
        // Otherwise, try to detect from request
        try {
            String scheme = (String) request.getClass().getMethod("getScheme").invoke(request);
            String serverName = (String) request.getClass().getMethod("getServerName").invoke(request);
            int port = (int) request.getClass().getMethod("getServerPort").invoke(request);
            String contextPath = (String) request.getClass().getMethod("getContextPath").invoke(request);
            
            StringBuilder redirectUri = new StringBuilder();
            redirectUri.append(scheme).append("://").append(serverName);
            
            // Add port if it's not the default port for the scheme
            if ((scheme.equals("http") && port != 80) || 
                (scheme.equals("https") && port != 443)) {
                redirectUri.append(":").append(port);
            }
            
            redirectUri.append(contextPath).append("/oauth-callback");
            System.out.println("GoogleOAuthConfig: Auto-detected redirect URI: " + redirectUri.toString());
            return redirectUri.toString();
        } catch (Exception e) {
            System.err.println("Error building redirect URI: " + e.getMessage());
            e.printStackTrace();
            // Fallback
            return "http://localhost:8080/EZMart_Supermarket_Management-war/oauth-callback";
        }
    }
}
