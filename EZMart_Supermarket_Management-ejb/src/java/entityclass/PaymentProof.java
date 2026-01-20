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
@Table(name = "PaymentProof")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PaymentProof.findAll", query = "SELECT p FROM PaymentProof p"),
    @NamedQuery(name = "PaymentProof.findByProofID", query = "SELECT p FROM PaymentProof p WHERE p.proofID = :proofID"),
    @NamedQuery(name = "PaymentProof.findByImagePath", query = "SELECT p FROM PaymentProof p WHERE p.imagePath = :imagePath"),
    @NamedQuery(name = "PaymentProof.findByNote", query = "SELECT p FROM PaymentProof p WHERE p.note = :note"),
    @NamedQuery(name = "PaymentProof.findByUploadedAt", query = "SELECT p FROM PaymentProof p WHERE p.uploadedAt = :uploadedAt"),
    @NamedQuery(name = "PaymentProof.findByVerifiedAt", query = "SELECT p FROM PaymentProof p WHERE p.verifiedAt = :verifiedAt")})
public class PaymentProof implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ProofID")
    private Integer proofID;
    @Size(max = 255)
    @Column(name = "ImagePath")
    private String imagePath;
    @Size(max = 500)
    @Column(name = "Note")
    private String note;
    @Size(max = 50)
    @Column(name = "TransactionID")
    private String transactionID;
    @Size(max = 20)
    @Column(name = "Status")
    private String status;
    @Column(name = "UploadedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date uploadedAt;
    @Column(name = "VerifiedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date verifiedAt;
    @JoinColumn(name = "OrderID", referencedColumnName = "OrderID")
    @ManyToOne
    private Orders orderID;
    @JoinColumn(name = "VerifiedBy", referencedColumnName = "UserID")
    @ManyToOne
    private Users verifiedBy;

    public PaymentProof() {
    }

    public PaymentProof(Integer proofID) {
        this.proofID = proofID;
    }

    public Integer getProofID() {
        return proofID;
    }

    public void setProofID(Integer proofID) {
        this.proofID = proofID;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Date uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Date getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Date verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Orders getOrderID() {
        return orderID;
    }

    public void setOrderID(Orders orderID) {
        this.orderID = orderID;
    }

    public Users getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Users verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (proofID != null ? proofID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PaymentProof)) {
            return false;
        }
        PaymentProof other = (PaymentProof) object;
        if ((this.proofID == null && other.proofID != null) || (this.proofID != null && !this.proofID.equals(other.proofID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.PaymentProof[ proofID=" + proofID + " ]";
    }

}
