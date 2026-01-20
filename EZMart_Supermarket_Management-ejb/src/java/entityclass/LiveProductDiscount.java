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
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author EZMart Team
 */
@Entity
@Table(name = "LiveProductDiscount")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "LiveProductDiscount.findAll", query = "SELECT l FROM LiveProductDiscount l"),
    @NamedQuery(name = "LiveProductDiscount.findByDiscountHistoryID", query = "SELECT l FROM LiveProductDiscount l WHERE l.discountHistoryID = :discountHistoryID"),
    @NamedQuery(name = "LiveProductDiscount.findByLiveProductID", query = "SELECT l FROM LiveProductDiscount l WHERE l.liveProductID.liveProductID = :liveProductID ORDER BY l.changedAt DESC")
})
public class LiveProductDiscount implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "DiscountHistoryID")
    private Integer discountHistoryID;
    
    @Column(name = "OldPrice")
    private BigDecimal oldPrice;
    
    @Column(name = "NewPrice")
    private BigDecimal newPrice;
    
    @Column(name = "ChangedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date changedAt;
    
    @Size(max = 200)
    @Column(name = "ChangeReason")
    private String changeReason;
    
    @JoinColumn(name = "LiveProductID", referencedColumnName = "LiveProductID")
    @ManyToOne(optional = false)
    private LiveProduct liveProductID;
    
    @JoinColumn(name = "ChangedBy", referencedColumnName = "UserID")
    @ManyToOne
    private Users changedBy;

    public LiveProductDiscount() {
    }

    public LiveProductDiscount(Integer discountHistoryID) {
        this.discountHistoryID = discountHistoryID;
    }

    public Integer getDiscountHistoryID() {
        return discountHistoryID;
    }

    public void setDiscountHistoryID(Integer discountHistoryID) {
        this.discountHistoryID = discountHistoryID;
    }

    public BigDecimal getOldPrice() {
        return oldPrice;
    }

    public void setOldPrice(BigDecimal oldPrice) {
        this.oldPrice = oldPrice;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public void setNewPrice(BigDecimal newPrice) {
        this.newPrice = newPrice;
    }

    public Date getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Date changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public LiveProduct getLiveProductID() {
        return liveProductID;
    }

    public void setLiveProductID(LiveProduct liveProductID) {
        this.liveProductID = liveProductID;
    }

    public Users getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Users changedBy) {
        this.changedBy = changedBy;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (discountHistoryID != null ? discountHistoryID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LiveProductDiscount)) {
            return false;
        }
        LiveProductDiscount other = (LiveProductDiscount) object;
        if ((this.discountHistoryID == null && other.discountHistoryID != null) || (this.discountHistoryID != null && !this.discountHistoryID.equals(other.discountHistoryID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.LiveProductDiscount[ discountHistoryID=" + discountHistoryID + " ]";
    }

}
