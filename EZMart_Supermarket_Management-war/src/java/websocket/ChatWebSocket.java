package websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ejb.EJB;
import entityclass.ChatMessages;
import entityclass.ChatConversations;
import sessionbeans.ChatConversationsFacadeLocal;
import sessionbeans.ChatMessagesFacadeLocal;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket Endpoint for Real-time Chat
 * Provides smooth, Messenger-like real-time messaging between Staff and Customer
 */
@ServerEndpoint("/chat/{conversationId}/{userId}")
public class ChatWebSocket {

    private static final Logger LOGGER = Logger.getLogger(ChatWebSocket.class.getName());

    // Store active connections: conversationId -> userId -> session
    private static final Map<Integer, Map<Integer, Session>> conversationConnections = new ConcurrentHashMap<>();

    // Store user info for broadcasting
    private static final Map<Integer, String> userNames = new ConcurrentHashMap<>();

    private Session session;
    private Integer conversationId;
    private Integer userId;

    @EJB
    private static ChatMessagesFacadeLocal messagesFacade;

    @EJB
    private static ChatConversationsFacadeLocal conversationsFacade;

    /**
     * Called when a new WebSocket connection is established
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("conversationId") String convId, 
                       @PathParam("userId") String userIdStr) {
        try {
            this.session = session;
            this.conversationId = Integer.parseInt(convId);
            this.userId = Integer.parseInt(userIdStr);

            // Add connection to conversation
            conversationConnections
                .computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
                .put(userId, session);

            LOGGER.info("ChatWebSocket: User " + userId + " connected to conversation " + conversationId);

            // Send confirmation
            session.getBasicRemote().sendText("{\"type\":\"CONNECTED\",\"conversationId\":" + conversationId + "}");

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid conversation ID or user ID", e);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error sending connection confirmation", e);
        }
    }

    /**
     * Called when a message is received from client
     */
    @OnMessage
    public void onMessage(String message, Session clientSession) {
        LOGGER.info("ChatWebSocket: Received message - " + message);

        try {
            // Parse JSON message
            jakarta.json.JsonObject json = jakarta.json.Json.createReader(
                new java.io.StringReader(message)).readObject();

            String type = json.getString("type", "MESSAGE");

            if ("MESSAGE".equals(type)) {
                // Broadcast message to all users in the conversation
                broadcastMessage(json);
            } else if ("TYPING".equals(type)) {
                // Broadcast typing indicator
                broadcastTyping(json);
            } else if ("READ".equals(type)) {
                // Mark messages as read
                markAsRead(json);
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing message", e);
        }
    }

    /**
     * Broadcast a chat message to all users in the conversation
     */
    private void broadcastMessage(jakarta.json.JsonObject json) {
        Integer convId = json.getInt("conversationId", 0);
        Integer senderId = json.getInt("senderId", 0);
        String content = json.getString("content", "");
        String senderRole = json.getString("senderRole", "");

        // Create message payload
        String messagePayload = String.format(
            "{\"type\":\"NEW_MESSAGE\",\"conversationId\":%d,\"senderId\":%d,\"senderRole\":\"%s\",\"content\":\"%s\",\"timestamp\":%d}",
            convId, senderId, senderRole, escapeJson(content), System.currentTimeMillis()
        );

        // Send to all connected users in this conversation
        Map<Integer, Session> convSessions = conversationConnections.get(convId);
        if (convSessions != null) {
            convSessions.forEach((uid, sess) -> {
                try {
                    if (sess.isOpen()) {
                        sess.getBasicRemote().sendText(messagePayload);
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error sending message to user " + uid, e);
                }
            });
        }
    }

    /**
     * Broadcast typing indicator
     */
    private void broadcastTyping(jakarta.json.JsonObject json) {
        Integer convId = json.getInt("conversationId", 0);
        Integer senderId = json.getInt("senderId", 0);
        boolean isTyping = json.getBoolean("typing", false);

        String typingPayload = String.format(
            "{\"type\":\"TYPING\",\"conversationId\":%d,\"senderId\":%d,\"isTyping\":%b}",
            convId, senderId, isTyping
        );

        Map<Integer, Session> convSessions = conversationConnections.get(convId);
        if (convSessions != null) {
            convSessions.forEach((uid, sess) -> {
                // Don't send typing indicator back to the sender
                if (uid != senderId && sess.isOpen()) {
                    try {
                        sess.getBasicRemote().sendText(typingPayload);
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Error sending typing indicator", e);
                    }
                }
            });
        }
    }

    /**
     * Mark messages as read
     */
    private void markAsRead(jakarta.json.JsonObject json) {
        Integer convId = json.getInt("conversationId", 0);
        Integer readerId = json.getInt("readerId", 0);

        String readPayload = String.format(
            "{\"type\":\"READ_RECEIPT\",\"conversationId\":%d,\"readerId\":%d}",
            convId, readerId
        );

        Map<Integer, Session> convSessions = conversationConnections.get(convId);
        if (convSessions != null) {
            convSessions.forEach((uid, sess) -> {
                if (uid != readerId && sess.isOpen()) {
                    try {
                        sess.getBasicRemote().sendText(readPayload);
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Error sending read receipt", e);
                    }
                }
            });
        }
    }

    /**
     * Called when WebSocket connection is closed
     */
    @OnClose
    public void onClose(Session session, @PathParam("conversationId") String convId,
                        @PathParam("userId") String userIdStr) {
        try {
            Integer convIdInt = Integer.parseInt(convId);
            Integer userIdInt = Integer.parseInt(userIdStr);

            // Remove connection from conversation
            Map<Integer, Session> convSessions = conversationConnections.get(convId);
            if (convSessions != null) {
                convSessions.remove(userId);
                if (convSessions.isEmpty()) {
                    conversationConnections.remove(convId);
                }
            }

            LOGGER.info("ChatWebSocket: User " + userId + " disconnected from conversation " + convId);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid conversation ID or user ID on close", e);
        }
    }

    /**
     * Called when an error occurs
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        LOGGER.log(Level.WARNING, "ChatWebSocket error", throwable);
    }

    /**
     * Send a message to a specific user
     */
    public static void sendToUser(Integer userId, String message) {
        conversationConnections.values().forEach(convSessions -> {
            Session session = convSessions.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error sending message to user " + userId, e);
                }
            }
        });
    }

    /**
     * Broadcast message to all users in a conversation
     */
    public static void broadcastToConversation(Integer conversationId, String message) {
        Map<Integer, Session> convSessions = conversationConnections.get(conversationId);
        if (convSessions != null) {
            convSessions.forEach((userId, session) -> {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(message);
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Error broadcasting to user " + userId, e);
                    }
                }
            });
        }
    }

    /**
     * Get count of connected users in a conversation
     */
    public static int getOnlineCount(Integer conversationId) {
        Map<Integer, Session> convSessions = conversationConnections.get(conversationId);
        return convSessions != null ? convSessions.size() : 0;
    }

    /**
     * Escape special characters for JSON
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
