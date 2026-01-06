package controllers;

import entityclass.Brands;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import sessionbeans.BrandsFacadeLocal;
import sessionbeans.BrandsFacade;

/**
 *
 * @author TRUONG LAM
 */
@Named(value = "supplierMB")
@ViewScoped
public class SupplierManagementMB implements Serializable {

    @EJB
    private BrandsFacadeLocal brandsFacade;

    private List<Brands> suppliersList;
    private Brands selectedSupplier;
    private Brands newSupplier;
    private boolean editSupplierMode = false;
    private String searchTerm;
    private Integer supplierId;

    @PostConstruct
    public void init() {
        loadSuppliers();
        newSupplier = new Brands();
    }

    public void loadSuppliers() {
        suppliersList = brandsFacade.findAll();
    }

    // Supplier CRUD operations
    public String addSupplier() {
        try {
            brandsFacade.create(newSupplier);
            loadSuppliers();
            newSupplier = new Brands();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Supplier added successfully"));
            return "suppliermanage?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to add supplier"));
            return null;
        }
    }

    public String editSupplier(Brands supplier) {
        selectedSupplier = supplier;
        editSupplierMode = true;
        return "editsupplier?faces-redirect=true";
    }

    public String updateSupplier() {
        try {
            brandsFacade.edit(selectedSupplier);
            loadSuppliers();
            editSupplierMode = false;
            selectedSupplier = null;
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Supplier updated successfully"));
            return "suppliermanage?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to update supplier"));
            return null;
        }
    }

    public void deleteSupplier(Brands supplier) {
        try {
            brandsFacade.remove(supplier);
            loadSuppliers();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Supplier deleted successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to delete supplier"));
        }
    }

    public void cancelEditSupplier() {
        editSupplierMode = false;
        selectedSupplier = null;
    }

    public void loadSupplierForEdit() {
        if (supplierId != null) {
            selectedSupplier = brandsFacade.find(supplierId);
        }
    }

    public void searchSuppliers() {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            suppliersList = brandsFacade.findByBrandName(searchTerm);
        } else {
            loadSuppliers();
        }
    }

    // Getters and Setters
    public List<Brands> getSuppliersList() {
        return suppliersList;
    }

    public void setSuppliersList(List<Brands> suppliersList) {
        this.suppliersList = suppliersList;
    }

    public Brands getSelectedSupplier() {
        return selectedSupplier;
    }

    public void setSelectedSupplier(Brands selectedSupplier) {
        this.selectedSupplier = selectedSupplier;
    }

    public Brands getNewSupplier() {
        return newSupplier;
    }

    public void setNewSupplier(Brands newSupplier) {
        this.newSupplier = newSupplier;
    }

    public boolean isEditSupplierMode() {
        return editSupplierMode;
    }

    public void setEditSupplierMode(boolean editSupplierMode) {
        this.editSupplierMode = editSupplierMode;
    }

    public String getSearchTerm() {
        return searchTerm; 
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }
}
