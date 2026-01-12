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
import sessionbeans.ProductsFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@Named(value = "supplierMB")
@ViewScoped
public class SupplierManagementMB implements Serializable {

    @EJB
    private BrandsFacadeLocal brandsFacade;

    @EJB
    private ProductsFacadeLocal productsFacade;

    private List<Brands> suppliersList;
    private Brands selectedSupplier;
    private Brands newSupplier;
    private boolean editSupplierMode = false;
    private String searchTerm;
    private Integer supplierId;

    // Pagination properties
    private int currentPage = 1;
    private int pageSize = 10;
    private int totalRecords;
    private int totalPages;
    private List<Brands> paginatedSuppliers;

    @PostConstruct
    public void init() {
        loadSuppliers();
        newSupplier = new Brands();
        updatePagination();
    }

    public void loadSuppliers() {
        suppliersList = brandsFacade.findAll();
    }

    // Supplier CRUD operations
    public String addSupplier() {
        // Validation
        if (newSupplier.getBrandName() == null || newSupplier.getBrandName().trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Supplier name is required"));
            return null;
        }
        if (newSupplier.getBrandName().length() < 2) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Supplier name must be at least 2 characters"));
            return null;
        }
        if (newSupplier.getBrandName().length() > 50) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Supplier name must not exceed 50 characters"));
            return null;
        }
        // Check for duplicate supplier name
        List<Brands> existingSuppliers = brandsFacade.findByBrandName(newSupplier.getBrandName());
        if (!existingSuppliers.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Supplier name already exists"));
            return null;
        }

        try {
            brandsFacade.create(newSupplier);
            loadSuppliers();
            updatePagination();
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
        // Validation
        if (selectedSupplier.getBrandName() == null || selectedSupplier.getBrandName().trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Supplier name is required"));
            return null;
        }
        if (selectedSupplier.getBrandName().length() < 2) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Supplier name must be at least 2 characters"));
            return null;
        }
        if (selectedSupplier.getBrandName().length() > 50) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Supplier name must not exceed 50 characters"));
            return null;
        }
        // Check for duplicate supplier name (excluding current supplier)
        List<Brands> existingSuppliers = brandsFacade.findByBrandName(selectedSupplier.getBrandName());
        if (existingSuppliers.size() > 0 && !existingSuppliers.get(0).getBrandID().equals(selectedSupplier.getBrandID())) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Supplier name already exists"));
            return null;
        }

        try {
            brandsFacade.edit(selectedSupplier);
            loadSuppliers();
            updatePagination();
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
        // Check if supplier has associated products
        if (productsFacade.hasProductsByBrand(supplier.getBrandID())) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot delete supplier because it has associated products"));
            return;
        }

        try {
            brandsFacade.remove(supplier);
            loadSuppliers();
            updatePagination();
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
        // Reset pagination after search
        currentPage = 1;
        updatePagination();
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

    // Pagination methods
    public void updatePagination() {
        if (suppliersList != null) {
            totalRecords = suppliersList.size();
            totalPages = (int) Math.ceil((double) totalRecords / pageSize);
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }
            if (currentPage < 1) {
                currentPage = 1;
            }
            int startIndex = (currentPage - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalRecords);
            paginatedSuppliers = suppliersList.subList(startIndex, endIndex);
        } else {
            totalRecords = 0;
            totalPages = 0;
            paginatedSuppliers = null;
        }
    }

    public void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }

    public void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
        }
    }

    public void goToPage(int page) {
        if (page >= 1 && page <= totalPages) {
            currentPage = page;
            updatePagination();
        }
    }

    // Pagination getters and setters
    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<Brands> getPaginatedSuppliers() {
        return paginatedSuppliers;
    }

    public void setPaginatedSuppliers(List<Brands> paginatedSuppliers) {
        this.paginatedSuppliers = paginatedSuppliers;
    }
}
