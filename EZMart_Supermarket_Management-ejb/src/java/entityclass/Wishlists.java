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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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
@Table(name = "Wishlists")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Wishlists.findAll", query = "SELECT w FROM Wishlists w"),
    @NamedQuery(name = "Wishlists.findByWishlistID", query = "SELECT w FROM Wishlists w WHERE w.wishlistID = :wishlistID"),
    @NamedQuery(name = "Wishlists.findByCustomerID", query = "SELECT w FROM Wishlists w WHERE w.customerID.customerID = :customerID"),
    @NamedQuery(name = "Wishlists.findByCustomerIDAndDefault", query = "SELECT w FROM Wishlists w WHERE w.customerID.customerID = :customerID AND w.isDefault = true"),
    @NamedQuery(name = "Wishlists.findByCreatedAt", query = "SELECT w FROM Wishlists w WHERE w.createdAt = :createdAt"),
    @NamedQuery(name = "Wishlists.findByUpdatedAt", query = "SELECT w FROM Wishlists w WHERE w.updatedAt = :updatedAt")})
public class Wishlists implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "WishlistID")
    private Integer wishlistID;
    
    @Size(max = 200)
    @Column(name = "Name")
    private String name;
    
    @Column(name = "IsDefault")
    private Boolean isDefault;
    
    @Basic(optional = false)
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Column(name = "UpdatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID")
    @ManyToOne(optional = false)
    private Customers customerID;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "wishlistID", orphanRemoval = true)
    private List<WishlistItems> wishlistItemsList;

    public Wishlists() {
    }

    public Wishlists(Integer wishlistID) {
        this.wishlistID = wishlistID;
    }

    public Wishlists(Integer wishlistID, Date createdAt) {
        this.wishlistID = wishlistID;
        this.createdAt = createdAt;
    }

    public Integer getWishlistID() {
        return wishlistID;
    }

    public void setWishlistID(Integer wishlistID) {
        this.wishlistID = wishlistID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Customers getCustomerID() {
        return customerID;
    }

    public void setCustomerID(Customers customerID) {
        this.customerID = customerID;
    }

    @XmlTransient
    public List<WishlistItems> getWishlistItemsList() {
        return wishlistItemsList;
    }

    public void setWishlistItemsList(List<WishlistItems> wishlistItemsList) {
        this.wishlistItemsList = wishlistItemsList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (wishlistID != null ? wishlistID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Wishlists)) {
            return false;
        }
        Wishlists other = (Wishlists) object;
        if ((this.wishlistID == null && other.wishlistID != null) || (this.wishlistID != null && !this.wishlistID.equals(other.wishlistID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.Wishlists[ wishlistID=" + wishlistID + " ]";
    }

}
