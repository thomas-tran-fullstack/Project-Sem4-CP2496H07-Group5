package sessionbeans;

import entityclass.LiveSessionViewer;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author EZMart Team
 */
@Stateless
public class LiveSessionViewerFacade extends AbstractFacade<LiveSessionViewer> {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public LiveSessionViewerFacade() {
        super(LiveSessionViewer.class);
    }

    public List<LiveSessionViewer> findBySessionID(Integer sessionID) {
        return em.createNamedQuery("LiveSessionViewer.findBySessionID")
                .setParameter("sessionID", sessionID)
                .getResultList();
    }

    public List<LiveSessionViewer> findByCustomerID(Integer customerID) {
        return em.createNamedQuery("LiveSessionViewer.findByCustomerID")
                .setParameter("customerID", customerID)
                .getResultList();
    }

    public List<LiveSessionViewer> findByCustomer(Integer customerId) {
        try {
            return em.createQuery(
                    "SELECT v FROM LiveSessionViewer v WHERE v.customerID = :customerId ORDER BY v.joinedAt DESC",
                    LiveSessionViewer.class
            )
                    .setParameter("customerId", customerId)
                    .getResultList();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    public List<LiveSessionViewer> findActiveViewers(Integer sessionID) {
        return em.createNamedQuery("LiveSessionViewer.findActiveViewers")
                .setParameter("sessionID", sessionID)
                .getResultList();
    }

    public LiveSessionViewer findBySessionAndCustomer(Integer sessionID, Integer customerID) {
        try {
            List<LiveSessionViewer> results = em.createNamedQuery("LiveSessionViewer.findBySessionIDAndCustomerID")
                    .setParameter("sessionID", sessionID)
                    .setParameter("customerID", customerID)
                    .getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    public int getActiveViewerCount(Integer sessionID) {
        try {
            Long count = (Long) em.createQuery(
                    "SELECT COUNT(l) FROM LiveSessionViewer l WHERE l.sessionID.sessionID = :sessionID AND l.leftAt IS NULL"
            )
                    .setParameter("sessionID", sessionID)
                    .getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public int getTotalUniqueViewerCount(Integer sessionID) {
        try {
            Long count = (Long) em.createQuery(
                    "SELECT COUNT(DISTINCT l.customerID) FROM LiveSessionViewer l WHERE l.sessionID.sessionID = :sessionID AND l.customerID IS NOT NULL"
            )
                    .setParameter("sessionID", sessionID)
                    .getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public int getAverageViewDuration(Integer sessionID) {
        try {
            Object result = em.createQuery(
                    "SELECT AVG(l.totalDuration) FROM LiveSessionViewer l WHERE l.sessionID.sessionID = :sessionID AND l.totalDuration IS NOT NULL"
            )
                    .setParameter("sessionID", sessionID)
                    .getSingleResult();
            return result != null ? ((Number) result).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

}
