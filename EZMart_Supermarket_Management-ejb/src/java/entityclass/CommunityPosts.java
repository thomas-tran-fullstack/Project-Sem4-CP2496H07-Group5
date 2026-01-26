package entityclass;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "CommunityPosts")
@NamedQueries({
    @NamedQuery(name = "CommunityPosts.findAll", query = "SELECT c FROM CommunityPosts c"),
    @NamedQuery(name = "CommunityPosts.findByStatus", query = "SELECT c FROM CommunityPosts c WHERE c.status = :status ORDER BY c.submittedAt DESC"),
    @NamedQuery(name = "CommunityPosts.findByCustomerID", query = "SELECT c FROM CommunityPosts c WHERE c.customerID = :customerID ORDER BY c.submittedAt DESC"),
    @NamedQuery(name = "CommunityPosts.findApprovedPosts", query = "SELECT c FROM CommunityPosts c WHERE c.status = 'APPROVED' ORDER BY c.submittedAt DESC"),
    @NamedQuery(name = "CommunityPosts.findPendingPosts", query = "SELECT c FROM CommunityPosts c WHERE c.status = 'PENDING' ORDER BY c.submittedAt DESC"),
    @NamedQuery(name = "CommunityPosts.findByAuthor", query = "SELECT c FROM CommunityPosts c WHERE c.authorUserID = :userID ORDER BY c.submittedAt DESC")
})
public class CommunityPosts implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PostID")
    private Long postID;

    @Column(name = "Title", nullable = false, length = 255)
    private String title;

    @Column(name = "Content", columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "Slug", length = 255)
    private String slug;

    @Column(name = "CoverImageUrl", length = 500)
    private String coverImageUrl;

    @Column(name = "Status", length = 20)
    private String status;

    @Column(name = "Visibility", length = 20)
    private String visibility;

    @Column(name = "AuthorUserID")
    private Integer authorUserID;

    @Column(name = "CustomerID")
    private Integer customerID;

    @Column(name = "LikeCount")
    private Long likeCount = 0L;

    @Column(name = "CommentCount")
    private Long commentCount = 0L;

    @Column(name = "ViewCount")
    private Long viewCount = 0L;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "SubmittedAt")
    private Date submittedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ApprovedAt")
    private Date approvedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "RejectedAt")
    private Date rejectedAt;

    @Column(name = "ApprovedByUserID")
    private Integer approvedByUserID;

    @Column(name = "RejectedByUserID")
    private Integer rejectedByUserID;

    @Column(name = "RejectReason", columnDefinition = "NVARCHAR(MAX)")
    private String rejectReason;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UpdatedAt")
    private Date updatedAt;

    @ManyToOne
    @JoinColumn(name = "AuthorUserID", referencedColumnName = "UserID", insertable = false, updatable = false)
    private Users authorUser;

    @ManyToOne
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID", insertable = false, updatable = false)
    private Customers customer;

    // Constructors
    public CommunityPosts() {
    }

    public CommunityPosts(String title, String content, Integer authorUserID) {
        this.title = title;
        this.content = content;
        this.authorUserID = authorUserID;
        this.status = "PENDING";
        this.visibility = "PUBLIC";
        this.likeCount = 0L;
        this.commentCount = 0L;
        this.viewCount = 0L;
        this.submittedAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public Long getPostID() {
        return postID;
    }

    public void setPostID(Long postID) {
        this.postID = postID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
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

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public Long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Date getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Date submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Date getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Date rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public Integer getApprovedByUserID() {
        return approvedByUserID;
    }

    public void setApprovedByUserID(Integer approvedByUserID) {
        this.approvedByUserID = approvedByUserID;
    }

    public Integer getRejectedByUserID() {
        return rejectedByUserID;
    }

    public void setRejectedByUserID(Integer rejectedByUserID) {
        this.rejectedByUserID = rejectedByUserID;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
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
        int hash = 0;
        hash += (postID != null ? postID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CommunityPosts)) {
            return false;
        }
        CommunityPosts other = (CommunityPosts) object;
        if ((this.postID == null && other.postID != null) || (this.postID != null && !this.postID.equals(other.postID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CommunityPosts[ postID=" + postID + " ]";
    }
}
