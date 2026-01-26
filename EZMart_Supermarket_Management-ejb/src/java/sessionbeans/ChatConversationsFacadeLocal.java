package sessionbeans;

import entityclass.ChatConversations;
import jakarta.ejb.Local;
import java.util.List;

/**
 * Local interface for ChatConversationsFacade
 */
@Local
public interface ChatConversationsFacadeLocal {

    void create(ChatConversations chatConversations);

    void edit(ChatConversations chatConversations);

    void remove(ChatConversations chatConversations);

    ChatConversations find(Object id);

    List<ChatConversations> findAll();

    List<ChatConversations> findRange(int[] range);

    int count();

    ChatConversations findByCustomerAndStaff(Integer customerID, Integer staffID);

    List<ChatConversations> findByCustomerID(Integer customerID);

    List<ChatConversations> findByStaffID(Integer staffID);

    List<ChatConversations> findActiveByStaffID(Integer staffID);

    List<ChatConversations> findActiveByCustomerID(Integer customerID);

    /**
     * Find only ACCEPTED conversations for customer
     */
    List<ChatConversations> findAcceptedByCustomerID(Integer customerID);

    void updateLastMessageAt(Integer conversationID);

    ChatConversations findLatestActiveByCustomer(Integer customerId);

    Integer pickLeastLoadedActiveStaffId();

    List<ChatConversations> findPendingRequests();

    /**
     * Find pending requests for a specific staff member (excluding ones they rejected)
     */
    List<ChatConversations> findPendingRequestsForStaff(Integer staffId);

    /**
     * Find conversations accepted by a specific staff member
     */
    List<ChatConversations> findAcceptedByStaff(Integer staffId);

    /**
     * Atomically claim a conversation for a staff member
     */
    boolean claimConversation(Integer conversationId, Integer staffId);

}
