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
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 *
 * @author TRUONG LAM
 */
@Entity
@Table(name = "CreditCards")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "CreditCards.findAll", query = "SELECT c FROM CreditCards c"),
    @NamedQuery(name = "CreditCards.findByCardID", query = "SELECT c FROM CreditCards c WHERE c.cardID = :cardID"),
    @NamedQuery(name = "CreditCards.findByCardNumber", query = "SELECT c FROM CreditCards c WHERE c.cardNumber = :cardNumber"),
    @NamedQuery(name = "CreditCards.findByCardExpiry", query = "SELECT c FROM CreditCards c WHERE c.cardExpiry = :cardExpiry"),
    @NamedQuery(name = "CreditCards.findByCardType", query = "SELECT c FROM CreditCards c WHERE c.cardType = :cardType"),
    @NamedQuery(name = "CreditCards.findByIsDefault", query = "SELECT c FROM CreditCards c WHERE c.isDefault = :isDefault")})
public class CreditCards implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "CardID")
    private Integer cardID;
    @Size(max = 20)
    @Column(name = "CardNumber")
    private String cardNumber;
    @Size(max = 10)
    @Column(name = "CardExpiry")
    private String cardExpiry;
    @Size(max = 20)
    @Column(name = "CardType")
    private String cardType;
    @Column(name = "IsDefault")
    private Boolean isDefault;
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID")
    @ManyToOne(optional = false)
    private Customers customerID;

    public CreditCards() {
    }

    public CreditCards(Integer cardID) {
        this.cardID = cardID;
    }

    public Integer getCardID() {
        return cardID;
    }

    public void setCardID(Integer cardID) {
        this.cardID = cardID;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardExpiry() {
        return cardExpiry;
    }

    public void setCardExpiry(String cardExpiry) {
        this.cardExpiry = cardExpiry;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Customers getCustomerID() {
        return customerID;
    }

    public void setCustomerID(Customers customerID) {
        this.customerID = customerID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (cardID != null ? cardID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CreditCards)) {
            return false;
        }
        CreditCards other = (CreditCards) object;
        if ((this.cardID == null && other.cardID != null) || (this.cardID != null && !this.cardID.equals(other.cardID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.CreditCards[ cardID=" + cardID + " ]";
    }
    
}
