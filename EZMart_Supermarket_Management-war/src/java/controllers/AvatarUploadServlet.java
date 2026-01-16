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
import jakarta.ejb.EJB;
import entityclass.Customers;
import sessionbeans.CustomersFacadeLocal;

@WebServlet(name = "AvatarUploadServlet", urlPatterns = {"/avatar-upload"})
@MultipartConfig(maxFileSize = 5242880) // 5 MB max
public class AvatarUploadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    @EJB
    private CustomersFacadeLocal customersFacade;
    
    private static final long MAX_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
        Arrays.asList("jpg", "jpeg", "png", "gif")
    );
    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(
        Arrays.asList("image/jpeg", "image/png", "image/gif")
    );
    private static final String RATE_LIMIT_SESSION_KEY = "avatar_upload_last_time";
    private static final long UPLOAD_COOLDOWN_MS = 10000; // 10 seconds

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
            // Temporarily disabled for testing
            /*
            if (!checkRateLimit(request, userId)) {
                jsonBuilder.add("success", false).add("error", "Rate limit exceeded");
                sendJsonResponse(response, jsonBuilder.build(), 429); // Too Many Requests
                return;
            }
            */

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
            
            // Save avatarUrl to database
            try {
                Customers customer = customersFacade.find(userId);
                if (customer != null) {
                    customer.setAvatarUrl(avatarUrl);
                    customersFacade.edit(customer);
                    System.out.println("AvatarUploadServlet: Updated customer " + userId + " with avatarUrl: " + avatarUrl);
                } else {
                    System.out.println("AvatarUploadServlet: Customer " + userId + " not found in database");
                }
            } catch (Exception e) {
                System.err.println("AvatarUploadServlet: Error saving avatar URL to database: " + e.getMessage());
                e.printStackTrace();
                // Don't fail the upload, just log the error
            }
            
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
     * Authenticate user from session and return customer ID
     */
    private Integer authenticateUser(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("AvatarUploadServlet: No session found");
            return null;
        }

        // Try to get userId from multiple possible session attributes
        Integer customerId = null;
        
        // Method 1: Direct currentCustomerId attribute (set by auth controller)
        Object idObj = session.getAttribute("currentCustomerId");
        if (idObj != null) {
            if (idObj instanceof Integer) customerId = (Integer) idObj;
            else if (idObj instanceof String) {
                try {
                    customerId = Integer.valueOf((String) idObj);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            System.out.println("AvatarUploadServlet: Got customerId from currentCustomerId: " + customerId);
        }
        
        // Method 2: Check for JSF sessionMap stored currentCustomer
        // JSF stores the customer object which has a customerID
        if (customerId == null) {
            Object customerObj = session.getAttribute("currentCustomer");
            if (customerObj != null) {
                try {
                    // Use reflection to get customerID if it's a Customers entity
                    java.lang.reflect.Method getIdMethod = customerObj.getClass().getMethod("getCustomerID");
                    Object id = getIdMethod.invoke(customerObj);
                    if (id instanceof Integer) {
                        customerId = (Integer) id;
                    }
                    System.out.println("AvatarUploadServlet: Got customerId from currentCustomer: " + customerId);
                } catch (Exception e) {
                    System.out.println("AvatarUploadServlet: Failed to extract customer ID via reflection: " + e.getMessage());
                }
            }
        }
        
        // Method 3: Try from currentUser
        if (customerId == null) {
            Object userObj = session.getAttribute("currentUser");
            if (userObj != null) {
                try {
                    java.lang.reflect.Method getIdMethod = userObj.getClass().getMethod("getUserID");
                    Object id = getIdMethod.invoke(userObj);
                    if (id instanceof Integer) {
                        // Try to get customer for that user
                        try {
                            java.lang.reflect.Method getCustomersMethod = userObj.getClass().getMethod("getCustomersList");
                            Object customersList = getCustomersMethod.invoke(userObj);
                            if (customersList instanceof java.util.List) {
                                java.util.List<?> list = (java.util.List<?>) customersList;
                                if (!list.isEmpty()) {
                                    Object firstCustomer = list.get(0);
                                    java.lang.reflect.Method getCustIdMethod = firstCustomer.getClass().getMethod("getCustomerID");
                                    Object custId = getCustIdMethod.invoke(firstCustomer);
                                    if (custId instanceof Integer) {
                                        customerId = (Integer) custId;
                                    }
                                }
                            }
                        } catch (Exception e2) {
                            // ignore, use userId from user object
                        }
                    }
                    System.out.println("AvatarUploadServlet: Got customerId from currentUser: " + customerId);
                } catch (Exception e) {
                    System.out.println("AvatarUploadServlet: Failed to extract customerId from currentUser: " + e.getMessage());
                }
            }
        }

        if (customerId != null) {
            System.out.println("AvatarUploadServlet: User authenticated, customerId=" + customerId);
        } else {
            System.out.println("AvatarUploadServlet: User not authenticated");
            // Debug: print all session attributes
            java.util.Enumeration<String> names = session.getAttributeNames();
            System.out.println("AvatarUploadServlet: Available session attributes:");
            while (names.hasMoreElements()) {
                String n = names.nextElement();
                try {
                    System.out.println("  " + n + "=" + session.getAttribute(n).getClass().getSimpleName());
                } catch (Exception e) {
                    System.out.println("  " + n + "=(error reading)");
                }
            }
        }

        return customerId;
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