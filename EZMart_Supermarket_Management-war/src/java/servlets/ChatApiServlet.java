package servlets;

import entityclass.ChatMessages;
import entityclass.ChatConversations;
import entityclass.Customers;
import entityclass.Staffs;
import entityclass.Users;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sessionbeans.ChatConversationsFacadeLocal;
import sessionbeans.ChatMessagesFacadeLocal;
import sessionbeans.CustomersFacadeLocal;
import sessionbeans.StaffsFacadeLocal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * REST API Servlet for Chat functionality
 * Provides endpoints for sending/receiving messages, getting conversations, etc.
 */
public class ChatApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private ChatConversationsFacadeLocal conversationsFacade;

    @EJB
    private ChatMessagesFacadeLocal messagesFacade;

    @EJB
    private CustomersFacadeLocal customersFacade;

    @EJB
    private StaffsFacadeLocal staffsFacade;

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");
        Integer userId = getUserIdFromSession(request);

        if (userId == null) {
            sendError(response, "Unauthorized", 401);
            return;
        }

        try {
            switch (action) {
                case "conversations":
                    getConversations(request, response, userId);
                    break;
                case "messages":
                    getMessages(request, response, userId);
                    break;
                case "unread-count":
                    getUnreadCount(request, response, userId);
                    break;
                default:
                    sendError(response, "Invalid action", 400);
            }
        } catch (Exception e) {
            sendError(response, e.getMessage(), 500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");
        Integer userId = getUserIdFromSession(request);

        if (userId == null) {
            sendError(response, "Unauthorized", 401);
            return;
        }

        try {
            // Read request body
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JsonObject data = gson.fromJson(sb.toString(), JsonObject.class);

            switch (action) {
                case "send-message":
                    sendMessage(request, response, userId, data);
                    break;
                case "mark-read":
                    markAsRead(request, response, userId, data);
                    break;
                case "create-conversation":
                    createConversation(request, response, userId, data);
                    break;
                default:
                    sendError(response, "Invalid action", 400);
            }
        } catch (Exception e) {
            sendError(response, e.getMessage(), 500);
        }
    }

    /**
     * Get all conversations for user
     */
    private void getConversations(HttpServletRequest request, HttpServletResponse response, Integer userId) 
            throws IOException {
        PrintWriter out = response.getWriter();

        Users user = getUserById(userId);
        if (user == null) {
            sendError(response, "User not found", 404);
            return;
        }

        List<ChatConversations> conversations;
        String userRole = user.getRole();

        if ("STAFF".equals(userRole)) {
            Staffs staff = staffsFacade.findByUserID(userId);
            if (staff != null) {
                conversations = conversationsFacade.findActiveByStaffID(staff.getStaffID());
            } else {
                conversations = List.of();
            }
        } else if ("CUSTOMER".equals(userRole)) {
            Customers customer = customersFacade.findByUserID(userId);
            if (customer != null) {
                conversations = conversationsFacade.findActiveByCustomerID(customer.getCustomerID());
            } else {
                conversations = List.of();
            }
        } else {
            conversations = List.of();
        }

        // Convert to response format
        JsonObject responseObj = new JsonObject();
        responseObj.addProperty("success", true);
        responseObj.add("conversations", gson.toJsonTree(conversations.stream().map(conv -> {
            JsonObject convObj = new JsonObject();
            convObj.addProperty("conversationId", conv.getConversationID());
            convObj.addProperty("customerId", conv.getCustomerID() != null ? conv.getCustomerID().getCustomerID() : 0);
            convObj.addProperty("staffId", conv.getStaffID() != null ? conv.getStaffID().getStaffID() : 0);
            convObj.addProperty("customerName", getCustomerFullName(conv.getCustomerID()));
            convObj.addProperty("staffName", getStaffFullName(conv.getStaffID()));
            convObj.addProperty("lastMessageAt", conv.getLastMessageAt() != null ? conv.getLastMessageAt().toString() : "");
            convObj.addProperty("createdAt", conv.getCreatedAt() != null ? conv.getCreatedAt().toString() : "");
            return convObj;
        }).toList()));

        out.print(gson.toJson(responseObj));
    }

    /**
     * Get messages for a conversation
     */
    private void getMessages(HttpServletRequest request, HttpServletResponse response, Integer userId) 
            throws IOException {
        PrintWriter out = response.getWriter();

        String convIdStr = request.getParameter("conversationId");
        if (convIdStr == null || convIdStr.isEmpty()) {
            sendError(response, "conversationId is required", 400);
            return;
        }

        int conversationId = Integer.parseInt(convIdStr);
        List<ChatMessages> messages = messagesFacade.findByConversationIDOrdered(conversationId);

        JsonObject responseObj = new JsonObject();
        responseObj.addProperty("success", true);
        responseObj.add("messages", gson.toJsonTree(messages.stream().map(msg -> {
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("messageId", msg.getMessageID());
            msgObj.addProperty("conversationId", msg.getConversationID().getConversationID());
            msgObj.addProperty("senderRole", msg.getSenderRole());
            msgObj.addProperty("senderUserId", msg.getSenderUserID());
            msgObj.addProperty("content", msg.getContent());
            msgObj.addProperty("sentAt", msg.getSentAt() != null ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(msg.getSentAt()) : "");
            msgObj.addProperty("isRead", msg.getIsRead());
            msgObj.addProperty("messageType", msg.getMessageType());
            msgObj.addProperty("isOwnMessage", msg.getSenderUserID().equals(userId));
            return msgObj;
        }).toList()));

        out.print(gson.toJson(responseObj));
    }

    /**
     * Get unread message count
     */
    private void getUnreadCount(HttpServletRequest request, HttpServletResponse response, Integer userId) 
            throws IOException {
        PrintWriter out = response.getWriter();

        Users user = getUserById(userId);
        if (user == null) {
            sendError(response, "User not found", 404);
            return;
        }

        String userRole = user.getRole();
        int unreadCount = 0;

        if ("STAFF".equals(userRole)) {
            Staffs staff = staffsFacade.findByUserID(userId);
            if (staff != null) {
                List<ChatConversations> convs = conversationsFacade.findActiveByStaffID(staff.getStaffID());
                for (ChatConversations conv : convs) {
                    unreadCount += messagesFacade.countUnreadByConversation(
                        conv.getConversationID(), userRole, userId);
                }
            }
        } else if ("CUSTOMER".equals(userRole)) {
            Customers customer = customersFacade.findByUserID(userId);
            if (customer != null) {
                List<ChatConversations> convs = conversationsFacade.findActiveByCustomerID(customer.getCustomerID());
                for (ChatConversations conv : convs) {
                    unreadCount += messagesFacade.countUnreadByConversation(
                        conv.getConversationID(), userRole, userId);
                }
            }
        }

        JsonObject responseObj = new JsonObject();
        responseObj.addProperty("success", true);
        responseObj.addProperty("count", unreadCount);

        out.print(gson.toJson(responseObj));
    }

    /**
     * Send a new message
     */
    private void sendMessage(HttpServletRequest request, HttpServletResponse response, Integer userId, JsonObject data) 
            throws IOException {
        PrintWriter out = response.getWriter();

        int conversationId = data.get("conversationId").getAsInt();
        String content = data.get("content").getAsString();

        if (content == null || content.trim().isEmpty()) {
            sendError(response, "Content is required", 400);
            return;
        }

        Users user = getUserById(userId);
        if (user == null) {
            sendError(response, "User not found", 404);
            return;
        }

        ChatConversations conversation = conversationsFacade.find(conversationId);
        if (conversation == null) {
            sendError(response, "Conversation not found", 404);
            return;
        }

        // Create and save message
        ChatMessages message = new ChatMessages();
        message.setConversationID(conversation);
        message.setSenderRole(user.getRole());
        message.setSenderUserID(userId);
        message.setContent(content.trim());
        message.setSentAt(new Date());
        message.setIsRead(false);
        message.setMessageType("TEXT");

        messagesFacade.create(message);

        // Update conversation's last message time
        conversationsFacade.updateLastMessageAt(conversationId);

        JsonObject responseObj = new JsonObject();
        responseObj.addProperty("success", true);
        responseObj.addProperty("messageId", message.getMessageID());
        responseObj.addProperty("sentAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(message.getSentAt()));

        out.print(gson.toJson(responseObj));
    }

    /**
     * Mark messages as read
     */
    private void markAsRead(HttpServletRequest request, HttpServletResponse response, Integer userId, JsonObject data) 
            throws IOException {
        PrintWriter out = response.getWriter();

        int conversationId = data.get("conversationId").getAsInt();
        Users user = getUserById(userId);

        if (user == null) {
            sendError(response, "User not found", 404);
            return;
        }

        messagesFacade.markAsRead(conversationId, user.getRole(), userId);

        JsonObject responseObj = new JsonObject();
        responseObj.addProperty("success", true);
        out.print(gson.toJson(responseObj));
    }

    /**
     * Create a new conversation
     */
    private void createConversation(HttpServletRequest request, HttpServletResponse response, Integer userId, JsonObject data) 
            throws IOException {
        PrintWriter out = response.getWriter();

        Users user = getUserById(userId);
        if (user == null) {
            sendError(response, "User not found", 404);
            return;
        }

        int staffId = data.get("staffId").getAsInt();

        ChatConversations conversation = null;

        if ("CUSTOMER".equals(user.getRole())) {
            Customers customer = customersFacade.findByUserID(userId);
            if (customer == null) {
                sendError(response, "Customer not found", 404);
                return;
            }

            // Check if conversation already exists
            conversation = conversationsFacade.findByCustomerAndStaff(customer.getCustomerID(), staffId);

            if (conversation == null) {
                // Create new conversation
                Staffs staff = staffsFacade.find(staffId);
                if (staff == null) {
                    sendError(response, "Staff not found", 404);
                    return;
                }

                conversation = new ChatConversations();
                conversation.setCustomerID(customer);
                conversation.setStaffID(staff);
                conversation.setCreatedAt(new Date());
                conversation.setStatus("ACTIVE");
                conversationsFacade.create(conversation);
            }
        } else {
            sendError(response, "Only customers can create conversations", 403);
            return;
        }

        JsonObject responseObj = new JsonObject();
        responseObj.addProperty("success", true);
        responseObj.addProperty("conversationId", conversation.getConversationID());

        out.print(gson.toJson(responseObj));
    }

    /**
     * Get user ID from session
     */
    private Integer getUserIdFromSession(HttpServletRequest request) {
        try {
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                Users user = (Users) session.getAttribute("currentUser");
                if (user != null) {
                    return user.getUserID();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Get user by ID
     */
    private Users getUserById(Integer userId) {
        try {
            // This would typically use the UsersFacade
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get customer full name
     */
    private String getCustomerFullName(Customers customer) {
        if (customer == null) return "Unknown";
        StringBuilder sb = new StringBuilder();
        if (customer.getFirstName() != null) sb.append(customer.getFirstName()).append(" ");
        if (customer.getMiddleName() != null) sb.append(customer.getMiddleName()).append(" ");
        if (customer.getLastName() != null) sb.append(customer.getLastName());
        return sb.toString().trim();
    }

    /**
     * Get staff full name
     */
    private String getStaffFullName(Staffs staff) {
        if (staff == null) return "Unknown";
        StringBuilder sb = new StringBuilder();
        if (staff.getFirstName() != null) sb.append(staff.getFirstName()).append(" ");
        if (staff.getMiddleName() != null) sb.append(staff.getMiddleName()).append(" ");
        if (staff.getLastName() != null) sb.append(staff.getLastName());
        return sb.toString().trim();
    }

    /**
     * Send error response
     */
    private void sendError(HttpServletResponse response, String message, int status) throws IOException {
        response.setStatus(status);
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        response.getWriter().print(gson.toJson(error));
    }
}
