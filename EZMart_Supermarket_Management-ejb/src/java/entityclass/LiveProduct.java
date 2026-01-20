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
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 *
 * @author EZMart Team
 */
@Entity
@Table(name = "LiveProduct")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "LiveProduct.findAll", query = "SELECT l FROM LiveProduct l"),
    @NamedQuery(name = "LiveProduct.findByLiveProductID", query = "SELECT l FROM LiveProduct l WHERE l.liveProductID = :liveProductID"),
    @NamedQuery(name = "LiveProduct.findBySessionID", query = "SELECT l FROM LiveProduct l WHERE l.sessionID.sessionID = :sessionID AND l.isActive = TRUE"),
    @NamedQuery(name = "LiveProduct.findByProductID", query = "SELECT l FROM LiveProduct l WHERE l.productID.productID = :productID"),
    @NamedQuery(name = "LiveProduct.findBySessionIDAll", query = "SELECT l FROM LiveProduct l WHERE l.sessionID.sessionID = :sessionID")
})
public class LiveProduct implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "LiveProductID")
    private Integer liveProductID;
    
    @Column(name = "OriginalPrice")
    private BigDecimal originalPrice;
    
    @Column(name = "DiscountedPrice")
    private BigDecimal discountedPrice;
    
    @Column(name = "DiscountPercentage")
    private BigDecimal discountPercentage;
    
    @Column(name = "IsActive")
    private Boolean isActive;
    
    @Column(name = "AddedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date addedAt;
    
    @Column(name = "RemovedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date removedAt;
    
    @Column(name = "SalesCount")
    private Integer salesCount;
    
    @JoinColumn(name = "SessionID", referencedColumnName = "SessionID")
    @ManyToOne(optional = false)
    private LiveSession sessionID;
    
    @JoinColumn(name = "ProductID", referencedColumnName = "ProductID")
    @ManyToOne(optional = false)
    private Products productID;
    
    @OneToMany(mappedBy = "liveProductID")
    private List<LiveProductDiscount> liveProductDiscountList;

    public LiveProduct() {
    }

    public LiveProduct(Integer liveProductID) {
        this.liveProductID = liveProductID;
    }

    public Integer getLiveProductID() {
        return liveProductID;
    }

    public void setLiveProductID(Integer liveProductID) {
        this.liveProductID = liveProductID;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Date getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Date addedAt) {
        this.addedAt = addedAt;
    }

    public Date getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(Date removedAt) {
        this.removedAt = removedAt;
    }

    public Integer getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(Integer salesCount) {
        this.salesCount = salesCount;
    }

    public LiveSession getSessionID() {
        return sessionID;
    }

    public void setSessionID(LiveSession sessionID) {
        this.sessionID = sessionID;
    }

    public Products getProductID() {
        return productID;
    }

    public void setProductID(Products productID) {
        this.productID = productID;
    }

    @XmlTransient
    public List<LiveProductDiscount> getLiveProductDiscountList() {
        return liveProductDiscountList;
    }

    public void setLiveProductDiscountList(List<LiveProductDiscount> liveProductDiscountList) {
        this.liveProductDiscountList = liveProductDiscountList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (liveProductID != null ? liveProductID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LiveProduct)) {
            return false;
        }
        LiveProduct other = (LiveProduct) object;
        if ((this.liveProductID == null && other.liveProductID != null) || (this.liveProductID != null && !this.liveProductID.equals(other.liveProductID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.LiveProduct[ liveProductID=" + liveProductID + " ]";
    }

}
