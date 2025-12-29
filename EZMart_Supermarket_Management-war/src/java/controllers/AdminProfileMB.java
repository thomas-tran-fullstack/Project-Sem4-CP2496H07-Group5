package controllers;

import entityclass.Admins;
import entityclass.Users;
import jakarta.inject.Named;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import sessionbeans.AdminsFacadeLocal;

/**
 * Controller for admin profile data - displays Users with ADMIN role
 * @author TRUONG LAM
 */
@Named(value = "adminProfileMB")
@RequestScoped
public class AdminProfileMB {

    @Inject
    private AuthController auth;

    @Inject
    private AdminsFacadeLocal adminsFacade;

    private String adminName;
    private String adminEmail;
    private String adminLevel;
    private String createdAt;

    public void loadAdminProfile() {
        if (auth.getCurrentUser() != null && "ADMIN".equals(auth.getCurrentUser().getRole())) {
            adminName = auth.getCurrentUser().getUsername();
            adminEmail = auth.getCurrentUser().getEmail();
            if (auth.getCurrentUser().getCreatedAt() != null) {
                createdAt = auth.getCurrentUser().getCreatedAt().toString();
            }

            // Get admin-specific data
            Admins admin = adminsFacade.findByUserID(auth.getCurrentUser().getUserID());
            if (admin != null) {
                adminLevel = admin.getAdminLevel();
            }
        }
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
