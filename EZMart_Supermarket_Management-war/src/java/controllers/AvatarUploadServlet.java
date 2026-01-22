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

import jakarta.ejb.EJB;
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

import entityclass.Users;
import sessionbeans.UsersFacadeLocal;

@WebServlet(name = "AvatarUploadServlet", urlPatterns = {"/avatar-upload"})
@MultipartConfig(maxFileSize = 5242880) // 5 MB max
public class AvatarUploadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private UsersFacadeLocal usersFacade;

    private static final long MAX_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
        Arrays.asList("jpg", "jpeg", "png", "gif")
    );
    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(
        Arrays.asList("image/jpeg", "image/png", "image/gif")
    );
    private static final String RATE_LIMIT_SESSION_KEY = "avatar_upload_last_time_";
    private static final long UPLOAD_COOLDOWN_MS = 10000; // 10 seconds

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();

        try {
            // Authenticate user from session (MUST be USER ID, not customerId)
            Integer userId = authenticateUser(request);
            System.out.println("AvatarUploadServlet: incoming upload request, sessionId="
                    + (request.getSession(false) != null ? request.getSession(false).getId() : "null"));

            if (request.getSession(false) != null) {
                HttpSession s = request.getSession(false);
                System.out.println("AvatarUploadServlet: session attributes:");
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

            // Rate limit (optional)
            // if (!checkRateLimit(request, userId)) {
            //     jsonBuilder.add("success", false).add("error", "Rate limit exceeded");
            //     sendJsonResponse(response, jsonBuilder.build(), 429);
            //     return;
            // }

            // Validate file
            Part filePart = request.getPart("avatarFile");
            if (filePart == null || filePart.getSize() == 0) {
                jsonBuilder.add("success", false).add("error", "No file provided");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (filePart.getSize() > MAX_BYTES) {
                jsonBuilder.add("success", false).add("error", "File too large");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String contentType = filePart.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
                jsonBuilder.add("success", false).add("error", "Invalid file type");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String extension = getExtensionFromMimeType(contentType);
            if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
                jsonBuilder.add("success", false).add("error", "Invalid file type");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Prevent path traversal
            String fileName = filePart.getSubmittedFileName();
            if (fileName != null && (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\"))) {
                jsonBuilder.add("success", false).add("error", "Invalid filename");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Validate magic bytes
            if (!isValidImageFile(filePart, contentType)) {
                jsonBuilder.add("success", false).add("error", "Invalid image file");
                sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Save file to persistent folder OUTSIDE WAR
            String uploadsPath = System.getProperty("user.home") + File.separator + ".ezmart_avatars";
            File uploadsDir = new File(uploadsPath);
            if (!uploadsDir.exists() && !uploadsDir.mkdirs()) {
                // Fallback only if cannot create persistent folder
                uploadsPath = getServletContext().getRealPath("/WEB-INF/uploads/avatars");
                uploadsDir = new File(uploadsPath != null ? uploadsPath : "");
                if (!uploadsDir.exists()) {
                    uploadsDir.mkdirs();
                }
            }

            File outFile = new File(uploadsDir, "user_" + userId + "." + extension);

            try (InputStream in = filePart.getInputStream()) {
                System.out.println("AvatarUploadServlet: writing file to " + outFile.getAbsolutePath()
                        + " (size=" + filePart.getSize() + ", contentType=" + contentType + ")");
                Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("AvatarUploadServlet: saved avatar to " + outFile.getAbsolutePath()
                    + ", exists=" + outFile.exists());

            // Update rate limit timestamp + marker
            HttpSession session = request.getSession();
            session.setAttribute(RATE_LIMIT_SESSION_KEY + userId, System.currentTimeMillis());
            session.setAttribute("avatarUpdatedAt", System.currentTimeMillis());

            // IMPORTANT:
            // - DB should store stable URL WITHOUT &t=...
            // - Response can return cache-busted URL so UI updates immediately
            String avatarUrlToStore = request.getContextPath() + "/avatar?userId=" + userId;
            String avatarUrlForClient = avatarUrlToStore + "&t=" + System.currentTimeMillis();

            // Absolute (optional debug)
            String absoluteBase = request.getScheme() + "://" + request.getServerName()
                    + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : (":" + request.getServerPort()))
                    + request.getContextPath();
            String avatarUrlAbsoluteForClient = absoluteBase + "/avatar?userId=" + userId + "&t=" + System.currentTimeMillis();

            // Save to Users table
            try {
                Users user = usersFacade.find(userId);
                if (user != null) {
                    user.setAvatarUrl(avatarUrlToStore); // store stable link
                    usersFacade.edit(user);
                    System.out.println("AvatarUploadServlet: Updated user " + userId + " with avatarUrl: " + avatarUrlToStore);
                    
                    // Update session with new avatar URL so profile page immediately reflects change
                    session.setAttribute("currentUserProfileImageUrl", avatarUrlForClient);
                    System.out.println("AvatarUploadServlet: Updated session currentUserProfileImageUrl: " + avatarUrlForClient);
                } else {
                    System.out.println("AvatarUploadServlet: User " + userId + " not found in database");
                }
            } catch (Exception e) {
                System.err.println("AvatarUploadServlet: Error saving avatar URL to database: " + e.getMessage());
                e.printStackTrace();
                // do not fail upload
            }

            jsonBuilder.add("success", true)
                    .add("message", "Avatar uploaded successfully")
                    .add("userId", userId)
                    .add("avatarUrl", avatarUrlForClient)
                    .add("avatarUrlAbsolute", avatarUrlAbsoluteForClient)
                    .add("timestamp", System.currentTimeMillis());

            sendJsonResponse(response, jsonBuilder.build(), HttpServletResponse.SC_OK);

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
     * Authenticate user from session and return USER ID (not customerId).
     */
    private Integer authenticateUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("AvatarUploadServlet: No session found");
            return null;
        }

        // Method 1: Direct currentUserId attribute (recommended)
        Object idObj = session.getAttribute("currentUserId");
        Integer userId = tryParseInt(idObj);
        if (userId != null) {
            System.out.println("AvatarUploadServlet: Got userId from currentUserId: " + userId);
            return userId;
        }

        // Method 2: From currentUser object via reflection getUserID()
        Object userObj = session.getAttribute("currentUser");
        if (userObj != null) {
            try {
                java.lang.reflect.Method getIdMethod = userObj.getClass().getMethod("getUserID");
                Object id = getIdMethod.invoke(userObj);
                userId = tryParseInt(id);
                if (userId != null) {
                    System.out.println("AvatarUploadServlet: Got userId from currentUser: " + userId);
                    return userId;
                }
            } catch (Exception e) {
                System.out.println("AvatarUploadServlet: Failed to extract userId from currentUser: " + e.getMessage());
            }
        }

        System.out.println("AvatarUploadServlet: User not authenticated");
        java.util.Enumeration<String> names = session.getAttributeNames();
        System.out.println("AvatarUploadServlet: Available session attributes:");
        while (names.hasMoreElements()) {
            String n = names.nextElement();
            Object v = session.getAttribute(n);
            System.out.println("  " + n + "=" + (v == null ? "null" : v.getClass().getSimpleName()));
        }

        return null;
    }

    private Integer tryParseInt(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Integer) return (Integer) obj;
            if (obj instanceof Long) return ((Long) obj).intValue();
            if (obj instanceof String) return Integer.valueOf((String) obj);
        } catch (Exception ignore) {}
        return null;
    }

    /**
     * Check if user has exceeded rate limit (1 upload per cooldown)
     */
    private boolean checkRateLimit(HttpServletRequest request, Integer userId) {
        HttpSession session = request.getSession();
        String lastUploadKey = RATE_LIMIT_SESSION_KEY + userId;
        Object lastUploadObj = session.getAttribute(lastUploadKey);

        if (lastUploadObj == null) return true;

        try {
            long lastUploadTime = (Long) lastUploadObj;
            long elapsed = System.currentTimeMillis() - lastUploadTime;
            return elapsed >= UPLOAD_COOLDOWN_MS;
        } catch (Exception e) {
            return true; // invalid format, allow
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

        // PNG signature (89 50 4E 47)
        if (contentType.contains("png")) {
            return magic[0] == (byte) 0x89 && magic[1] == (byte) 0x50;
        }

        // JPEG signature (FF D8)
        if (contentType.contains("jpeg") || contentType.contains("jpg")) {
            return magic[0] == (byte) 0xFF && magic[1] == (byte) 0xD8;
        }

        // GIF signature (47 49)
        if (contentType.contains("gif")) {
            return magic[0] == (byte) 0x47 && magic[1] == (byte) 0x49;
        }

        return false;
    }
}
