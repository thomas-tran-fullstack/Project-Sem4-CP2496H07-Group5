package sessionbeans;

import entityclass.PostShares;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.Date;
import java.util.List;

@Stateless
public class PostSharesFacade extends AbstractFacade<PostShares> implements PostSharesFacadeLocal {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PostSharesFacade() {
        super(PostShares.class);
    }

    public List<PostShares> findByPostID(Long postID) {
        Query query = em.createNamedQuery("PostShares.findByPostID");
        query.setParameter("postID", postID);
        return query.getResultList();
    }

    public List<PostShares> findByUserID(Integer userID) {
        Query query = em.createNamedQuery("PostShares.findByUserID");
        query.setParameter("userID", userID);
        return query.getResultList();
    }

    public PostShares findByUserAndPost(Integer userID, Long postID) {
        Query query = em.createNamedQuery("PostShares.findByUserAndPost");
        query.setParameter("userID", userID);
        query.setParameter("postID", postID);
        List<PostShares> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public Long countByPostID(Long postID) {
        Query query = em.createNamedQuery("PostShares.countByPostID");
        query.setParameter("postID", postID);
        return (Long) query.getSingleResult();
    }

    public void sharePost(Long postID, Integer userID, Integer customerID, String message, 
                         Boolean facebook, Boolean twitter, Boolean whatsapp) {
        PostShares share = new PostShares();
        share.setPostID(postID);
        share.setUserID(userID);
        share.setCustomerID(customerID);
        share.setSharedMessage(message);
        share.setIsSharedToFacebook(facebook != null ? facebook : false);
        share.setIsSharedToTwitter(twitter != null ? twitter : false);
        share.setIsSharedToWhatsapp(whatsapp != null ? whatsapp : false);
        share.setCreatedAt(new Date());
        create(share);
    }
}
