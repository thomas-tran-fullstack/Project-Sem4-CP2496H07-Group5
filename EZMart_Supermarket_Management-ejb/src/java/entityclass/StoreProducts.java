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
@Table(name = "StoreProducts")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "StoreProducts.findAll", query = "SELECT s FROM StoreProducts s"),
    @NamedQuery(name = "StoreProducts.findByStoreProductID", query = "SELECT s FROM StoreProducts s WHERE s.storeProductID = :storeProductID"),
    @NamedQuery(name = "StoreProducts.findByStockQuantity", query = "SELECT s FROM StoreProducts s WHERE s.stockQuantity = :stockQuantity")})
public class StoreProducts implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "StoreProductID")
    private Integer storeProductID;
    @Column(name = "StockQuantity")
    private Integer stockQuantity;
    @JoinColumn(name = "ProductID", referencedColumnName = "ProductID")
    @ManyToOne
    private Products productID;
    @JoinColumn(name = "StoreID", referencedColumnName = "StoreID")
    @ManyToOne
    private Stores storeID;

    public StoreProducts() {
    }

    public StoreProducts(Integer storeProductID) {
        this.storeProductID = storeProductID;
    }

    public Integer getStoreProductID() {
        return storeProductID;
    }

    public void setStoreProductID(Integer storeProductID) {
        this.storeProductID = storeProductID;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Products getProductID() {
        return productID;
    }

    public void setProductID(Products productID) {
        this.productID = productID;
    }

    public Stores getStoreID() {
        return storeID;
    }

    public void setStoreID(Stores storeID) {
        this.storeID = storeID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (storeProductID != null ? storeProductID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StoreProducts)) {
            return false;
        }
        StoreProducts other = (StoreProducts) object;
        if ((this.storeProductID == null && other.storeProductID != null) || (this.storeProductID != null && !this.storeProductID.equals(other.storeProductID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.StoreProducts[ storeProductID=" + storeProductID + " ]";
    }
    
}
