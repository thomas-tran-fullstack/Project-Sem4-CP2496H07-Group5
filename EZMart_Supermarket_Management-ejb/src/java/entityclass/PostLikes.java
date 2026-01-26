package entityclass;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "PostLikes")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PostLikes.findAll", query = "SELECT p FROM PostLikes p"),
    @NamedQuery(name = "PostLikes.findByLikeID", query = "SELECT p FROM PostLikes p WHERE p.likeID = :likeID"),
    @NamedQuery(name = "PostLikes.findByPostID", query = "SELECT p FROM PostLikes p WHERE p.postID = :postID ORDER BY p.createdAt DESC"),
    @NamedQuery(name = "PostLikes.countByPostID", query = "SELECT COUNT(p) FROM PostLikes p WHERE p.postID = :postID"),
    @NamedQuery(name = "PostLikes.findByUserAndPost", query = "SELECT p FROM PostLikes p WHERE p.userID = :userID AND p.postID = :postID"),
    @NamedQuery(name = "PostLikes.findByUserID", query = "SELECT p FROM PostLikes p WHERE p.userID = :userID ORDER BY p.createdAt DESC")
})
public class PostLikes implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LikeID")
    private Long likeID;

    @Column(name = "PostID", nullable = false)
    private Long postID;

    @Column(name = "UserID", nullable = false)
    private Integer userID;

    @Column(name = "CustomerID")
    private Integer customerID;

    @Column(name = "ReactionType")
    private String reactionType = "LIKE";

    @Column(name = "CreatedAt", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @ManyToOne
    @JoinColumn(name = "PostID", referencedColumnName = "PostID", insertable = false, updatable = false)
    private CommunityPosts post;

    @ManyToOne
    @JoinColumn(name = "UserID", referencedColumnName = "UserID", insertable = false, updatable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID", insertable = false, updatable = false)
    private Customers customer;

    public PostLikes() {
    }

    public PostLikes(Long likeID) {
        this.likeID = likeID;
    }

    public Long getLikeID() {
        return likeID;
    }

    public void setLikeID(Long likeID) {
        this.likeID = likeID;
    }

    public Long getPostID() {
        return postID;
    }

    public void setPostID(Long postID) {
        this.postID = postID;
    }

    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public Integer getCustomerID() {
        return customerID;
    }

    public void setCustomerID(Integer customerID) {
        this.customerID = customerID;
    }

    public String getReactionType() {
        return reactionType;
    }

    public void setReactionType(String reactionType) {
        this.reactionType = reactionType;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public CommunityPosts getPost() {
        return post;
    }

    public void setPost(CommunityPosts post) {
        this.post = post;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public Customers getCustomer() {
        return customer;
    }

    public void setCustomer(Customers customer) {
        this.customer = customer;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(likeID);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PostLikes)) {
            return false;
        }
        PostLikes other = (PostLikes) object;
        return this.likeID != null && this.likeID.equals(other.likeID);
    }

    @Override
    public String toString() {
        return "PostLikes[ likeID=" + likeID + ", postID=" + postID + " ]";
    }
}
