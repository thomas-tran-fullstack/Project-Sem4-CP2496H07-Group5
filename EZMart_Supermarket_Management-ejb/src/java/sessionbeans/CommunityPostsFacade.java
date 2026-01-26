package sessionbeans;

import entityclass.CommunityPosts;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

@Stateless
public class CommunityPostsFacade extends AbstractFacade<CommunityPosts> implements CommunityPostsFacadeLocal {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CommunityPostsFacade() {
        super(CommunityPosts.class);
    }

    public List<CommunityPosts> findByStatus(String status) {
        Query query = em.createNamedQuery("CommunityPosts.findByStatus");
        query.setParameter("status", status);
        return query.getResultList();
    }

    public List<CommunityPosts> findByCustomerID(Integer customerID) {
        Query query = em.createNamedQuery("CommunityPosts.findByCustomerID");
        query.setParameter("customerID", customerID);
        return query.getResultList();
    }

    public List<CommunityPosts> findApprovedPosts() {
        Query query = em.createNamedQuery("CommunityPosts.findApprovedPosts");
        return query.getResultList();
    }

    public List<CommunityPosts> findPendingPosts() {
        Query query = em.createNamedQuery("CommunityPosts.findPendingPosts");
        return query.getResultList();
    }

    public List<CommunityPosts> findApprovedPosts(int maxResults) {
        Query query = em.createNamedQuery("CommunityPosts.findApprovedPosts");
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    public List<CommunityPosts> findApprovedPosts(int firstResult, int maxResults) {
        Query query = em.createNamedQuery("CommunityPosts.findApprovedPosts");
        query.setFirstResult(firstResult);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }
}
