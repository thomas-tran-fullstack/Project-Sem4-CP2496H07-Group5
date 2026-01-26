package sessionbeans;

import entityclass.CommentLikes;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

@Stateless
public class CommentLikesFacade extends AbstractFacade<CommentLikes> implements CommentLikesFacadeLocal {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CommentLikesFacade() {
        super(CommentLikes.class);
    }

    public List<CommentLikes> findByCommentID(Long commentID) {
        Query query = em.createNamedQuery("CommentLikes.findByCommentID");
        query.setParameter("commentID", commentID);
        return query.getResultList();
    }

    public Long countByCommentID(Long commentID) {
        Query query = em.createNamedQuery("CommentLikes.countByCommentID");
        query.setParameter("commentID", commentID);
        return (Long) query.getSingleResult();
    }

    public List<CommentLikes> findByUserID(Integer userID) {
        Query query = em.createNamedQuery("CommentLikes.findByUserID");
        query.setParameter("userID", userID);
        return query.getResultList();
    }

    public CommentLikes findByUserAndComment(Integer userID, Long commentID) {
        Query query = em.createNamedQuery("CommentLikes.findByUserAndComment");
        query.setParameter("userID", userID);
        query.setParameter("commentID", commentID);
        List<CommentLikes> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public boolean userLikedComment(Integer userID, Long commentID) {
        return findByUserAndComment(userID, commentID) != null;
    }

    public void likeComment(Long commentID, Integer userID, Integer customerID) {
        CommentLikes like = new CommentLikes();
        like.setCommentID(commentID);
        like.setUserID(userID);
        like.setCustomerID(customerID);
        create(like);
    }

    public void unlikeComment(Integer userID, Long commentID) {
        CommentLikes like = findByUserAndComment(userID, commentID);
        if (like != null) {
            remove(like);
        }
    }
}
