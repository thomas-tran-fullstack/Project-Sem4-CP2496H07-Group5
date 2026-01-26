package entityclass;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "CommentLikes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"CommentID", "UserID"})
})
@NamedQueries({
    @NamedQuery(name = "CommentLikes.findAll", query = "SELECT c FROM CommentLikes c"),
    @NamedQuery(name = "CommentLikes.findByCommentID", query = "SELECT c FROM CommentLikes c WHERE c.commentID = :commentID"),
    @NamedQuery(name = "CommentLikes.findByUserID", query = "SELECT c FROM CommentLikes c WHERE c.userID = :userID"),
    @NamedQuery(name = "CommentLikes.findByUserAndComment", query = "SELECT c FROM CommentLikes c WHERE c.userID = :userID AND c.commentID = :commentID"),
    @NamedQuery(name = "CommentLikes.countByCommentID", query = "SELECT COUNT(c) FROM CommentLikes c WHERE c.commentID = :commentID"),
    @NamedQuery(name = "CommentLikes.findById", query = "SELECT c FROM CommentLikes c WHERE c.likeID = :id")
})
public class CommentLikes implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LikeID")
    private Long likeID;

    @Column(name = "CommentID", nullable = false)
    private Long commentID;

    @Column(name = "UserID", nullable = false)
    private Integer userID;

    @Column(name = "CustomerID")
    private Integer customerID;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CreatedAt")
    private Date createdAt;

    @ManyToOne
    @JoinColumn(name = "UserID", referencedColumnName = "UserID", insertable = false, updatable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID", insertable = false, updatable = false)
    private Customers customer;

    // Constructors
    public CommentLikes() {
    }

    public CommentLikes(Long commentID, Integer userID) {
        this.commentID = commentID;
        this.userID = userID;
        this.createdAt = new Date();
    }

    // Getters and Setters
    public Long getLikeID() {
        return likeID;
    }

    public void setLikeID(Long likeID) {
        this.likeID = likeID;
    }

    public Long getCommentID() {
        return commentID;
    }

    public void setCommentID(Long commentID) {
        this.commentID = commentID;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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
        int hash = 0;
        hash += (likeID != null ? likeID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CommentLikes)) {
            return false;
        }
        CommentLikes other = (CommentLikes) object;
        if ((this.likeID == null && other.likeID != null) || (this.likeID != null && !this.likeID.equals(other.likeID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CommentLikes[ likeID=" + likeID + " ]";
    }
}
