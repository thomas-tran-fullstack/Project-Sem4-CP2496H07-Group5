/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entityclass;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Entity
@Table(name = "Offers")
@XmlRootElement
    @NamedQueries({
    @NamedQuery(name = "Offers.findAll", query = "SELECT o FROM Offers o"),
    @NamedQuery(name = "Offers.findByOfferID", query = "SELECT o FROM Offers o WHERE o.offerID = :offerID"),
    @NamedQuery(name = "Offers.findByOfferName", query = "SELECT o FROM Offers o WHERE o.offerName = :offerName"),
    @NamedQuery(name = "Offers.findByDescription", query = "SELECT o FROM Offers o WHERE o.description = :description"),
    @NamedQuery(name = "Offers.findByOfferType", query = "SELECT o FROM Offers o WHERE o.offerType = :offerType"),
    @NamedQuery(name = "Offers.findByDiscountValue", query = "SELECT o FROM Offers o WHERE o.discountValue = :discountValue"),
    @NamedQuery(name = "Offers.findByStartDate", query = "SELECT o FROM Offers o WHERE o.startDate = :startDate"),
    @NamedQuery(name = "Offers.findByEndDate", query = "SELECT o FROM Offers o WHERE o.endDate = :endDate"),
    @NamedQuery(name = "Offers.findByStatus", query = "SELECT o FROM Offers o WHERE o.status = :status"),
    @NamedQuery(name = "Offers.findByBannerImage", query = "SELECT o FROM Offers o WHERE o.bannerImage = :bannerImage"),
    @NamedQuery(name = "Offers.findByVoucherEnabled", query = "SELECT o FROM Offers o WHERE o.voucherEnabled = :voucherEnabled")})
public class Offers implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "OfferID")
    private Integer offerID;
    @Column(name = "OfferName")
    private String offerName;
    @Column(name = "Description")
    private String description;
    @Size(max = 30)
    @Column(name = "OfferType")
    private String offerType;
    @Column(name = "DiscountValue")
    private Integer discountValue;
    @Column(name = "StartDate")
    @Temporal(TemporalType.DATE)
    private Date startDate;
    @Column(name = "EndDate")
    @Temporal(TemporalType.DATE)
    private Date endDate;
    @Size(max = 20)
    @Column(name = "Status")
    private String status;
    @Size(max = 255)
    @Column(name = "BannerImage")
    private String bannerImage;
    @Column(name = "VoucherEnabled")
    private Boolean voucherEnabled;
    @OneToMany(mappedBy = "offerID")
    private List<ProductOffers> productOffersList;
    @OneToMany(mappedBy = "offerID")
    private List<Vouchers> vouchersList;

    public Offers() {
    }

    public Offers(Integer offerID) {
        this.offerID = offerID;
    }

    public Integer getOfferID() {
        return offerID;
    }

    public void setOfferID(Integer offerID) {
        this.offerID = offerID;
    }

    public String getOfferName() {
        return offerName;
    }

    public void setOfferName(String offerName) {
        this.offerName = offerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOfferType() {
        return offerType;
    }

    public void setOfferType(String offerType) {
        this.offerType = offerType;
    }

    public Integer getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(Integer discountValue) {
        this.discountValue = discountValue;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBannerImage() {
        return bannerImage;
    }

    public void setBannerImage(String bannerImage) {
        this.bannerImage = bannerImage;
    }

    public Boolean getVoucherEnabled() {
        return voucherEnabled;
    }

    public void setVoucherEnabled(Boolean voucherEnabled) {
        this.voucherEnabled = voucherEnabled;
    }

    @XmlTransient
    public List<ProductOffers> getProductOffersList() {
        return productOffersList;
    }

    public void setProductOffersList(List<ProductOffers> productOffersList) {
        this.productOffersList = productOffersList;
    }

    @XmlTransient
    public List<Vouchers> getVouchersList() {
        return vouchersList;
    }

    public void setVouchersList(List<Vouchers> vouchersList) {
        this.vouchersList = vouchersList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (offerID != null ? offerID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Offers)) {
            return false;
        }
        Offers other = (Offers) object;
        if ((this.offerID == null && other.offerID != null) || (this.offerID != null && !this.offerID.equals(other.offerID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.Offers[ offerID=" + offerID + " ]";
    }

}
