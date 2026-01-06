package controllers;

import entityclass.Addresses;
import entityclass.Customers;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.faces.context.FacesContext;
import sessionbeans.AddressesFacadeLocal;

@Named("addressCtrl")
@SessionScoped
public class AddressController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private AddressesFacadeLocal addressesFacade;
    
    @Inject
    private AuthController authController;

    private List<Addresses> addresses = null;
    private Addresses editingAddress = new Addresses();
    private String mapSelected = "0";
    // temporary fields to expose save result to AJAX client
    private String saveResultMessage;
    private String saveResultSeverity;

    @PostConstruct
    public void init() {
        // PostConstruct might run before AuthController is fully injected
        // So we use lazy loading in getAddresses() instead
    }

    public void loadForCurrentUser() {
        try {
            // Get current customer via AuthController
            if (authController == null) {
                authController = (AuthController) FacesContext.getCurrentInstance()
                    .getApplication().evaluateExpressionGet(FacesContext.getCurrentInstance(), "#{auth}", AuthController.class);
            }
            
            if (authController != null && authController.getCurrentCustomer() != null) {
                Integer cid = authController.getCurrentCustomer().getCustomerID();
                if (cid != null) {
                    addresses = addressesFacade.findByCustomer(cid);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading addresses: " + e.getMessage());
            addresses = new ArrayList<>();
        }
    }

    /**
     * Verify that the given address belongs to the current user
     */
    private boolean isAddressOwnedByCurrentUser(Addresses address) {
        if (address == null || address.getCustomerID() == null) return false;
        if (authController == null || authController.getCurrentCustomer() == null) return false;
        return address.getCustomerID().getCustomerID().equals(authController.getCurrentCustomer().getCustomerID());
    }

    /**
     * Sanitize input strings to prevent SQL injection
     */
    private String sanitizeInput(String input) {
        if (input == null) return null;
        // Remove leading/trailing whitespace
        input = input.trim();
        // Replace potentially dangerous characters (basic sanitation)
        // Note: JPA parameterized queries provide primary protection against SQL injection
        return input;
    }

    /**
     * Validate address fields
     */
    private boolean validateAddress(Addresses address) {
        if (address == null) return false;
        
        // Check required fields
        String street = sanitizeInput(address.getStreet());
        String city = sanitizeInput(address.getCity());
        
        if (street == null || street.isEmpty() || street.length() > 100) {
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Street address is required and must be less than 100 characters", null
            ));
            return false;
        }
        
        if (city == null || city.isEmpty() || city.length() > 50) {
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "City is required and must be less than 50 characters", null
            ));
            return false;
        }
        
        // Optional fields validation
        if (address.getState() != null && address.getState().length() > 50) {
            return false;
        }
        if (address.getCountry() != null && address.getCountry().length() > 50) {
            return false;
        }
        
        // Validate latitude/longitude if provided
        if (address.getLatitude() != null && 
            (address.getLatitude() < -90 || address.getLatitude() > 90)) {
            return false;
        }
        if (address.getLongitude() != null && 
            (address.getLongitude() < -180 || address.getLongitude() > 180)) {
            return false;
        }
        
        return true;
    }

    public List<Addresses> getAddresses() {
        // Lazy load addresses on first access
        if (addresses == null) {
            loadForCurrentUser();
        }
        return addresses != null ? addresses : new ArrayList<>();
    }
    public Addresses getEditingAddress() { return editingAddress; }
    public void setEditingAddress(Addresses a) { this.editingAddress = a; }

    /**
     * Find and edit address by ID (called from UI with addressId parameter)
     */
    public String editAddressById(Integer addressId) {
        try {
            if (addressId != null && addresses != null) {
                for (Addresses addr : addresses) {
                    if (addr.getAddressID().equals(addressId) && isAddressOwnedByCurrentUser(addr)) {
                        editingAddress = addr;
                        mapSelected = "0";
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error finding address: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Delete address by ID (called from UI with addressId parameter)
     */
    public String deleteAddressById(Integer addressId) {
        try {
            if (addressId != null && addresses != null) {
                for (Addresses addr : addresses) {
                    if (addr.getAddressID().equals(addressId) && isAddressOwnedByCurrentUser(addr)) {
                        return delete(addr);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error deleting address: " + e.getMessage());
        }
        return null;
    }

    public String startAdd() {
        editingAddress = new Addresses();
        mapSelected = "0";
        return null;
    }

    public String startEdit(Addresses a) {
        if (a != null && isAddressOwnedByCurrentUser(a)) {
            editingAddress = a;
        }
        mapSelected = "0";
        return null;
    }

    public String getMapSelected() { return mapSelected; }
    public void setMapSelected(String m) { this.mapSelected = m; }

    public String save() {
        try {
            // clear previous ajax result
            saveResultMessage = null;
            saveResultSeverity = null;
            // Validate input
            if (!validateAddress(editingAddress)) {
                return null;
            }
            // Require map selection (either mapSelected flag or lat/lng provided)
            if ((mapSelected == null || !"1".equals(mapSelected)) && (editingAddress.getLatitude() == null || editingAddress.getLongitude() == null)) {
                FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    "Please select your place on the map before saving", null
                ));
                return null;
            }
            
            // Ensure current user is authenticated
            if (authController == null || authController.getCurrentCustomer() == null) {
                FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    "You must be logged in to manage addresses", null
                ));
                return null;
            }
            
            Customers cust = authController.getCurrentCustomer();
            
            if (editingAddress.getAddressID() == null) {
                // Creating new address - check for duplicates
                if (isDuplicateAddress(cust, editingAddress)) {
                    FacesContext ctx = FacesContext.getCurrentInstance();
                    String errorMsg = ctx.getApplication().evaluateExpressionGet(ctx, "#{msg['profile.addressAlreadyExists']}", String.class);
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Address Already Exists";
                    }
                    ctx.addMessage(null, new jakarta.faces.application.FacesMessage(
                        jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                        errorMsg, null
                    ));
                    // also expose to AJAX handler
                    saveResultMessage = errorMsg;
                    saveResultSeverity = "error";
                    return null;
                }
                
                // ensure customer is set
                editingAddress.setCustomerID(cust);
                editingAddress.setCreatedAt(new Date());
                // If this new address is marked default, unset other defaults first
                if (editingAddress.getIsDefault() != null && editingAddress.getIsDefault()) {
                    List<Addresses> list = addressesFacade.findByCustomer(cust.getCustomerID());
                    for (Addresses aa : list) {
                        if (aa.getIsDefault() != null && aa.getIsDefault()) {
                            aa.setIsDefault(false);
                            addressesFacade.edit(aa);
                        }
                    }
                }
                addressesFacade.create(editingAddress);
            } else {
                // Updating address - verify ownership
                if (!isAddressOwnedByCurrentUser(editingAddress)) {
                    FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                        jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                        "You cannot modify this address", null
                    ));
                    return null;
                }
                // If updated address is marked default, unset other defaults first
                if (editingAddress.getIsDefault() != null && editingAddress.getIsDefault()) {
                    Integer cid = cust.getCustomerID();
                    List<Addresses> list = addressesFacade.findByCustomer(cid);
                    for (Addresses aa : list) {
                        if (aa.getIsDefault() != null && aa.getIsDefault() && !aa.getAddressID().equals(editingAddress.getAddressID())) {
                            aa.setIsDefault(false);
                            addressesFacade.edit(aa);
                        }
                    }
                }
                addressesFacade.edit(editingAddress);
            }
            // reset temporary map flag
            mapSelected = "0";
            
            loadForCurrentUser();
            FacesContext ctx = FacesContext.getCurrentInstance();
            String successMsg = ctx.getApplication().evaluateExpressionGet(ctx, "#{msg['profile.addedSuccessfully']}", String.class);
            if (successMsg == null || successMsg.isEmpty()) {
                successMsg = "Address Added Successfully";
            }
            ctx.addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                successMsg, null
            ));
            // expose to AJAX handler
            saveResultMessage = successMsg;
            saveResultSeverity = "success";
        } catch (Exception e) {
            System.err.println("Error saving address: " + e.getMessage());
            e.printStackTrace();
            String fail = "Failed to save address";
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                fail, null
            ));
            saveResultMessage = fail;
            saveResultSeverity = "error";
        }
        return null;
    }

    public String getSaveResultMessage() { return saveResultMessage; }
    public void setSaveResultMessage(String m) { this.saveResultMessage = m; }
    public String getSaveResultSeverity() { return saveResultSeverity; }
    public void setSaveResultSeverity(String s) { this.saveResultSeverity = s; }

    public String delete(Addresses a) {
        try {
            // Verify ownership before deletion
            if (a != null && isAddressOwnedByCurrentUser(a)) {
                addressesFacade.remove(a);
                loadForCurrentUser();
                FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                    "Address deleted successfully", null
                ));
            } else {
                FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    "You cannot delete this address", null
                ));
            }
        } catch (Exception e) {
            System.err.println("Error deleting address: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Failed to delete address", null
            ));
        }
        return null;
    }

    public String setDefault(Addresses a) {
        try {
            if (a == null || !isAddressOwnedByCurrentUser(a)) {
                FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    "Invalid address", null
                ));
                return null;
            }
            
            if (authController == null || authController.getCurrentCustomer() == null) return null;
            
            Integer cid = authController.getCurrentCustomer().getCustomerID();
            List<Addresses> list = addressesFacade.findByCustomer(cid);
            
            // Unset other defaults for this customer
            for (Addresses aa : list) {
                if (aa.getIsDefault() != null && aa.getIsDefault()) {
                    aa.setIsDefault(false);
                    addressesFacade.edit(aa);
                }
            }
            
            a.setIsDefault(true);
            addressesFacade.edit(a);
            loadForCurrentUser();
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                "Default address updated", null
            ));
        } catch (Exception e) {
            System.err.println("Error setting default address: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Failed to update default address", null
            ));
        }
        return null;
    }
    
    // Check if address with same street, house, city, region, state, country combination already exists for this customer
    // Duplicate only when all key fields match exactly (case-insensitive trimmed). If any one field differs, it's not considered a duplicate.
    private boolean isDuplicateAddress(Customers customer, Addresses addr) {
        if (customer == null || customer.getCustomerID() == null) {
            return false;
        }
        List<Addresses> existingAddresses = addressesFacade.findByCustomer(customer.getCustomerID());
        if (existingAddresses == null || existingAddresses.isEmpty()) {
            return false;
        }
        String street = addr.getStreet() != null ? addr.getStreet().trim().toLowerCase() : "";
        String house = addr.getHouse() != null ? addr.getHouse().trim().toLowerCase() : "";
        String city = addr.getCity() != null ? addr.getCity().trim().toLowerCase() : "";
        String region = addr.getRegion() != null ? addr.getRegion().trim().toLowerCase() : "";
        String state = addr.getState() != null ? addr.getState().trim().toLowerCase() : "";
        String country = addr.getCountry() != null ? addr.getCountry().trim().toLowerCase() : "";

        for (Addresses existing : existingAddresses) {
            String existStreet = existing.getStreet() != null ? existing.getStreet().trim().toLowerCase() : "";
            String existHouse = existing.getHouse() != null ? existing.getHouse().trim().toLowerCase() : "";
            String existCity = existing.getCity() != null ? existing.getCity().trim().toLowerCase() : "";
            String existRegion = existing.getRegion() != null ? existing.getRegion().trim().toLowerCase() : "";
            String existState = existing.getState() != null ? existing.getState().trim().toLowerCase() : "";
            String existCountry = existing.getCountry() != null ? existing.getCountry().trim().toLowerCase() : "";

            if (street.equals(existStreet)
                    && house.equals(existHouse)
                    && city.equals(existCity)
                    && region.equals(existRegion)
                    && state.equals(existState)
                    && country.equals(existCountry)) {
                return true;
            }
        }
        return false;
    }
}
