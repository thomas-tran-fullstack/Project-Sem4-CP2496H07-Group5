package entityclass;

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
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "PersistentLogins")
public class PersistentLogins implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "UserID", referencedColumnName = "UserID")
    private Users userID;

    @Column(name = "Selector", length = 64, unique = true)
    private String selector;

    @Column(name = "ValidatorHash", length = 255)
    private String validatorHash;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ExpiresAt")
    private Date expiresAt;

    public PersistentLogins() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Users getUserID() { return userID; }
    public void setUserID(Users userID) { this.userID = userID; }
    public String getSelector() { return selector; }
    public void setSelector(String selector) { this.selector = selector; }
    public String getValidatorHash() { return validatorHash; }
    public void setValidatorHash(String validatorHash) { this.validatorHash = validatorHash; }
    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
}
