package entityclass;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author EZMart Team
 */
@Entity
@Table(name = "LiveChat")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "LiveChat.findAll", query = "SELECT l FROM LiveChat l"),
    @NamedQuery(name = "LiveChat.findByChatMessageID", query = "SELECT l FROM LiveChat l WHERE l.chatMessageID = :chatMessageID"),
    @NamedQuery(name = "LiveChat.findBySessionID", query = "SELECT l FROM LiveChat l WHERE l.sessionID.sessionID = :sessionID AND l.isDeleted = FALSE ORDER BY l.createdAt DESC"),
    @NamedQuery(name = "LiveChat.findByUserID", query = "SELECT l FROM LiveChat l WHERE l.userID.userID = :userID"),
    @NamedQuery(name = "LiveChat.findRecentMessages", query = "SELECT l FROM LiveChat l WHERE l.sessionID.sessionID = :sessionID AND l.isDeleted = FALSE ORDER BY l.createdAt DESC")
})
public class LiveChat implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ChatMessageID")
    private Integer chatMessageID;
    
    @Size(max = 500)
    @Column(name = "MessageText")
    private String messageText;
    
    @Size(max = 20)
    @Column(name = "MessageType")
    private String messageType; // TEXT, EMOJI, PRODUCT_MENTION, SYSTEM
    
    @Column(name = "IsDeleted")
    private Boolean isDeleted;
    
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @JoinColumn(name = "SessionID", referencedColumnName = "SessionID")
    @ManyToOne(optional = false)
    private LiveSession sessionID;
    
    @JoinColumn(name = "UserID", referencedColumnName = "UserID")
    @ManyToOne(optional = false)
    private Users userID;
    
    @JoinColumn(name = "DeletedBy", referencedColumnName = "UserID")
    @ManyToOne
    private Users deletedBy;

    public LiveChat() {
    }

    public LiveChat(Integer chatMessageID) {
        this.chatMessageID = chatMessageID;
    }

    public Integer getChatMessageID() {
        return chatMessageID;
    }

    public void setChatMessageID(Integer chatMessageID) {
        this.chatMessageID = chatMessageID;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public LiveSession getSessionID() {
        return sessionID;
    }

    public void setSessionID(LiveSession sessionID) {
        this.sessionID = sessionID;
    }

    public Users getUserID() {
        return userID;
    }

    public void setUserID(Users userID) {
        this.userID = userID;
    }

    public Users getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(Users deletedBy) {
        this.deletedBy = deletedBy;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (chatMessageID != null ? chatMessageID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LiveChat)) {
            return false;
        }
        LiveChat other = (LiveChat) object;
        if ((this.chatMessageID == null && other.chatMessageID != null) || (this.chatMessageID != null && !this.chatMessageID.equals(other.chatMessageID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.LiveChat[ chatMessageID=" + chatMessageID + " ]";
    }

}
