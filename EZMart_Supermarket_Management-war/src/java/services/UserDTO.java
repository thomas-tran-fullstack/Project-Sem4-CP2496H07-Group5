package services;

import java.io.Serializable;
import java.util.Date;

/**
 * Data Transfer Object for User management
 * @author Admin
 */
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer userID;
    private String username;
    private String email;
    private String role;
    private String status;
    private Date createdAt;
    private Date lastOnlineAt;
    private String avatarUrl;
    private String firstName;
    private String lastName;
    private String middleName;
    private String adminLevel;

    public UserDTO() {
    }

    public UserDTO(Integer userID, String username, String email, String role, 
                   String status, Date createdAt, Date lastOnlineAt, String avatarUrl) {
        this.userID = userID;
        this.username = username;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.lastOnlineAt = lastOnlineAt;
        this.avatarUrl = avatarUrl;
    }

    // Getters and Setters
    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastOnlineAt() {
        return lastOnlineAt;
    }

    public void setLastOnlineAt(Date lastOnlineAt) {
        this.lastOnlineAt = lastOnlineAt;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }

    /**
     * Get full name for display (First Middle Last format)
     */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        
        // Add first name
        if (firstName != null && !firstName.trim().isEmpty()) {
            sb.append(firstName.trim());
        }
        
        // Add middle name
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middleName.trim());
        }
        
        // Add last name
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName.trim());
        }
        
        String fullName = sb.toString();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        
        return username != null ? username : "";
    }
}
