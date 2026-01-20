package websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ejb.EJB;
import entityclass.LiveChat;
import entityclass.LiveSession;
import entityclass.Users;
import sessionbeans.LiveChatFacade;
import sessionbeans.LiveSessionFacade;

@ServerEndpoint("/ws/livestream/{sessionId}/{userId}")
public class LiveStreamChatEndpoint {

    // Map to store connected sessions: sessionId -> list of WebSocket sessions
    private static final Map<Integer, List<ChatSession>> SESSION_CONNECTIONS = new ConcurrentHashMap<>();
    
    // Inner class to track user info per WebSocket connection
    private static class ChatSession {
        Session wsSession;
        int userId;
        String username;
        
        ChatSession(Session wsSession, int userId, String username) {
            this.wsSession = wsSession;
            this.userId = userId;
            this.username = username;
        }
    }

    @EJB
    private LiveChatFacade liveChatFacade;

    @EJB
    private LiveSessionFacade liveSessionFacade;

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionId") int sessionId, @PathParam("userId") int userId) {
        try {
            LiveSession liveSession = liveSessionFacade.find(sessionId);
            if (liveSession == null || !"ACTIVE".equalsIgnoreCase(liveSession.getStatus())) {
                session.close();
                return;
            }

            // Get username from session attributes (assuming passed during connection)
            String username = "User_" + userId;

            ChatSession chatSession = new ChatSession(session, userId, username);

            SESSION_CONNECTIONS
                    .computeIfAbsent(sessionId, k -> new ArrayList<>())
                    .add(chatSession);

            System.out.println("User " + username + " joined chat session " + sessionId);

            // Notify all users that someone joined
            broadcastToSession(sessionId, Json.createObjectBuilder()
                    .add("type", "user-joined")
                    .add("username", username)
                    .add("userId", userId)
                    .add("timestamp", System.currentTimeMillis())
                    .build().toString());

        } catch (Exception e) {
            System.err.println("Error in WebSocket onOpen: " + e.getMessage());
            e.printStackTrace();
            try {
                session.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("sessionId") int sessionId, @PathParam("userId") int userId) {
        try {
            // Parse incoming message
            JsonObject msgObj = Json.createReader(new java.io.StringReader(message)).readObject();
            String msgText = msgObj.getString("message", "");
            String msgType = msgObj.getString("type", "TEXT");

            if (msgText.isEmpty()) {
                return;
            }

            // Spam filter: max 500 chars
            if (msgText.length() > 500) {
                msgText = msgText.substring(0, 500);
            }

            // Get username
            ChatSession chatSession = findChatSession(sessionId, userId);
            String username = chatSession != null ? chatSession.username : "User_" + userId;

            // Save to database
            LiveChat chat = new LiveChat();
            chat.setSessionID(liveSessionFacade.find(sessionId));
            // setUserID expects Users object
            Users user = new Users();
            user.setUserID(userId);
            chat.setUserID(user);
            chat.setMessageText(msgText);
            chat.setMessageType(msgType);
            chat.setIsDeleted(false);
            chat.setCreatedAt(new Date());

            liveChatFacade.create(chat);

            // Broadcast to all connected clients
            JsonObject broadcastMsg = Json.createObjectBuilder()
                    .add("type", "message")
                    .add("chatId", chat.getChatMessageID())
                    .add("userId", userId)
                    .add("username", username)
                    .add("message", msgText)
                    .add("messageType", msgType)
                    .add("timestamp", System.currentTimeMillis())
                    .build();

            broadcastToSession(sessionId, broadcastMsg.toString());

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("sessionId") int sessionId, @PathParam("userId") int userId) {
        try {
            List<ChatSession> connections = SESSION_CONNECTIONS.get(sessionId);
            if (connections != null) {
                ChatSession removed = connections.stream()
                        .filter(cs -> cs.wsSession.equals(session))
                        .findFirst()
                        .orElse(null);

                if (removed != null) {
                    connections.remove(removed);
                    System.out.println("User " + removed.username + " left chat session " + sessionId);

                    // Notify others that user left
                    broadcastToSession(sessionId, Json.createObjectBuilder()
                            .add("type", "user-left")
                            .add("username", removed.username)
                            .add("userId", userId)
                            .add("timestamp", System.currentTimeMillis())
                            .build().toString());

                    if (connections.isEmpty()) {
                        SESSION_CONNECTIONS.remove(sessionId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in onClose: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("sessionId") int sessionId) {
        System.err.println("WebSocket error in session " + sessionId + ": " + throwable.getMessage());
        throwable.printStackTrace();
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper method to broadcast message to all users in a session
    private void broadcastToSession(int sessionId, String message) {
        List<ChatSession> connections = SESSION_CONNECTIONS.get(sessionId);
        if (connections != null) {
            List<ChatSession> closedSessions = new ArrayList<>();
            for (ChatSession cs : connections) {
                try {
                    if (cs.wsSession.isOpen()) {
                        cs.wsSession.getBasicRemote().sendText(message);
                    } else {
                        closedSessions.add(cs);
                    }
                } catch (IOException e) {
                    System.err.println("Error sending message: " + e.getMessage());
                    closedSessions.add(cs);
                }
            }
            // Remove closed sessions
            connections.removeAll(closedSessions);
        }
    }

    // Helper method to find chat session info
    private ChatSession findChatSession(int sessionId, int userId) {
        List<ChatSession> connections = SESSION_CONNECTIONS.get(sessionId);
        if (connections != null) {
            return connections.stream()
                    .filter(cs -> cs.userId == userId)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    // Public method to send system message (called from other services if needed)
    public static void sendSystemMessage(int sessionId, String message) {
        List<ChatSession> connections = SESSION_CONNECTIONS.get(sessionId);
        if (connections != null) {
            JsonObject sysMsg = Json.createObjectBuilder()
                    .add("type", "system")
                    .add("message", message)
                    .add("timestamp", System.currentTimeMillis())
                    .build();

            for (ChatSession cs : connections) {
                try {
                    if (cs.wsSession.isOpen()) {
                        cs.wsSession.getBasicRemote().sendText(sysMsg.toString());
                    }
                } catch (IOException e) {
                    System.err.println("Error sending system message: " + e.getMessage());
                }
            }
        }
    }

    // Public method to get active connections count
    public static int getActiveConnections(int sessionId) {
        List<ChatSession> connections = SESSION_CONNECTIONS.get(sessionId);
        return connections != null ? connections.size() : 0;
    }
}
