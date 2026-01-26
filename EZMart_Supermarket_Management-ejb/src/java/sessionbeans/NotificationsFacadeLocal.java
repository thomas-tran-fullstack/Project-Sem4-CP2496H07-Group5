package sessionbeans;

import entityclass.Notifications;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface NotificationsFacadeLocal {
    void create(Notifications notifications);
    void edit(Notifications notifications);
    void remove(Notifications notifications);
    Notifications find(Object id);
    List<Notifications> findAll();
    List<Notifications> findRange(int[] range);
    int count();
    List<Notifications> findByRecipient(Integer userID);
    List<Notifications> findUnread(Integer userID);
    Long countUnread(Integer userID);
    List<Notifications> findByType(Integer userID, String type);
    List<Notifications> findByRecipientPaginated(Integer userID, int firstResult, int maxResults);
    void markAsRead(Long notificationID);
    void markAllAsRead(Integer userID);
    void deleteNotification(Long notificationID);
}
