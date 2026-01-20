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
@Table(name = "LiveSessionViewer")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "LiveSessionViewer.findAll", query = "SELECT l FROM LiveSessionViewer l"),
    @NamedQuery(name = "LiveSessionViewer.findByViewerID", query = "SELECT l FROM LiveSessionViewer l WHERE l.viewerID = :viewerID"),
    @NamedQuery(name = "LiveSessionViewer.findBySessionID", query = "SELECT l FROM LiveSessionViewer l WHERE l.sessionID.sessionID = :sessionID"),
    @NamedQuery(name = "LiveSessionViewer.findBySessionIDAndCustomerID", query = "SELECT l FROM LiveSessionViewer l WHERE l.sessionID.sessionID = :sessionID AND l.customerID.customerID = :customerID"),
    @NamedQuery(name = "LiveSessionViewer.findActiveViewers", query = "SELECT l FROM LiveSessionViewer l WHERE l.sessionID.sessionID = :sessionID AND l.leftAt IS NULL"),
    @NamedQuery(name = "LiveSessionViewer.findByCustomerID", query = "SELECT l FROM LiveSessionViewer l WHERE l.customerID.customerID = :customerID")
})
public class LiveSessionViewer implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ViewerID")
    private Integer viewerID;
    
    @Size(max = 100)
    @Column(name = "ViewerSessionID")
    private String viewerSessionID;
    
    @Column(name = "JoinedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date joinedAt;
    
    @Column(name = "LeftAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date leftAt;
    
    @Column(name = "TotalDuration")
    private Integer totalDuration;
    
    @JoinColumn(name = "SessionID", referencedColumnName = "SessionID")
    @ManyToOne(optional = false)
    private LiveSession sessionID;
    
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID")
    @ManyToOne
    private Customers customerID;

    public LiveSessionViewer() {
    }

    public LiveSessionViewer(Integer viewerID) {
        this.viewerID = viewerID;
    }

    public Integer getViewerID() {
        return viewerID;
    }

    public void setViewerID(Integer viewerID) {
        this.viewerID = viewerID;
    }

    public String getViewerSessionID() {
        return viewerSessionID;
    }

    public void setViewerSessionID(String viewerSessionID) {
        this.viewerSessionID = viewerSessionID;
    }

    public Date getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Date joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Date getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(Date leftAt) {
        this.leftAt = leftAt;
    }

    public Integer getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Integer totalDuration) {
        this.totalDuration = totalDuration;
    }

    public LiveSession getSessionID() {
        return sessionID;
    }

    public void setSessionID(LiveSession sessionID) {
        this.sessionID = sessionID;
    }

    public Customers getCustomerID() {
        return customerID;
    }

    public void setCustomerID(Customers customerID) {
        this.customerID = customerID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (viewerID != null ? viewerID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LiveSessionViewer)) {
            return false;
        }
        LiveSessionViewer other = (LiveSessionViewer) object;
        if ((this.viewerID == null && other.viewerID != null) || (this.viewerID != null && !this.viewerID.equals(other.viewerID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.LiveSessionViewer[ viewerID=" + viewerID + " ]";
    }

}
