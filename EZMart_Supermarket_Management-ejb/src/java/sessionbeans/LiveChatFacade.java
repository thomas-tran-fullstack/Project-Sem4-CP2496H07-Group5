package sessionbeans;

import entityclass.LiveChat;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author EZMart Team
 */
@Stateless
public class LiveChatFacade extends AbstractFacade<LiveChat> {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public LiveChatFacade() {
        super(LiveChat.class);
    }

    public List<LiveChat> findBySessionID(Integer sessionID) {
        return em.createNamedQuery("LiveChat.findBySessionID")
                .setParameter("sessionID", sessionID)
                .getResultList();
    }

    public List<LiveChat> findByUserID(Integer userID) {
        return em.createNamedQuery("LiveChat.findByUserID")
                .setParameter("userID", userID)
                .getResultList();
    }

    public List<LiveChat> findRecentMessages(Integer sessionID) {
        return em.createNamedQuery("LiveChat.findRecentMessages")
                .setParameter("sessionID", sessionID)
                .setMaxResults(50)
                .getResultList();
    }

    public int getMessageCountBySession(Integer sessionID) {
        try {
            Long count = (Long) em.createQuery(
                    "SELECT COUNT(l) FROM LiveChat l WHERE l.sessionID.sessionID = :sessionID AND l.isDeleted = FALSE"
            )
                    .setParameter("sessionID", sessionID)
                    .getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public void deleteMessage(Integer chatMessageID, Integer deletedByUserID) {
        try {
            LiveChat chat = find(chatMessageID);
            if (chat != null) {
                chat.setIsDeleted(true);
                if (deletedByUserID != null) {
                    entityclass.Users user = em.find(entityclass.Users.class, deletedByUserID);
                    chat.setDeletedBy(user);
                }
                edit(chat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
