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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *
 * @author EZMart Team
 */
@Entity
@Table(name = "LiveSession")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "LiveSession.findAll", query = "SELECT l FROM LiveSession l"),
    @NamedQuery(name = "LiveSession.findBySessionID", query = "SELECT l FROM LiveSession l WHERE l.sessionID = :sessionID"),
    @NamedQuery(name = "LiveSession.findByStaffID", query = "SELECT l FROM LiveSession l WHERE l.staffID.userID = :staffID"),
    @NamedQuery(name = "LiveSession.findByStatus", query = "SELECT l FROM LiveSession l WHERE l.status = :status"),
    @NamedQuery(name = "LiveSession.findActive", query = "SELECT l FROM LiveSession l WHERE l.status = 'ACTIVE' ORDER BY l.createdAt DESC"),
    @NamedQuery(name = "LiveSession.findByScheduledTime", query = "SELECT l FROM LiveSession l WHERE l.scheduledStartTime BETWEEN :startDate AND :endDate ORDER BY l.scheduledStartTime DESC")
})
public class LiveSession implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "SessionID")
    private Integer sessionID;
    
    @Size(max = 200)
    @Column(name = "Title")
    private String title;
    
    @Size(max = 2147483647)
    @Column(name = "Description")
    private String description;
    
    @Size(max = 20)
    @Column(name = "Status")
    private String status; // PENDING, ACTIVE, PAUSED, ENDED
    
    @Column(name = "ScheduledStartTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date scheduledStartTime;
    
    @Column(name = "ScheduledEndTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date scheduledEndTime;
    
    @Column(name = "ActualStartTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date actualStartTime;
    
    @Column(name = "ActualEndTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date actualEndTime;
    
    @Size(max = 500)
    @Column(name = "ThumbnailURL")
    private String thumbnailURL;
    
    @Size(max = 100)
    @Column(name = "StreamKey")
    private String streamKey;
    
    @Size(max = 500)
    @Column(name = "HLSPlaylistURL")
    private String hlsPlaylistURL;
    
    @Size(max = 500)
    @Column(name = "RtmpURL")
    private String rtmpURL;
    
    @Column(name = "CurrentViewers")
    private Integer currentViewers;
    
    @Column(name = "PeakViewers")
    private Integer peakViewers;
    
    @Column(name = "TotalViewers")
    private Integer totalViewers;
    
    @Column(name = "ChatMessageCount")
    private Integer chatMessageCount;
    
    @Size(max = 500)
    @Column(name = "RecordingPath")
    private String recordingPath;
    
    @Column(name = "IsRecording")
    private Boolean isRecording;
    
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Column(name = "UpdatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    
    @JoinColumn(name = "StaffID", referencedColumnName = "UserID")
    @ManyToOne(optional = false)
    private Users staffID;
    
    @OneToMany(mappedBy = "sessionID")
    private List<LiveProduct> liveProductList;
    
    @OneToMany(mappedBy = "sessionID")
    private List<LiveChat> liveChatList;
    
    @OneToMany(mappedBy = "sessionID")
    private List<LiveSessionStat> liveSessionStatList;
    
    @OneToMany(mappedBy = "sessionID")
    private List<LiveSessionViewer> liveSessionViewerList;

    public LiveSession() {
    }

    public LiveSession(Integer sessionID) {
        this.sessionID = sessionID;
    }

    public Integer getSessionID() {
        return sessionID;
    }

    public void setSessionID(Integer sessionID) {
        this.sessionID = sessionID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getScheduledStartTime() {
        return scheduledStartTime;
    }

    public void setScheduledStartTime(Date scheduledStartTime) {
        this.scheduledStartTime = scheduledStartTime;
    }

    public Date getScheduledEndTime() {
        return scheduledEndTime;
    }

    public void setScheduledEndTime(Date scheduledEndTime) {
        this.scheduledEndTime = scheduledEndTime;
    }

    public Date getActualStartTime() {
        return actualStartTime;
    }

    public void setActualStartTime(Date actualStartTime) {
        this.actualStartTime = actualStartTime;
    }

    public Date getActualEndTime() {
        return actualEndTime;
    }

    public void setActualEndTime(Date actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public String getHlsPlaylistURL() {
        return hlsPlaylistURL;
    }

    public void setHlsPlaylistURL(String hlsPlaylistURL) {
        this.hlsPlaylistURL = hlsPlaylistURL;
    }

    public String getRtmpURL() {
        return rtmpURL;
    }

    public void setRtmpURL(String rtmpURL) {
        this.rtmpURL = rtmpURL;
    }

    public Integer getCurrentViewers() {
        return currentViewers;
    }

    public void setCurrentViewers(Integer currentViewers) {
        this.currentViewers = currentViewers;
    }

    public Integer getPeakViewers() {
        return peakViewers;
    }

    public void setPeakViewers(Integer peakViewers) {
        this.peakViewers = peakViewers;
    }

    public Integer getTotalViewers() {
        return totalViewers;
    }

    public void setTotalViewers(Integer totalViewers) {
        this.totalViewers = totalViewers;
    }

    public Integer getChatMessageCount() {
        return chatMessageCount;
    }

    public void setChatMessageCount(Integer chatMessageCount) {
        this.chatMessageCount = chatMessageCount;
    }

    public String getRecordingPath() {
        return recordingPath;
    }

    public void setRecordingPath(String recordingPath) {
        this.recordingPath = recordingPath;
    }

    public Boolean getIsRecording() {
        return isRecording;
    }

    public void setIsRecording(Boolean isRecording) {
        this.isRecording = isRecording;
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

    public Users getStaffID() {
        return staffID;
    }

    public void setStaffID(Users staffID) {
        this.staffID = staffID;
    }

    @XmlTransient
    public List<LiveProduct> getLiveProductList() {
        return liveProductList;
    }

    public void setLiveProductList(List<LiveProduct> liveProductList) {
        this.liveProductList = liveProductList;
    }

    @XmlTransient
    public List<LiveChat> getLiveChatList() {
        return liveChatList;
    }

    public void setLiveChatList(List<LiveChat> liveChatList) {
        this.liveChatList = liveChatList;
    }

    @XmlTransient
    public List<LiveSessionStat> getLiveSessionStatList() {
        return liveSessionStatList;
    }

    public void setLiveSessionStatList(List<LiveSessionStat> liveSessionStatList) {
        this.liveSessionStatList = liveSessionStatList;
    }

    @XmlTransient
    public List<LiveSessionViewer> getLiveSessionViewerList() {
        return liveSessionViewerList;
    }

    public void setLiveSessionViewerList(List<LiveSessionViewer> liveSessionViewerList) {
        this.liveSessionViewerList = liveSessionViewerList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (sessionID != null ? sessionID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LiveSession)) {
            return false;
        }
        LiveSession other = (LiveSession) object;
        if ((this.sessionID == null && other.sessionID != null) || (this.sessionID != null && !this.sessionID.equals(other.sessionID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.LiveSession[ sessionID=" + sessionID + " ]";
    }

}
