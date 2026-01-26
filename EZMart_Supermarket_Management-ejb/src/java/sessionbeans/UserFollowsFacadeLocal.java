package sessionbeans;

import entityclass.UserFollows;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface UserFollowsFacadeLocal {
    void create(UserFollows userFollows);
    void edit(UserFollows userFollows);
    void remove(UserFollows userFollows);
    UserFollows find(Object id);
    List<UserFollows> findAll();
    List<UserFollows> findRange(int[] range);
    int count();
    List<UserFollows> findFollowers(Integer userID);
    List<UserFollows> findFollowing(Integer userID);
    UserFollows findFollowRelation(Integer followerID, Integer followingID);
    Long countFollowers(Integer userID);
    Long countFollowing(Integer userID);
    boolean isFollowing(Integer followerID, Integer followingID);
    void followUser(Integer followerID, Integer followerCustomerID, Integer followingID, Integer followingCustomerID);
    void unfollowUser(Integer followerID, Integer followingID);
}
