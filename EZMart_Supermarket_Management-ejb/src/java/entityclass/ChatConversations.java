package entityclass;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Entity class for ChatConversation - represents a 1:1 chat between a staff and customer
 */
@Entity
@Table(name = "ChatConversations")
@NamedQueries({
    @NamedQuery(name = "ChatConversations.findByCustomerAndStaff", 
        query = "SELECT c FROM ChatConversations c WHERE c.customerID.customerID = :customerID AND c.staffID.staffID = :staffID"),
    @NamedQuery(name = "ChatConversations.findByCustomerID", 
        query = "SELECT c FROM ChatConversations c WHERE c.customerID.customerID = :customerID ORDER BY c.lastMessageAt DESC"),
    @NamedQuery(name = "ChatConversations.findByStaffID", 
        query = "SELECT c FROM ChatConversations c WHERE c.staffID.staffID = :staffID ORDER BY c.lastMessageAt DESC"),
    @NamedQuery(name = "ChatConversations.findActiveByStaffID", 
        query = "SELECT c FROM ChatConversations c WHERE c.staffID.staffID = :staffID AND c.status = :status ORDER BY c.lastMessageAt DESC"),
    @NamedQuery(name = "ChatConversations.findActiveByCustomerID", 
        query = "SELECT c FROM ChatConversations c WHERE c.customerID.customerID = :customerID AND c.status = :status ORDER BY c.lastMessageAt DESC"),
    @NamedQuery(name = "ChatConversations.findAll", query = "SELECT c FROM ChatConversations c")
})
public class ChatConversations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ConversationID")
    private Integer conversationID;

    @ManyToOne
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID", nullable = false)
    private Customers customerID;

    @ManyToOne(optional = true)
    @JoinColumn(name = "StaffID", referencedColumnName = "StaffID", nullable = true)
    private Staffs staffID;

    @Column(name = "CreatedAt", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "LastMessageAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastMessageAt;

    @Column(name = "Status", length = 20)
    private String status = "ACTIVE";

    @Column(name = "RequestStatus", length = 20)
    private String requestStatus = "ACCEPTED";
    // RequestStatus: 'PENDING' (waiting for staff acceptance), 'ACCEPTED' (staff accepted), 
    // 'REJECTED' (all staff rejected), 'CLOSED' (chat ended)

    @Column(name = "AcceptedStaffID")
    private Integer acceptedStaffID;
    // Which staff member accepted this conversation (NULL if PENDING)

    @Column(name = "ChatStartTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date chatStartTime;
    // When the staff accepted and chat actually started

    @Column(name = "AutoRejectTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date autoRejectTime;
    // When auto-reject timer will trigger (5 minutes after PENDING creation)

    @Column(name = "RejectedStaffIDs", length = 4000)
    private String rejectedStaffIDs;
    // Comma-separated StaffIDs that rejected this request

    // Constructors
    public ChatConversations() {
        this.createdAt = new Date();
        this.requestStatus = "ACCEPTED";
    }

    public ChatConversations(Integer conversationID) {
        this.conversationID = conversationID;
        this.createdAt = new Date();
        this.requestStatus = "ACCEPTED";
    }

    // Helper method to get the other participant's name
    public String getOtherParticipantName(String currentUserRole, Integer currentUserId) {
        if ("CUSTOMER".equals(currentUserRole)) {
            return staffID != null ? staffID.getFullName() : "Unknown Staff";
        } else if ("STAFF".equals(currentUserRole)) {
            return customerID != null ? getCustomerFullName() : "Unknown Customer";
        }
        return "Unknown";
    }

    private String getCustomerFullName() {
        if (customerID == null) return "Unknown";
        StringBuilder sb = new StringBuilder();
        if (customerID.getFirstName() != null) sb.append(customerID.getFirstName()).append(" ");
        if (customerID.getMiddleName() != null) sb.append(customerID.getMiddleName()).append(" ");
        if (customerID.getLastName() != null) sb.append(customerID.getLastName());
        return sb.toString().trim();
    }

    // Getters and Setters
    public Integer getConversationID() {
        return conversationID;
    }

    public void setConversationID(Integer conversationID) {
        this.conversationID = conversationID;
    }

    public Customers getCustomerID() {
        return customerID;
    }

    public void setCustomerID(Customers customerID) {
        this.customerID = customerID;
    }

    public Staffs getStaffID() {
        return staffID;
    }

    public void setStaffID(Staffs staffID) {
        this.staffID = staffID;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Date lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public Integer getAcceptedStaffID() {
        return acceptedStaffID;
    }

    public void setAcceptedStaffID(Integer acceptedStaffID) {
        this.acceptedStaffID = acceptedStaffID;
    }

    public Date getChatStartTime() {
        return chatStartTime;
    }

    public void setChatStartTime(Date chatStartTime) {
        this.chatStartTime = chatStartTime;
    }

    public Date getAutoRejectTime() {
        return autoRejectTime;
    }

    public void setAutoRejectTime(Date autoRejectTime) {
        this.autoRejectTime = autoRejectTime;
    }

    public String getRejectedStaffIDs() {
        return rejectedStaffIDs;
    }

    public void setRejectedStaffIDs(String rejectedStaffIDs) {
        this.rejectedStaffIDs = rejectedStaffIDs;
    }
}