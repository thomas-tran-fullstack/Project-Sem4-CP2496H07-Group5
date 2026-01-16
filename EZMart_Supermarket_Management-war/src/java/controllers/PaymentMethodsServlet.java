package controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import jakarta.json.Json;
import jakarta.json.JsonArray;
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
import entityclass.CreditCards;
import entityclass.Customers;
import sessionbeans.CreditCardsFacadeLocal;
import sessionbeans.CustomersFacadeLocal;

@WebServlet(name = "PaymentMethodsServlet", urlPatterns = {"/resources/api/payment-methods"})
@jakarta.servlet.annotation.MultipartConfig
public class PaymentMethodsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private CreditCardsFacadeLocal creditCardsFacade;
    
    @EJB
    private CustomersFacadeLocal customersFacade;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // Authenticate user from session
            Integer customerId = authenticateUser(request);
            
            if (customerId == null) {
                sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Not authenticated")
                    .build(), HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Fetch payment methods for current user
            List<CreditCards> paymentMethods = creditCardsFacade.findByCustomer(customerId);
            
            if (paymentMethods == null) {
                paymentMethods = new ArrayList<>();
            }

            // Convert to JSON array
            JsonArrayBuilder jsonArray = Json.createArrayBuilder();
            
            for (CreditCards card : paymentMethods) {
                JsonObjectBuilder cardJson = Json.createObjectBuilder()
                    .add("id", card.getCardID())
                    .add("type", card.getCardType() != null ? card.getCardType() : "")
                    .add("cardNumber", maskCardNumber(card.getCardNumber()))
                    .add("fullCardNumber", card.getCardNumber() != null ? card.getCardNumber() : "")
                    .add("expiry", card.getCardExpiry() != null ? card.getCardExpiry() : "")
                    .add("isDefault", card.getIsDefault() != null ? card.getIsDefault() : false);
                
                jsonArray.add(cardJson.build());
            }

            // Send successful response with payment methods array
            response.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(jsonArray.build().toString());
            }

        } catch (Exception e) {
            System.err.println("Error in PaymentMethodsServlet: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(response, Json.createObjectBuilder()
                .add("success", false)
                .add("error", "Server error: " + e.getMessage())
                .build(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // Authenticate user from session
            Integer customerId = authenticateUser(request);
            
            if (customerId == null) {
                sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Not authenticated")
                    .build(), HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Get parameters from request
            String cardType = request.getParameter("cardType");
            String cardNumber = request.getParameter("cardNumber");
            String cardExpiry = request.getParameter("cardExpiry");
            String cardholderName = request.getParameter("cardholderName");
            String isDefaultStr = request.getParameter("isDefault");
            boolean isDefault = "true".equalsIgnoreCase(isDefaultStr);

            // Debug logging
            System.out.println("PaymentMethodsServlet.doPost: Received parameters:");
            System.out.println("  cardType=" + cardType);
            System.out.println("  cardNumber=" + (cardNumber != null ? "***" + cardNumber.substring(Math.max(0, cardNumber.length()-4)) : "null"));
            System.out.println("  cardExpiry=" + cardExpiry);
            System.out.println("  cardholderName=" + cardholderName);
            System.out.println("  isDefault=" + isDefaultStr);
            System.out.println("  customerId=" + customerId);

            // Validate required fields
            if (cardType == null || cardType.isEmpty()) {
                System.out.println("PaymentMethodsServlet.doPost: Validation failed - cardType is null or empty");
                sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Card type is required")
                    .build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Get customer
            Customers customer = customersFacade.find(customerId);
            if (customer == null) {
                sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Customer not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Create new credit card
            CreditCards newCard = new CreditCards();
            newCard.setCardType(cardType);
            newCard.setCardNumber(cardNumber);
            newCard.setCardExpiry(cardExpiry);
            newCard.setIsDefault(isDefault);
            newCard.setCustomerID(customer);

            // If this is default, unset other defaults
            if (isDefault) {
                List<CreditCards> existingCards = creditCardsFacade.findByCustomer(customerId);
                for (CreditCards card : existingCards) {
                    if (card.getIsDefault() != null && card.getIsDefault()) {
                        card.setIsDefault(false);
                        creditCardsFacade.edit(card);
                    }
                }
            }

            // Save to database
            creditCardsFacade.create(newCard);

            System.out.println("PaymentMethodsServlet: Created payment method for customer " + customerId);

            sendJsonResponse(response, Json.createObjectBuilder()
                .add("success", true)
                .add("message", "Payment method saved successfully")
                .add("id", newCard.getCardID())
                .build(), HttpServletResponse.SC_OK);

        } catch (Exception e) {
            System.err.println("Error in PaymentMethodsServlet: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(response, Json.createObjectBuilder()
                .add("success", false)
                .add("error", "Server error: " + e.getMessage())
                .build(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // Authenticate user from session
            Integer customerId = authenticateUser(request);
            
            if (customerId == null) {
                sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Not authenticated")
                    .build(), HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Get card ID from request parameter
            String cardIdStr = request.getParameter("cardId");
            System.out.println("PaymentMethodsServlet.doPut: cardIdStr=" + cardIdStr);
            
            if (cardIdStr == null || cardIdStr.isEmpty()) {
                System.out.println("PaymentMethodsServlet.doPut: cardId is missing!");
                sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Card ID is required")
                    .build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            int cardId = Integer.parseInt(cardIdStr);
            CreditCards card = creditCardsFacade.find(cardId);

            if (card == null) {
                sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Card not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Verify card belongs to current user
            if (!card.getCustomerID().getCustomerID().equals(customerId)) {
                sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Unauthorized")
                    .build(), HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // Update card fields
            String cardType = request.getParameter("cardType");
            String cardNumber = request.getParameter("cardNumber");
            String cardExpiry = request.getParameter("cardExpiry");
            String isDefaultStr = request.getParameter("isDefault");
            boolean isDefault = "true".equalsIgnoreCase(isDefaultStr);

            if (cardType != null && !cardType.isEmpty()) {
                card.setCardType(cardType);
            }
            if (cardNumber != null && !cardNumber.isEmpty()) {
                card.setCardNumber(cardNumber);
            }
            if (cardExpiry != null && !cardExpiry.isEmpty()) {
                card.setCardExpiry(cardExpiry);
            }
            
            // Handle default flag
            if (isDefault && !card.getIsDefault()) {
                // Unset other defaults
                List<CreditCards> existingCards = creditCardsFacade.findByCustomer(customerId);
                for (CreditCards c : existingCards) {
                    if (c.getIsDefault() != null && c.getIsDefault() && !c.getCardID().equals(cardId)) {
                        c.setIsDefault(false);
                        creditCardsFacade.edit(c);
                    }
                }
            }
            card.setIsDefault(isDefault);

            creditCardsFacade.edit(card);

            System.out.println("PaymentMethodsServlet: Updated payment method " + cardId);

            sendJsonResponse(response, Json.createObjectBuilder()
                .add("success", true)
                .add("message", "Payment method updated successfully")
                .build(), HttpServletResponse.SC_OK);

        } catch (Exception e) {
            System.err.println("Error in PaymentMethodsServlet: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(response, Json.createObjectBuilder()
                .add("success", false)
                .add("error", "Server error: " + e.getMessage())
                .build(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // Authenticate user from session
            Integer customerId = authenticateUser(request);
            
            if (customerId == null) {
                sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Not authenticated")
                    .build(), HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Get card ID from request parameter
            String cardIdStr = request.getParameter("cardId");
            if (cardIdStr == null || cardIdStr.isEmpty()) {
                sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Card ID is required")
                    .build(), HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            int cardId = Integer.parseInt(cardIdStr);
            CreditCards card = creditCardsFacade.find(cardId);

            if (card == null) {
                sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Card not found")
                    .build(), HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Verify card belongs to current user
            if (!card.getCustomerID().getCustomerID().equals(customerId)) {
                sendJsonResponse(response, Json.createObjectBuilder()
                    .add("success", false)
                    .add("error", "Unauthorized")
                    .build(), HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // Delete the card
            creditCardsFacade.remove(card);

            System.out.println("PaymentMethodsServlet: Deleted payment method " + cardId);

            sendJsonResponse(response, Json.createObjectBuilder()
                .add("success", true)
                .add("message", "Payment method deleted successfully")
                .build(), HttpServletResponse.SC_OK);

        } catch (Exception e) {
            System.err.println("Error in PaymentMethodsServlet: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(response, Json.createObjectBuilder()
                .add("success", false)
                .add("error", "Server error: " + e.getMessage())
                .build(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Authenticate user from session and return customer ID
     */
    private Integer authenticateUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("PaymentMethodsServlet: No session found");
            return null;
        }

        Integer customerId = null;
        
        // Try to get customerId from multiple possible session attributes
        Object idObj = session.getAttribute("currentCustomerId");
        if (idObj != null) {
            if (idObj instanceof Integer) {
                customerId = (Integer) idObj;
            } else if (idObj instanceof String) {
                try {
                    customerId = Integer.valueOf((String) idObj);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            System.out.println("PaymentMethodsServlet: Got customerId from session: " + customerId);
            return customerId;
        }

        // Try from currentCustomer object
        Object customerObj = session.getAttribute("currentCustomer");
        if (customerObj != null) {
            try {
                java.lang.reflect.Method getIdMethod = customerObj.getClass().getMethod("getCustomerID");
                Object id = getIdMethod.invoke(customerObj);
                if (id instanceof Integer) {
                    customerId = (Integer) id;
                }
                System.out.println("PaymentMethodsServlet: Got customerId from currentCustomer: " + customerId);
                return customerId;
            } catch (Exception e) {
                System.out.println("PaymentMethodsServlet: Failed to extract customerId: " + e.getMessage());
            }
        }

        System.out.println("PaymentMethodsServlet: User not authenticated");
        // Debug: print all session attributes
        java.util.Enumeration<String> names = session.getAttributeNames();
        System.out.println("PaymentMethodsServlet: Available session attributes:");
        while (names.hasMoreElements()) {
            String n = names.nextElement();
            try {
                System.out.println("  " + n + "=" + session.getAttribute(n).getClass().getSimpleName());
            } catch (Exception e) {
                System.out.println("  " + n + "=(error reading)");
            }
        }
        return null;
    }

    private void sendJsonResponse(HttpServletResponse response, JsonObject json, int statusCode) throws IOException {
        response.setStatus(statusCode);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
        }
    }

    /**
     * Mask card number for display (show last 4 digits only)
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }
}
