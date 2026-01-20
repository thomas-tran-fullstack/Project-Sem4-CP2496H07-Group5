package sessionbeans;

import entityclass.LiveProductDiscount;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author EZMart Team
 */
@Stateless
public class LiveProductDiscountFacade extends AbstractFacade<LiveProductDiscount> {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public LiveProductDiscountFacade() {
        super(LiveProductDiscount.class);
    }

    public List<LiveProductDiscount> findByLiveProductID(Integer liveProductID) {
        return em.createNamedQuery("LiveProductDiscount.findByLiveProductID")
                .setParameter("liveProductID", liveProductID)
                .getResultList();
    }

    public LiveProductDiscount getLatestPriceChange(Integer liveProductID) {
        try {
            List<LiveProductDiscount> results = em.createNamedQuery("LiveProductDiscount.findByLiveProductID")
                    .setParameter("liveProductID", liveProductID)
                    .setMaxResults(1)
                    .getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            return null;
        }
    }

}
