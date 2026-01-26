package entityclass;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "PostShares", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"PostID", "UserID"})
})
@NamedQueries({
    @NamedQuery(name = "PostShares.findAll", query = "SELECT p FROM PostShares p"),
    @NamedQuery(name = "PostShares.findByPostID", query = "SELECT p FROM PostShares p WHERE p.postID = :postID"),
    @NamedQuery(name = "PostShares.findByUserID", query = "SELECT p FROM PostShares p WHERE p.userID = :userID"),
    @NamedQuery(name = "PostShares.findByUserAndPost", query = "SELECT p FROM PostShares p WHERE p.userID = :userID AND p.postID = :postID"),
    @NamedQuery(name = "PostShares.countByPostID", query = "SELECT COUNT(p) FROM PostShares p WHERE p.postID = :postID"),
    @NamedQuery(name = "PostShares.findById", query = "SELECT p FROM PostShares p WHERE p.shareID = :id")
})
public class PostShares implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShareID")
    private Long shareID;

    @Column(name = "PostID", nullable = false)
    private Long postID;

    @Column(name = "UserID", nullable = false)
    private Integer userID;

    @Column(name = "CustomerID")
    private Integer customerID;

    @Column(name = "SharedMessage", columnDefinition = "NVARCHAR(MAX)")
    private String sharedMessage;

    @Column(name = "IsSharedToFacebook")
    private Boolean isSharedToFacebook = false;

    @Column(name = "IsSharedToTwitter")
    private Boolean isSharedToTwitter = false;

    @Column(name = "IsSharedToWhatsapp")
    private Boolean isSharedToWhatsapp = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CreatedAt")
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

    // Constructors
    public PostShares() {
    }

    public PostShares(Long postID, Integer userID) {
        this.postID = postID;
        this.userID = userID;
        this.createdAt = new Date();
    }

    // Getters and Setters
    public Long getShareID() {
        return shareID;
    }

    public void setShareID(Long shareID) {
        this.shareID = shareID;
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

    public String getSharedMessage() {
        return sharedMessage;
    }

    public void setSharedMessage(String sharedMessage) {
        this.sharedMessage = sharedMessage;
    }

    public Boolean getIsSharedToFacebook() {
        return isSharedToFacebook;
    }

    public void setIsSharedToFacebook(Boolean isSharedToFacebook) {
        this.isSharedToFacebook = isSharedToFacebook;
    }

    public Boolean getIsSharedToTwitter() {
        return isSharedToTwitter;
    }

    public void setIsSharedToTwitter(Boolean isSharedToTwitter) {
        this.isSharedToTwitter = isSharedToTwitter;
    }

    public Boolean getIsSharedToWhatsapp() {
        return isSharedToWhatsapp;
    }

    public void setIsSharedToWhatsapp(Boolean isSharedToWhatsapp) {
        this.isSharedToWhatsapp = isSharedToWhatsapp;
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
        int hash = 0;
        hash += (shareID != null ? shareID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PostShares)) {
            return false;
        }
        PostShares other = (PostShares) object;
        if ((this.shareID == null && other.shareID != null) || (this.shareID != null && !this.shareID.equals(other.shareID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PostShares[ shareID=" + shareID + " ]";
    }
}
