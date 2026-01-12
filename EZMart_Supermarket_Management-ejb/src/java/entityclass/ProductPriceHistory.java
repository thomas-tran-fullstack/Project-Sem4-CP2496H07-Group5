/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
 * @author TRUONG LAM
 */
@Entity
@Table(name = "ProductPriceHistory")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProductPriceHistory.findAll", query = "SELECT p FROM ProductPriceHistory p"),
    @NamedQuery(name = "ProductPriceHistory.findByPriceHistoryID", query = "SELECT p FROM ProductPriceHistory p WHERE p.priceHistoryID = :priceHistoryID"),
    @NamedQuery(name = "ProductPriceHistory.findByProductID", query = "SELECT p FROM ProductPriceHistory p WHERE p.productID.productID = :productID ORDER BY p.changedAt DESC"),
    @NamedQuery(name = "ProductPriceHistory.findByDateRange", query = "SELECT p FROM ProductPriceHistory p WHERE p.changedAt BETWEEN :startDate AND :endDate ORDER BY p.changedAt DESC")})
public class ProductPriceHistory implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "PriceHistoryID")
    private Integer priceHistoryID;
    @JoinColumn(name = "ProductID", referencedColumnName = "ProductID")
    @ManyToOne
    private Products productID;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "OldPrice")
    private BigDecimal oldPrice;
    @Column(name = "NewPrice")
    private BigDecimal newPrice;
    @Size(max = 100)
    @Column(name = "ChangeReason")
    private String changeReason;
    @JoinColumn(name = "ChangedBy", referencedColumnName = "UserID")
    @ManyToOne
    private Users changedBy;
    @Column(name = "ChangedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date changedAt;

    public ProductPriceHistory() {
    }

    public ProductPriceHistory(Integer priceHistoryID) {
        this.priceHistoryID = priceHistoryID;
    }

    public Integer getPriceHistoryID() {
        return priceHistoryID;
    }

    public void setPriceHistoryID(Integer priceHistoryID) {
        this.priceHistoryID = priceHistoryID;
    }

    public Products getProductID() {
        return productID;
    }

    public void setProductID(Products productID) {
        this.productID = productID;
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

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public Users getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Users changedBy) {
        this.changedBy = changedBy;
    }

    public Date getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Date changedAt) {
        this.changedAt = changedAt;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (priceHistoryID != null ? priceHistoryID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProductPriceHistory)) {
            return false;
        }
        ProductPriceHistory other = (ProductPriceHistory) object;
        if ((this.priceHistoryID == null && other.priceHistoryID != null) || (this.priceHistoryID != null && !this.priceHistoryID.equals(other.priceHistoryID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.ProductPriceHistory[ priceHistoryID=" + priceHistoryID + " ]";
    }

}
