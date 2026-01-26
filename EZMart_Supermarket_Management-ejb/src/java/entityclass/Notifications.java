package entityclass;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "Notifications")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Notifications.findAll", query = "SELECT n FROM Notifications n"),
    @NamedQuery(name = "Notifications.findByNotificationID", query = "SELECT n FROM Notifications n WHERE n.notificationID = :notificationID"),
    @NamedQuery(name = "Notifications.findByRecipient", query = "SELECT n FROM Notifications n WHERE n.recipientUserID = :userID ORDER BY n.createdAt DESC"),
    @NamedQuery(name = "Notifications.findUnread", query = "SELECT n FROM Notifications n WHERE n.recipientUserID = :userID AND n.isRead = false ORDER BY n.createdAt DESC"),
    @NamedQuery(name = "Notifications.countUnread", query = "SELECT COUNT(n) FROM Notifications n WHERE n.recipientUserID = :userID AND n.isRead = false"),
    @NamedQuery(name = "Notifications.findByType", query = "SELECT n FROM Notifications n WHERE n.recipientUserID = :userID AND n.notificationType = :type ORDER BY n.createdAt DESC")
})
public class Notifications implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID")
    private Long notificationID;

    @Column(name = "RecipientUserID", nullable = false)
    private Integer recipientUserID;

    @Column(name = "ActorUserID", nullable = false)
    private Integer actorUserID;

    @Column(name = "NotificationType", nullable = false)
    private String notificationType;

    @Column(name = "PostID")
    private Long postID;

    @Column(name = "CommentID")
    private Long commentID;

    @Column(name = "Message", nullable = false)
    private String message;

    @Column(name = "IsRead")
    private Boolean isRead = false;

    @Column(name = "CreatedAt", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "ReadAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date readAt;

    @ManyToOne
    @JoinColumn(name = "RecipientUserID", referencedColumnName = "UserID", insertable = false, updatable = false)
    private Users recipientUser;

    @ManyToOne
    @JoinColumn(name = "ActorUserID", referencedColumnName = "UserID", insertable = false, updatable = false)
    private Users actorUser;

    @ManyToOne
    @JoinColumn(name = "PostID", referencedColumnName = "PostID", insertable = false, updatable = false)
    private CommunityPosts post;

    @ManyToOne
    @JoinColumn(name = "CommentID", referencedColumnName = "CommentID", insertable = false, updatable = false)
    private PostComments comment;

    public Notifications() {
    }

    public Notifications(Long notificationID) {
        this.notificationID = notificationID;
    }

    public Long getNotificationID() {
        return notificationID;
    }

    public void setNotificationID(Long notificationID) {
        this.notificationID = notificationID;
    }

    public Integer getRecipientUserID() {
        return recipientUserID;
    }

    public void setRecipientUserID(Integer recipientUserID) {
        this.recipientUserID = recipientUserID;
    }

    public Integer getActorUserID() {
        return actorUserID;
    }

    public void setActorUserID(Integer actorUserID) {
        this.actorUserID = actorUserID;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Long getPostID() {
        return postID;
    }

    public void setPostID(Long postID) {
        this.postID = postID;
    }

    public Long getCommentID() {
        return commentID;
    }

    public void setCommentID(Long commentID) {
        this.commentID = commentID;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getReadAt() {
        return readAt;
    }

    public void setReadAt(Date readAt) {
        this.readAt = readAt;
    }

    public Users getRecipientUser() {
        return recipientUser;
    }

    public void setRecipientUser(Users recipientUser) {
        this.recipientUser = recipientUser;
    }

    public Users getActorUser() {
        return actorUser;
    }

    public void setActorUser(Users actorUser) {
        this.actorUser = actorUser;
    }

    public CommunityPosts getPost() {
        return post;
    }

    public void setPost(CommunityPosts post) {
        this.post = post;
    }

    public PostComments getComment() {
        return comment;
    }

    public void setComment(PostComments comment) {
        this.comment = comment;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(notificationID);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Notifications)) {
            return false;
        }
        Notifications other = (Notifications) object;
        return this.notificationID != null && this.notificationID.equals(other.notificationID);
    }

    @Override
    public String toString() {
        return "Notifications[ notificationID=" + notificationID + ", type=" + notificationType + " ]";
    }
}
