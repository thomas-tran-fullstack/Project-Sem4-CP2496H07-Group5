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
@Table(name = "Vouchers")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Vouchers.findAll", query = "SELECT v FROM Vouchers v"),
    @NamedQuery(name = "Vouchers.findByVoucherID", query = "SELECT v FROM Vouchers v WHERE v.voucherID = :voucherID"),
    @NamedQuery(name = "Vouchers.findByVoucherCode", query = "SELECT v FROM Vouchers v WHERE v.voucherCode = :voucherCode"),
    @NamedQuery(name = "Vouchers.findByIsUsed", query = "SELECT v FROM Vouchers v WHERE v.isUsed = :isUsed"),
    @NamedQuery(name = "Vouchers.findByExpiryDate", query = "SELECT v FROM Vouchers v WHERE v.expiryDate = :expiryDate"),
    @NamedQuery(name = "Vouchers.findByCreatedAt", query = "SELECT v FROM Vouchers v WHERE v.createdAt = :createdAt")})
public class Vouchers implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "VoucherID")
    private Integer voucherID;
    @Size(max = 50)
    @Column(name = "VoucherCode")
    private String voucherCode;
    @Column(name = "IsUsed")
    private Boolean isUsed;
    @Column(name = "ExpiryDate")
    @Temporal(TemporalType.DATE)
    private Date expiryDate;
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Column(name = "DiscountValue")
    private BigDecimal discountValue;
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID")
    @ManyToOne
    private Customers customerID;
    @JoinColumn(name = "OfferID", referencedColumnName = "OfferID")
    @ManyToOne
    private Offers offerID;

    public Vouchers() {
    }

    public Vouchers(Integer voucherID) {
        this.voucherID = voucherID;
    }

    public Integer getVoucherID() {
        return voucherID;
    }

    public void setVoucherID(Integer voucherID) {
        this.voucherID = voucherID;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public Boolean getIsUsed() {
        return isUsed;
    }

    public void setIsUsed(Boolean isUsed) {
        this.isUsed = isUsed;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public Customers getCustomerID() {
        return customerID;
    }

    public void setCustomerID(Customers customerID) {
        this.customerID = customerID;
    }

    public Offers getOfferID() {
        return offerID;
    }

    public void setOfferID(Offers offerID) {
        this.offerID = offerID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (voucherID != null ? voucherID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Vouchers)) {
            return false;
        }
        Vouchers other = (Vouchers) object;
        if ((this.voucherID == null && other.voucherID != null) || (this.voucherID != null && !this.voucherID.equals(other.voucherID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.Vouchers[ voucherID=" + voucherID + " ]";
    }

}
