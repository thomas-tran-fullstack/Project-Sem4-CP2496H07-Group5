package controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ejb.EJB;
import entityclass.LiveSession;
import entityclass.LiveProduct;
import entityclass.LiveChat;
import entityclass.LiveSessionViewer;
import entityclass.Customers;
import entityclass.Users;
import sessionbeans.LiveSessionFacade;
import sessionbeans.LiveProductFacade;
import sessionbeans.LiveChatFacade;
import sessionbeans.LiveSessionViewerFacade;

@WebServlet(name = "LiveStreamCustomerServlet", urlPatterns = {"/resources/api/livestream/customer/*"})
public class LiveStreamCustomerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private LiveSessionFacade liveSessionFacade;

    @EJB
    private LiveProductFacade liveProductFacade;

    @EJB
    private LiveChatFacade liveChatFacade;

    @EJB
    private LiveSessionViewerFacade liveSessionViewerFacade;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                pathInfo = "/live-sessions";
            }

            if (pathInfo.equals("/live-sessions")) {
                // GET /api/livestream/customer/live-sessions
                getLiveSessions(response);
            } else if (pathInfo.equals("/my-history")) {
                // GET /api/livestream/customer/my-history
                Customers customer = getCurrentCustomer(request);
                if (customer != null) {
                    getMyViewHistory(customer.getCustomerID(), response);
                } else {
                    sendJsonResponse(response, Json.createObjectBuilder()
                            .add("success", false)
                            .add("error", "Customer login required")
                            .build(), HttpServletResponse.SC_FORBIDDEN);
                }
            } else if (pathInfo.startsWith("/sessions/")) {
                String[] parts = pathInfo.split("/");
                if (parts.length > 2) {
                    int sessionId = Integer.parseInt(parts[2]);
                    if (pathInfo.endsWith("/products")) {
                        // GET /api/livestream/customer/sessions/{id}/products
                        getSessionProducts(sessionId, response);
                    } else if (pathInfo.endsWith("/chat")) {
                        // GET /api/livestream/customer/sessions/{id}/chat
                        getSessionChat(sessionId, response);
                    } else if (pathInfo.endsWith("/stats")) {
                        // GET /api/livestream/customer/sessions/{id}/stats
                        getSessionStats(sessionId, response);
                    } else {
                        // GET /api/livestream/customer/sessions/{id}
                        getSession(sessionId, response);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error in LiveStreamCustomerServlet GET: " + e.getMessage());
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
            Customers customer = getCurrentCustomer(request);
            if (customer == null) {
                sendJsonResponse(response, Json.createObjectBuilder()
                        .add("success", false)
                        .add("error", "Customer login required")
                        .build(), HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (pathInfo != null && pathInfo.contains("/join")) {
                String[] parts = pathInfo.split("/");
                if (parts.length > 2) {
                    int sessionId = Integer.parseInt(parts[2]);
                    // POST /api/livestream/customer/sessions/{id}/join
                    joinSession(sessionId, customer.getCustomerID(), response);
                }
            } else if (pathInfo != null && pathInfo.contains("/send-message")) {
                String[] parts = pathInfo.split("/");
                if (parts.length > 2) {
                    int sessionId = Integer.parseInt(parts[2]);
                    // POST /api/livestream/customer/sessions/{id}/send-message
                    sendMessage(sessionId, customer.getCustomerID(), request, response);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in LiveStreamCustomerServlet POST: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Server error: " + e.getMessage())
                    .build(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void getLiveSessions(HttpServletResponse response) throws Exception {
        List<LiveSession> sessions = liveSessionFacade.findActive();

        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        for (LiveSession session : sessions) {
            jsonArray.add(buildSessionJson(session));
        }

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("data", jsonArray.build())
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

        if (!"ACTIVE".equalsIgnoreCase(session.getStatus())) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "This session is not active")
                    .build(), HttpServletResponse.SC_BAD_REQUEST);
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

    private void getSessionProducts(int sessionId, HttpServletResponse response) throws Exception {
        List<LiveProduct> products = liveProductFacade.findBySessionIDActive(sessionId);

        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        for (LiveProduct product : products) {
            if (product.getIsActive() != null && product.getIsActive()) {
                jsonArray.add(Json.createObjectBuilder()
                        .add("liveProductId", product.getLiveProductID())
                        .add("productId", product.getProductID().getProductID())
                        .add("productName", product.getProductID().getProductName())
                        .add("originalPrice", product.getOriginalPrice().doubleValue())
                        .add("discountedPrice", product.getDiscountedPrice() != null ? product.getDiscountedPrice().doubleValue() : 0)
                        .add("discountPercentage", product.getDiscountPercentage() != null ? product.getDiscountPercentage().doubleValue() : 0)
                        .add("salesCount", product.getSalesCount() != null ? product.getSalesCount() : 0)
                        .add("stock", product.getProductID().getStockQuantity() != null ? product.getProductID().getStockQuantity() : 0)
                        .build());
            }
        }

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("data", jsonArray.build())
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void getSessionChat(int sessionId, HttpServletResponse response) throws Exception {
        List<LiveChat> messages = liveChatFacade.findRecentMessages(sessionId);

        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        for (LiveChat chat : messages) {
            if (!(chat.getIsDeleted() != null && chat.getIsDeleted())) {
                jsonArray.add(Json.createObjectBuilder()
                        .add("chatId", chat.getChatMessageID())
                        .add("userId", chat.getUserID().getUserID())
                        .add("username", chat.getUserID().getUsername())
                        .add("messageText", chat.getMessageText() != null ? chat.getMessageText() : "")
                        .add("messageType", chat.getMessageType() != null ? chat.getMessageType() : "TEXT")
                        .add("createdAt", chat.getCreatedAt().getTime())
                        .build());
            }
        }

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("data", jsonArray.build())
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
                            .add("currentViewers", session.getCurrentViewers() != null ? session.getCurrentViewers() : 0)
                            .add("peakViewers", session.getPeakViewers() != null ? session.getPeakViewers() : 0)
                            .add("totalViewers", session.getTotalViewers() != null ? session.getTotalViewers() : 0)
                            .add("chatMessageCount", session.getChatMessageCount() != null ? session.getChatMessageCount() : 0)
                            .add("status", session.getStatus())
                            .build())
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void getMyViewHistory(int customerId, HttpServletResponse response) throws Exception {
        List<LiveSessionViewer> viewHistory = liveSessionViewerFacade.findByCustomer(customerId);

        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        for (LiveSessionViewer viewer : viewHistory) {
            if (viewer.getSessionID() != null) {
                jsonArray.add(Json.createObjectBuilder()
                        .add("viewerId", viewer.getViewerID())
                        .add("sessionId", viewer.getSessionID().getSessionID())
                        .add("sessionTitle", viewer.getSessionID().getTitle())
                        .add("staffName", viewer.getSessionID().getStaffID().getUsername())
                        .add("joinedAt", viewer.getJoinedAt().getTime())
                        .add("leftAt", viewer.getLeftAt() != null ? viewer.getLeftAt().getTime() : 0)
                        .add("totalDuration", viewer.getTotalDuration() != null ? viewer.getTotalDuration() : 0)
                        .build());
            }
        }

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("data", jsonArray.build())
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void joinSession(int sessionId, int customerId, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!"ACTIVE".equalsIgnoreCase(session.getStatus())) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "This session is not active")
                    .build(), HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Create viewer record
        LiveSessionViewer viewer = new LiveSessionViewer();
        viewer.setSessionID(session);
        // setCustomerID expects Customers object
        Customers customer = new Customers();
        customer.setCustomerID(customerId);
        viewer.setCustomerID(customer);
        viewer.setJoinedAt(new Date());

        liveSessionViewerFacade.create(viewer);

        // Increment current viewer count
        if (session.getCurrentViewers() == null) {
            session.setCurrentViewers(1);
        } else {
            session.setCurrentViewers(session.getCurrentViewers() + 1);
        }

        // Update total viewers
        if (session.getTotalViewers() == null) {
            session.setTotalViewers(1);
        } else {
            session.setTotalViewers(session.getTotalViewers() + 1);
        }

        liveSessionFacade.edit(session);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Successfully joined session")
                    .add("data", Json.createObjectBuilder()
                            .add("viewerId", viewer.getViewerID())
                            .add("hlsPlaylistURL", session.getHlsPlaylistURL())
                            .build())
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    private void sendMessage(int sessionId, int customerId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String messageText = request.getParameter("message");
        String messageType = request.getParameter("messageType");

        if (messageText == null || messageText.trim().isEmpty()) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Message cannot be empty")
                    .build(), HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (messageType == null || messageType.isEmpty()) {
            messageType = "TEXT";
        }

        // Simple spam filter: max 500 chars
        if (messageText.length() > 500) {
            messageText = messageText.substring(0, 500);
        }

        LiveChat chat = new LiveChat();
        chat.setSessionID(session);
        // setUserID expects Users object
        Users user = new Users();
        user.setUserID(customerId);
        chat.setUserID(user);
        chat.setMessageText(messageText);
        chat.setMessageType(messageType);
        chat.setIsDeleted(false);
        chat.setCreatedAt(new Date());

        liveChatFacade.create(chat);

        // Increment message count
        if (session.getChatMessageCount() == null) {
            session.setChatMessageCount(1);
        } else {
            session.setChatMessageCount(session.getChatMessageCount() + 1);
        }
        liveSessionFacade.edit(session);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Message sent successfully")
                    .add("data", Json.createObjectBuilder()
                            .add("chatId", chat.getChatMessageID())
                            .add("createdAt", chat.getCreatedAt().getTime())
                            .build())
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    private JsonObjectBuilder buildSessionJson(LiveSession session) {
        return Json.createObjectBuilder()
                .add("sessionId", session.getSessionID())
                .add("title", session.getTitle() != null ? session.getTitle() : "")
                .add("description", session.getDescription() != null ? session.getDescription() : "")
                .add("status", session.getStatus() != null ? session.getStatus() : "PENDING")
                .add("staffId", session.getStaffID().getUserID())
                .add("staffName", session.getStaffID().getUsername())
                .add("thumbnailUrl", session.getThumbnailURL() != null ? session.getThumbnailURL() : "")
                .add("hlsPlaylistURL", session.getHlsPlaylistURL() != null ? session.getHlsPlaylistURL() : "")
                .add("currentViewers", session.getCurrentViewers() != null ? session.getCurrentViewers() : 0)
                .add("peakViewers", session.getPeakViewers() != null ? session.getPeakViewers() : 0)
                .add("startTime", session.getActualStartTime() != null ? session.getActualStartTime().getTime() : 0)
                .add("scheduledStartTime", session.getScheduledStartTime() != null ? session.getScheduledStartTime().getTime() : 0);
    }

    private Customers getCurrentCustomer(HttpServletRequest request) {
        try {
            HttpSession httpSession = request.getSession(false);
            if (httpSession == null) {
                return null;
            }

            Object customerObj = httpSession.getAttribute("currentCustomer");
            if (customerObj instanceof Customers) {
                return (Customers) customerObj;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void sendJsonResponse(HttpServletResponse response, JsonObject json, int statusCode) throws IOException {
        response.setStatus(statusCode);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
        }
    }
}
