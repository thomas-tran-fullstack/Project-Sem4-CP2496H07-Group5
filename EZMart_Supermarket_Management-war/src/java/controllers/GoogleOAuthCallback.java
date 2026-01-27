package controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Handles OAuth callback from Google
 * Receives authorization code, exchanges it for token, and gets user info
 */
@WebServlet(name = "GoogleOAuthCallback", urlPatterns = {"/oauth-callback"})
public class GoogleOAuthCallback extends HttpServlet {
    
    // Static initializer to set up trust-all SSL context for development
    static {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // For development only - accept all certificates
                        System.out.println("Dev Mode: Accepting client certificate");
                    }
                    
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // For development only - accept all certificates
                        System.out.println("Dev Mode: Accepting server certificate for " + (certs.length > 0 ? certs[0].getSubjectDN() : "unknown"));
                    }
                }
            }, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            System.err.println("Failed to initialize SSL context: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String code = request.getParameter("code");
        String state = request.getParameter("state");
        String error = request.getParameter("error");
        
        // Handle error
        if (error != null) {
            response.sendRedirect(request.getContextPath() + "/pages/user/login.xhtml?oauth_error=" + error);
            return;
        }
        
        if (code == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing authorization code");
            return;
        }
        
        try {
            // Exchange code for token
            String tokenResponse = exchangeCodeForToken(code, request);
            JsonReader reader = Json.createReader(new java.io.StringReader(tokenResponse));
            JsonObject tokenJson = reader.readObject();

            // token endpoint can return an error JSON when the code is invalid.
            if (!tokenJson.containsKey("access_token")) {
                String err = tokenJson.getString("error", "unknown_error");
                String errDesc = tokenJson.getString("error_description", "");
                String msg = err + (errDesc.isEmpty() ? "" : (": " + errDesc));
                // Redirect back to login with error message
                response.sendRedirect(request.getContextPath() + "/pages/user/login.xhtml?oauth_error=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8));
                return;
            }

            String accessToken = tokenJson.getString("access_token");

            // Get user info
            String userInfo = getUserInfo(accessToken);
            JsonReader userReader = Json.createReader(new java.io.StringReader(userInfo));
            JsonObject userJson = userReader.readObject();
            
            String email = userJson.getString("email", "");
            String name = userJson.getString("name", "");
            String picture = userJson.getString("picture", "");
            
            // Store in session and redirect to verify page
            request.getSession().setAttribute("googleEmail", email);
            request.getSession().setAttribute("googleFullName", name);
            request.getSession().setAttribute("googlePicture", picture);
            request.getSession().setAttribute("googleAccessToken", accessToken);
            
            // Redirect to a page that will trigger the AuthController method
            response.sendRedirect(request.getContextPath() + "/pages/user/oauth-register.xhtml");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth processing failed: " + e.getMessage());
        }
    }
    
    private String exchangeCodeForToken(String code, HttpServletRequest request) throws Exception {
        URL url = new URL(GoogleOAuthConfig.TOKEN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        
        // Get dynamic redirect URI based on current request
        String redirectUri = GoogleOAuthConfig.getRedirectUri(request);
        System.out.println("=== GoogleOAuthCallback.exchangeCodeForToken ===");
        System.out.println("Request URL: " + request.getRequestURL());
        System.out.println("Redirect URI: " + redirectUri);
        System.out.println("================================================");
        
        String params = "client_id=" + URLEncoder.encode(GoogleOAuthConfig.CLIENT_ID, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(GoogleOAuthConfig.CLIENT_SECRET, StandardCharsets.UTF_8)
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = params.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        
        return response;
    }
    
    private String getUserInfo(String accessToken) throws Exception {
        URL url = new URL(GoogleOAuthConfig.USERINFO_URL + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        
        return response;
    }
}
