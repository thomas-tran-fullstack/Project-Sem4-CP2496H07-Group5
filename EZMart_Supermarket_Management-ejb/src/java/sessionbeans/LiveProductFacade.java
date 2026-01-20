package sessionbeans;

import entityclass.LiveProduct;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author EZMart Team
 */
@Stateless
public class LiveProductFacade extends AbstractFacade<LiveProduct> {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public LiveProductFacade() {
        super(LiveProduct.class);
    }

    public List<LiveProduct> findBySessionID(Integer sessionID) {
        return em.createNamedQuery("LiveProduct.findBySessionID")
                .setParameter("sessionID", sessionID)
                .getResultList();
    }

    public List<LiveProduct> findBySessionIDAll(Integer sessionID) {
        return em.createNamedQuery("LiveProduct.findBySessionIDAll")
                .setParameter("sessionID", sessionID)
                .getResultList();
    }

    public List<LiveProduct> findByProductID(Integer productID) {
        return em.createNamedQuery("LiveProduct.findByProductID")
                .setParameter("productID", productID)
                .getResultList();
    }

    public LiveProduct findActiveBySessionAndProduct(Integer sessionID, Integer productID) {
        try {
            return (LiveProduct) em.createQuery(
                    "SELECT l FROM LiveProduct l WHERE l.sessionID.sessionID = :sessionID " +
                    "AND l.productID.productID = :productID AND l.isActive = TRUE"
            )
                    .setParameter("sessionID", sessionID)
                    .setParameter("productID", productID)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public List<LiveProduct> findBySessionIDActive(Integer sessionID) {
        return em.createQuery(
                "SELECT l FROM LiveProduct l WHERE l.sessionID.sessionID = :sessionID " +
                "AND l.isActive = TRUE ORDER BY l.addedAt DESC",
                LiveProduct.class
        )
                .setParameter("sessionID", sessionID)
                .getResultList();
    }

    public int getTotalSalesCountBySession(Integer sessionID) {
        try {
            Long count = (Long) em.createQuery(
                    "SELECT SUM(l.salesCount) FROM LiveProduct l WHERE l.sessionID.sessionID = :sessionID"
            )
                    .setParameter("sessionID", sessionID)
                    .getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

}
