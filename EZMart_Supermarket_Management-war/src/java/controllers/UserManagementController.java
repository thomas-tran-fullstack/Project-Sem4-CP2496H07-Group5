package controllers;

import entityclass.Admins;
import entityclass.Customers;
import entityclass.Staffs;
import entityclass.Users;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.text.SimpleDateFormat;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import services.TimeFormatUtil;
import services.UserDTO;
import sessionbeans.AdminsFacadeLocal;
import sessionbeans.CustomersFacadeLocal;
import sessionbeans.StaffsFacadeLocal;
import sessionbeans.UsersFacadeLocal;

/**
 * Backing bean for User Management page
 *
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

    @EJB
    private StaffsFacadeLocal staffsFacade;

    private List<UserDTO> allUsers = new ArrayList<>();
    private List<UserDTO> users = new ArrayList<>();
    private String selectedRole = "All";
    private String selectedStatus = "All";
    private String searchQuery = "";
    private String sortBy = "Name: A - Z";
    
    // Pagination
    private int currentPage = 1;
    private int itemsPerPage = 10;
    private int totalPages = 1;
    
    // New user form fields
    private Users newUser = new Users();
    private String newUserUsername = "";
    private String newUserPassword = "";
    private String newUserRole = "";
    private String newUserFirstName = "";
    private String newUserMiddleName = "";
    private String newUserLastName = "";
    private String newUserEmail = "";
    private String newUserPhone = "";
    
    // Form success indicator
    private boolean formSaveSuccess = false;

    // Edit user form fields
    private Integer editUserId;
    private String editUsername = "";
    private String editPassword = "";
    private String editRole = "";
    private String editFirstName = "";
    private String editMiddleName = "";
    private String editLastName = "";
    private String editEmail = "";
    private String editPhone = "";
    private String editStatus = "ACTIVE";
    private boolean editBanned = false;
    private Integer editBanDays = 3;
    private String editBanUntilDisplay = "";
    private boolean editFormSaveSuccess = false;

    // Delete action parameters
    private String deleteActionParam;
    private String deleteUserIdParam;

    @PostConstruct
    public void init() {
        System.out.println("=== UserManagementController.init() called ===");
        loadUsers();
        System.out.println("Loaded " + users.size() + " users");
    }

    // Wrapper đúng tên theo usermanage.xhtml
    public boolean userOnline(UserDTO u) {
        return isUserOnline(u);
    }

    public String formattedOnlineTime(UserDTO u) {
        return getFormattedOnlineTime(u);
    }

    /**
     * Load all users from database (for initial load or filter changes)
     * Resets pagination to page 1
     */
    public void loadUsers() {
        currentPage = 1;
        refreshUsers();
    }
    
    /**
     * Refresh user data while preserving current page
     * Used for auto-refresh polling
     */
    public void refreshUsers() {
        allUsers.clear();

        try {
            List<Users> allDbUsers = usersFacade.findAll();

            for (Users user : allDbUsers) {
                normalizeBanIfExpired(user);
                UserDTO dto = convertToDTO(user);

                // Apply filters
                if (shouldIncludeUser(dto)) {
                    allUsers.add(dto);
                }
            }

            // Apply sorting
            applySorting();
            
            // Calculate pagination (preserves currentPage if it's still valid)
            updatePagination();
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Update pagination based on total filtered users
     */
    private void updatePagination() {
        totalPages = (allUsers.size() + itemsPerPage - 1) / itemsPerPage;
        if (totalPages < 1) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;
        
        // Extract current page users
        users.clear();
        int startIdx = (currentPage - 1) * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, allUsers.size());
        
        if (startIdx < allUsers.size()) {
            users.addAll(allUsers.subList(startIdx, endIdx));
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
        dto.setStatus(getEffectiveStatus(user));
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastOnlineAt(user.getLastOnlineAt());

        // Avatar is stored on Users.AvatarUrl (null/empty => default /images/user.png)
        String avatarUrl = user.getAvatarUrl();

        // Personal info is stored in Customers table (used by customers, but also
        // reused as a profile record for other roles when available)
        try {
            // Try relationship first
            if (user.getCustomersList() != null && !user.getCustomersList().isEmpty()) {
                Customers customer = user.getCustomersList().get(0);
                if (customer != null) {
                    dto.setFirstName(customer.getFirstName());
                    dto.setLastName(customer.getLastName());
                    dto.setMiddleName(customer.getMiddleName());
                }
            }

            // If still empty, query Customers table directly
            if ((dto.getFirstName() == null || dto.getFirstName().isEmpty()) && user.getUserID() != null) {
                Customers customer = customersFacade.findByUserID(user.getUserID());
                if (customer != null) {
                    dto.setFirstName(customer.getFirstName());
                    dto.setLastName(customer.getLastName());
                    dto.setMiddleName(customer.getMiddleName());
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting customer info: " + e.getMessage());
            e.printStackTrace();
        }

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            try {
                if (user.getAdminsList() != null && !user.getAdminsList().isEmpty()) {
                    Admins admin = user.getAdminsList().get(0);
                    if (admin != null) {
                        dto.setAdminLevel(admin.getAdminLevel());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error getting admin info: " + e.getMessage());
            }
        } else if ("STAFF".equalsIgnoreCase(user.getRole())) {
            dto.setFirstName(user.getUsername());
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
            Collections.sort(allUsers, (u1, u2) -> {
                String name1 = u1.getDisplayName() != null ? u1.getDisplayName() : "";
                String name2 = u2.getDisplayName() != null ? u2.getDisplayName() : "";
                return name1.compareToIgnoreCase(name2);
            });
        } else if ("Name: Z - A".equals(sortBy)) {
            Collections.sort(allUsers, (u1, u2) -> {
                String name1 = u1.getDisplayName() != null ? u1.getDisplayName() : "";
                String name2 = u2.getDisplayName() != null ? u2.getDisplayName() : "";
                return name2.compareToIgnoreCase(name1);
            });
        } else if ("Oldest online".equals(sortBy)) {
            Collections.sort(allUsers, (u1, u2) -> {
                Date d1 = u1.getLastOnlineAt();
                Date d2 = u2.getLastOnlineAt();
                
                // Nulls first (never logged in)
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return -1;
                if (d2 == null) return 1;
                
                return d1.compareTo(d2);
            });
        } else if ("Newest online".equals(sortBy)) {
            Collections.sort(allUsers, (u1, u2) -> {
                Date d1 = u1.getLastOnlineAt();
                Date d2 = u2.getLastOnlineAt();
                
                // Nulls last (never logged in)
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                
                return d2.compareTo(d1);
            });
        }
    }

    /**
     * Filter users and refresh the list
     */
    public void applyFilters() {
        loadUsers();
        updatePagination();
    }

    /**
     * Apply sort and refresh
     */
    public void applySortBy() {
        applySorting();
        updatePagination();
    }
    
    /**
     * Go to next page
     */
    public void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }
    
    /**
     * Go to previous page
     */
    public void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
        }
    }
    
    /**
     * Update page (called from pagination buttons after currentPage is set)
     */
    public void updatePageDisplay() {
        updatePagination();
    }
    
    /**
     * Get list of page numbers for pagination
     */
    public List<Integer> getPageNumbers() {
        List<Integer> pages = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pages.add(i);
        }
        return pages;
    }

    /**
     * Get formatted online time for a user
     * Returns proper display text based on status:
     * - "Online" for currently logged-in users
     * - "Just Now" for users offline < 1 minute
     * - "X minutes/hours/days ago" for users offline >= 1 minute
     * - "Offline" for users never logged in (null lastOnlineAt)
     */
    public String getFormattedOnlineTime(UserDTO user) {
        if (user == null) {
            return "";
        }
        
        // Check if user is currently online
        if (isUserOnline(user)) {
            return "Online";
        }
        
        // Check if user recently went offline (< 1 minute)
        if (isUserJustNow(user)) {
            return "Just Now";
        }
        
        // If lastOnlineAt exists but > 1 minute ago, show time ago
        if (user.getLastOnlineAt() != null) {
            return TimeFormatUtil.formatOnlineTime(user.getLastOnlineAt());
        }
        
        // If no lastOnlineAt set, user never logged in
        return "Offline";
    }

    /**
     * Get CSS class for online status badge
     * Returns:
     * - "online-badge online-now" for online (green)
     * - "online-badge online-off" for just now (green)
     * - "online-badge online-offline" for offline or time ago (gray)
     */
    public String getOnlineStatusCssClass(UserDTO user) {
        if (user == null) {
            return "online-badge online-offline";
        }
        
        // Currently online
        if (isUserOnline(user)) {
            return "online-badge online-now";
        }
        
        // Recently offline (< 1 minute)
        if (isUserJustNow(user)) {
            return "online-badge online-off";
        }
        
        // Offline or time ago
        return "online-badge online-offline";
    }

    /**
     * Check if user is currently online (actively logged in)
     */
    public boolean isUserOnline(UserDTO user) {
        if (user == null) {
            return false;
        }
        return TimeFormatUtil.isOnline(user.getLastOnlineAt());
    }
    
    /**
     * Check if user recently went offline (within 1 minute)
     */
    public boolean isUserJustNow(UserDTO user) {
        if (user == null) {
            return false;
        }
        return TimeFormatUtil.isJustNow(user.getLastOnlineAt());
    }

    /**
     * Resolve avatar URL for UI. If Users.AvatarUrl is empty -> bundled
     * default. Adds cache-busting timestamp for proper refresh.
     */
    public String getAvatarSrc(UserDTO user) {
        try {
            String ctx = jakarta.faces.context.FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
            if (user == null) {
                return ctx + "/resources/images/user.png";
            }
            String a = user.getAvatarUrl();
            if (a == null || a.trim().isEmpty()) {
                return ctx + "/resources/images/user.png";
            }
            // Add cache-busting timestamp to force fresh avatar on update
            String cacheBuster = "?t=" + System.currentTimeMillis();
            if (a.contains("?")) {
                return a + "&t=" + System.currentTimeMillis();
            } else {
                return a + cacheBuster;
            }
        } catch (Exception e) {
            return "/resources/images/user.png";
        }
    }

    public String avatarSrc(UserDTO u) {
        String ctx = jakarta.faces.context.FacesContext.getCurrentInstance()
                .getExternalContext().getRequestContextPath();

        String url = (u != null) ? u.getAvatarUrl() : null;
        if (url == null || url.trim().isEmpty()) {
            return ctx + "/resources/images/user.png";
        }
        url = url.trim();

        // Add cache-busting timestamp to force fresh avatar
        String cacheBuster = "?t=" + System.currentTimeMillis();
        
        // absolute url
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) {
            if (url.contains("?")) {
                return url + "&t=" + System.currentTimeMillis();
            }
            return url + cacheBuster;
        }

        // already context path
        if (url.startsWith(ctx + "/")) {
            if (url.contains("?")) {
                return url + "&t=" + System.currentTimeMillis();
            }
            return url + cacheBuster;
        }

        // starts with /
        if (url.startsWith("/")) {
            String fullUrl = ctx + url;
            if (fullUrl.contains("?")) {
                return fullUrl + "&t=" + System.currentTimeMillis();
            }
            return fullUrl + cacheBuster;
        }

        // relative
        String fullUrl = ctx + "/" + url;
        if (fullUrl.contains("?")) {
            return fullUrl + "&t=" + System.currentTimeMillis();
        }
        return fullUrl + cacheBuster;
    }

    /**
     * Deactivate a user (set status to INACTIVE)
     */
    public void deactivateUser(Integer userId) {
        System.out.println("=== deactivateUser called with userId: " + userId + " ===");
        try {
            Users user = usersFacade.find(userId);
            System.out.println("Found user: " + (user != null ? user.getUsername() : "null"));
            if (user != null) {
                System.out.println("Current status: " + user.getStatus());
                user.setStatus("INACTIVE");
                user.setBanUntil(null);
                System.out.println("Setting status to INACTIVE");
                usersFacade.edit(user);
                System.out.println("User edited successfully");

                // Force logout immediately if this user is currently logged in
                try {
                    utils.OnlineUserRegistry.forceLogout(userId);
                } catch (Exception ignored) {
                }

                // Verify the change
                Users updatedUser = usersFacade.find(userId);
                System.out.println("After edit - status: " + (updatedUser != null ? updatedUser.getStatus() : "null"));

                loadUsers();
                System.out.println("Users reloaded");
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "User '" + user.getUsername() + "' has been deactivated.", ""));
            } else {
                System.out.println("User not found with ID: " + userId);
            }
        } catch (Exception e) {
            System.err.println("Error deactivating user: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error deactivating user: " + e.getMessage(), ""));
        }
    }

    /**
     * Permanently delete a user
     */
    public void deleteUserPermanently(Integer userId) {
        try {
            Users user = usersFacade.find(userId);
            if (user != null) {
                // Delete related customer/admin records first
                if ("CUSTOMER".equalsIgnoreCase(user.getRole())) {
                    try {
                        Customers customer = customersFacade.findByUserID(user.getUserID());
                        if (customer != null) {
                            customersFacade.remove(customer);
                        }
                    } catch (Exception e) {
                        System.err.println("Error deleting customer record: " + e.getMessage());
                    }
                } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                    try {
                        Admins admin = adminsFacade.findByUserID(user.getUserID());
                        if (admin != null) {
                            adminsFacade.remove(admin);
                        }
                    } catch (Exception e) {
                        System.err.println("Error deleting admin record: " + e.getMessage());
                    }
                }

                // Delete the user
                usersFacade.remove(user);
                loadUsers();
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "User '" + user.getUsername() + "' has been permanently deleted.", ""));
            }
        } catch (Exception e) {
            System.err.println("Error deleting user: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error deleting user: " + e.getMessage(), ""));
        }
    }

    /**
     * Get the appropriate delete action text based on user status
     */
    public String getDeleteActionText(UserDTO user) {
        if (user == null) return "";
        return "INACTIVE".equalsIgnoreCase(user.getStatus()) ? "Delete Permanently" : "Deactivate";
    }

    /**
     * Get the appropriate delete confirmation message based on user status
     */
    public String getDeleteConfirmationMessage(UserDTO user) {
        if (user == null) return "";
        if ("INACTIVE".equalsIgnoreCase(user.getStatus())) {
            return "Are you sure you want to delete this user? This action cannot be reversed.";
        } else {
            return "First time deletion will deactivate this account (Inactive), and second deletion will remove the account permanently. Are you sure you want to Deactivate (Inactive) this account?";
        }
    }

    /**
     * Handle delete action from modal
     */
    public void handleDeleteAction() {
        FacesContext context = FacesContext.getCurrentInstance();

        System.out.println("=== handleDeleteAction called ===");
        System.out.println("deleteActionParam: " + deleteActionParam);
        System.out.println("deleteUserIdParam: " + deleteUserIdParam);

        if (deleteActionParam != null && deleteUserIdParam != null) {
            try {
                Integer userId = Integer.parseInt(deleteUserIdParam);
                System.out.println("Parsed userId: " + userId);

                if ("deactivate".equals(deleteActionParam)) {
                    System.out.println("Calling deactivateUser for userId: " + userId);
                    deactivateUser(userId);
                } else if ("permanent".equals(deleteActionParam)) {
                    System.out.println("Calling deleteUserPermanently for userId: " + userId);
                    deleteUserPermanently(userId);
                }
            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException: " + e.getMessage());
                context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Invalid user ID format.", ""));
            }
        } else {
            System.out.println("deleteActionParam or deleteUserIdParam is null");
        }

        // Reset the parameters
        deleteActionParam = null;
        deleteUserIdParam = null;
    }

    private void normalizeBanIfExpired(Users user) {
        if (user == null) {
            return;
        }

        try {
            Date now = new Date();
            Date banUntil = user.getBanUntil();

            boolean statusIsBanned = user.getStatus() != null && "BANNED".equalsIgnoreCase(user.getStatus());
            boolean banExpired = banUntil != null && !banUntil.after(now);
            boolean shouldClear = banExpired || (statusIsBanned && banUntil == null);

            if (!shouldClear) {
                return;
            }

            user.setBanUntil(null);
            if (statusIsBanned) {
                user.setStatus("ACTIVE");
            }

            usersFacade.edit(user);
        } catch (Exception e) {
            // best-effort normalization
        }
    }

    private String getEffectiveStatus(Users user) {
        if (user == null) {
            return "ACTIVE";
        }

        String base = user.getStatus() != null ? user.getStatus().toUpperCase() : "ACTIVE";
        if ("INACTIVE".equalsIgnoreCase(base)) {
            return "INACTIVE";
        }

        Date banUntil = user.getBanUntil();
        if (banUntil != null && banUntil.after(new Date())) {
            return "BANNED";
        }

        if ("BANNED".equalsIgnoreCase(base)) {
            return "ACTIVE";
        }

        return base;
    }

    private String formatBanUntil(Date banUntil) {
        if (banUntil == null) {
            return "";
        }
        try {
            return new SimpleDateFormat("HH:mm dd/MM/yyyy").format(banUntil);
        } catch (Exception e) {
            return "";
        }
    }

    private int guessBanDays(Date banUntil, Date now) {
        if (banUntil == null || now == null) {
            return 3;
        }
        long diffMs = banUntil.getTime() - now.getTime();
        long days = (diffMs + 86_400_000L - 1) / 86_400_000L;
        int[] options = new int[]{1, 3, 7, 14, 30};
        for (int opt : options) {
            if (days <= opt) {
                return opt;
            }
        }
        return 30;
    }

    public void editUser(Integer userId) {
        editFormSaveSuccess = false;
        editPassword = "";
        editBanUntilDisplay = "";
        editBanned = false;

        if (userId == null) {
            return;
        }

        try {
            Users user = usersFacade.find(userId);
            if (user == null) {
                return;
            }

            normalizeBanIfExpired(user);

            editUserId = user.getUserID();
            editUsername = user.getUsername() != null ? user.getUsername() : "";
            editRole = user.getRole() != null ? user.getRole().toUpperCase() : "CUSTOMER";
            editEmail = user.getEmail() != null ? user.getEmail() : "";
            editStatus = user.getStatus() != null ? user.getStatus().toUpperCase() : "ACTIVE";
            if (!"ACTIVE".equalsIgnoreCase(editStatus) && !"INACTIVE".equalsIgnoreCase(editStatus)) {
                editStatus = "ACTIVE";
            }

            Customers customer = customersFacade.findByUserID(user.getUserID());
            if (customer != null) {
                editFirstName = customer.getFirstName() != null ? customer.getFirstName() : "";
                editMiddleName = customer.getMiddleName() != null ? customer.getMiddleName() : "";
                editLastName = customer.getLastName() != null ? customer.getLastName() : "";
                editPhone = customer.getMobilePhone() != null ? customer.getMobilePhone() : "";
            } else {
                editFirstName = "";
                editMiddleName = "";
                editLastName = "";
                editPhone = "";
            }

            Date now = new Date();
            Date banUntil = user.getBanUntil();
            editBanned = banUntil != null && banUntil.after(now) && "ACTIVE".equalsIgnoreCase(editStatus);
            if (editBanned) {
                editBanUntilDisplay = formatBanUntil(banUntil);
                editBanDays = guessBanDays(banUntil, now);
            } else {
                editBanDays = 3;
            }

        } catch (Exception e) {
            // keep modal closed if errors
        }
    }

    public void updateUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        editFormSaveSuccess = false;

        boolean hasErrors = false;

        if (editUserId == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "No user selected to update", ""));
            return;
        }

        if (editUsername == null || editUsername.trim().isEmpty()) {
            context.addMessage("editUserForm:editUsername",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Username is required", ""));
            hasErrors = true;
        }
        if (editRole == null || editRole.trim().isEmpty()) {
            context.addMessage("editUserForm:editRole",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Role is required", ""));
            hasErrors = true;
        }
        if (editFirstName == null || editFirstName.trim().isEmpty()) {
            context.addMessage("editUserForm:editFirstName",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "First name is required", ""));
            hasErrors = true;
        }
        if (editLastName == null || editLastName.trim().isEmpty()) {
            context.addMessage("editUserForm:editLastName",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Last name is required", ""));
            hasErrors = true;
        }
        if (editEmail == null || editEmail.trim().isEmpty()) {
            context.addMessage("editUserForm:editEmail",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Email is required", ""));
            hasErrors = true;
        }
        if (editPhone == null || editPhone.trim().isEmpty()) {
            context.addMessage("editUserForm:editPhone",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Phone number is required", ""));
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }

        if (!isValidEmail(editEmail.trim())) {
            context.addMessage("editUserForm:editEmail",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid email format", ""));
            return;
        }

        if (!isValidPhone(editPhone.trim())) {
            context.addMessage("editUserForm:editPhone",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid phone number format. Use format: +84XXXXXXXXX or 0XXXXXXXXX", ""));
            return;
        }

        Users user = usersFacade.find(editUserId);
        if (user == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "User not found", ""));
            return;
        }

        String newUsername = editUsername.trim();
        if (user.getUsername() == null || !newUsername.equalsIgnoreCase(user.getUsername())) {
            Users existing = usersFacade.findByUsername(newUsername);
            if (existing != null && existing.getUserID() != null && !existing.getUserID().equals(user.getUserID())) {
                context.addMessage("editUserForm:editUsername",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Username already exists", ""));
                return;
            }
        }

        String newEmail = editEmail.trim();
        String currentEmail = user.getEmail() != null ? user.getEmail().trim() : "";
        if (!newEmail.equalsIgnoreCase(currentEmail)) {
            Users emailExist = usersFacade.findByEmail(newEmail);
            if (emailExist != null && emailExist.getUserID() != null && !emailExist.getUserID().equals(user.getUserID())) {
                context.addMessage("editUserForm:editEmail",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Email already in use", ""));
                return;
            }
        }

        Customers customerWithPhone = customersFacade.findByMobilePhone(editPhone.trim());
        if (customerWithPhone != null && customerWithPhone.getUserID() != null
                && customerWithPhone.getUserID().getUserID() != null
                && !customerWithPhone.getUserID().getUserID().equals(user.getUserID())) {
            context.addMessage("editUserForm:editPhone",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Phone number already in use", ""));
            return;
        }

        try {
            user.setUsername(newUsername);
            user.setEmail(newEmail);
            user.setRole(editRole.trim().toUpperCase());

            String status = (editStatus != null ? editStatus.trim().toUpperCase() : "ACTIVE");
            if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
                status = "ACTIVE";
            }
            user.setStatus(status);

            // Optional password update
            if (editPassword != null && !editPassword.trim().isEmpty()) {
                user.setPasswordHash(hashPassword(editPassword.trim()));
            }

            // Ban logic: banned is derived from BanUntil
            if ("INACTIVE".equalsIgnoreCase(status)) {
                user.setBanUntil(null);
            } else if (editBanned) {
                int days = (editBanDays != null ? editBanDays : 3);
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_MONTH, days);
                user.setBanUntil(cal.getTime());
            } else {
                user.setBanUntil(null);
            }

            usersFacade.edit(user);

            // Upsert customer record for personal info storage
            Customers customer = customersFacade.findByUserID(user.getUserID());
            if (customer == null) {
                customer = new Customers();
                customer.setUserID(user);
                customer.setCreatedAt(new Date());
                customer.setFirstName(editFirstName.trim());
                customer.setMiddleName(editMiddleName != null ? editMiddleName.trim() : "");
                customer.setLastName(editLastName.trim());
                customer.setMobilePhone(editPhone.trim());
                customersFacade.create(customer);
            } else {
                customer.setFirstName(editFirstName.trim());
                customer.setMiddleName(editMiddleName != null ? editMiddleName.trim() : "");
                customer.setLastName(editLastName.trim());
                customer.setMobilePhone(editPhone.trim());
                customersFacade.edit(customer);
            }

            // Update display string
            editBanUntilDisplay = formatBanUntil(user.getBanUntil());

            // Force logout if user becomes INACTIVE or BANNED
            try {
                boolean shouldKick = "INACTIVE".equalsIgnoreCase(status)
                        || (user.getBanUntil() != null && user.getBanUntil().after(new Date()));
                if (shouldKick) {
                    utils.OnlineUserRegistry.forceLogout(user.getUserID());
                }
            } catch (Exception ignored) {
            }

            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "User '" + user.getUsername() + "' updated successfully!", ""));

            editFormSaveSuccess = true;
            refreshUsers();

        } catch (Exception e) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error updating user: " + e.getMessage(), ""));
            editFormSaveSuccess = false;
        }
    }

    public void resetEditUserForm() {
        editUserId = null;
        editUsername = "";
        editPassword = "";
        editRole = "";
        editFirstName = "";
        editMiddleName = "";
        editLastName = "";
        editEmail = "";
        editPhone = "";
        editStatus = "ACTIVE";
        editBanned = false;
        editBanDays = 3;
        editBanUntilDisplay = "";
        editFormSaveSuccess = false;
    }

    // Getters and Setters
    public List<UserDTO> getUsers() {
        return users;
    }

    public void setUsers(List<UserDTO> users) {
        this.users = users;
    }
    
    public List<UserDTO> getAllUsers() {
        return allUsers;
    }

    public void setAllUsers(List<UserDTO> allUsers) {
        this.allUsers = allUsers;
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
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        updatePagination();
    }
    
    public int getItemsPerPage() {
        return itemsPerPage;
    }
    
    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    // New user form getters/setters
    public Users getNewUser() {
        return newUser;
    }
    
    public void setNewUser(Users newUser) {
        this.newUser = newUser;
    }
    
    public String getNewUserUsername() {
        return newUserUsername;
    }
    
    public void setNewUserUsername(String newUserUsername) {
        this.newUserUsername = newUserUsername;
    }
    
    public String getNewUserPassword() {
        return newUserPassword;
    }
    
    public void setNewUserPassword(String newUserPassword) {
        this.newUserPassword = newUserPassword;
    }
    
    public String getNewUserRole() {
        return newUserRole;
    }
    
    public void setNewUserRole(String newUserRole) {
        this.newUserRole = newUserRole;
    }
    
    public String getNewUserFirstName() {
        return newUserFirstName;
    }
    
    public void setNewUserFirstName(String newUserFirstName) {
        this.newUserFirstName = newUserFirstName;
    }
    
    public String getNewUserMiddleName() {
        return newUserMiddleName;
    }
    
    public void setNewUserMiddleName(String newUserMiddleName) {
        this.newUserMiddleName = newUserMiddleName;
    }
    
    public String getNewUserLastName() {
        return newUserLastName;
    }
    
    public void setNewUserLastName(String newUserLastName) {
        this.newUserLastName = newUserLastName;
    }
    
    public String getNewUserEmail() {
        return newUserEmail;
    }
    
    public void setNewUserEmail(String newUserEmail) {
        this.newUserEmail = newUserEmail;
    }
    
    public String getNewUserPhone() {
        return newUserPhone;
    }
    
    public void setNewUserPhone(String newUserPhone) {
        this.newUserPhone = newUserPhone;
    }
    
    /**
     * Create new user and related customer/staff records with full validation
     */
    public void createNewUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        formSaveSuccess = false; // Reset at the beginning of the method
        
        System.out.println("=== createNewUser() called ===");
        System.out.println("Username: " + (newUserUsername != null ? newUserUsername : "null"));
        System.out.println("Password: " + (newUserPassword != null ? "provided" : "null"));
        System.out.println("Email: " + (newUserEmail != null ? newUserEmail : "null"));
        System.out.println("Role: " + (newUserRole != null ? newUserRole : "null"));
        
        // Flag to track if there are validation errors
        boolean hasErrors = false;
        
        // FIRST PHASE: Check all required fields - collect ALL errors
        if (newUserUsername == null || newUserUsername.trim().isEmpty()) {
            System.out.println("Validation error: Username is required");
            context.addMessage("addUserForm:username", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Username is required", ""));
            hasErrors = true;
        }
        
        if (newUserPassword == null || newUserPassword.trim().isEmpty()) {
            context.addMessage("addUserForm:password", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Password is required", ""));
            hasErrors = true;
        }
        
        if (newUserEmail == null || newUserEmail.trim().isEmpty()) {
            context.addMessage("addUserForm:email", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Email is required", ""));
            hasErrors = true;
        }
        
        if (newUserRole == null || newUserRole.trim().isEmpty()) {
            context.addMessage("addUserForm:role", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Role is required", ""));
            hasErrors = true;
        }
        
        if (newUserFirstName == null || newUserFirstName.trim().isEmpty()) {
            context.addMessage("addUserForm:firstName", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "First name is required", ""));
            hasErrors = true;
        }
        
        if (newUserLastName == null || newUserLastName.trim().isEmpty()) {
            context.addMessage("addUserForm:lastName", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Last name is required", ""));
            hasErrors = true;
        }
        
        if (newUserPhone == null || newUserPhone.trim().isEmpty()) {
            context.addMessage("addUserForm:phone", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Phone number is required", ""));
            hasErrors = true;
        }
        
        // If required fields are empty, stop here and show all errors
        if (hasErrors) {
            System.out.println("Required field validation failed. Showing all errors.");
            return;
        }
        
        // SECOND PHASE: Check format of email and phone
        if (!isValidEmail(newUserEmail)) {
            context.addMessage("addUserForm:email", 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid email format", ""));
            return;
        }
        
        // Validate phone format if provided
        if (newUserPhone != null && !newUserPhone.trim().isEmpty()) {
            if (!isValidPhone(newUserPhone)) {
                context.addMessage("addUserForm:phone", 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid phone number format. Use format: +84 XXX XXX XXXX or 0XXX XXX XXXX", ""));
                return;
            }
        }
        
        // THIRD PHASE: Check for duplicates (username, email, phone)
        try {
            Users existingUser = usersFacade.findByUsername(newUserUsername.trim());
            if (existingUser != null) {
                context.addMessage("addUserForm:username", 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Username already exists", ""));
                return;
            }
        } catch (Exception e) {
            System.out.println("Error checking username: " + e.getMessage());
        }
        
        try {
            Users emailExist = usersFacade.findByEmail(newUserEmail.trim());
            if (emailExist != null) {
                context.addMessage("addUserForm:email", 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Email already in use", ""));
                return;
            }
        } catch (Exception e) {
            System.out.println("Error checking email: " + e.getMessage());
        }
        
        // Check if phone number already exists in Customer
        try {
            Customers customerWithPhone = customersFacade.findByMobilePhone(newUserPhone.trim());
            if (customerWithPhone != null) {
                context.addMessage("addUserForm:phone", 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Phone number already in use", ""));
                return;
            }
        } catch (Exception e) {
            System.out.println("Error checking phone number in customers: " + e.getMessage());
        }
        
        // All validations passed, try to save
        try {
            // Hash password
            String hashedPassword = hashPassword(newUserPassword);
            newUser.setPasswordHash(hashedPassword);
            
            // Set user properties
            newUser.setUsername(newUserUsername.trim());
            newUser.setRole(newUserRole.toUpperCase());
            newUser.setStatus("ACTIVE");
            newUser.setEmail(newUserEmail.trim());
            newUser.setCreatedAt(new Date());
            
            // Save user to database
            usersFacade.create(newUser);
            System.out.println("UserManagementController.createNewUser: Created user " + newUser.getUsername());
            
            // Create related customer or staff record if needed
            if ("CUSTOMER".equalsIgnoreCase(newUserRole)) {
                try {
                    Customers customer = new Customers();
                    customer.setFirstName(newUserFirstName != null ? newUserFirstName : "");
                    customer.setMiddleName(newUserMiddleName != null ? newUserMiddleName : "");
                    customer.setLastName(newUserLastName != null ? newUserLastName : "");
                    customer.setMobilePhone(newUserPhone != null ? newUserPhone : "");
                    customer.setUserID(newUser);
                    customersFacade.create(customer);
                    System.out.println("UserManagementController.createNewUser: Created customer for user " + newUser.getUsername());
                } catch (Exception e) {
                    System.err.println("UserManagementController.createNewUser: Error creating customer: " + e.getMessage());
                }
            } else if ("STAFF".equalsIgnoreCase(newUserRole)) {
                try {
                    Staffs staff = new Staffs();
                    staff.setFirstName(newUserFirstName != null ? newUserFirstName : "");
                    staff.setMiddleName(newUserMiddleName != null ? newUserMiddleName : "");
                    staff.setLastName(newUserLastName != null ? newUserLastName : "");
                    staff.setMobilePhone(newUserPhone != null ? newUserPhone : "");
                    staff.setStatus("ACTIVE");
                    staff.setCreatedAt(new Date());
                    staff.setUserID(newUser);
                    staffsFacade.create(staff);
                    System.out.println("UserManagementController.createNewUser: Created staff for user " + newUser.getUsername());
                } catch (Exception e) {
                    System.err.println("UserManagementController.createNewUser: Error creating staff: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Success message - show with notification
            context.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "User '" + newUser.getUsername() + "' created successfully!", ""));
            formSaveSuccess = true; // Set to true on successful creation
            
            // Reload user list
            loadUsers();
            
            // Reset form
            resetNewUserForm();
            
        } catch (Exception e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
            context.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error saving user: " + e.getMessage(), ""));
            formSaveSuccess = false; // Ensure it's false on error
        }
    }
    
    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        return email.matches(emailRegex);
    }
    
    /**
     * Validate phone format: +84 XXX XXX XXXX or 0XXX XXX XXXX
     */
    private boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) return false;
        String phoneRegex = "^(\\+84|0)[0-9]{8,9}$|^(\\+84 |0)?[0-9]{3} [0-9]{3} [0-9]{4}$";
        return phone.replaceAll("\\s", "").matches("^(\\+84|0)[0-9]{8,9}$");
    }
    
    /**
     * Reset new user form
     */
    public void resetNewUserForm() {
        newUser = new Users();
        newUserUsername = "";
        newUserPassword = "";
        newUserRole = "";
        newUserFirstName = "";
        newUserMiddleName = "";
        newUserLastName = "";
        newUserEmail = "";
        newUserPhone = "";
        formSaveSuccess = false; // Reset success flag
    }
    
    /**
     * Hash password using same logic as AuthController
     */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return password; // fallback
        }
    }
    
    public boolean isFormSaveSuccess() {
        return formSaveSuccess;
    }

    public void setFormSaveSuccess(boolean formSaveSuccess) {
        this.formSaveSuccess = formSaveSuccess;
    }

    // Delete action getters and setters
    public String getDeleteActionParam() {
        return deleteActionParam;
    }

    public void setDeleteActionParam(String deleteActionParam) {
        this.deleteActionParam = deleteActionParam;
    }

    public String getDeleteUserIdParam() {
        return deleteUserIdParam;
    }

    public void setDeleteUserIdParam(String deleteUserIdParam) {
        this.deleteUserIdParam = deleteUserIdParam;
    }

    // Edit user getters/setters
    public Integer getEditUserId() {
        return editUserId;
    }

    public void setEditUserId(Integer editUserId) {
        this.editUserId = editUserId;
    }

    public String getEditUsername() {
        return editUsername;
    }

    public void setEditUsername(String editUsername) {
        this.editUsername = editUsername;
    }

    public String getEditPassword() {
        return editPassword;
    }

    public void setEditPassword(String editPassword) {
        this.editPassword = editPassword;
    }

    public String getEditRole() {
        return editRole;
    }

    public void setEditRole(String editRole) {
        this.editRole = editRole;
    }

    public String getEditFirstName() {
        return editFirstName;
    }

    public void setEditFirstName(String editFirstName) {
        this.editFirstName = editFirstName;
    }

    public String getEditMiddleName() {
        return editMiddleName;
    }

    public void setEditMiddleName(String editMiddleName) {
        this.editMiddleName = editMiddleName;
    }

    public String getEditLastName() {
        return editLastName;
    }

    public void setEditLastName(String editLastName) {
        this.editLastName = editLastName;
    }

    public String getEditEmail() {
        return editEmail;
    }

    public void setEditEmail(String editEmail) {
        this.editEmail = editEmail;
    }

    public String getEditPhone() {
        return editPhone;
    }

    public void setEditPhone(String editPhone) {
        this.editPhone = editPhone;
    }

    public String getEditStatus() {
        return editStatus;
    }

    public void setEditStatus(String editStatus) {
        this.editStatus = editStatus;
    }

    public boolean isEditBanned() {
        return editBanned;
    }

    public void setEditBanned(boolean editBanned) {
        this.editBanned = editBanned;
    }

    public Integer getEditBanDays() {
        return editBanDays;
    }

    public void setEditBanDays(Integer editBanDays) {
        this.editBanDays = editBanDays;
    }

    public String getEditBanUntilDisplay() {
        return editBanUntilDisplay;
    }

    public void setEditBanUntilDisplay(String editBanUntilDisplay) {
        this.editBanUntilDisplay = editBanUntilDisplay;
    }

    public boolean isEditFormSaveSuccess() {
        return editFormSaveSuccess;
    }

    public void setEditFormSaveSuccess(boolean editFormSaveSuccess) {
        this.editFormSaveSuccess = editFormSaveSuccess;
    }
}
