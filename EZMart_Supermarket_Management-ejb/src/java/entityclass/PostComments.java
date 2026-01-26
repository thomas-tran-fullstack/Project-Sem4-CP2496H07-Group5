package entityclass;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "PostComments")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PostComments.findAll", query = "SELECT c FROM PostComments c"),
    @NamedQuery(name = "PostComments.findByCommentID", query = "SELECT c FROM PostComments c WHERE c.commentID = :commentID"),
    @NamedQuery(name = "PostComments.findByPostID", query = "SELECT c FROM PostComments c WHERE c.postID = :postID AND c.isDeleted = false ORDER BY c.createdAt DESC"),
    @NamedQuery(name = "PostComments.countByPostID", query = "SELECT COUNT(c) FROM PostComments c WHERE c.postID = :postID AND c.isDeleted = false"),
    @NamedQuery(name = "PostComments.findByUserID", query = "SELECT c FROM PostComments c WHERE c.authorUserID = :userID ORDER BY c.createdAt DESC"),
    @NamedQuery(name = "PostComments.findRecentComments", query = "SELECT c FROM PostComments c WHERE c.isDeleted = false ORDER BY c.createdAt DESC")
})
public class PostComments implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CommentID")
    private Long commentID;

    @Column(name = "PostID", nullable = false)
    private Long postID;

    @Column(name = "AuthorUserID", nullable = false)
    private Integer authorUserID;

    @Column(name = "CustomerID")
    private Integer customerID;

    @Column(name = "Content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "LikeCount")
    private Long likeCount = 0L;

    @Column(name = "CreatedAt", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "UpdatedAt", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "IsEdited")
    private Boolean isEdited = false;

    @Column(name = "IsDeleted")
    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "PostID", referencedColumnName = "PostID", insertable = false, updatable = false)
    private CommunityPosts post;

    @ManyToOne
    @JoinColumn(name = "AuthorUserID", referencedColumnName = "UserID", insertable = false, updatable = false)
    private Users authorUser;

    @ManyToOne
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID", insertable = false, updatable = false)
    private Customers customer;

    public PostComments() {
    }

    public PostComments(Long commentID) {
        this.commentID = commentID;
    }

    public Long getCommentID() {
        return commentID;
    }

    public void setCommentID(Long commentID) {
        this.commentID = commentID;
    }

    public Long getPostID() {
        return postID;
    }

    public void setPostID(Long postID) {
        this.postID = postID;
    }

    public Integer getAuthorUserID() {
        return authorUserID;
    }

    public void setAuthorUserID(Integer authorUserID) {
        this.authorUserID = authorUserID;
    }

    public Integer getCustomerID() {
        return customerID;
    }

    public void setCustomerID(Integer customerID) {
        this.customerID = customerID;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsEdited() {
        return isEdited;
    }

    public void setIsEdited(Boolean isEdited) {
        this.isEdited = isEdited;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public CommunityPosts getPost() {
        return post;
    }

    public void setPost(CommunityPosts post) {
        this.post = post;
    }

    public Users getAuthorUser() {
        return authorUser;
    }

    public void setAuthorUser(Users authorUser) {
        this.authorUser = authorUser;
    }

    public Customers getCustomer() {
        return customer;
    }

    public void setCustomer(Customers customer) {
        this.customer = customer;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(commentID);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PostComments)) {
            return false;
        }
        PostComments other = (PostComments) object;
        return this.commentID != null && this.commentID.equals(other.commentID);
    }

    @Override
    public String toString() {
        return "PostComments[ commentID=" + commentID + ", postID=" + postID + " ]";
    }
}
