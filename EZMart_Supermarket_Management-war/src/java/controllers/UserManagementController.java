package controllers;

import entityclass.Admins;
import entityclass.Customers;
import entityclass.Users;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import services.TimeFormatUtil;
import services.UserDTO;
import sessionbeans.AdminsFacadeLocal;
import sessionbeans.CustomersFacadeLocal;
import sessionbeans.UsersFacadeLocal;

/**
 * Backing bean for User Management page
 * @author Admin
 */
@Named(value = "userManagementController")
@ViewScoped
public class UserManagementController implements Serializable {
    private static final long serialVersionUID = 1L;

    @EJB
    private UsersFacadeLocal usersFacade;

    @EJB
    private CustomersFacadeLocal customersFacade;

    @EJB
    private AdminsFacadeLocal adminsFacade;

    private List<UserDTO> users = new ArrayList<>();
    private String selectedRole = "All";
    private String selectedStatus = "All";
    private String searchQuery = "";
    private String sortBy = "Name: A - Z";

    @PostConstruct
    public void init() {
        System.out.println("=== UserManagementController.init() called ===");
        loadUsers();
        System.out.println("Loaded " + users.size() + " users");
    }

    /**
     * Load all users from database
     */
    public void loadUsers() {
        users.clear();

        try {
            List<Users> allUsers = usersFacade.findAll();

            for (Users user : allUsers) {
                UserDTO dto = convertToDTO(user);
                
                // Apply filters
                if (shouldIncludeUser(dto)) {
                    users.add(dto);
                }
            }
            
            // Apply sorting
            applySorting();
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Convert Users entity to UserDTO with related customer/admin info
     */
    private UserDTO convertToDTO(Users user) {
        UserDTO dto = new UserDTO();
        dto.setUserID(user.getUserID());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() != null ? user.getRole().toUpperCase() : "CUSTOMER");
        dto.setStatus(user.getStatus() != null ? user.getStatus().toUpperCase() : "ACTIVE");
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastOnlineAt(user.getLastOnlineAt());

        // Get additional info from Customers or Admins table
        String avatarUrl = null;

        if ("CUSTOMER".equalsIgnoreCase(user.getRole())) {
            try {
                // Get customer info if exists
                if (user.getCustomersList() != null && !user.getCustomersList().isEmpty()) {
                    Customers customer = user.getCustomersList().get(0);
                    dto.setFirstName(customer.getFirstName());
                    dto.setLastName(customer.getLastName());
                    dto.setMiddleName(customer.getMiddleName());
                    avatarUrl = customer.getAvatarUrl();
                    System.out.println("Customer " + user.getUsername() + ": firstName=" + customer.getFirstName() + ", lastName=" + customer.getLastName() + ", avatar=" + avatarUrl);
                }
            } catch (Exception e) {
                System.err.println("Error getting customer info: " + e.getMessage());
                e.printStackTrace();
            }
        } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            try {
                if (user.getAdminsList() != null && !user.getAdminsList().isEmpty()) {
                    Admins admin = user.getAdminsList().get(0);
                    dto.setAdminLevel(admin.getAdminLevel());
                }
            } catch (Exception e) {
                System.err.println("Error getting admin info: " + e.getMessage());
            }
        } else if ("STAFF".equalsIgnoreCase(user.getRole())) {
            dto.setFirstName(user.getUsername());
        }

        if (avatarUrl == null) {
            avatarUrl = null; // Will use placeholder in JSF
        }
        dto.setAvatarUrl(avatarUrl);
        System.out.println("DTO for " + dto.getUsername() + ": displayName=" + dto.getDisplayName());
        return dto;
    }

    /**
     * Check if user should be included based on current filters
     */
    private boolean shouldIncludeUser(UserDTO user) {
        // Filter by role
        if (!selectedRole.equals("All") && !selectedRole.equalsIgnoreCase(user.getRole())) {
            return false;
        }

        // Filter by status
        if (!selectedStatus.equals("All") && !selectedStatus.equalsIgnoreCase(user.getStatus())) {
            return false;
        }

        // Filter by search query
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String query = searchQuery.trim().toLowerCase();
            String displayName = (user.getFirstName() != null ? user.getFirstName() : "").toLowerCase();
            String email = (user.getEmail() != null ? user.getEmail() : "").toLowerCase();
            
            if (!displayName.contains(query) && !email.contains(query)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Apply sorting based on sortBy selection
     */
    private void applySorting() {
        if ("Name: A - Z".equals(sortBy)) {
            Collections.sort(users, (u1, u2) -> {
                String name1 = u1.getDisplayName() != null ? u1.getDisplayName() : "";
                String name2 = u2.getDisplayName() != null ? u2.getDisplayName() : "";
                return name1.compareToIgnoreCase(name2);
            });
        } else if ("Name: Z - A".equals(sortBy)) {
            Collections.sort(users, (u1, u2) -> {
                String name1 = u1.getDisplayName() != null ? u1.getDisplayName() : "";
                String name2 = u2.getDisplayName() != null ? u2.getDisplayName() : "";
                return name2.compareToIgnoreCase(name1);
            });
        }
    }

    /**
     * Filter users and refresh the list
     */
    public void applyFilters() {
        loadUsers();
    }

    /**
     * Apply sort and refresh
     */
    public void applySortBy() {
        applySorting();
    }

    /**
     * Refresh user list - call this after any user data changes
     */
    public void refresh() {
        System.out.println("UserManagementController.refresh() called");
        loadUsers();
    }

    /**
     * Get avatar URL with fallback for a user
     */
    public String getAvatarUrl(UserDTO user) {
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            return user.getAvatarUrl();
        }
        return "/EZMart_Supermarket_Management-war/avatar?customerId=" + user.getUserID();
    }

    /**
     * Get formatted online time for a user
     */
    public String getFormattedOnlineTime(UserDTO user) {
        if (user.getLastOnlineAt() == null) {
            return "Never";
        }
        return TimeFormatUtil.formatOnlineTime(user.getLastOnlineAt());
    }

    /**
     * Check if user is currently online (online if lastOnlineAt is within last 5 minutes)
     */
    public boolean isUserOnline(UserDTO user) {
        return TimeFormatUtil.isOnline(user.getLastOnlineAt());
    }

    /**
     * Delete a user
     */
    public void deleteUser(Integer userId) {
        try {
            Users user = usersFacade.find(userId);
            if (user != null) {
                usersFacade.remove(user);
                loadUsers();
            }
        } catch (Exception e) {
            System.err.println("Error deleting user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Getters and Setters
    public List<UserDTO> getUsers() {
        return users;
    }

    public void setUsers(List<UserDTO> users) {
        this.users = users;
    }

    public String getSelectedRole() {
        return selectedRole;
    }

    public void setSelectedRole(String selectedRole) {
        this.selectedRole = selectedRole;
    }

    public String getSelectedStatus() {
        return selectedStatus;
    }

    public void setSelectedStatus(String selectedStatus) {
        this.selectedStatus = selectedStatus;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
}

