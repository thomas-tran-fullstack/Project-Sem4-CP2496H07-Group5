package sessionbeans;

import entityclass.CommentLikes;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface CommentLikesFacadeLocal {

    void create(CommentLikes commentLikes);

    void edit(CommentLikes commentLikes);

    void remove(CommentLikes commentLikes);

    CommentLikes find(Object id);

    List<CommentLikes> findAll();

    List<CommentLikes> findRange(int[] range);

    int count();

    List<CommentLikes> findByCommentID(Long commentID);

    Long countByCommentID(Long commentID);

    CommentLikes findByUserAndComment(Integer userID, Long commentID);

    List<CommentLikes> findByUserID(Integer userID);

    boolean userLikedComment(Integer userID, Long commentID);

    void likeComment(Long commentID, Integer userID, Integer customerID);

    void unlikeComment(Integer userID, Long commentID);
}
