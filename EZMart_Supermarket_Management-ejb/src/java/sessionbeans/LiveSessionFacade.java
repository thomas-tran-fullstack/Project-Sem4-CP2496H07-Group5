package sessionbeans;

import entityclass.LiveSession;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 *
 * @author EZMart Team
 */
@Stateless
public class LiveSessionFacade extends AbstractFacade<LiveSession> {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public LiveSessionFacade() {
        super(LiveSession.class);
    }

    public List<LiveSession> findActive() {
        return em.createNamedQuery("LiveSession.findActive").getResultList();
    }

    public List<LiveSession> findByStaff(Integer staffID) {
        return em.createNamedQuery("LiveSession.findByStaffID")
                .setParameter("staffID", staffID)
                .getResultList();
    }

    public List<LiveSession> findByStatus(String status) {
        return em.createNamedQuery("LiveSession.findByStatus")
                .setParameter("status", status)
                .getResultList();
    }

    public List<LiveSession> findByScheduledTime(Date startDate, Date endDate) {
        return em.createNamedQuery("LiveSession.findByScheduledTime")
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();
    }

    public LiveSession findByStreamKey(String streamKey) {
        try {
            Query query = em.createQuery("SELECT l FROM LiveSession l WHERE l.streamKey = :streamKey");
            query.setParameter("streamKey", streamKey);
            return (LiveSession) query.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public int countActiveSessions() {
        try {
            Long count = (Long) em.createQuery("SELECT COUNT(l) FROM LiveSession l WHERE l.status = 'ACTIVE'")
                    .getSingleResult();
            return count.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    public int countViewersAcrossActiveSessions() {
        try {
            Object result = em.createQuery("SELECT SUM(l.currentViewers) FROM LiveSession l WHERE l.status = 'ACTIVE'")
                    .getSingleResult();
            return result != null ? ((Number) result).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

}
