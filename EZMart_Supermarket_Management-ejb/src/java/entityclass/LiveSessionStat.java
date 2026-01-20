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
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author EZMart Team
 */
@Entity
@Table(name = "LiveSessionStat")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "LiveSessionStat.findAll", query = "SELECT l FROM LiveSessionStat l"),
    @NamedQuery(name = "LiveSessionStat.findByStatID", query = "SELECT l FROM LiveSessionStat l WHERE l.statID = :statID"),
    @NamedQuery(name = "LiveSessionStat.findBySessionID", query = "SELECT l FROM LiveSessionStat l WHERE l.sessionID.sessionID = :sessionID ORDER BY l.snapshotTime DESC"),
    @NamedQuery(name = "LiveSessionStat.findLatestBySessionID", query = "SELECT l FROM LiveSessionStat l WHERE l.sessionID.sessionID = :sessionID ORDER BY l.snapshotTime DESC")
})
public class LiveSessionStat implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "StatID")
    private Integer statID;
    
    @Column(name = "SnapshotTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date snapshotTime;
    
    @Column(name = "ActiveViewers")
    private Integer activeViewers;
    
    @Column(name = "TotalMessages")
    private Integer totalMessages;
    
    @Column(name = "UniqueViewers")
    private Integer uniqueViewers;
    
    @Column(name = "ProductsViewed")
    private Integer productsViewed;
    
    @Column(name = "ProductsPurchased")
    private Integer productsPurchased;
    
    @Column(name = "TotalRevenue")
    private BigDecimal totalRevenue;
    
    @Column(name = "AverageDuration")
    private Integer averageDuration;
    
    @JoinColumn(name = "SessionID", referencedColumnName = "SessionID")
    @ManyToOne(optional = false)
    private LiveSession sessionID;

    public LiveSessionStat() {
    }

    public LiveSessionStat(Integer statID) {
        this.statID = statID;
    }

    public Integer getStatID() {
        return statID;
    }

    public void setStatID(Integer statID) {
        this.statID = statID;
    }

    public Date getSnapshotTime() {
        return snapshotTime;
    }

    public void setSnapshotTime(Date snapshotTime) {
        this.snapshotTime = snapshotTime;
    }

    public Integer getActiveViewers() {
        return activeViewers;
    }

    public void setActiveViewers(Integer activeViewers) {
        this.activeViewers = activeViewers;
    }

    public Integer getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(Integer totalMessages) {
        this.totalMessages = totalMessages;
    }

    public Integer getUniqueViewers() {
        return uniqueViewers;
    }

    public void setUniqueViewers(Integer uniqueViewers) {
        this.uniqueViewers = uniqueViewers;
    }

    public Integer getProductsViewed() {
        return productsViewed;
    }

    public void setProductsViewed(Integer productsViewed) {
        this.productsViewed = productsViewed;
    }

    public Integer getProductsPurchased() {
        return productsPurchased;
    }

    public void setProductsPurchased(Integer productsPurchased) {
        this.productsPurchased = productsPurchased;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Integer getAverageDuration() {
        return averageDuration;
    }

    public void setAverageDuration(Integer averageDuration) {
        this.averageDuration = averageDuration;
    }

    public LiveSession getSessionID() {
        return sessionID;
    }

    public void setSessionID(LiveSession sessionID) {
        this.sessionID = sessionID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (statID != null ? statID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LiveSessionStat)) {
            return false;
        }
        LiveSessionStat other = (LiveSessionStat) object;
        if ((this.statID == null && other.statID != null) || (this.statID != null && !this.statID.equals(other.statID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.LiveSessionStat[ statID=" + statID + " ]";
    }

}
