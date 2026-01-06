package sessionbeans;

import entityclass.PersistentLogins;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

@Stateless
public class PersistentLoginsFacade extends AbstractFacade<PersistentLogins> implements PersistentLoginsFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PersistentLoginsFacade() {
        super(PersistentLogins.class);
    }

    @Override
    public PersistentLogins findBySelector(String selector) {
        try {
            TypedQuery<PersistentLogins> query = em.createQuery(
                    "SELECT p FROM PersistentLogins p WHERE p.selector = :selector",
                    PersistentLogins.class);
            query.setParameter("selector", selector);
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<PersistentLogins> findByUser(Integer userID) {
        TypedQuery<PersistentLogins> query = em.createQuery(
                "SELECT p FROM PersistentLogins p WHERE p.userID.userID = :uid ORDER BY p.expiresAt DESC",
                PersistentLogins.class);
        query.setParameter("uid", userID);
        return query.getResultList();
    }

    @Override
    public void deleteExpired() {
        em.createQuery("DELETE FROM PersistentLogins p WHERE p.expiresAt < CURRENT_TIMESTAMP")
                .executeUpdate();
    }
}
