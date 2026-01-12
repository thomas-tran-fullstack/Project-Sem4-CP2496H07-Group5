package controllers;

import entityclass.Offers;
import entityclass.ProductOffers;
import entityclass.Products;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.faces.event.AjaxBehaviorEvent;
import sessionbeans.OffersFacadeLocal;
import sessionbeans.ProductOffersFacadeLocal;
import sessionbeans.ProductsFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@Named("productOfferMB")
@ViewScoped
public class ProductOfferManagementMB implements Serializable {

    @EJB
    private ProductsFacadeLocal productsFacade;

    @EJB
    private OffersFacadeLocal offersFacade;

    @EJB
    private ProductOffersFacadeLocal productOffersFacade;

    private List<Products> productsList;
    private List<Offers> availableOffers;
    private Products selectedProduct;
    private List<ProductOffers> productOffersList;
    private Integer selectedOfferId;
    private List<Integer> selectedOfferIds;

    @PostConstruct
    public void init() {
        loadProducts();
        loadAvailableOffers();
        handleProductIdParameter();
    }

    public void handleProductIdParameter() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();
        String productIdParam = params.get("productId");

        if (productIdParam != null && !productIdParam.isEmpty()) {
            try {
                Integer productId = Integer.parseInt(productIdParam);
                for (Products product : productsList) {
                    if (product.getProductID().equals(productId)) {
                        selectedProduct = product;
                        loadProductOffers();
                        break;
                    }
                }
            } catch (NumberFormatException e) {
                // Invalid productId parameter, ignore
            }
        }
    }

    public void loadProducts() {
        productsList = productsFacade.findAll();
    }

    public void loadAvailableOffers() {
        availableOffers = offersFacade.findAll();
    }

    public void loadProductOffers() {
        if (selectedProduct != null) {
            // Load existing offers for selected product
            productOffersList = new ArrayList<>();
            List<ProductOffers> allProductOffers = productOffersFacade.findAll();
            for (ProductOffers po : allProductOffers) {
                if (po.getProductID() != null && po.getProductID().getProductID().equals(selectedProduct.getProductID())) {
                    productOffersList.add(po);
                }
            }
        }
    }

    public void onProductChange() {
    loadProductOffers();
}

    public void addOfferToProduct() {
        try {
            if (selectedProduct == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please select a product first"));
                return;
            }

            if (selectedOfferId == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please select an offer"));
                return;
            }

            // Check if offer is already assigned to this product
            for (ProductOffers existing : productOffersList) {
                if (existing.getOfferID().getOfferID().equals(selectedOfferId)) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "This offer is already assigned to the product"));
                    return;
                }
            }

            // Create new ProductOffers relationship
            ProductOffers newProductOffer = new ProductOffers();
            newProductOffer.setProductID(selectedProduct);

            // Find the selected offer
            Offers selectedOffer = offersFacade.find(selectedOfferId);
            newProductOffer.setOfferID(selectedOffer);

            productOffersFacade.create(newProductOffer);
            loadProductOffers();

            selectedOfferId = null;

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Offer added to product successfully"));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to add offer to product: " + e.getMessage()));
        }
    }

    public void removeOfferFromProduct(ProductOffers productOffer) {
        try {
            // Find the managed entity by ID to ensure it's attached to the persistence context
            ProductOffers managedProductOffer = productOffersFacade.find(productOffer.getProductOfferID());
            if (managedProductOffer != null) {
                productOffersFacade.remove(managedProductOffer);
                loadProductOffers();

                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Offer removed from product successfully"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Product offer not found"));
            }

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to remove offer from product: " + e.getMessage()));
        }
    }

    public List<Offers> getAvailableOffersForProduct() {
        if (selectedProduct == null) {
            return availableOffers;
        }

        // Filter out offers that are already assigned to the product
        List<Offers> filteredOffers = new ArrayList<>();
        for (Offers offer : availableOffers) {
            boolean alreadyAssigned = false;
            for (ProductOffers po : productOffersList) {
                if (po.getOfferID().getOfferID().equals(offer.getOfferID())) {
                    alreadyAssigned = true;
                    break;
                }
            }
            if (!alreadyAssigned) {
                filteredOffers.add(offer);
            }
        }
        return filteredOffers;
    }

    // Getters and Setters
    public List<Products> getProductsList() {
        return productsList;
    }

    public void setProductsList(List<Products> productsList) {
        this.productsList = productsList;
    }

    public List<Offers> getAvailableOffers() {
        return availableOffers;
    }

    public void setAvailableOffers(List<Offers> availableOffers) {
        this.availableOffers = availableOffers;
    }

    public Products getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Products selectedProduct) {
        this.selectedProduct = selectedProduct;
    }

    public List<ProductOffers> getProductOffersList() {
        return productOffersList;
    }

    public void setProductOffersList(List<ProductOffers> productOffersList) {
        this.productOffersList = productOffersList;
    }

    public Integer getSelectedOfferId() {
        return selectedOfferId;
    }

    public void setSelectedOfferId(Integer selectedOfferId) {
        this.selectedOfferId = selectedOfferId;
    }

    public void addMultipleOffersToProduct() {
        try {
            if (selectedProduct == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please select a product first"));
                return;
            }

            if (selectedOfferIds == null || selectedOfferIds.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please select at least one offer"));
                return;
            }

            int addedCount = 0;
            int skippedCount = 0;

            for (Integer offerId : selectedOfferIds) {
                // Check if offer is already assigned to this product
                boolean alreadyAssigned = false;
                for (ProductOffers existing : productOffersList) {
                    if (existing.getOfferID().getOfferID().equals(offerId)) {
                        alreadyAssigned = true;
                        skippedCount++;
                        break;
                    }
                }

                if (!alreadyAssigned) {
                    // Create new ProductOffers relationship
                    ProductOffers newProductOffer = new ProductOffers();
                    newProductOffer.setProductID(selectedProduct);

                    // Find the selected offer
                    Offers selectedOffer = offersFacade.find(offerId);
                    newProductOffer.setOfferID(selectedOffer);

                    productOffersFacade.create(newProductOffer);
                    addedCount++;
                }
            }
            loadProductOffers();
            selectedOfferIds = new ArrayList<>();

            String message = "Added " + addedCount + " offers successfully";
            if (skippedCount > 0) {
                message += " (" + skippedCount + " already assigned)";
            }

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", message));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to add offers to product: " + e.getMessage()));
        }
    }

    public void removeAllOffersFromProduct() {
        try {
            if (selectedProduct == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please select a product first"));
                return;
            }

            if (productOffersList.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "No offers to remove"));
                return;
            }

            int removedCount = 0;
            for (ProductOffers productOffer : productOffersList) {
                productOffersFacade.remove(productOffer);
                removedCount++;
            }

            loadProductOffers();

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Removed " + removedCount + " offers from product"));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to remove offers from product: " + e.getMessage()));
        }
    }

    public String selectProductForOffers(Products product) {
        this.selectedProduct = product;
        loadProductOffers();
        return "productsmanage?faces-redirect=false";
    }

    // Getters and Setters
    public List<Integer> getSelectedOfferIds() {
        return selectedOfferIds;
    }

    public void setSelectedOfferIds(List<Integer> selectedOfferIds) {
        this.selectedOfferIds = selectedOfferIds;
    }

    public List<Products> getAssociatedProducts(Offers offer) {
        if (offer == null) {
            return new ArrayList<>();
        }

        List<Products> associatedProducts = new ArrayList<>();
        List<ProductOffers> allProductOffers = productOffersFacade.findAll();

        for (ProductOffers po : allProductOffers) {
            if (po.getOfferID() != null && po.getOfferID().getOfferID().equals(offer.getOfferID())) {
                associatedProducts.add(po.getProductID());
            }
        }

        return associatedProducts;
    }
}
