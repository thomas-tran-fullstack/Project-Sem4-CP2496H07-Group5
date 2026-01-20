package sessionbeans;

import entityclass.LiveSessionStat;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author EZMart Team
 */
@Stateless
public class LiveSessionStatFacade extends AbstractFacade<LiveSessionStat> {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public LiveSessionStatFacade() {
        super(LiveSessionStat.class);
    }

    public List<LiveSessionStat> findBySessionID(Integer sessionID) {
        return em.createNamedQuery("LiveSessionStat.findBySessionID")
                .setParameter("sessionID", sessionID)
                .getResultList();
    }

    public LiveSessionStat findLatestBySessionID(Integer sessionID) {
        try {
            List<LiveSessionStat> results = em.createNamedQuery("LiveSessionStat.findLatestBySessionID")
                    .setParameter("sessionID", sessionID)
                    .setMaxResults(1)
                    .getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    public int getPeakViewersBySession(Integer sessionID) {
        try {
            Object result = em.createQuery(
                    "SELECT MAX(l.activeViewers) FROM LiveSessionStat l WHERE l.sessionID.sessionID = :sessionID"
            )
                    .setParameter("sessionID", sessionID)
                    .getSingleResult();
            return result != null ? ((Number) result).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public double getAverageViewersBySession(Integer sessionID) {
        try {
            Object result = em.createQuery(
                    "SELECT AVG(l.activeViewers) FROM LiveSessionStat l WHERE l.sessionID.sessionID = :sessionID"
            )
                    .setParameter("sessionID", sessionID)
                    .getSingleResult();
            return result != null ? ((Number) result).doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

}
