package controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import jakarta.ejb.EJB;
import entityclass.LiveSession;
import entityclass.LiveProduct;
import entityclass.Users;
import sessionbeans.LiveSessionFacade;
import sessionbeans.LiveProductFacade;
import sessionbeans.UsersFacadeLocal;
import sessionbeans.ProductsFacadeLocal;

@WebServlet(name = "LiveStreamAdminServlet", urlPatterns = {"/resources/api/livestream/admin/*"})
@MultipartConfig
public class LiveStreamAdminServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private LiveSessionFacade liveSessionFacade;

    @EJB
    private LiveProductFacade liveProductFacade;

    @EJB
    private UsersFacadeLocal usersFacade;

    @EJB
    private ProductsFacadeLocal productsFacade;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        try {
            // Check admin authorization
            if (!isAdmin(request)) {
                sendJsonResponse(response, Json.createObjectBuilder()
                        .add("success", false)
                        .add("error", "Admin access required")
                        .build(), HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (pathInfo == null || pathInfo.equals("/")) {
                pathInfo = "/sessions";
            }

            if (pathInfo.equals("/sessions")) {
                // GET /api/livestream/admin/sessions - List all sessions
                listSessions(request, response);
            } else if (pathInfo.startsWith("/sessions/")) {
                String[] parts = pathInfo.split("/");
                if (parts.length > 2) {
                    int sessionId = Integer.parseInt(parts[2]);
                    if (pathInfo.endsWith("/details")) {
                        // GET /api/livestream/admin/sessions/{id}/details
                        getSessionDetails(sessionId, response);
                    } else if (pathInfo.endsWith("/stats")) {
                        // GET /api/livestream/admin/sessions/{id}/stats
                        getSessionStats(sessionId, response);
                    } else {
                        // GET /api/livestream/admin/sessions/{id}
                        getSession(sessionId, response);
                    }
                }
            } else if (pathInfo.equals("/dashboard")) {
                // GET /api/livestream/admin/dashboard - Dashboard stats
                getDashboardStats(response);
            }

        } catch (Exception e) {
            System.err.println("Error in LiveStreamAdminServlet GET: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Server error: " + e.getMessage())
                    .build(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        try {
            // Check admin authorization
            if (!isAdmin(request)) {
                sendJsonResponse(response, Json.createObjectBuilder()
                        .add("success", false)
                        .add("error", "Admin access required")
                        .build(), HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (pathInfo == null || pathInfo.equals("/")) {
                pathInfo = "/sessions";
            }

            if (pathInfo.equals("/sessions")) {
                // POST /api/livestream/admin/sessions - Create new session
                createSession(request, response);
            } else if (pathInfo.contains("/activate")) {
                String[] parts = pathInfo.split("/");
                if (parts.length > 2) {
                    int sessionId = Integer.parseInt(parts[2]);
                    // POST /api/livestream/admin/sessions/{id}/activate
                    activateSession(sessionId, response);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in LiveStreamAdminServlet POST: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Server error: " + e.getMessage())
                    .build(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        try {
            // Check admin authorization
            if (!isAdmin(request)) {
                sendJsonResponse(response, Json.createObjectBuilder()
                        .add("success", false)
                        .add("error", "Admin access required")
                        .build(), HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (pathInfo != null && pathInfo.startsWith("/sessions/")) {
                String[] parts = pathInfo.split("/");
                if (parts.length > 2) {
                    int sessionId = Integer.parseInt(parts[2]);
                    // PUT /api/livestream/admin/sessions/{id}
                    updateSession(sessionId, request, response);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in LiveStreamAdminServlet PUT: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Server error: " + e.getMessage())
                    .build(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        try {
            // Check admin authorization
            if (!isAdmin(request)) {
                sendJsonResponse(response, Json.createObjectBuilder()
                        .add("success", false)
                        .add("error", "Admin access required")
                        .build(), HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (pathInfo != null && pathInfo.startsWith("/sessions/")) {
                String[] parts = pathInfo.split("/");
                if (parts.length > 2) {
                    int sessionId = Integer.parseInt(parts[2]);
                    // DELETE /api/livestream/admin/sessions/{id}
                    deleteSession(sessionId, response);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in LiveStreamAdminServlet DELETE: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Server error: " + e.getMessage())
                    .build(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void listSessions(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String statusFilter = request.getParameter("status");
        List<LiveSession> sessions;

        if (statusFilter != null && !statusFilter.isEmpty()) {
            sessions = liveSessionFacade.findByStatus(statusFilter);
        } else {
            sessions = liveSessionFacade.findAll();
        }

        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        for (LiveSession session : sessions) {
            jsonArray.add(buildSessionJson(session));
        }

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("data", jsonArray.build())
                    .add("count", sessions.size())
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void getSession(int sessionId, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("data", buildSessionJson(session))
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void getSessionDetails(int sessionId, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        JsonObjectBuilder sessionJson = buildSessionJson(session);

        // Add products
        List<LiveProduct> products = liveProductFacade.findBySessionIDAll(sessionId);
        JsonArrayBuilder productsArray = Json.createArrayBuilder();
        for (LiveProduct product : products) {
            productsArray.add(Json.createObjectBuilder()
                    .add("liveProductId", product.getLiveProductID())
                    .add("productId", product.getProductID().getProductID())
                    .add("productName", product.getProductID().getProductName())
                    .add("originalPrice", product.getOriginalPrice().doubleValue())
                    .add("discountedPrice", product.getDiscountedPrice() != null ? product.getDiscountedPrice().doubleValue() : 0)
                    .add("discountPercentage", product.getDiscountPercentage() != null ? product.getDiscountPercentage().doubleValue() : 0)
                    .add("isActive", product.getIsActive() != null ? product.getIsActive() : false)
                    .add("salesCount", product.getSalesCount() != null ? product.getSalesCount() : 0)
                    .build());
        }
        sessionJson.add("products", productsArray.build());

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("data", sessionJson.build())
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void getSessionStats(int sessionId, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("data", Json.createObjectBuilder()
                            .add("sessionId", session.getSessionID())
                            .add("currentViewers", session.getCurrentViewers() != null ? session.getCurrentViewers() : 0)
                            .add("peakViewers", session.getPeakViewers() != null ? session.getPeakViewers() : 0)
                            .add("totalViewers", session.getTotalViewers() != null ? session.getTotalViewers() : 0)
                            .add("chatMessageCount", session.getChatMessageCount() != null ? session.getChatMessageCount() : 0)
                            .add("productsCount", session.getLiveProductList() != null ? session.getLiveProductList().size() : 0)
                            .build())
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void getDashboardStats(HttpServletResponse response) throws Exception {
        int activeSessions = liveSessionFacade.countActiveSessions();
        int totalViewers = liveSessionFacade.countViewersAcrossActiveSessions();

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("data", Json.createObjectBuilder()
                            .add("activeSessions", activeSessions)
                            .add("totalViewersActive", totalViewers)
                            .add("timestamp", System.currentTimeMillis())
                            .build())
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void createSession(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            String title = request.getParameter("title");
            String description = request.getParameter("description");
            String staffIdStr = request.getParameter("staffId");
            String scheduledStartStr = request.getParameter("scheduledStartTime");
            String scheduledEndStr = request.getParameter("scheduledEndTime");
            
            String thumbnailUrl = "";
            
            // Try to get uploaded file
            try {
                if (request.getPart("thumbnailFile") != null) {
                    Part filePart = request.getPart("thumbnailFile");
                    if (filePart != null && filePart.getSize() > 0) {
                        // Save file to uploads directory
                        String fileName = System.currentTimeMillis() + "_" + filePart.getSubmittedFileName();
                        String uploadDir = getServletContext().getRealPath("/resources/uploads");
                        java.io.File uploadFolder = new java.io.File(uploadDir);
                        if (!uploadFolder.exists()) {
                            uploadFolder.mkdirs();
                        }
                        String filePath = uploadDir + java.io.File.separator + fileName;
                        filePart.write(filePath);
                        thumbnailUrl = "/EZMart_Supermarket_Management-war/resources/uploads/" + fileName;
                    }
                }
            } catch (Exception e) {
                System.err.println("File upload error: " + e.getMessage());
                // Continue without file
            }

            if (title == null || title.isEmpty() || staffIdStr == null || staffIdStr.isEmpty()) {
                sendJsonResponse(response, Json.createObjectBuilder()
                        .add("success", false)
                        .add("error", "Title and StaffId are required")
                        .build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            int staffId = Integer.parseInt(staffIdStr);
            Users staff = usersFacade.find(staffId);

            if (staff == null || !"STAFF".equalsIgnoreCase(staff.getRole())) {
                sendJsonResponse(response, Json.createObjectBuilder()
                        .add("success", false)
                        .add("error", "Invalid staff user")
                        .build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            LiveSession session = new LiveSession();
            session.setTitle(title);
            session.setDescription(description);
            session.setStaffID(staff);
            session.setStatus("PENDING");
            session.setStreamKey(generateStreamKey());
            session.setCreatedAt(new Date());
            session.setUpdatedAt(new Date());
            session.setCurrentViewers(0);
            session.setPeakViewers(0);
            session.setTotalViewers(0);
            session.setChatMessageCount(0);
            session.setIsRecording(true);
            session.setThumbnailURL(thumbnailUrl);

            if (scheduledStartStr != null && !scheduledStartStr.isEmpty()) {
                session.setScheduledStartTime(new Date(Long.parseLong(scheduledStartStr)));
            }
            if (scheduledEndStr != null && !scheduledEndStr.isEmpty()) {
                session.setScheduledEndTime(new Date(Long.parseLong(scheduledEndStr)));
            }

            liveSessionFacade.create(session);

            try (PrintWriter writer = response.getWriter()) {
                writer.write(Json.createObjectBuilder()
                        .add("success", true)
                        .add("message", "Session created successfully")
                        .add("data", buildSessionJson(session))
                        .build().toString());
            }
            response.setStatus(HttpServletResponse.SC_CREATED);

        } catch (NumberFormatException e) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Invalid number format")
                    .build(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void updateSession(int sessionId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String thumbnailUrl = request.getParameter("thumbnailUrl");

        if (title != null && !title.isEmpty()) {
            session.setTitle(title);
        }
        if (description != null) {
            session.setDescription(description);
        }
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            session.setThumbnailURL(thumbnailUrl);
        }

        session.setUpdatedAt(new Date());
        liveSessionFacade.edit(session);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Session updated successfully")
                    .add("data", buildSessionJson(session))
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void activateSession(int sessionId, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        session.setStatus("ACTIVE");
        session.setActualStartTime(new Date());
        session.setUpdatedAt(new Date());
        session.setRtmpURL("rtmp://localhost:1935/live/" + session.getStreamKey());
        session.setHlsPlaylistURL("http://localhost:8080/hls/" + session.getStreamKey() + "/playlist.m3u8");
        liveSessionFacade.edit(session);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Session activated successfully")
                    .add("data", buildSessionJson(session))
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void deleteSession(int sessionId, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if ("ACTIVE".equalsIgnoreCase(session.getStatus())) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Cannot delete active session. End it first.")
                    .build(), HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        liveSessionFacade.remove(session);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Session deleted successfully")
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private JsonObjectBuilder buildSessionJson(LiveSession session) {
        return Json.createObjectBuilder()
                .add("sessionId", session.getSessionID())
                .add("title", session.getTitle() != null ? session.getTitle() : "")
                .add("description", session.getDescription() != null ? session.getDescription() : "")
                .add("status", session.getStatus() != null ? session.getStatus() : "PENDING")
                .add("staffId", session.getStaffID().getUserID())
                .add("staffName", session.getStaffID().getUsername())
                .add("scheduledStartTime", session.getScheduledStartTime() != null ? session.getScheduledStartTime().getTime() : 0)
                .add("scheduledEndTime", session.getScheduledEndTime() != null ? session.getScheduledEndTime().getTime() : 0)
                .add("actualStartTime", session.getActualStartTime() != null ? session.getActualStartTime().getTime() : 0)
                .add("actualEndTime", session.getActualEndTime() != null ? session.getActualEndTime().getTime() : 0)
                .add("streamKey", session.getStreamKey() != null ? session.getStreamKey() : "")
                .add("rtmpURL", session.getRtmpURL() != null ? session.getRtmpURL() : "")
                .add("hlsPlaylistURL", session.getHlsPlaylistURL() != null ? session.getHlsPlaylistURL() : "")
                .add("thumbnailURL", session.getThumbnailURL() != null ? session.getThumbnailURL() : "")
                .add("currentViewers", session.getCurrentViewers() != null ? session.getCurrentViewers() : 0)
                .add("peakViewers", session.getPeakViewers() != null ? session.getPeakViewers() : 0)
                .add("totalViewers", session.getTotalViewers() != null ? session.getTotalViewers() : 0)
                .add("chatMessageCount", session.getChatMessageCount() != null ? session.getChatMessageCount() : 0)
                .add("isRecording", session.getIsRecording() != null ? session.getIsRecording() : true)
                .add("createdAt", session.getCreatedAt() != null ? session.getCreatedAt().getTime() : 0);
    }

    private String generateStreamKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean isAdmin(HttpServletRequest request) {
        try {
            HttpSession httpSession = request.getSession(false);
            if (httpSession == null) {
                return false;
            }

            Object userObj = httpSession.getAttribute("currentUser");
            if (userObj instanceof Users) {
                Users user = (Users) userObj;
                return "ADMIN".equalsIgnoreCase(user.getRole());
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendJsonResponse(HttpServletResponse response, JsonObject json, int statusCode) throws IOException {
        response.setStatus(statusCode);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
        }
    }
}
