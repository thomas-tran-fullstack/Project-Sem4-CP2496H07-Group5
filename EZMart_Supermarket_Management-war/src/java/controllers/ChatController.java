package controllers;

import entityclass.ChatConversations;
import entityclass.ChatMessages;
import entityclass.Customers;
import entityclass.Staffs;
import entityclass.Users;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;
import sessionbeans.ChatConversationsFacadeLocal;
import sessionbeans.ChatMessagesFacadeLocal;
import sessionbeans.CustomersFacadeLocal;
import sessionbeans.StaffsFacadeLocal;
import sessionbeans.UsersFacadeLocal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main Chat Controller - handles 1:1 chat between Staff and Customer
 * Uses WebSocket for real-time messaging
 */
@Named("chatController")
@SessionScoped
public class ChatController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ChatConversationsFacadeLocal conversationsFacade;

    @EJB
    private ChatMessagesFacadeLocal messagesFacade;

    @EJB
    private CustomersFacadeLocal customersFacade;

    @EJB
    private StaffsFacadeLocal staffsFacade;

    @EJB
    private UsersFacadeLocal usersFacade;

    @Inject
    private AuthController authController;

    // Current user info
    private Users currentUser;
    private String currentUserRole; // "STAFF" or "CUSTOMER"
    private Integer currentCustomerId;
    private Integer currentStaffId;
    private Integer lastLoadedUserId; // Track for cache invalidation on logout

    // Current conversation
    private ChatConversations selectedConversation;
    private Integer selectedConversationId;

    // Message input
    private String newMessage;

    // Conversation lists
    private List<ChatConversations> conversations;

    // Pending customers waiting for acceptance
    private List<ChatConversations> pendingCustomers;

    // Unread message count
    private int unreadCount = 0;

    // Chat request status (for customer widget)
    private boolean chatRequestPending = false;
    private boolean chatRequestRejected = false;

    // WebSocket sessions - maps userId to WebSocket session
    private static final Map<Integer, String> onlineUsers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // NOTE: SessionScoped bean can be created before login/session attributes exist.
        // So we only do a best-effort load here; getters/actions will refresh again.
        loadCurrentUser();
    }

    /**
     * Load current user from session.
     * IMPORTANT: Role must be taken from Users table/session, not inferred from existence of Customers/Staffs,
     * because a UserID might exist in both tables (old test data).
     */
    private void loadCurrentUser() {
        try {
            // Try to get from AuthController first (more reliable)
            if (authController != null) {
                currentUser = authController.getCurrentUser();
                if (currentUser != null && currentUser.getUserID() != null) {
                    currentUserRole = currentUser.getRole() != null ? currentUser.getRole().trim().toUpperCase() : null;
                    
                    // Load staff/customer IDs
                    if ("CUSTOMER".equals(currentUserRole)) {
                        Customers currentCustomer = authController.getCurrentCustomer();
                        currentCustomerId = (currentCustomer != null) ? currentCustomer.getCustomerID() : null;
                        currentStaffId = null;
                    } else if ("STAFF".equals(currentUserRole)) {
                        currentStaffId = null;
                        currentCustomerId = null;
                        // Load StaffId from database
                        try {
                            Staffs s = staffsFacade.findByUserID(currentUser.getUserID());
                            if (s != null) {
                                currentStaffId = s.getStaffID();
                            } else {
                                System.out.println("⚠️ ChatController: No staff record found for userId=" + currentUser.getUserID());
                            }
                        } catch (Exception e) {
                            System.out.println("⚠️ ChatController: Failed to find staff by userID - " + e.getMessage());
                        }
                    }
                    
                    System.out.println("ChatController.loadCurrentUser: Loaded from authController - ID: " + currentUser.getUserID()
                            + ", Role: " + currentUserRole
                            + ", customerId=" + currentCustomerId
                            + ", staffId=" + currentStaffId);
                    return;
                }
            }

            // Fallback: load from session manually
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc == null) {
                System.out.println("ChatController: No FacesContext available");
                return;
            }

            HttpSession session = (HttpSession) fc.getExternalContext().getSession(false);
            if (session == null) {
                System.out.println("ChatController: No session available");
                return;
            }

            // Try full user object first
            currentUser = (Users) session.getAttribute("currentUser");

            Integer userId = null;
            if (currentUser != null) {
                userId = currentUser.getUserID();
            } else {
                userId = (Integer) session.getAttribute("currentUserId");
            }

            if (userId == null) {
                currentUser = null;
                currentUserRole = null;
                currentCustomerId = null;
                currentStaffId = null;
                System.out.println("ChatController: No user found in session");
                return;
            }

            // Load authoritative user from DB if needed
            if (currentUser == null || currentUser.getUserID() == null) {
                try {
                    Users dbUser = usersFacade.find(userId);
                    if (dbUser != null) {
                        currentUser = dbUser;
                    } else {
                        currentUser = new Users();
                        currentUser.setUserID(userId);
                    }
                } catch (Exception ignored) {
                    currentUser = new Users();
                    currentUser.setUserID(userId);
                }
            }

            // Determine role: session override -> DB value
            String role = (String) session.getAttribute("userRole");
            if (role == null || role.trim().isEmpty()) {
                role = currentUser.getRole();
            }
            role = role != null ? role.trim().toUpperCase() : null;
            currentUserRole = role;

            // Load staff/customer IDs based on role (do NOT infer role from table existence)
            if ("CUSTOMER".equals(currentUserRole)) {
                currentStaffId = null;

                Integer sessCustomerId = (Integer) session.getAttribute("currentCustomerId");
                if (sessCustomerId != null) {
                    currentCustomerId = sessCustomerId;
                } else {
                    Customers c = customersFacade.findByUserID(userId);
                    currentCustomerId = (c != null) ? c.getCustomerID() : null;
                }

            } else if ("STAFF".equals(currentUserRole)) {
                currentCustomerId = null;

                Integer sessStaffId = (Integer) session.getAttribute("currentStaffId");
                if (sessStaffId != null) {
                    currentStaffId = sessStaffId;
                } else {
                    Staffs s = staffsFacade.findByUserID(userId);
                    currentStaffId = (s != null) ? s.getStaffID() : null;
                }
            } else {
                currentCustomerId = null;
                currentStaffId = null;
            }

            if (currentUser != null) {
                System.out.println("ChatController: Loaded user - ID: " + userId
                        + ", Role: " + currentUserRole
                        + ", customerId=" + currentCustomerId
                        + ", staffId=" + currentStaffId);

                markUserOnline(userId);
            }

        } catch (Exception e) {
            System.out.println("ChatController.loadCurrentUser: Error - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Refresh current user from session (safe to call anytime)
     */
    public void refreshCurrentUser() {
        loadCurrentUser();
    }

    /**
     * Get all conversations for current user
     */
    public List<ChatConversations> getConversations() {
        // Always refresh because SessionScoped bean can be created before login
        if (currentUser == null || currentUserRole == null) {
            refreshCurrentUser();
        }
        
        // CACHE INVALIDATION: If user changed (logout/login), clear data
        if (currentUser != null && lastLoadedUserId != null && !lastLoadedUserId.equals(currentUser.getUserID())) {
            System.out.println("⚠️ User changed (logout detected) - clearing cache: was " + lastLoadedUserId + ", now " + currentUser.getUserID());
            conversations = null;
            selectedConversation = null;
            selectedConversationId = null;
        }
        
        if (currentUser != null) {
            lastLoadedUserId = currentUser.getUserID();
        }
        
        // Force reload staffId/customerId if null (critical for staff panel)
        if ("STAFF".equals(currentUserRole) && currentStaffId == null && currentUser != null) {
            try {
                Staffs s = staffsFacade.findByUserID(currentUser.getUserID());
                if (s != null) {
                    currentStaffId = s.getStaffID();
                    System.out.println("✓ Auto-loaded staffId: " + currentStaffId);
                }
            } catch (Exception e) {
                System.out.println("✗ Failed to load staffId: " + e.getMessage());
            }
        }
        
        if ("CUSTOMER".equals(currentUserRole) && currentCustomerId == null && currentUser != null) {
            try {
                Customers c = customersFacade.findByUserID(currentUser.getUserID());
                if (c != null) {
                    currentCustomerId = c.getCustomerID();
                    System.out.println("✓ Auto-loaded customerId: " + currentCustomerId);
                }
            } catch (Exception e) {
                System.out.println("✗ Failed to load customerId: " + e.getMessage());
            }
        }
        
        loadConversations();
        return conversations != null ? conversations : new ArrayList<>();
    }

    /**
     * Load conversations based on user role
     */
    private void loadConversations() {
        conversations = new ArrayList<>();

        try {
            if ("STAFF".equals(currentUserRole) && currentStaffId != null) {
                // STAFF: Load only conversations they accepted (not pending or others' conversations)
                conversations = conversationsFacade.findAcceptedByStaff(currentStaffId);
                System.out.println("✓ Staff ChatController.loadConversations: staffId=" + currentStaffId + ", found " + (conversations != null ? conversations.size() : 0) + " accepted conversations");
                
                // Load pending requests for this staff (excluding ones they rejected)
                pendingCustomers = conversationsFacade.findPendingRequestsForStaff(currentStaffId);
                System.out.println("✓ Staff pending requests for " + currentStaffId + ": " + (pendingCustomers != null ? pendingCustomers.size() : 0));
            } else if ("CUSTOMER".equals(currentUserRole) && currentCustomerId != null) {
                conversations = conversationsFacade.findActiveByCustomerID(currentCustomerId);
                System.out.println("✓ Customer ChatController.loadConversations: customerId=" + currentCustomerId + ", found " + (conversations != null ? conversations.size() : 0) + " conversations");
            } else {
                System.out.println("✗ ChatController.loadConversations: SKIP - role=" + currentUserRole + ", staffId=" + currentStaffId + ", customerId=" + currentCustomerId);
            }
        } catch (Exception e) {
            System.out.println("ChatController.loadConversations: Error - " + e.getMessage());
        }
    }

    /**
     * Select a conversation
     */
    public String selectConversation(Integer conversationId) {
        try {
            refreshCurrentUser();

            selectedConversation = conversationsFacade.find(conversationId);
            selectedConversationId = conversationId;

            // Mark messages as read
            if (selectedConversation != null && currentUser != null && currentUserRole != null) {
                messagesFacade.markAsRead(conversationId, currentUserRole, currentUser.getUserID());
                loadUnreadCount();
            }
        } catch (Exception e) {
            System.out.println("ChatController.selectConversation: Error - " + e.getMessage());
        }
        return null;
    }

    /**
     * Clear selection (for staff UI back button)
     */
    public String clearSelection() {
        selectedConversation = null;
        selectedConversationId = null;
        return null;
    }

    /**
     * Get messages for selected conversation
     * IMPORTANT: Always fresh query from DB (no cache) to ensure AJAX updates work
     */
    public List<ChatMessages> getMessages() {
        if (selectedConversationId == null) {
            return new ArrayList<>();
        }

        // Always query fresh from DB - CRITICAL for real-time AJAX updates
        try {
            List<ChatMessages> messages = messagesFacade.findByConversationIDOrdered(selectedConversationId);
            return messages != null ? messages : new ArrayList<>();
        } catch (Exception e) {
            System.out.println("ChatController.getMessages: Error - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Send a new message
     * Guards based on conversation status and user role
     */
    public String sendMessage() {
        try {
            // Always refresh user right at action time
            refreshCurrentUser();

            if (currentUser == null || currentUserRole == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new jakarta.faces.application.FacesMessage(
                                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                                "Chat error", "Bạn chưa đăng nhập hoặc session không hợp lệ."
                        ));
                return null;
            }

            if (newMessage == null || newMessage.trim().isEmpty()) {
                return null;
            }

            ChatConversations conversation = getOrCreateConversation();

            if (conversation != null) {
                this.selectedConversation = conversation;
                this.selectedConversationId = conversation.getConversationID();
            }

            if (conversation == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new jakarta.faces.application.FacesMessage(
                                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                                "Chat error", "Không tạo/không tìm được conversation (thiếu staff/customer id)."
                        ));
                return null;
            }

            // Guard based on role and conversation status
            String requestStatus = conversation.getRequestStatus();
            
            if ("STAFF".equals(currentUserRole)) {
                // Staff can only send if: ACCEPTED AND this staff accepted it
                if (!"ACCEPTED".equals(requestStatus)) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new jakarta.faces.application.FacesMessage(
                            jakarta.faces.application.FacesMessage.SEVERITY_WARN,
                            "Chat error", "This chat is not accepted or has been closed."
                        ));
                    return null;
                }
                
                if (!currentStaffId.equals(conversation.getAcceptedStaffID())) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new jakarta.faces.application.FacesMessage(
                            jakarta.faces.application.FacesMessage.SEVERITY_WARN,
                            "Chat error", "Another staff member accepted this chat."
                        ));
                    return null;
                }
                System.out.println("✓ Staff " + currentStaffId + " sending message in accepted conversation");
            } else if ("CUSTOMER".equals(currentUserRole)) {
                // Customer can send if PENDING or ACCEPTED (not CLOSED/REJECTED)
                if ("CLOSED".equals(requestStatus) || "REJECTED".equals(requestStatus)) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new jakarta.faces.application.FacesMessage(
                            jakarta.faces.application.FacesMessage.SEVERITY_WARN,
                            "Chat error", "This chat has been closed or rejected. Start a new request."
                        ));
                    return null;
                }
                System.out.println("✓ Customer sending message (status=" + requestStatus + ")");
            }

            ChatMessages message = new ChatMessages();
            message.setConversationID(conversation);
            message.setSenderRole(currentUserRole);
            message.setSenderUserID(currentUser.getUserID());
            message.setContent(newMessage.trim());
            message.setSentAt(new Date());
            message.setIsRead(false);
            message.setMessageType("TEXT");
            message.setAttachmentUrl(null);

            messagesFacade.create(message);

            // Update conversation last message time
            conversationsFacade.updateLastMessageAt(conversation.getConversationID());

            // Clear message input
            newMessage = "";

            // Reload conversations + unread + selected conversation
            loadConversations();
            loadUnreadCount();
            
            // Refresh selectedConversation so template sees new messages
            if (selectedConversationId != null) {
                selectedConversation = conversationsFacade.find(selectedConversationId);
            }

            // Broadcast (placeholder)
            broadcastMessage(conversation.getConversationID(), message);

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new jakarta.faces.application.FacesMessage(
                            jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                            "Send failed", e.getClass().getSimpleName() + ": " + e.getMessage()
                    ));
            return null;
        }
    }

    /**
     * Get or create conversation with a staff/customer
     */
   private ChatConversations getOrCreateConversation() {
    try {
        refreshCurrentUser();

        if (!"CUSTOMER".equals(currentUserRole) || currentCustomerId == null) {
            // STAFF: dùng selectedConversation như bạn đang làm
            if ("STAFF".equals(currentUserRole) && currentStaffId != null) {
                return selectedConversation;
            }
            return null;
        }

        // 1) Nếu customer đã có ACTIVE conversation => dùng lại (sticky)
        ChatConversations existing = conversationsFacade.findLatestActiveByCustomer(currentCustomerId);
        if (existing != null) {
            return existing;
        }

        // 2) Customer mới => chọn staff theo thuật toán
        Integer pickedStaffId = conversationsFacade.pickLeastLoadedActiveStaffId();
        if (pickedStaffId == null) {
            System.out.println("getOrCreateConversation: No ACTIVE staff available!");
            return null;
        }

        Customers customer = customersFacade.find(currentCustomerId);
        Staffs staff = staffsFacade.find(pickedStaffId);

        if (customer == null || staff == null) return null;

        // 3) Tạo conversation mới
        ChatConversations conv = new ChatConversations();
        conv.setCustomerID(customer);
        conv.setStaffID(staff);
        conv.setCreatedAt(new Date());
        conv.setLastMessageAt(new Date());
        conv.setStatus("ACTIVE");
        conversationsFacade.create(conv);

        return conv;

    } catch (Exception e) {
        System.out.println("ChatController.getOrCreateConversation: Error - " + e.getMessage());
        return null;
    }
}


    // helper to keep compilation if conversation null check gets refactored in IDE
    private boolean indicateNull(Object o) {
        return o == null;
    }


    /**
     * Count unread messages for a specific conversation (used by staff list badge)
     */
    public int countUnreadByConversation(Integer conversationId) {
        try {
            refreshCurrentUser();
            if (conversationId == null || currentUser == null || currentUserRole == null) return 0;
            return messagesFacade.countUnreadByConversation(conversationId, currentUserRole, currentUser.getUserID());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get unread message count
     */
    public void loadUnreadCount() {
        try {
            refreshCurrentUser();

            if (currentUser == null || currentUserRole == null) {
                unreadCount = 0;
                return;
            }

            if ("STAFF".equals(currentUserRole) && currentStaffId != null) {
                List<ChatConversations> convs = conversationsFacade.findActiveByStaffID(currentStaffId);
                unreadCount = 0;
                if (convs != null) {
                    for (ChatConversations conv : convs) {
                        unreadCount += messagesFacade.countUnreadByConversation(
                                conv.getConversationID(), currentUserRole, currentUser.getUserID());
                    }
                }
            } else if ("CUSTOMER".equals(currentUserRole) && currentCustomerId != null) {
                List<ChatConversations> convs = conversationsFacade.findActiveByCustomerID(currentCustomerId);
                unreadCount = 0;
                if (convs != null) {
                    for (ChatConversations conv : convs) {
                        unreadCount += messagesFacade.countUnreadByConversation(
                                conv.getConversationID(), currentUserRole, currentUser.getUserID());
                    }
                }
            } else {
                unreadCount = 0;
            }
        } catch (Exception e) {
            System.out.println("ChatController.loadUnreadCount: Error - " + e.getMessage());
        }
    }

    /**
     * Check if chat is available for current user
     */
    public boolean isChatAvailable() {
        if (currentUser == null || currentUserRole == null) {
            refreshCurrentUser();
        }
        return currentUser != null &&
                ("STAFF".equals(currentUserRole) || "CUSTOMER".equals(currentUserRole));
    }

    /**
     * Get other participant's name in conversation
     */
    public String otherParticipantName(ChatConversations conv) {
        if (conv == null) return "Unknown";

        if ("CUSTOMER".equals(currentUserRole)) {
            return conv.getStaffID() != null ? conv.getStaffID().getFullName() : "Staff";
        } else {
            return conv.getCustomerID() != null ? conv.getCustomerID().getFullName() : "Customer";
        }
    }

    /**
     * Mark user as online
     */
    public void markUserOnline(Integer userId) {
        if (userId != null) {
            onlineUsers.put(userId, "online");
        }
    }

    /**
     * Mark user as offline
     */
    public void markUserOffline(Integer userId) {
        if (userId != null) {
            onlineUsers.remove(userId);
        }
    }

    /**
     * Check if a user is online
     */
    public boolean isUserOnline(Integer userId) {
        return userId != null && onlineUsers.containsKey(userId);
    }

    /**
     * Broadcast message to WebSocket clients
     */
    private void broadcastMessage(Integer conversationId, ChatMessages message) {
        System.out.println("ChatController.broadcastMessage: Broadcasting to conversation " + conversationId);
    }

    // Getters and Setters

    public Users getCurrentUser() {
        if (currentUser == null) {
            refreshCurrentUser();
        }
        return currentUser;
    }

    public String getCurrentUserRole() {
        if (currentUserRole == null) {
            refreshCurrentUser();
        }
        return currentUserRole;
    }

    public Integer getCurrentCustomerId() {
        return currentCustomerId;
    }

    public Integer getCurrentStaffId() {
        return currentStaffId;
    }

    public ChatConversations getSelectedConversation() {
        return selectedConversation;
    }

    public void setSelectedConversation(ChatConversations selectedConversation) {
        this.selectedConversation = selectedConversation;
    }

    public Integer getSelectedConversationId() {
        return selectedConversationId;
    }

    public void setSelectedConversationId(Integer selectedConversationId) {
        this.selectedConversationId = selectedConversationId;
    }

    public String getNewMessage() {
        return newMessage;
    }

    public void setNewMessage(String newMessage) {
        this.newMessage = newMessage;
    }

    public int getUnreadCount() {
        loadUnreadCount();
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    /**
     * Load pending chat requests for current staff member
     * Returns list of conversations where requestStatus = 'PENDING'
     */
    public List<ChatConversations> getPendingCustomers() {
        try {
            refreshCurrentUser();

            // Force reload staffId if null
            if ("STAFF".equals(currentUserRole) && currentStaffId == null && currentUser != null) {
                try {
                    Staffs s = staffsFacade.findByUserID(currentUser.getUserID());
                    if (s != null) {
                        currentStaffId = s.getStaffID();
                        System.out.println("✓ getPendingCustomers: Auto-loaded staffId: " + currentStaffId);
                    }
                } catch (Exception e) {
                    System.out.println("✗ getPendingCustomers: Failed to load staffId: " + e.getMessage());
                }
            }

            if (!"STAFF".equals(currentUserRole) || currentStaffId == null) {
                System.out.println("✗ getPendingCustomers: Not staff or staffId is null - returning empty");
                pendingCustomers = new ArrayList<>();
                return pendingCustomers;
            }

            // Find all conversations with RequestStatus = 'PENDING' for any staff
            // (not filtered by current staff yet - all staff see all pending)
            // This is configurable based on your business logic
            pendingCustomers = conversationsFacade.findPendingRequests();
            System.out.println("✓ getPendingCustomers: Found " + (pendingCustomers != null ? pendingCustomers.size() : 0) + " pending requests");

            return pendingCustomers != null ? pendingCustomers : new ArrayList<>();

        } catch (Exception e) {
            System.out.println("ChatController.getPendingCustomers: Error - " + e.getMessage());
            e.printStackTrace();
            pendingCustomers = new ArrayList<>();
            return pendingCustomers;
        }
    }

    /**
     * Pre-render listener to initialize chat data for staff
     */
    public void initializeChatData(jakarta.faces.event.ComponentSystemEvent event) {
        if ("STAFF".equals(currentUserRole)) {
            getPendingCustomers();
            getConversations();
            loadUnreadCount();
        }
    }

    /**
     * Reset customer chat state - ensures Start Chat button shows only when no pending/active chat
     */
    public void resetCustomerChatState() {
        refreshCurrentUser();
        
        if ("CUSTOMER".equals(currentUserRole)) {
            System.out.println("=== resetCustomerChatState called for customer: " + currentCustomerId);
            
            if (currentCustomerId != null) {
                // Load ACCEPTED conversations
                conversations = conversationsFacade.findAcceptedByCustomerID(currentCustomerId);
                
                // Also check if there's a PENDING conversation
                List<ChatConversations> allConversations = conversationsFacade.findActiveByCustomerID(currentCustomerId);
                System.out.println("Total active conversations: " + (allConversations != null ? allConversations.size() : 0));
                
                boolean hasPendingChat = false;
                
                if (allConversations != null) {
                    for (ChatConversations conv : allConversations) {
                        System.out.println("  - Conv ID: " + conv.getConversationID() + ", Status: " + conv.getRequestStatus());
                        
                        if ("PENDING".equals(conv.getRequestStatus())) {
                            System.out.println("    Found PENDING conversation!");
                            hasPendingChat = true;
                            selectedConversation = conv;
                            selectedConversationId = conv.getConversationID();
                            chatRequestPending = true;
                            break;
                        }
                    }
                }
                
                System.out.println("hasPendingChat: " + hasPendingChat + ", chatRequestPending now: " + chatRequestPending);
                
                // If no PENDING, check for ACCEPTED
                if (!hasPendingChat) {
                    if (conversations == null || conversations.isEmpty()) {
                        selectedConversation = null;
                        selectedConversationId = null;
                        chatRequestPending = false;
                    } else {
                        selectedConversation = conversations.get(0);
                        selectedConversationId = selectedConversation.getConversationID();
                        chatRequestPending = false;
                    }
                }
            }
            
            loadUnreadCount();
        }
    }

    /**
     * Accept a chat request from customer
     * Uses atomic claimConversation to prevent race conditions
     * Updates conversation status to ACCEPTED and assigns acceptedStaffID (not staffID)
     */
    public String acceptChatRequest(Integer conversationId) {
        try {
            refreshCurrentUser();

            if (!"STAFF".equals(currentUserRole) || currentStaffId == null) {
                return null;
            }

            if (conversationId == null) {
                return null;
            }

            // Attempt to claim the conversation atomically
            boolean claimed = conversationsFacade.claimConversation(conversationId, currentStaffId);
            
            if (!claimed) {
                // Another staff member already accepted this request
                System.out.println("✗ Could not claim conversation " + conversationId + " - another staff accepted it first");
                loadConversations();
                loadUnreadCount();
                pendingCustomers = null;
                FacesContext.getCurrentInstance().addMessage(null,
                    new jakarta.faces.application.FacesMessage(
                        jakarta.faces.application.FacesMessage.SEVERITY_WARN,
                        "Chat already accepted", "Another staff member accepted this request first."
                    ));
                return null;
            }

            ChatConversations conversation = conversationsFacade.find(conversationId);
            if (conversation != null) {
                // Add system message
                ChatMessages systemMsg = new ChatMessages();
                systemMsg.setConversationID(conversation);
                systemMsg.setSenderRole("SYSTEM");
                systemMsg.setSenderUserID(currentUser.getUserID());
                systemMsg.setContent("Staff member has accepted your chat request");
                systemMsg.setSentAt(new Date());
                systemMsg.setIsRead(false);
                systemMsg.setMessageType("SYSTEM");
                messagesFacade.create(systemMsg);

                System.out.println("✓ Staff " + currentStaffId + " claimed conversation " + conversationId);
            }

            // Reload lists
            loadConversations();
            loadUnreadCount();
            pendingCustomers = null;

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reject a chat request from customer
     * Marks this staff as rejecting, checks if all staff have rejected
     * Only sets status to REJECTED if all active staff rejected
     */
    public String rejectChatRequest(Integer conversationId) {
        try {
            refreshCurrentUser();

            if (!"STAFF".equals(currentUserRole) || currentStaffId == null) {
                return null;
            }

            if (conversationId == null) {
                return null;
            }

            ChatConversations conversation = conversationsFacade.find(conversationId);
            if (conversation == null) {
                return null;
            }

            // Add current staff to rejectedStaffIDs list
            String rejectedIds = conversation.getRejectedStaffIDs();
            if (rejectedIds == null || rejectedIds.trim().isEmpty()) {
                rejectedIds = String.valueOf(currentStaffId);
            } else if (!rejectedIds.contains(String.valueOf(currentStaffId))) {
                rejectedIds += "," + currentStaffId;
            }
            conversation.setRejectedStaffIDs(rejectedIds);

            // Check if all active staff have rejected
            long totalActiveStaff = staffsFacade.countActiveStaff();
            int rejectionCount = (rejectedIds != null) ? rejectedIds.split(",").length : 0;

            if (rejectionCount >= totalActiveStaff && totalActiveStaff > 0) {
                // All staff have rejected
                conversation.setRequestStatus("REJECTED");
                
                // Add system message: rejected by all
                ChatMessages systemMsg = new ChatMessages();
                systemMsg.setConversationID(conversation);
                systemMsg.setSenderRole("SYSTEM");
                systemMsg.setSenderUserID(currentUser.getUserID());
                systemMsg.setContent("All staff members are currently busy. Please try again later or contact support.");
                systemMsg.setSentAt(new Date());
                systemMsg.setIsRead(false);
                systemMsg.setMessageType("SYSTEM");
                messagesFacade.create(systemMsg);
                
                System.out.println("✓ Conversation " + conversationId + " marked REJECTED (all " + totalActiveStaff + " staff rejected)");
            } else {
                // Still PENDING - other staff can still accept
                conversation.setRequestStatus("PENDING");
                System.out.println("✓ Staff " + currentStaffId + " rejected conversation " + conversationId + 
                    " (" + rejectionCount + "/" + totalActiveStaff + " active staff rejected)");
            }

            conversationsFacade.edit(conversation);

            // Reload
            loadConversations();
            loadUnreadCount();
            pendingCustomers = null;

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Getter for pendingCustomers
    public void setPendingCustomers(List<ChatConversations> pending) {
        this.pendingCustomers = pending;
    }

    public void refreshConversations() {
        refreshCurrentUser();
        loadConversations();
        loadUnreadCount();
    }

    /**
     * Start a chat request from customer
     * Sets request status to PENDING and auto-reject timer
     */
    public String startChatRequest() {
        try {
            System.out.println("=== ChatController.startChatRequest() called ===");
            refreshCurrentUser();
            
            System.out.println("Current role: " + currentUserRole + ", customerId: " + currentCustomerId);

            if (!"CUSTOMER".equals(currentUserRole) || currentCustomerId == null) {
                System.out.println("NOT A CUSTOMER or customerId is null, returning");
                return null;
            }

            System.out.println("Creating chat request conversation...");

            ChatConversations conversation = new ChatConversations();
            conversation.setCustomerID(customersFacade.find(currentCustomerId));
            // Use system/default staff (ID=1) since DB requires non-null StaffID
            // Will be replaced when an actual staff accepts the request
            conversation.setStaffID(staffsFacade.find(1));
            conversation.setCreatedAt(new Date());
            conversation.setRequestStatus("PENDING");
            conversation.setStatus("ACTIVE");

            // Calculate auto-reject time (5 minutes from now)
            Date autoRejectTime = new Date(System.currentTimeMillis() + 5 * 60 * 1000);
            conversation.setAutoRejectTime(autoRejectTime);

            // Create conversation
            conversationsFacade.create(conversation);
            selectedConversation = conversation;
            selectedConversationId = conversation.getConversationID();
            
            System.out.println("Conversation created: ID = " + conversation.getConversationID());

            // Add system message to conversation
            ChatMessages systemMsg = new ChatMessages();
            systemMsg.setConversationID(conversation);
            systemMsg.setSenderRole("CUSTOMER");
            systemMsg.setSenderUserID(currentUser.getUserID());
            systemMsg.setContent("Chat request sent");
            systemMsg.setSentAt(new Date());
            systemMsg.setIsRead(false);
            systemMsg.setMessageType("SYSTEM");
            messagesFacade.create(systemMsg);

            // Update chat status flags
            chatRequestPending = true;
            chatRequestRejected = false;

            // Reload
            loadConversations();
            loadUnreadCount();
            
            System.out.println("=== Chat request completed successfully ===");

            return null;

        } catch (Exception e) {
            System.out.println("=== ERROR in startChatRequest: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    /**
     * End the chat from customer side ONLY
     * Sets conversation status to CLOSED
     * Staff cannot call this endpoint
     */
    public String endChat() {
        try {
            refreshCurrentUser();

            // Only CUSTOMER can end chat
            if (!"CUSTOMER".equals(currentUserRole)) {
                System.out.println("⚠️ EndChat called by non-customer role: " + currentUserRole);
                return null;
            }

            if (selectedConversation != null) {
                selectedConversation.setRequestStatus("CLOSED");
                selectedConversation.setStatus("INACTIVE");
                conversationsFacade.edit(selectedConversation);

                // Add system message
                ChatMessages systemMsg = new ChatMessages();
                systemMsg.setConversationID(selectedConversation);
                systemMsg.setSenderRole("SYSTEM");
                systemMsg.setSenderUserID(currentUser.getUserID());
                systemMsg.setContent("- This Chat Ended -");
                systemMsg.setSentAt(new Date());
                systemMsg.setIsRead(false);
                systemMsg.setMessageType("SYSTEM");
                messagesFacade.create(systemMsg);
                
                System.out.println("✓ Chat ended by customer: conversationId=" + selectedConversation.getConversationID());
            }

            // Reset state
            selectedConversation = null;
            selectedConversationId = null;
            newMessage = "";
            chatRequestPending = false;
            chatRequestRejected = false;

            // Reload
            loadConversations();
            loadUnreadCount();

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if current conversation has a pending chat request
     */
    public boolean isChatRequestPending() {
        return chatRequestPending;
    }

    /**
     * Check if chat request was rejected by all staff
     */
    public boolean isChatRequestRejected() {
        return chatRequestRejected;
    }

    // Getters and Setters for chat status flags
    public boolean getChatRequestPending() {
        return chatRequestPending;
    }

    public void setChatRequestPending(boolean pending) {
        this.chatRequestPending = pending;
    }

    public boolean getChatRequestRejected() {
        return chatRequestRejected;
    }

    public void setChatRequestRejected(boolean rejected) {
        this.chatRequestRejected = rejected;
    }
}
