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
import jakarta.persistence.Lob;
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
@Table(name = "AuditLogs")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AuditLogs.findAll", query = "SELECT a FROM AuditLogs a"),
    @NamedQuery(name = "AuditLogs.findByAuditID", query = "SELECT a FROM AuditLogs a WHERE a.auditID = :auditID"),
    @NamedQuery(name = "AuditLogs.findByAction", query = "SELECT a FROM AuditLogs a WHERE a.action = :action"),
    @NamedQuery(name = "AuditLogs.findByActionDate", query = "SELECT a FROM AuditLogs a WHERE a.actionDate = :actionDate")})
public class AuditLogs implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "AuditID")
    private Integer auditID;
    @Size(max = 100)
    @Column(name = "Action")
    private String action;
    @Column(name = "ActionDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date actionDate;
    @Lob
    @Size(max = 2147483647)
    @Column(name = "Description")
    private String description;
    @JoinColumn(name = "UserID", referencedColumnName = "UserID")
    @ManyToOne
    private Users userID;

    public AuditLogs() {
    }

    public AuditLogs(Integer auditID) {
        this.auditID = auditID;
    }

    public Integer getAuditID() {
        return auditID;
    }

    public void setAuditID(Integer auditID) {
        this.auditID = auditID;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Date getActionDate() {
        return actionDate;
    }

    public void setActionDate(Date actionDate) {
        this.actionDate = actionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Users getUserID() {
        return userID;
    }

    public void setUserID(Users userID) {
        this.userID = userID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (auditID != null ? auditID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AuditLogs)) {
            return false;
        }
        AuditLogs other = (AuditLogs) object;
        if ((this.auditID == null && other.auditID != null) || (this.auditID != null && !this.auditID.equals(other.auditID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entityclass.AuditLogs[ auditID=" + auditID + " ]";
    }
    
}
