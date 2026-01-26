package controllers;

import entityclass.Staffs;
import entityclass.Users;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Date;
import sessionbeans.StaffsFacadeLocal;
import sessionbeans.UsersFacadeLocal;

@Named(value = "staffProfileMB")
@ViewScoped
public class StaffProfileMB implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private AuthController auth;

    @EJB
    private StaffsFacadeLocal staffsFacade;

    @EJB
    private UsersFacadeLocal usersFacade;

    private Staffs staff;
    private String email;
    private boolean editing;

    public void loadStaffProfile() {
        Users u = auth.getCurrentUserLazy();
        if (u == null || u.getUserID() == null) {
            return;
        }

        Staffs s = staffsFacade.findByUserID(u.getUserID());
        if (s == null) {
            s = new Staffs();
            s.setUserID(u);
            s.setCreatedAt(new Date());
            s.setStatus("ACTIVE");
        }
        
        // If staff name fields are empty, try to populate from customer profile or display name
        if ((s.getFirstName() == null || s.getFirstName().trim().isEmpty()) &&
            (s.getMiddleName() == null || s.getMiddleName().trim().isEmpty()) &&
            (s.getLastName() == null || s.getLastName().trim().isEmpty())) {
            
            // Try to get from currentCustomer first (if staff is also a customer)
            if (auth.getCurrentCustomer() != null) {
                if (auth.getCurrentCustomer().getFirstName() != null && !auth.getCurrentCustomer().getFirstName().isEmpty()) {
                    s.setFirstName(auth.getCurrentCustomer().getFirstName());
                }
                if (auth.getCurrentCustomer().getMiddleName() != null && !auth.getCurrentCustomer().getMiddleName().isEmpty()) {
                    s.setMiddleName(auth.getCurrentCustomer().getMiddleName());
                }
                if (auth.getCurrentCustomer().getLastName() != null && !auth.getCurrentCustomer().getLastName().isEmpty()) {
                    s.setLastName(auth.getCurrentCustomer().getLastName());
                }
            } else {
                // Fallback: parse from display name
                String displayName = auth.getDisplayName();
                if (displayName != null && !displayName.trim().isEmpty()) {
                    String[] parts = displayName.trim().split("\\s+");
                    if (parts.length == 1) {
                        s.setFirstName(parts[0]);
                    } else if (parts.length == 2) {
                        s.setFirstName(parts[0]);
                        s.setLastName(parts[1]);
                    } else if (parts.length >= 3) {
                        s.setFirstName(parts[0]);
                        StringBuilder middleName = new StringBuilder();
                        for (int i = 1; i < parts.length - 1; i++) {
                            if (middleName.length() > 0) middleName.append(" ");
                            middleName.append(parts[i]);
                        }
                        s.setMiddleName(middleName.toString());
                        s.setLastName(parts[parts.length - 1]);
                    }
                }
            }
        }
        
        this.staff = s;
        this.email = u.getEmail();
        this.editing = false;
    }

    public String startEdit() {
        editing = true;
        return null;
    }

    public String cancelEdit() {
        editing = false;
        loadStaffProfile();
        return null;
    }

    public String saveProfile() {
        FacesContext fc = FacesContext.getCurrentInstance();
        try {
            Users u = auth.getCurrentUserLazy();
            if (u == null || u.getUserID() == null) {
                throw new IllegalStateException("No logged-in user");
            }
            if (staff == null) {
                staff = staffsFacade.findByUserID(u.getUserID());
                if (staff == null) {
                    staff = new Staffs();
                    staff.setUserID(u);
                    staff.setCreatedAt(new Date());
                    staff.setStatus("ACTIVE");
                }
            }

            // Update email in Users (optional)
            String nextEmail = email != null ? email.trim() : null;
            if (nextEmail != null && !nextEmail.isEmpty()) {
                Users existing = usersFacade.findByEmail(nextEmail);
                if (existing != null && existing.getUserID() != null && !existing.getUserID().equals(u.getUserID())) {
                    throw new IllegalArgumentException("Email already exists");
                }
                u.setEmail(nextEmail);
                usersFacade.edit(u);
                auth.setCurrentUser(u);
            }

            if (staff.getStaffID() == null) {
                staffsFacade.create(staff);
            } else {
                staffsFacade.edit(staff);
            }

            editing = false;
            String ok = "Profile updated successfully";
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, ok, null));
            fc.getExternalContext().getRequestMap().put("staffProfileSaveStatus", "success");
            fc.getExternalContext().getRequestMap().put("staffProfileSaveMessage", ok);
        } catch (Exception e) {
            String err = "Save profile failed" + (e.getMessage() != null ? (": " + e.getMessage()) : "");
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, err, null));
            fc.getExternalContext().getRequestMap().put("staffProfileSaveStatus", "error");
            fc.getExternalContext().getRequestMap().put("staffProfileSaveMessage", err);
        }
        return null;
    }

    public Staffs getStaff() {
        return staff;
    }

    public void setStaff(Staffs staff) {
        this.staff = staff;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    public String getFullName() {
        if (staff == null) return auth.getDisplayName();
        StringBuilder sb = new StringBuilder();
        if (staff.getFirstName() != null && !staff.getFirstName().trim().isEmpty()) sb.append(staff.getFirstName().trim()).append(' ');
        if (staff.getMiddleName() != null && !staff.getMiddleName().trim().isEmpty()) sb.append(staff.getMiddleName().trim()).append(' ');
        if (staff.getLastName() != null && !staff.getLastName().trim().isEmpty()) sb.append(staff.getLastName().trim());
        String res = sb.toString().trim();
        return res.isEmpty() ? auth.getDisplayName() : res;
    }
}
