package sessionbeans;

import entityclass.CommunityPosts;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface CommunityPostsFacadeLocal {
    void create(CommunityPosts communityPosts);
    void edit(CommunityPosts communityPosts);
    void remove(CommunityPosts communityPosts);
    CommunityPosts find(Object id);
    List<CommunityPosts> findAll();
    List<CommunityPosts> findRange(int[] range);
    int count();
    List<CommunityPosts> findByStatus(String status);
    List<CommunityPosts> findByCustomerID(Integer customerID);
    List<CommunityPosts> findApprovedPosts();
    List<CommunityPosts> findApprovedPosts(int maxResults);
    List<CommunityPosts> findApprovedPosts(int firstResult, int maxResults);
    List<CommunityPosts> findPendingPosts();
}
