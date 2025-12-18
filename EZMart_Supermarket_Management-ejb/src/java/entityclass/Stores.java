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
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Entity
@Table(name = "Stores")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Stores.findAll", query = "SELECT s FROM Stores s"),
    @NamedQuery(name = "Stores.findByStoreID", query = "SELECT s FROM Stores s WHERE s.storeID = :storeID"),
    @NamedQuery(name = "Stores.findByStoreName", query = "SELECT s FROM Stores s WHERE s.storeName = :storeName"),
    @NamedQuery(name = "Stores.findByTown", query = "SELECT s FROM Stores s WHERE s.town = :town"),
    @NamedQuery(name = "Stores.findByCity", query = "SELECT s FROM Stores s WHERE s.city = :city"),
    @NamedQuery(name = "Stores.findByLatitude", query = "SELECT s FROM Stores s WHERE s.latitude = :latitude"),
    @NamedQuery(name = "Stores.findByLongitude", query = "SELECT s FROM Stores s WHERE s.longitude = :longitude")})
public class Stores implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "StoreID")
    private Integer storeID;
    @Size(max = 100)
    @Column(name = "StoreName")
    private String storeName;
    @Size(max = 50)
    @Column(name = "Town")
    private String town;
    @Size(max = 50)
    @Column(name = "City")
    private String city;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "Latitude")
    private BigDecimal latitude;
    @Column(name = "Longitude")
    private BigDecimal longitude;
    @OneToMany(mappedBy = "storeID")
    private List<StoreProducts> storeProductsList;

    public Stores() {
    }

    public Stores(Integer storeID) {
        this.storeID = storeID;
    }

    public Integer getStoreID() {
        return storeID;
    }

    public void setStoreID(Integer storeID) {
        this.storeID = storeID;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    @XmlTransient
    public List<StoreProducts> getStoreProductsList() {
        return storeProductsList;
    }

    public void setStoreProductsList(List<StoreProducts> storeProductsList) {
        this.storeProductsList = storeProductsList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (storeID != null ? storeID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Stores)) {
            return false;
        }
        Stores other = (Stores) object;
        if ((this.storeID == null && other.storeID != null) || (this.storeID != null && !this.storeID.equals(other.storeID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.Stores[ storeID=" + storeID + " ]";
    }
    
}
