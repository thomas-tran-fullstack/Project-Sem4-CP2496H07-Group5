package sessionbeans;

import entityclass.PostLikes;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface PostLikesFacadeLocal {
    void create(PostLikes postLikes);
    void edit(PostLikes postLikes);
    void remove(PostLikes postLikes);
    PostLikes find(Object id);
    List<PostLikes> findAll();
    List<PostLikes> findRange(int[] range);
    int count();
    List<PostLikes> findByPostID(Long postID);
    Long countByPostID(Long postID);
    PostLikes findByUserAndPost(Integer userID, Long postID);
    List<PostLikes> findByUserID(Integer userID);
    boolean userLikedPost(Integer userID, Long postID);
    void likePost(Long postID, Integer userID, Integer customerID);
    void unlikePost(Integer userID, Long postID);
}
