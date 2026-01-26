package sessionbeans;

import entityclass.UserFollows;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

@Stateless
public class UserFollowsFacade extends AbstractFacade<UserFollows> implements UserFollowsFacadeLocal {
    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public UserFollowsFacade() {
        super(UserFollows.class);
    }

    public List<UserFollows> findFollowers(Integer userID) {
        Query query = em.createNamedQuery("UserFollows.findFollowers");
        query.setParameter("userID", userID);
        return query.getResultList();
    }

    public List<UserFollows> findFollowing(Integer userID) {
        Query query = em.createNamedQuery("UserFollows.findFollowing");
        query.setParameter("userID", userID);
        return query.getResultList();
    }

    public UserFollows findFollowRelation(Integer followerID, Integer followingID) {
        Query query = em.createNamedQuery("UserFollows.findFollowRelation");
        query.setParameter("followerID", followerID);
        query.setParameter("followingID", followingID);
        List<UserFollows> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public Long countFollowers(Integer userID) {
        Query query = em.createNamedQuery("UserFollows.countFollowers");
        query.setParameter("userID", userID);
        return (Long) query.getSingleResult();
    }

    public Long countFollowing(Integer userID) {
        Query query = em.createNamedQuery("UserFollows.countFollowing");
        query.setParameter("userID", userID);
        return (Long) query.getSingleResult();
    }

    public boolean isFollowing(Integer followerID, Integer followingID) {
        return findFollowRelation(followerID, followingID) != null;
    }

    public void followUser(Integer followerID, Integer followerCustomerID, Integer followingID, Integer followingCustomerID) {
        if (!isFollowing(followerID, followingID)) {
            UserFollows follow = new UserFollows();
            follow.setFollowerUserID(followerID);
            follow.setFollowerCustomerID(followerCustomerID);
            follow.setFollowingUserID(followingID);
            follow.setFollowingCustomerID(followingCustomerID);
            follow.setCreatedAt(new java.util.Date());
            follow.setIsBlocked(false);
            create(follow);
        }
    }

    public void unfollowUser(Integer followerID, Integer followingID) {
        UserFollows follow = findFollowRelation(followerID, followingID);
        if (follow != null) {
            remove(follow);
        }
    }
}
