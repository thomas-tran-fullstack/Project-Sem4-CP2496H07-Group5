package entityclass;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Entity class for ChatMessages - represents individual chat messages
 */
@Entity
@Table(name = "ChatMessages")
@NamedQueries({
    @NamedQuery(name = "ChatMessages.findByConversationID", 
        query = "SELECT m FROM ChatMessages m WHERE m.conversationID.conversationID = :conversationID ORDER BY m.sentAt ASC"),
    @NamedQuery(name = "ChatMessages.findAll", query = "SELECT m FROM ChatMessages m")
})
public class ChatMessages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MessageID")
    private Long messageID;

    @ManyToOne
    @JoinColumn(name = "ConversationID", referencedColumnName = "ConversationID", nullable = false)
    private ChatConversations conversationID;

    @Column(name = "SenderRole", length = 20, nullable = false)
    private String senderRole;

    @Column(name = "SenderUserID", nullable = false)
    private Integer senderUserID;

    @Column(name = "Content", length = 2000, nullable = false)
    private String content;

    @Column(name = "SentAt", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentAt;

    @Column(name = "IsRead", nullable = false)
    private Boolean isRead = false;

    @Column(name = "MessageType", length = 20)
    private String messageType = "TEXT"; // TEXT, IMAGE, FILE

    @Column(name = "AttachmentUrl", length = 500)
    private String attachmentUrl;

    // Constructors
    public ChatMessages() {
        this.sentAt = new Date();
        this.isRead = false;
    }

    public ChatMessages(Long messageID) {
        this.messageID = messageID;
        this.sentAt = new Date();
        this.isRead = false;
    }

    // Helper method to check if message is from current user
    public boolean isFromUser(String userRole, Integer userId) {
        return senderRole.equals(userRole) && senderUserID.equals(userId);
    }

    // Getters and Setters
    public Long getMessageID() {
        return messageID;
    }

    public void setMessageID(Long messageID) {
        this.messageID = messageID;
    }

    public ChatConversations getConversationID() {
        return conversationID;
    }

    public void setConversationID(ChatConversations conversationID) {
        this.conversationID = conversationID;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public Integer getSenderUserID() {
        return senderUserID;
    }

    public void setSenderUserID(Integer senderUserID) {
        this.senderUserID = senderUserID;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }
}
