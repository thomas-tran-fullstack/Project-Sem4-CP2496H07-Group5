package controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
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
import entityclass.Users;
import entityclass.Products;
import sessionbeans.LiveSessionFacade;
import sessionbeans.LiveProductFacade;
import sessionbeans.UsersFacadeLocal;
import sessionbeans.ProductsFacadeLocal;

@WebServlet(name = "LiveStreamStaffServlet", urlPatterns = {"/resources/api/livestream/staff/*"})
public class LiveStreamStaffServlet extends HttpServlet {

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
            Users currentStaff = getCurrentStaff(request);
            if (currentStaff == null) {
                sendJsonResponse(response, Json.createObjectBuilder()
                        .add("success", false)
                        .add("error", "Staff access required")
                        .build(), HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (pathInfo == null || pathInfo.equals("/")) {
                pathInfo = "/my-sessions";
            }

            if (pathInfo.equals("/my-sessions")) {
                // GET /api/livestream/staff/my-sessions
                getMyAvailableSessions(currentStaff.getUserID(), response);
            } else if (pathInfo.equals("/active-sessions")) {
                // GET /api/livestream/staff/active-sessions
                getActiveSessions(response);
            } else if (pathInfo.startsWith("/sessions/")) {
                String[] parts = pathInfo.split("/");
                if (parts.length > 2) {
                    int sessionId = Integer.parseInt(parts[2]);
                    if (pathInfo.endsWith("/stream-key")) {
                        // GET /api/livestream/staff/sessions/{id}/stream-key
                        getStreamKey(sessionId, response);
                    } else {
                        // GET /api/livestream/staff/sessions/{id}
                        getSession(sessionId, response);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error in LiveStreamStaffServlet GET: " + e.getMessage());
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
            Users currentStaff = getCurrentStaff(request);
            if (currentStaff == null) {
                sendJsonResponse(response, Json.createObjectBuilder()
                        .add("success", false)
                        .add("error", "Staff access required")
                        .build(), HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (pathInfo != null && pathInfo.contains("/go-live")) {
                String[] parts = pathInfo.split("/");
                if (parts.length > 2) {
                    int sessionId = Integer.parseInt(parts[2]);
                    // POST /api/livestream/staff/sessions/{id}/go-live
                    goLive(sessionId, currentStaff.getUserID(), response);
                }
            } else if (pathInfo != null && pathInfo.contains("/add-product")) {
                String[] parts = pathInfo.split("/");
                if (parts.length > 2) {
                    int sessionId = Integer.parseInt(parts[2]);
                    // POST /api/livestream/staff/sessions/{id}/add-product
                    addProduct(sessionId, request, response);
                }
            } else if (pathInfo != null && pathInfo.contains("/end-live")) {
                String[] parts = pathInfo.split("/");
                if (parts.length > 2) {
                    int sessionId = Integer.parseInt(parts[2]);
                    // POST /api/livestream/staff/sessions/{id}/end-live
                    endLive(sessionId, response);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in LiveStreamStaffServlet POST: " + e.getMessage());
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
            Users currentStaff = getCurrentStaff(request);
            if (currentStaff == null) {
                sendJsonResponse(response, Json.createObjectBuilder()
                        .add("success", false)
                        .add("error", "Staff access required")
                        .build(), HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (pathInfo != null && pathInfo.contains("/products/")) {
                // PUT /api/livestream/staff/sessions/{id}/products/{productId}/price
                String[] parts = pathInfo.split("/");
                if (parts.length > 4) {
                    int sessionId = Integer.parseInt(parts[2]);
                    int liveProductId = Integer.parseInt(parts[4]);
                    updateProductPrice(sessionId, liveProductId, request, response);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in LiveStreamStaffServlet PUT: " + e.getMessage());
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
            Users currentStaff = getCurrentStaff(request);
            if (currentStaff == null) {
                sendJsonResponse(response, Json.createObjectBuilder()
                        .add("success", false)
                        .add("error", "Staff access required")
                        .build(), HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (pathInfo != null && pathInfo.contains("/products/")) {
                // DELETE /api/livestream/staff/sessions/{id}/products/{productId}
                String[] parts = pathInfo.split("/");
                if (parts.length > 4) {
                    int sessionId = Integer.parseInt(parts[2]);
                    int liveProductId = Integer.parseInt(parts[4]);
                    removeProduct(sessionId, liveProductId, response);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in LiveStreamStaffServlet DELETE: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Server error: " + e.getMessage())
                    .build(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void getMyAvailableSessions(int staffId, HttpServletResponse response) throws Exception {
        List<LiveSession> sessions = liveSessionFacade.findByStaff(staffId);

        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        for (LiveSession session : sessions) {
            if ("PENDING".equalsIgnoreCase(session.getStatus()) || "SCHEDULED".equalsIgnoreCase(session.getStatus())) {
                jsonArray.add(buildSessionJson(session));
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

    private void getActiveSessions(HttpServletResponse response) throws Exception {
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
                    .add("isActive", product.getIsActive() != null ? product.getIsActive() : false)
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

    private void getStreamKey(int sessionId, HttpServletResponse response) throws Exception {
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
                            .add("streamKey", session.getStreamKey())
                            .add("rtmpURL", "rtmp://localhost:1935/live/")
                            .add("serverURL", "rtmp://your-domain.com:1935/live/")
                            .build())
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void goLive(int sessionId, int staffId, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!session.getStaffID().getUserID().equals(staffId)) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "You can only broadcast your own sessions")
                    .build(), HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        session.setStatus("ACTIVE");
        session.setActualStartTime(new Date());
        session.setRtmpURL("rtmp://localhost:1935/live/" + session.getStreamKey());
        session.setHlsPlaylistURL("http://localhost:8080/hls/" + session.getStreamKey() + "/playlist.m3u8");
        liveSessionFacade.edit(session);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Broadcast started successfully")
                    .add("data", buildSessionJson(session))
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void endLive(int sessionId, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        session.setStatus("ENDED");
        session.setActualEndTime(new Date());
        liveSessionFacade.edit(session);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Broadcast ended successfully")
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void addProduct(int sessionId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String productIdStr = request.getParameter("productId");
        String discountedPriceStr = request.getParameter("discountedPrice");

        if (productIdStr == null || productIdStr.isEmpty()) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "ProductId is required")
                    .build(), HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int productId = Integer.parseInt(productIdStr);
        Products product = productsFacade.find(productId);

        if (product == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Product not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Check if product already exists in session
        LiveProduct existing = liveProductFacade.findActiveBySessionAndProduct(sessionId, productId);
        if (existing != null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Product already added to this session")
                    .build(), HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        LiveProduct liveProduct = new LiveProduct();
        liveProduct.setSessionID(session);
        liveProduct.setProductID(product);
        liveProduct.setOriginalPrice(product.getUnitPrice());
        liveProduct.setIsActive(true);
        liveProduct.setAddedAt(new Date());
        liveProduct.setSalesCount(0);

        if (discountedPriceStr != null && !discountedPriceStr.isEmpty()) {
            BigDecimal discountedPrice = new BigDecimal(discountedPriceStr);
            liveProduct.setDiscountedPrice(discountedPrice);
            
            BigDecimal discount = product.getUnitPrice().subtract(discountedPrice);
            BigDecimal percentage = discount.multiply(new BigDecimal(100)).divide(product.getUnitPrice(), 2, BigDecimal.ROUND_HALF_UP);
            liveProduct.setDiscountPercentage(percentage);
        } else {
            liveProduct.setDiscountedPrice(product.getUnitPrice());
        }

        liveProductFacade.create(liveProduct);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Product added to stream successfully")
                    .add("data", Json.createObjectBuilder()
                            .add("liveProductId", liveProduct.getLiveProductID())
                            .add("productId", product.getProductID())
                            .add("productName", product.getProductName())
                            .add("originalPrice", product.getUnitPrice().doubleValue())
                            .add("discountedPrice", liveProduct.getDiscountedPrice().doubleValue())
                            .build())
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    private void updateProductPrice(int sessionId, int liveProductId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        LiveProduct liveProduct = liveProductFacade.find(liveProductId);

        if (liveProduct == null || !liveProduct.getSessionID().getSessionID().equals(sessionId)) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Live product not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String newPriceStr = request.getParameter("newPrice");

        if (newPriceStr == null || newPriceStr.isEmpty()) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "New price is required")
                    .build(), HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        BigDecimal newPrice = new BigDecimal(newPriceStr);
        BigDecimal oldPrice = liveProduct.getDiscountedPrice();

        liveProduct.setDiscountedPrice(newPrice);
        
        // Calculate discount percentage
        BigDecimal discount = liveProduct.getOriginalPrice().subtract(newPrice);
        BigDecimal percentage = discount.multiply(new BigDecimal(100))
                .divide(liveProduct.getOriginalPrice(), 2, BigDecimal.ROUND_HALF_UP);
        liveProduct.setDiscountPercentage(percentage);

        liveProductFacade.edit(liveProduct);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Product price updated successfully")
                    .add("data", Json.createObjectBuilder()
                            .add("liveProductId", liveProduct.getLiveProductID())
                            .add("oldPrice", oldPrice.doubleValue())
                            .add("newPrice", newPrice.doubleValue())
                            .add("discountPercentage", liveProduct.getDiscountPercentage().doubleValue())
                            .build())
                    .build().toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void removeProduct(int sessionId, int liveProductId, HttpServletResponse response) throws Exception {
        LiveSession session = liveSessionFacade.find(sessionId);

        if (session == null) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Session not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        LiveProduct liveProduct = liveProductFacade.find(liveProductId);

        if (liveProduct == null || !liveProduct.getSessionID().getSessionID().equals(sessionId)) {
            sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Live product not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        liveProduct.setIsActive(false);
        liveProduct.setRemovedAt(new Date());
        liveProductFacade.edit(liveProduct);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Product removed from stream")
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
                .add("streamKey", session.getStreamKey() != null ? session.getStreamKey() : "")
                .add("hlsPlaylistURL", session.getHlsPlaylistURL() != null ? session.getHlsPlaylistURL() : "")
                .add("currentViewers", session.getCurrentViewers() != null ? session.getCurrentViewers() : 0);
    }

    private Users getCurrentStaff(HttpServletRequest request) {
        try {
            HttpSession httpSession = request.getSession(false);
            if (httpSession == null) {
                return null;
            }

            Object userObj = httpSession.getAttribute("currentUser");
            if (userObj instanceof Users) {
                Users user = (Users) userObj;
                if ("STAFF".equalsIgnoreCase(user.getRole())) {
                    return user;
                }
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
