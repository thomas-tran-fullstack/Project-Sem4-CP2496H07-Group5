package sessionbeans;

import entityclass.ChatConversations;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 * Session Bean for ChatConversations - provides database operations for chat conversations
 */
@Stateless
public class ChatConversationsFacade extends AbstractFacade<ChatConversations> implements ChatConversationsFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    public ChatConversationsFacade() {
        super(ChatConversations.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public ChatConversations findByCustomerAndStaff(Integer customerID, Integer staffID) {
        try {
            Query query = em.createNamedQuery("ChatConversations.findByCustomerAndStaff");
            query.setParameter("customerID", customerID);
            query.setParameter("staffID", staffID);
            List<ChatConversations> results = query.getResultList();
            if (results != null && !results.isEmpty()) {
                return results.get(0);
            }
        } catch (Exception e) {
            System.out.println("ChatConversationsFacade.findByCustomerAndStaff: Error - " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<ChatConversations> findByCustomerID(Integer customerID) {
        try {
            Query query = em.createNamedQuery("ChatConversations.findByCustomerID");
            query.setParameter("customerID", customerID);
            return query.getResultList();
        } catch (Exception e) {
            System.out.println("ChatConversationsFacade.findByCustomerID: Error - " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<ChatConversations> findByStaffID(Integer staffID) {
        try {
            Query query = em.createNamedQuery("ChatConversations.findByStaffID");
            query.setParameter("staffID", staffID);
            return query.getResultList();
        } catch (Exception e) {
            System.out.println("ChatConversationsFacade.findByStaffID: Error - " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<ChatConversations> findActiveByStaffID(Integer staffID) {
        try {
            Query query = em.createNamedQuery("ChatConversations.findActiveByStaffID");
            query.setParameter("staffID", staffID);
            query.setParameter("status", "ACTIVE");
            return query.getResultList();
        } catch (Exception e) {
            System.out.println("ChatConversationsFacade.findActiveByStaffID: Error - " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<ChatConversations> findActiveByCustomerID(Integer customerID) {
        try {
            Query query = em.createNamedQuery("ChatConversations.findActiveByCustomerID");
            query.setParameter("customerID", customerID);
            query.setParameter("status", "ACTIVE");
            return query.getResultList();
        } catch (Exception e) {
            System.out.println("ChatConversationsFacade.findActiveByCustomerID: Error - " + e.getMessage());
            return null;
        }
    }

    /**
     * Find only ACCEPTED conversations for customer (for widget display)
     */
    public List<ChatConversations> findAcceptedByCustomerID(Integer customerID) {
        try {
            Query query = em.createQuery("SELECT c FROM ChatConversations c WHERE c.customerID.customerID = :customerID AND c.status = 'ACTIVE' AND c.requestStatus = 'ACCEPTED'");
            query.setParameter("customerID", customerID);
            return query.getResultList();
        } catch (Exception e) {
            System.out.println("ChatConversationsFacade.findAcceptedByCustomerID: Error - " + e.getMessage());
            return null;
        }
    }

    public void updateLastMessageAt(Integer conversationID) {
        try {
            Query query = em.createQuery("UPDATE ChatConversations c SET c.lastMessageAt = :now WHERE c.conversationID = :id");
            query.setParameter("now", new Date());
            query.setParameter("id", conversationID);
            query.executeUpdate();
        } catch (Exception e) {
            System.out.println("ChatConversationsFacade.updateLastMessageAt: Error - " + e.getMessage());
        }
    }

    @Override
public ChatConversations findLatestActiveByCustomer(Integer customerId) {
    try {
        return em.createQuery(
                "SELECT c FROM ChatConversations c " +
                "WHERE c.customerID.customerID = :cid AND c.status = 'ACTIVE' " +
                "ORDER BY c.lastMessageAt DESC, c.createdAt DESC",
                ChatConversations.class
        )
        .setParameter("cid", customerId)
        .setMaxResults(1)
        .getSingleResult();
    } catch (Exception e) {
        return null;
    }
}

@Override
public Integer pickLeastLoadedActiveStaffId() {
    // Trả về StaffID có ít conversation ACTIVE nhất
    // Chỉ tính staff đang ACTIVE (staff.status = 'ACTIVE')
    try {
        List<Integer> ids = em.createQuery(
            "SELECT s.staffID " +
            "FROM Staffs s " +
            "WHERE s.status = 'ACTIVE' " +
            "ORDER BY (" +
            "   SELECT COUNT(c) FROM ChatConversations c " +
            "   WHERE c.staffID.staffID = s.staffID AND c.status = 'ACTIVE'" +
            ") ASC, s.staffID ASC",
            Integer.class
        )
        .setMaxResults(1)
        .getResultList();

        return (ids == null || ids.isEmpty()) ? null : ids.get(0);
    } catch (Exception e) {
        return null;
    }
}

@Override
public List<ChatConversations> findPendingRequests() {
    try {
        return em.createQuery(
            "SELECT c FROM ChatConversations c " +
            "WHERE c.requestStatus = 'PENDING' " +
            "ORDER BY c.createdAt ASC",
            ChatConversations.class
        )
        .getResultList();
    } catch (Exception e) {
        System.out.println("ChatConversationsFacade.findPendingRequests: Error - " + e.getMessage());
        return null;
    }
}

/**
 * Find pending requests for a specific staff member
 * Excludes conversations that this staff has already rejected
 */
public List<ChatConversations> findPendingRequestsForStaff(Integer staffId) {
    try {
        return em.createQuery(
            "SELECT c FROM ChatConversations c " +
            "WHERE c.requestStatus = 'PENDING' " +
            "AND (c.rejectedStaffIDs IS NULL OR CONCAT(',', c.rejectedStaffIDs, ',') NOT LIKE :pat) " +
            "ORDER BY c.createdAt ASC",
            ChatConversations.class
        )
        .setParameter("pat", "%," + staffId + ",%")
        .getResultList();
    } catch (Exception e) {
        System.out.println("ChatConversationsFacade.findPendingRequestsForStaff: Error - " + e.getMessage());
        return null;
    }
}

/**
 * Find conversations accepted by a specific staff member
 */
public List<ChatConversations> findAcceptedByStaff(Integer staffId) {
    try {
        return em.createQuery(
            "SELECT c FROM ChatConversations c " +
            "WHERE c.requestStatus='ACCEPTED' AND c.acceptedStaffID=:sid " +
            "ORDER BY c.lastMessageAt DESC",
            ChatConversations.class
        )
        .setParameter("sid", staffId)
        .getResultList();
    } catch (Exception e) {
        System.out.println("ChatConversationsFacade.findAcceptedByStaff: Error - " + e.getMessage());
        return null;
    }
}

/**
 * Attempt to claim a conversation for a staff member
 * Returns true only if successfully updated (atomic operation to prevent race conditions)
 */
public boolean claimConversation(Integer conversationId, Integer staffId) {
    try {
        int updated = em.createQuery(
            "UPDATE ChatConversations c " +
            "SET c.requestStatus='ACCEPTED', c.acceptedStaffID=:sid, c.chatStartTime=:now " +
            "WHERE c.conversationID=:cid AND c.requestStatus='PENDING' AND c.acceptedStaffID IS NULL"
        )
        .setParameter("sid", staffId)
        .setParameter("cid", conversationId)
        .setParameter("now", new Date())
        .executeUpdate();

        return updated == 1;
    } catch (Exception e) {
        System.out.println("ChatConversationsFacade.claimConversation: Error - " + e.getMessage());
        return false;
    }
}
}


