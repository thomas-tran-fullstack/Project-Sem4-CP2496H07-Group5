package controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

@WebServlet(name = "AvatarUploadServlet", urlPatterns = {"/avatar-upload"})
@MultipartConfig(maxFileSize = 5242880) // 5 MB max
public class AvatarUploadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final long MAX_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
        Arrays.asList("jpg", "jpeg", "png", "gif")
    );
    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(
        Arrays.asList("image/jpeg", "image/png", "image/gif")
    );
    private static final String RATE_LIMIT_SESSION_KEY = "avatar_upload_last_time";
    private static final long UPLOAD_COOLDOWN_MS = 60000; // 1 minute

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        
        try {
            // Authenticate user from session
            Integer userId = authenticateUser(request, response);
            System.out.println("AvatarUploadServlet: incoming upload request, sessionId=" + (request.getSession(false)!=null?request.getSession(false).getId():"null"));
            if (request.getSession(false) != null) {
                HttpSession s = request.getSession(false);
                System.out.println("AvatarUploadServlet: session attributes: ");
                java.util.Enumeration<String> names = s.getAttributeNames();
                while (names.hasMoreElements()) {
                    String n = names.nextElement();
                    System.out.println("  " + n + "=" + s.getAttribute(n));
                }
            }
            if (userId == null) {
                jsonBuilder.add("success", false).add("error", "Not authenticated");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Check rate limiting (max 1 upload per minute)
            if (!checkRateLimit(request, userId)) {
                jsonBuilder.add("success", false).add("error", "Rate limit exceeded");
                sendJsonResponse(response, jsonBuilder.build(), 429); // Too Many Requests
                return;
            }

            // Validate file
            Part filePart = request.getPart("avatarFile");
            if (filePart == null || filePart.getSize() == 0) {
                jsonBuilder.add("success", false).add("error", "No file provided");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // File size validation
            if (filePart.getSize() > MAX_BYTES) {
                jsonBuilder.add("success", false).add("error", "File too large");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // MIME type validation
            String contentType = filePart.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
                jsonBuilder.add("success", false).add("error", "Invalid file type");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Determine file extension from MIME type
            String extension = getExtensionFromMimeType(contentType);
            if (extension == null) {
                jsonBuilder.add("success", false).add("error", "Invalid file type");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Validate file name doesn't contain path traversal attempts
            String fileName = filePart.getSubmittedFileName();
            if (fileName != null && (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\"))) {
                jsonBuilder.add("success", false).add("error", "Invalid filename");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Save file
            String uploadsPath = getServletContext().getRealPath("/WEB-INF/uploads/avatars");
            if (uploadsPath == null) {
                // Fallback: use a persistent directory in user's home if getRealPath returns null
                uploadsPath = System.getProperty("user.home") + File.separator + ".ezmart_avatars";
            }
            File uploadsDir = new File(uploadsPath);
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }

            File outFile = new File(uploadsDir, "user_" + userId + "." + extension);
            
            // Validate file to ensure it's actually an image (magic bytes check)
            if (!isValidImageFile(filePart, contentType)) {
                jsonBuilder.add("success", false).add("error", "Invalid image file");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            try (InputStream in = filePart.getInputStream()) {
                System.out.println("AvatarUploadServlet: writing file to " + outFile.getAbsolutePath() + " (size=" + filePart.getSize() + ", contentType=" + contentType + ")");
                Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            // debug log: saved path
            System.out.println("AvatarUploadServlet: saved avatar to " + outFile.getAbsolutePath() + ", exists=" + outFile.exists());

            // Update rate limit timestamp
            HttpSession session = request.getSession();
            session.setAttribute(RATE_LIMIT_SESSION_KEY + userId, System.currentTimeMillis());
            // Persist a simple marker in session so UI can detect immediate update if needed
            session.setAttribute("avatarUpdatedAt", System.currentTimeMillis());

            // Build avatar URL and return success JSON
            long ts = System.currentTimeMillis();
            String avatarUrl = request.getContextPath() + "/avatar?userId=" + userId + "&t=" + ts;
            String absoluteBase = request.getScheme() + "://" + request.getServerName() + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : (":" + request.getServerPort())) + request.getContextPath();
            String avatarUrlAbsolute = absoluteBase + "/avatar?userId=" + userId + "&t=" + ts;
            jsonBuilder.add("success", true)
                       .add("message", "Avatar uploaded successfully")
                       .add("userId", userId)
                       .add("avatarUrl", avatarUrl)
                       .add("avatarUrlAbsolute", avatarUrlAbsolute)
                       .add("timestamp", ts);
            sendJsonResponse(response, jsonBuilder.build(), 200);

        } catch (Exception e) {
            System.err.println("Error in AvatarUploadServlet: " + e.getMessage());
            e.printStackTrace();
            jsonBuilder.add("success", false).add("error", "Upload failed: " + e.getMessage());
            sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void sendJsonResponse(HttpServletResponse response, JsonObject json, int statusCode) throws IOException {
        response.setStatus(statusCode);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
        }
    }

    /**
     * Authenticate user from session and return user ID
     */
    private Integer authenticateUser(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("AvatarUploadServlet: No session found");
            return null;
        }

        // Try to get userId from multiple possible session attributes
        Integer userId = null;
        
        // Method 1: Direct currentUserId attribute (set by auth controller)
        Object idObj = session.getAttribute("currentUserId");
        if (idObj != null) {
            if (idObj instanceof Integer) userId = (Integer) idObj;
            else if (idObj instanceof String) {
                try {
                    userId = Integer.valueOf((String) idObj);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        
        // Method 2: Check for JSF sessionMap stored currentCustomer
        // JSF stores the customer object which has a customerID
        if (userId == null) {
            Object customerObj = session.getAttribute("currentCustomer");
            if (customerObj != null) {
                try {
                    // Use reflection to get customerID if it's a Customers entity
                    java.lang.reflect.Method getIdMethod = customerObj.getClass().getMethod("getCustomerID");
                    Object id = getIdMethod.invoke(customerObj);
                    if (id instanceof Integer) {
                        userId = (Integer) id;
                    }
                } catch (Exception e) {
                    System.out.println("AvatarUploadServlet: Failed to extract customer ID via reflection: " + e.getMessage());
                }
            }
        }

        if (userId != null) {
            System.out.println("AvatarUploadServlet: User authenticated, userId=" + userId);
        } else {
            System.out.println("AvatarUploadServlet: User not authenticated");
            // Debug: print all session attributes
            java.util.Enumeration<String> names = session.getAttributeNames();
            System.out.println("AvatarUploadServlet: Available session attributes:");
            while (names.hasMoreElements()) {
                String n = names.nextElement();
                System.out.println("  " + n + "=" + session.getAttribute(n).getClass().getSimpleName());
            }
        }

        return userId;
    }

    /**
     * Check if user has exceeded rate limit (1 upload per 60 seconds)
     */
    private boolean checkRateLimit(HttpServletRequest request, Integer userId) {
        HttpSession session = request.getSession();
        String lastUploadKey = RATE_LIMIT_SESSION_KEY + userId;
        Object lastUploadObj = session.getAttribute(lastUploadKey);

        if (lastUploadObj == null) {
            return true; // First upload, allowed
        }

        try {
            long lastUploadTime = (Long) lastUploadObj;
            long elapsed = System.currentTimeMillis() - lastUploadTime;
            return elapsed >= UPLOAD_COOLDOWN_MS;
        } catch (ClassCastException e) {
            return true; // Invalid format, allow upload
        }
    }

    /**
     * Get file extension from MIME type
     */
    private String getExtensionFromMimeType(String contentType) {
        if (contentType == null) return null;

        if (contentType.contains("png")) return "png";
        if (contentType.contains("gif")) return "gif";
        if (contentType.contains("jpeg") || contentType.contains("jpg")) return "jpg";

        return null;
    }

    /**
     * Validate file magic bytes to ensure it's a real image file
     */
    private boolean isValidImageFile(Part filePart, String contentType) throws IOException {
        byte[] magic = new byte[8];
        try (InputStream in = filePart.getInputStream()) {
            int read = in.read(magic);
            if (read < 2) return false;
        }

        // Check PNG signature (89 50 4E 47)
        if (contentType.contains("png")) {
            return magic[0] == (byte) 0x89 && magic[1] == (byte) 0x50;
        }

        // Check JPEG signature (FF D8 FF)
        if (contentType.contains("jpeg") || contentType.contains("jpg")) {
            return magic[0] == (byte) 0xFF && magic[1] == (byte) 0xD8;
        }

        // Check GIF signature (47 49 46)
        if (contentType.contains("gif")) {
            return magic[0] == (byte) 0x47 && magic[1] == (byte) 0x49;
        }

        return false;
    }}