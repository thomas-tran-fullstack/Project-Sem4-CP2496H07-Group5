package sessionbeans;

import entityclass.PostShares;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface PostSharesFacadeLocal {

    void create(PostShares postShares);

    void edit(PostShares postShares);

    void remove(PostShares postShares);

    PostShares find(Object id);

    List<PostShares> findAll();

    List<PostShares> findRange(int[] range);

    int count();

    List<PostShares> findByPostID(Long postID);

    Long countByPostID(Long postID);

    PostShares findByUserAndPost(Integer userID, Long postID);

    List<PostShares> findByUserID(Integer userID);

    void sharePost(Long postID, Integer userID, Integer customerID, String shareMessage, Boolean facebookShare, Boolean twitterShare, Boolean whatsappShare);
}
