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
import java.util.Date;

/**
 *
 * @author TRUONG LAM
 */
@Entity
@Table(name = "WishlistItems")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "WishlistItems.findAll", query = "SELECT w FROM WishlistItems w"),
    @NamedQuery(name = "WishlistItems.findByWishlistItemID", query = "SELECT w FROM WishlistItems w WHERE w.wishlistItemID = :wishlistItemID"),
    @NamedQuery(name = "WishlistItems.findByWishlistID", query = "SELECT w FROM WishlistItems w WHERE w.wishlistID.wishlistID = :wishlistID ORDER BY w.addedAt DESC"),
    @NamedQuery(name = "WishlistItems.findByProductID", query = "SELECT w FROM WishlistItems w WHERE w.productID.productID = :productID"),
    @NamedQuery(name = "WishlistItems.findByAddedAt", query = "SELECT w FROM WishlistItems w WHERE w.addedAt = :addedAt"),
    @NamedQuery(name = "WishlistItems.findByWishlistIDAndProductID", 
                 query = "SELECT w FROM WishlistItems w WHERE w.wishlistID.wishlistID = :wishlistID AND w.productID.productID = :productID")})
public class WishlistItems implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "WishlistItemID")
    private Integer wishlistItemID;
    
    @Column(name = "Quantity")
    private Integer quantity;
    
    @Size(max = 500)
    @Column(name = "Note")
    private String note;
    
    @Basic(optional = false)
    @Column(name = "AddedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date addedAt;
    
    @JoinColumn(name = "WishlistID", referencedColumnName = "WishlistID")
    @ManyToOne(optional = false)
    private Wishlists wishlistID;
    
    @JoinColumn(name = "ProductID", referencedColumnName = "ProductID")
    @ManyToOne(optional = false)
    private Products productID;

    public WishlistItems() {
    }

    public WishlistItems(Integer wishlistItemID) {
        this.wishlistItemID = wishlistItemID;
    }

    public WishlistItems(Integer wishlistItemID, Date addedAt) {
        this.wishlistItemID = wishlistItemID;
        this.addedAt = addedAt;
    }

    public Integer getWishlistItemID() {
        return wishlistItemID;
    }

    public void setWishlistItemID(Integer wishlistItemID) {
        this.wishlistItemID = wishlistItemID;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Date addedAt) {
        this.addedAt = addedAt;
    }

    public Wishlists getWishlistID() {
        return wishlistID;
    }

    public void setWishlistID(Wishlists wishlistID) {
        this.wishlistID = wishlistID;
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
        hash += (wishlistItemID != null ? wishlistItemID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof WishlistItems)) {
            return false;
        }
        WishlistItems other = (WishlistItems) object;
        if ((this.wishlistItemID == null && other.wishlistItemID != null) || (this.wishlistItemID != null && !this.wishlistItemID.equals(other.wishlistItemID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.WishlistItems[ wishlistItemID=" + wishlistItemID + " ]";
    }

}
