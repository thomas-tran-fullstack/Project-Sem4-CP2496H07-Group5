package sessionbeans;

import entityclass.PostComments;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

@Stateless
public class PostCommentsFacade extends AbstractFacade<PostComments> implements PostCommentsFacadeLocal {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PostCommentsFacade() {
        super(PostComments.class);
    }

    public List<PostComments> findByPostID(Long postID) {
        Query query = em.createNamedQuery("PostComments.findByPostID");
        query.setParameter("postID", postID);
        return query.getResultList();
    }

    public Long countByPostID(Long postID) {
        Query query = em.createNamedQuery("PostComments.countByPostID");
        query.setParameter("postID", postID);
        return (Long) query.getSingleResult();
    }

    public List<PostComments> findByUserID(Integer userID) {
        Query query = em.createNamedQuery("PostComments.findByUserID");
        query.setParameter("userID", userID);
        return query.getResultList();
    }

    public List<PostComments> findRecentComments(int limit) {
        Query query = em.createNamedQuery("PostComments.findRecentComments");
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public List<PostComments> findByPostIDPaginated(Long postID, int firstResult, int maxResults) {
        Query query = em.createNamedQuery("PostComments.findByPostID");
        query.setParameter("postID", postID);
        query.setFirstResult(firstResult);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    public void deleteComment(Long commentID) {
        PostComments comment = em.find(PostComments.class, commentID);
        if (comment != null) {
            comment.setIsDeleted(true);
            em.merge(comment);
        }
    }
}
