package controllers;

import entityclass.Brands;
import entityclass.Products;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import jakarta.faces.context.FacesContext;
import sessionbeans.BrandsFacadeLocal;
import sessionbeans.ProductsFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@Named(value = "supplierUserMB")
@ViewScoped
public class SupplierUserMB implements Serializable {

    @EJB
    private BrandsFacadeLocal brandsFacade;

    @EJB
    private ProductsFacadeLocal productsFacade;

    private Brands selectedSupplier;
    private List<Products> supplierProducts;
    private String searchTerm;
    private Integer supplierId;

    @PostConstruct
    public void init() {
        loadSupplierById();
    }

    public void loadSupplierById() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();
        String supplierIdParam = params.get("supplierId");
        if (supplierIdParam != null && !supplierIdParam.isEmpty()) {
            try {
                supplierId = Integer.valueOf(supplierIdParam);
                selectedSupplier = brandsFacade.find(supplierId);
                if (selectedSupplier != null) {
                    loadSupplierProducts();
                }
            } catch (NumberFormatException e) {
                // Handle invalid supplierId
            }
        }
    }

    public void loadSupplierProducts() {
        if (selectedSupplier != null) {
            supplierProducts = productsFacade.findByBrand(selectedSupplier.getBrandID());
        }
    }

    public void searchSuppliers() {
        // This method is for the search functionality in the supplier list view
        // For now, we'll reload all suppliers if no search term
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Note: This assumes BrandsFacade has a search method, adjust as needed
            // For simplicity, we'll just reload
        }
    }

    // Getters and Setters
    public Brands getSelectedSupplier() {
        return selectedSupplier;
    }

    public void setSelectedSupplier(Brands selectedSupplier) {
        this.selectedSupplier = selectedSupplier;
    }

    public List<Products> getSupplierProducts() {
        return supplierProducts;
    }

    public void setSupplierProducts(List<Products> supplierProducts) {
        this.supplierProducts = supplierProducts;
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

    // For backward compatibility with existing supplierinfo.xhtml
    public List<Brands> getSuppliers() {
        return brandsFacade.findAll();
    }
}
