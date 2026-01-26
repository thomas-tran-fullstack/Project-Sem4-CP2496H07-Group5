package entityclass;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "UserFollows")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "UserFollows.findAll", query = "SELECT u FROM UserFollows u"),
    @NamedQuery(name = "UserFollows.findByFollowID", query = "SELECT u FROM UserFollows u WHERE u.followID = :followID"),
    @NamedQuery(name = "UserFollows.findFollowers", query = "SELECT u FROM UserFollows u WHERE u.followingUserID = :userID AND u.isBlocked = false ORDER BY u.createdAt DESC"),
    @NamedQuery(name = "UserFollows.findFollowing", query = "SELECT u FROM UserFollows u WHERE u.followerUserID = :userID AND u.isBlocked = false ORDER BY u.createdAt DESC"),
    @NamedQuery(name = "UserFollows.findFollowRelation", query = "SELECT u FROM UserFollows u WHERE u.followerUserID = :followerID AND u.followingUserID = :followingID"),
    @NamedQuery(name = "UserFollows.countFollowers", query = "SELECT COUNT(u) FROM UserFollows u WHERE u.followingUserID = :userID AND u.isBlocked = false"),
    @NamedQuery(name = "UserFollows.countFollowing", query = "SELECT COUNT(u) FROM UserFollows u WHERE u.followerUserID = :userID AND u.isBlocked = false")
})
public class UserFollows implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FollowID")
    private Long followID;

    @Column(name = "FollowerUserID", nullable = false)
    private Integer followerUserID;

    @Column(name = "FollowerCustomerID")
    private Integer followerCustomerID;

    @Column(name = "FollowingUserID", nullable = false)
    private Integer followingUserID;

    @Column(name = "FollowingCustomerID")
    private Integer followingCustomerID;

    @Column(name = "CreatedAt", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "IsBlocked")
    private Boolean isBlocked = false;

    @ManyToOne
    @JoinColumn(name = "FollowerUserID", referencedColumnName = "UserID", insertable = false, updatable = false)
    private Users followerUser;

    @ManyToOne
    @JoinColumn(name = "FollowingUserID", referencedColumnName = "UserID", insertable = false, updatable = false)
    private Users followingUser;

    @ManyToOne
    @JoinColumn(name = "FollowerCustomerID", referencedColumnName = "CustomerID", insertable = false, updatable = false)
    private Customers followerCustomer;

    @ManyToOne
    @JoinColumn(name = "FollowingCustomerID", referencedColumnName = "CustomerID", insertable = false, updatable = false)
    private Customers followingCustomer;

    public UserFollows() {
    }

    public UserFollows(Long followID) {
        this.followID = followID;
    }

    public Long getFollowID() {
        return followID;
    }

    public void setFollowID(Long followID) {
        this.followID = followID;
    }

    public Integer getFollowerUserID() {
        return followerUserID;
    }

    public void setFollowerUserID(Integer followerUserID) {
        this.followerUserID = followerUserID;
    }

    public Integer getFollowerCustomerID() {
        return followerCustomerID;
    }

    public void setFollowerCustomerID(Integer followerCustomerID) {
        this.followerCustomerID = followerCustomerID;
    }

    public Integer getFollowingUserID() {
        return followingUserID;
    }

    public void setFollowingUserID(Integer followingUserID) {
        this.followingUserID = followingUserID;
    }

    public Integer getFollowingCustomerID() {
        return followingCustomerID;
    }

    public void setFollowingCustomerID(Integer followingCustomerID) {
        this.followingCustomerID = followingCustomerID;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsBlocked() {
        return isBlocked;
    }

    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    public Users getFollowerUser() {
        return followerUser;
    }

    public void setFollowerUser(Users followerUser) {
        this.followerUser = followerUser;
    }

    public Users getFollowingUser() {
        return followingUser;
    }

    public void setFollowingUser(Users followingUser) {
        this.followingUser = followingUser;
    }

    public Customers getFollowerCustomer() {
        return followerCustomer;
    }

    public void setFollowerCustomer(Customers followerCustomer) {
        this.followerCustomer = followerCustomer;
    }

    public Customers getFollowingCustomer() {
        return followingCustomer;
    }

    public void setFollowingCustomer(Customers followingCustomer) {
        this.followingCustomer = followingCustomer;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(followID);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UserFollows)) {
            return false;
        }
        UserFollows other = (UserFollows) object;
        return this.followID != null && this.followID.equals(other.followID);
    }

    @Override
    public String toString() {
        return "UserFollows[ followID=" + followID + " ]";
    }
}
