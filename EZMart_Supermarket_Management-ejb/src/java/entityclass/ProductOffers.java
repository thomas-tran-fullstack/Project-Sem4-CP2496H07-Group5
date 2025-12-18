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
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 *
 * @author TRUONG LAM
 */
@Entity
@Table(name = "ProductOffers")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProductOffers.findAll", query = "SELECT p FROM ProductOffers p"),
    @NamedQuery(name = "ProductOffers.findByProductOfferID", query = "SELECT p FROM ProductOffers p WHERE p.productOfferID = :productOfferID")})
public class ProductOffers implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ProductOfferID")
    private Integer productOfferID;
    @JoinColumn(name = "OfferID", referencedColumnName = "OfferID")
    @ManyToOne
    private Offers offerID;
    @JoinColumn(name = "ProductID", referencedColumnName = "ProductID")
    @ManyToOne
    private Products productID;

    public ProductOffers() {
    }

    public ProductOffers(Integer productOfferID) {
        this.productOfferID = productOfferID;
    }

    public Integer getProductOfferID() {
        return productOfferID;
    }

    public void setProductOfferID(Integer productOfferID) {
        this.productOfferID = productOfferID;
    }

    public Offers getOfferID() {
        return offerID;
    }

    public void setOfferID(Offers offerID) {
        this.offerID = offerID;
    }

    public Products getProductID() {
        return productID;
    }

    public void setProductID(Products productID) {
        this.productID = productID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (productOfferID != null ? productOfferID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProductOffers)) {
            return false;
        }
        ProductOffers other = (ProductOffers) object;
        if ((this.productOfferID == null && other.productOfferID != null) || (this.productOfferID != null && !this.productOfferID.equals(other.productOfferID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.ProductOffers[ productOfferID=" + productOfferID + " ]";
    }
    
}
