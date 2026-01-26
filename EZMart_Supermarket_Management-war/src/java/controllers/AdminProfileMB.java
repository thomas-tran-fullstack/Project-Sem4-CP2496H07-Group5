package controllers;

import entityclass.Admins;
import entityclass.Users;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import sessionbeans.AdminsFacadeLocal;
import sessionbeans.UsersFacadeLocal;

@Named(value = "adminProfileMB")
@ViewScoped
public class AdminProfileMB implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private AuthController auth;

    @EJB
    private UsersFacadeLocal usersFacade;

    @EJB
    private AdminsFacadeLocal adminsFacade;

    private String adminLevel;
    private String createdAt;

    private boolean editing;
    private String editUsername;
    private String editEmail;

    public void loadAdminProfile() {
        Users u = auth.getCurrentUserLazy();
        if (u == null || u.getUserID() == null) {
            return;
        }
        if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
            return;
        }

        this.editUsername = u.getUsername();
        this.editEmail = u.getEmail();
        this.createdAt = u.getCreatedAt() != null ? u.getCreatedAt().toString() : null;

        Admins admin = adminsFacade.findByUserID(u.getUserID());
        this.adminLevel = admin != null ? admin.getAdminLevel() : null;

        this.editing = false;
    }

    public String startEdit() {
        editing = true;
        return null;
    }

    public String cancelEdit() {
        editing = false;
        loadAdminProfile();
        return null;
    }

    public String saveProfile() {
        FacesContext fc = FacesContext.getCurrentInstance();
        try {
            Users u = auth.getCurrentUserLazy();
            if (u == null || u.getUserID() == null) {
                throw new IllegalStateException("No logged-in user");
            }
            if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
                throw new IllegalStateException("Not an admin account");
            }

            String nextUsername = editUsername != null ? editUsername.trim() : "";
            String nextEmail = editEmail != null ? editEmail.trim() : "";

            if (nextUsername.isEmpty()) {
                throw new IllegalArgumentException("Username is required");
            }

            // Username uniqueness check (if changed)
            if (!nextUsername.equals(u.getUsername())) {
                Users existingUser = usersFacade.findByUsername(nextUsername);
                if (existingUser != null && existingUser.getUserID() != null && !existingUser.getUserID().equals(u.getUserID())) {
                    throw new IllegalArgumentException("Username already exists");
                }
                u.setUsername(nextUsername);
            }

            // Email uniqueness check (if changed)
            if (!nextEmail.isEmpty() && (u.getEmail() == null || !nextEmail.equalsIgnoreCase(u.getEmail().trim()))) {
                Users existingEmail = usersFacade.findByEmail(nextEmail);
                if (existingEmail != null && existingEmail.getUserID() != null && !existingEmail.getUserID().equals(u.getUserID())) {
                    throw new IllegalArgumentException("Email already exists");
                }
                u.setEmail(nextEmail);
            }

            usersFacade.edit(u);
            auth.setCurrentUser(u);

            editing = false;
            String ok = "Profile updated successfully";
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, ok, null));
            fc.getExternalContext().getRequestMap().put("adminProfileSaveStatus", "success");
            fc.getExternalContext().getRequestMap().put("adminProfileSaveMessage", ok);
        } catch (Exception e) {
            String err = "Save profile failed" + (e.getMessage() != null ? (": " + e.getMessage()) : "");
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, err, null));
            fc.getExternalContext().getRequestMap().put("adminProfileSaveStatus", "error");
            fc.getExternalContext().getRequestMap().put("adminProfileSaveMessage", err);
        }
        return null;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    public String getEditUsername() {
        return editUsername;
    }

    public void setEditUsername(String editUsername) {
        this.editUsername = editUsername;
    }

    public String getEditEmail() {
        return editEmail;
    }

    public void setEditEmail(String editEmail) {
        this.editEmail = editEmail;
    }

    public String getAdminLevel() {
        return adminLevel;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getFullName() {
        return auth.getDisplayName();
    }
}
