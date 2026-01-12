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
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import sessionbeans.OffersFacadeLocal;
import sessionbeans.ProductOffersFacadeLocal;
import sessionbeans.ProductsFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@Named("offersMB")
@ViewScoped
public class OffersManagementMB implements Serializable {

    @EJB
    private OffersFacadeLocal offersFacade;

    @EJB
    private ProductsFacadeLocal productsFacade;

    @EJB
    private ProductOffersFacadeLocal productOffersFacade;

    private List<Offers> offersList;
    private Offers newOffer;
    private Offers selectedOffer;
    private Integer offerId;
    private String searchTerm;
    private Part bannerFile;
    private List<Products> availableProducts;
    private List<Products> selectedProducts;
    private List<ProductOffers> offerProductOffers;

    @PostConstruct
    public void init() {
        loadOffers();
        loadAvailableProducts();
        newOffer = new Offers();
        selectedProducts = new ArrayList<>();
    }

    public void loadOffers() {
        offersList = offersFacade.findAll();
    }

    public void searchOffers() {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            offersList = offersFacade.findByOfferName("%" + searchTerm + "%");
        } else {
            loadOffers();
        }
    }

    public void addOffer() {
        try {
            // Validate dates
            if (newOffer.getStartDate() != null && newOffer.getEndDate() != null) {
                if (newOffer.getStartDate().after(newOffer.getEndDate())) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Start date cannot be after end date"));
                    return;
                }
            }

            // Set default status if not set
            if (newOffer.getStatus() == null) {
                newOffer.setStatus("Active");
            }

            // Handle banner image upload
            if (bannerFile != null && bannerFile.getSize() > 0) {
                String fileName = saveBannerImage(bannerFile);
                if (fileName != null) {
                    newOffer.setBannerImage(fileName);
                }
            }

            offersFacade.create(newOffer);

            // Create ProductOffers relationships for selected products (only for Percentage offers)
            if (!"Fixed Amount".equals(newOffer.getOfferType()) && selectedProducts != null && !selectedProducts.isEmpty()) {
                for (Products product : selectedProducts) {
                    ProductOffers productOffer = new ProductOffers();
                    productOffer.setProductID(product);
                    productOffer.setOfferID(newOffer);
                    productOffersFacade.create(productOffer);
                }
            }

            loadOffers();
            newOffer = new Offers();
            selectedProducts = new ArrayList<>();
            bannerFile = null;

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Offer added successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to add offer: " + e.getMessage()));
        }
    }

    public void deleteOffer(Offers offer) {
        try {
            offersFacade.remove(offer);
            loadOffers();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Offer deleted successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to delete offer: " + e.getMessage()));
        }
    }

    public void loadOfferForEdit() {
        if (offerId != null) {
            selectedOffer = offersFacade.find(offerId);
            if (selectedOffer == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Offer not found"));
            } else {
                loadOfferProductOffers();
                // Reset banner file for editing
                bannerFile = null;
            }
        }
    }

    public void loadAvailableProducts() {
        List<Products> allProducts = productsFacade.findAll();
        List<ProductOffers> allProductOffers = productOffersFacade.findAll();

        // Create a set of product IDs that are already assigned to offers
        List<Integer> assignedProductIds = new ArrayList<>();
        for (ProductOffers po : allProductOffers) {
            if (po.getProductID() != null) {
                assignedProductIds.add(po.getProductID().getProductID());
            }
        }

        // Filter out products that are already assigned to offers
        availableProducts = new ArrayList<>();
        for (Products product : allProducts) {
            if (!assignedProductIds.contains(product.getProductID())) {
                availableProducts.add(product);
            }
        }
    }

    public void loadOfferProductOffers() {
        if (selectedOffer != null) {
            offerProductOffers = new ArrayList<>();
            List<ProductOffers> allProductOffers = productOffersFacade.findAll();
            for (ProductOffers po : allProductOffers) {
                if (po.getOfferID() != null && po.getOfferID().getOfferID().equals(selectedOffer.getOfferID())) {
                    offerProductOffers.add(po);
                }
            }
        }
    }

    public void addSelectedProductsToOffer() {
        try {
            if (selectedOffer == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No offer selected"));
                return;
            }

            if (selectedProducts == null || selectedProducts.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "No products selected"));
                return;
            }

            int addedCount = 0;
            int skippedCount = 0;

            for (Products product : selectedProducts) {
                // Check if product is already assigned to this offer
                boolean alreadyAssigned = false;
                for (ProductOffers existing : offerProductOffers) {
                    if (existing.getProductID().getProductID().equals(product.getProductID())) {
                        alreadyAssigned = true;
                        skippedCount++;
                        break;
                    }
                }

                if (!alreadyAssigned) {
                    // Create new ProductOffers relationship
                    ProductOffers newProductOffer = new ProductOffers();
                    newProductOffer.setProductID(product);
                    newProductOffer.setOfferID(selectedOffer);

                    productOffersFacade.create(newProductOffer);
                    addedCount++;
                }
            }

            loadOfferProductOffers();
            selectedProducts = new ArrayList<>();

            String message = "Added " + addedCount + " products to offer successfully";
            if (skippedCount > 0) {
                message += " (" + skippedCount + " already assigned)";
            }

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", message));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to add products to offer: " + e.getMessage()));
        }
    }

    public void removeProductFromOffer(ProductOffers productOffer) {
        try {
            productOffersFacade.remove(productOffer);
            loadOfferProductOffers();

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Product removed from offer successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to remove product from offer: " + e.getMessage()));
        }
    }

    public String updateOffer() {
        try {
            // Validate dates
            if (selectedOffer.getStartDate() != null && selectedOffer.getEndDate() != null) {
                if (selectedOffer.getStartDate().after(selectedOffer.getEndDate())) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Start date cannot be after end date"));
                    return null;
                }
            }

            // Handle banner image upload
            System.out.println("DEBUG: bannerFile is " + (bannerFile == null ? "null" : "not null"));
            if (bannerFile != null) {
                System.out.println("DEBUG: bannerFile size: " + bannerFile.getSize());
                System.out.println("DEBUG: bannerFile name: " + bannerFile.getSubmittedFileName());
            }

            if (bannerFile != null && bannerFile.getSize() > 0) {
                String fileName = saveBannerImage(bannerFile);
                if (fileName != null) {
                    selectedOffer.setBannerImage(fileName);
                    System.out.println("DEBUG: Banner image saved with filename: " + fileName);
                    System.out.println("DEBUG: selectedOffer.getBannerImage() after setting: " + selectedOffer.getBannerImage());
                } else {
                    System.out.println("DEBUG: Failed to save banner image");
                }
            } else {
                System.out.println("DEBUG: No banner file to upload or file is empty");
            }

            System.out.println("DEBUG: About to call offersFacade.edit(selectedOffer)");
            System.out.println("DEBUG: selectedOffer ID: " + selectedOffer.getOfferID());
            System.out.println("DEBUG: selectedOffer BannerImage before edit: " + selectedOffer.getBannerImage());

            offersFacade.edit(selectedOffer);

            System.out.println("DEBUG: offersFacade.edit() completed successfully");

            // Verify the entity was updated by fetching it again
            Offers updatedOffer = offersFacade.find(selectedOffer.getOfferID());
            System.out.println("DEBUG: After edit - BannerImage in database: " + (updatedOffer != null ? updatedOffer.getBannerImage() : "null"));

            loadOffers();
            bannerFile = null;

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Offer updated successfully"));
            return "offersmanage?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to update offer: " + e.getMessage()));
            return null;
        }
    }

    // Getters and Setters
    public List<Offers> getOffersList() {
        return offersList;
    }

    public void setOffersList(List<Offers> offersList) {
        this.offersList = offersList;
    }

    public Offers getNewOffer() {
        return newOffer;
    }

    public void setNewOffer(Offers newOffer) {
        this.newOffer = newOffer;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Offers getSelectedOffer() {
        return selectedOffer;
    }

    public void setSelectedOffer(Offers selectedOffer) {
        this.selectedOffer = selectedOffer;
    }

    public Integer getOfferId() {
        return offerId;
    }

    public void setOfferId(Integer offerId) {
        this.offerId = offerId;
    }

    public Part getBannerFile() {
        return bannerFile;
    }

    public void setBannerFile(Part bannerFile) {
        this.bannerFile = bannerFile;
    }

    public List<Products> getAvailableProducts() {
        return availableProducts;
    }

    public void setAvailableProducts(List<Products> availableProducts) {
        this.availableProducts = availableProducts;
    }

    public List<Products> getSelectedProducts() {
        return selectedProducts;
    }

    public void setSelectedProducts(List<Products> selectedProducts) {
        this.selectedProducts = selectedProducts;
    }

    public List<ProductOffers> getOfferProductOffers() {
        return offerProductOffers;
    }

    public void setOfferProductOffers(List<ProductOffers> offerProductOffers) {
        this.offerProductOffers = offerProductOffers;
    }

    public void onOfferTypeChange() {
        if ("Fixed Amount".equals(newOffer.getOfferType())) {
            newOffer.setVoucherEnabled(true);
            newOffer.setDiscountValue(null);
        } else {
            newOffer.setVoucherEnabled(false);
        }
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Type changed", "Type changed to " + newOffer.getOfferType()));
    }

    public void onSelectedOfferTypeChange() {
        if ("Fixed Amount".equals(selectedOffer.getOfferType())) {
            selectedOffer.setVoucherEnabled(true);
            selectedOffer.setDiscountValue(null);
        } else {
            selectedOffer.setVoucherEnabled(false);
        }
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Type changed", "Type changed to " + selectedOffer.getOfferType()));
    }

    private String saveBannerImage(Part file) {
        try {
            // Validate file
            if (file == null || file.getSize() <= 0) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No file selected or file is empty"));
                return null;
            }

            // Check file size (limit to 5MB)
            long maxFileSize = 5 * 1024 * 1024; // 5MB
            if (file.getSize() > maxFileSize) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "File size too large. Maximum size is 5MB"));
                return null;
            }

            // Use user's home directory for uploads (consistent with product images)
            String uploadDir = System.getProperty("user.home") + File.separator + "uploads" + File.separator + "banners";
            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.exists()) {
                boolean created = uploadDirFile.mkdirs();
                if (!created) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to create upload directory"));
                    return null;
                }
            }

            String originalFileName = Paths.get(file.getSubmittedFileName()).getFileName().toString();
            if (originalFileName.contains("..") || originalFileName.contains("/") || originalFileName.contains("\\")) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid file name"));
                return null;
            }

            String fileExtension = "";
            int lastDotIndex = originalFileName.lastIndexOf(".");
            if (lastDotIndex > 0) {
                fileExtension = originalFileName.substring(lastDotIndex);
            }

            // Validate file extension
            String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
            boolean validExtension = false;
            for (String ext : allowedExtensions) {
                if (fileExtension.equalsIgnoreCase(ext)) {
                    validExtension = true;
                    break;
                }
            }

            if (!validExtension) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid file type. Only image files are allowed"));
                return null;
            }

            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = Paths.get(uploadDir, uniqueFileName);

            try (InputStream input = file.getInputStream()) {
                Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("DEBUG: Banner image saved successfully to: " + filePath.toString());
            return uniqueFileName;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to upload banner image: " + e.getMessage()));
            e.printStackTrace(); // For debugging
            return null;
        }
    }
}
