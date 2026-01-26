package sessionbeans;

import entityclass.PostLikes;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

@Stateless
public class PostLikesFacade extends AbstractFacade<PostLikes> implements PostLikesFacadeLocal {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PostLikesFacade() {
        super(PostLikes.class);
    }

    public List<PostLikes> findByPostID(Long postID) {
        Query query = em.createNamedQuery("PostLikes.findByPostID");
        query.setParameter("postID", postID);
        return query.getResultList();
    }

    public Long countByPostID(Long postID) {
        Query query = em.createNamedQuery("PostLikes.countByPostID");
        query.setParameter("postID", postID);
        return (Long) query.getSingleResult();
    }

    public PostLikes findByUserAndPost(Integer userID, Long postID) {
        Query query = em.createNamedQuery("PostLikes.findByUserAndPost");
        query.setParameter("userID", userID);
        query.setParameter("postID", postID);
        List<PostLikes> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public List<PostLikes> findByUserID(Integer userID) {
        Query query = em.createNamedQuery("PostLikes.findByUserID");
        query.setParameter("userID", userID);
        return query.getResultList();
    }

    public boolean userLikedPost(Integer userID, Long postID) {
        return findByUserAndPost(userID, postID) != null;
    }

    public void likePost(Long postID, Integer userID, Integer customerID) {
        if (!userLikedPost(userID, postID)) {
            PostLikes like = new PostLikes();
            like.setPostID(postID);
            like.setUserID(userID);
            like.setCustomerID(customerID);
            like.setReactionType("LIKE");
            like.setCreatedAt(new java.util.Date());
            create(like);
        }
    }

    public void unlikePost(Integer userID, Long postID) {
        PostLikes like = findByUserAndPost(userID, postID);
        if (like != null) {
            remove(like);
        }
    }
}
