package sessionbeans;

import entityclass.Notifications;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

@Stateless
public class NotificationsFacade extends AbstractFacade<Notifications> implements NotificationsFacadeLocal {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public NotificationsFacade() {
        super(Notifications.class);
    }

    public List<Notifications> findByRecipient(Integer userID) {
        Query query = em.createNamedQuery("Notifications.findByRecipient");
        query.setParameter("userID", userID);
        return query.getResultList();
    }

    public List<Notifications> findUnread(Integer userID) {
        Query query = em.createNamedQuery("Notifications.findUnread");
        query.setParameter("userID", userID);
        return query.getResultList();
    }

    public Long countUnread(Integer userID) {
        Query query = em.createNamedQuery("Notifications.countUnread");
        query.setParameter("userID", userID);
        return (Long) query.getSingleResult();
    }

    public List<Notifications> findByType(Integer userID, String type) {
        Query query = em.createNamedQuery("Notifications.findByType");
        query.setParameter("userID", userID);
        query.setParameter("type", type);
        return query.getResultList();
    }

    public List<Notifications> findByRecipientPaginated(Integer userID, int firstResult, int maxResults) {
        Query query = em.createNamedQuery("Notifications.findByRecipient");
        query.setParameter("userID", userID);
        query.setFirstResult(firstResult);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    public void markAsRead(Long notificationID) {
        Notifications notification = em.find(Notifications.class, notificationID);
        if (notification != null && !notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(new java.util.Date());
            em.merge(notification);
        }
    }

    public void markAllAsRead(Integer userID) {
        Query query = em.createNamedQuery("Notifications.findUnread");
        query.setParameter("userID", userID);
        List<Notifications> unreadNotifications = query.getResultList();
        java.util.Date now = new java.util.Date();
        for (Notifications notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadAt(now);
            em.merge(notification);
        }
    }

    public void deleteNotification(Long notificationID) {
        Notifications notification = em.find(Notifications.class, notificationID);
        if (notification != null) {
            remove(notification);
        }
    }
}
