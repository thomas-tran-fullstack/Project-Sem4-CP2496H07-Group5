package sessionbeans;

import entityclass.ChatMessages;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

/**
 * Session Bean for ChatMessages - provides database operations for chat messages
 */
@Stateless
public class ChatMessagesFacade extends AbstractFacade<ChatMessages> implements ChatMessagesFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    public ChatMessagesFacade() {
        super(ChatMessages.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public List<ChatMessages> findByConversationID(Integer conversationID) {
        try {
            Query query = em.createNamedQuery("ChatMessages.findByConversationID");
            query.setParameter("conversationID", conversationID);
            return query.getResultList();
        } catch (Exception e) {
            System.out.println("ChatMessagesFacade.findByConversationID: Error - " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<ChatMessages> findByConversationIDOrdered(Integer conversationID) {
        try {
            Query query = em.createQuery("SELECT m FROM ChatMessages m WHERE m.conversationID.conversationID = :convID ORDER BY m.sentAt ASC");
            query.setParameter("convID", conversationID);
            return query.getResultList();
        } catch (Exception e) {
            System.out.println("ChatMessagesFacade.findByConversationIDOrdered: Error - " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<ChatMessages> findRecentByConversationID(Integer conversationID, int limit) {
        try {
            Query query = em.createQuery("SELECT m FROM ChatMessages m WHERE m.conversationID.conversationID = :convID ORDER BY m.sentAt DESC");
            query.setParameter("convID", conversationID);
            query.setMaxResults(limit);
            List<ChatMessages> result = query.getResultList();
            // Reverse to get chronological order
            if (result != null) {
                java.util.Collections.reverse(result);
            }
            return result;
        } catch (Exception e) {
            System.out.println("ChatMessagesFacade.findRecentByConversationID: Error - " + e.getMessage());
            return null;
        }
    }

    @Override
    public int countUnreadByConversation(Integer conversationID, String excludeRole, Integer excludeUserID) {
        try {
            Query query = em.createQuery("SELECT COUNT(m) FROM ChatMessages m WHERE m.conversationID.conversationID = :convID AND m.senderRole != :role AND m.senderUserID != :userID AND m.isRead = false");
            query.setParameter("convID", conversationID);
            query.setParameter("role", excludeRole);
            query.setParameter("userID", excludeUserID);
            Long result = (Long) query.getSingleResult();
            return result != null ? result.intValue() : 0;
        } catch (Exception e) {
            System.out.println("ChatMessagesFacade.countUnreadByConversation: Error - " + e.getMessage());
            return 0;
        }
    }

    @Override
    public void markAsRead(Integer conversationID, String currentRole, Integer currentUserID) {
        try {
            Query query = em.createQuery("UPDATE ChatMessages m SET m.isRead = true WHERE m.conversationID.conversationID = :convID AND m.senderRole != :role AND m.senderUserID != :userID AND m.isRead = false");
            query.setParameter("convID", conversationID);
            query.setParameter("role", currentRole);
            query.setParameter("userID", currentUserID);
            query.executeUpdate();
        } catch (Exception e) {
            System.out.println("ChatMessagesFacade.markAsRead: Error - " + e.getMessage());
        }
    }
}
