package sessionbeans;

import entityclass.PostComments;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface PostCommentsFacadeLocal {
    void create(PostComments postComments);
    void edit(PostComments postComments);
    void remove(PostComments postComments);
    PostComments find(Object id);
    List<PostComments> findAll();
    List<PostComments> findRange(int[] range);
    int count();
    List<PostComments> findByPostID(Long postID);
    Long countByPostID(Long postID);
    List<PostComments> findByUserID(Integer userID);
    List<PostComments> findRecentComments(int limit);
    List<PostComments> findByPostIDPaginated(Long postID, int firstResult, int maxResults);
    void deleteComment(Long commentID);
}
