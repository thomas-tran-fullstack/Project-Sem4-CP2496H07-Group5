package entityclass;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "Addresses")
public class Addresses implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AddressID")
    private Integer addressID;

    @Size(max = 50)
    @Column(name = "Label")
    private String label;

    @Size(max = 20)
    @Column(name = "Type")
    private String type;

    @Size(max = 50)
    @Column(name = "Region")
    private String region;

    @Size(max = 100)
    @Column(name = "Street")
    private String street;

    @Size(max = 50)
    @Column(name = "House")
    private String house;

    @Size(max = 50)
    @Column(name = "City")
    private String city;

    @Size(max = 50)
    @Column(name = "State")
    private String state;

    @Size(max = 50)
    @Column(name = "Country")
    private String country;

    @Column(name = "Latitude", precision = 9, scale = 6)
    private Double latitude;

    @Column(name = "Longitude", precision = 9, scale = 6)
    private Double longitude;

    @Column(name = "IsDefault")
    private Boolean isDefault;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CreatedAt")
    private Date createdAt;

    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID")
    @ManyToOne(optional = false)
    private Customers customerID;

    public Addresses() {}

    public Integer getAddressID() { return addressID; }
    public void setAddressID(Integer addressID) { this.addressID = addressID; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getHouse() { return house; }
    public void setHouse(String house) { this.house = house; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Customers getCustomerID() { return customerID; }
    public void setCustomerID(Customers customerID) { this.customerID = customerID; }
}
