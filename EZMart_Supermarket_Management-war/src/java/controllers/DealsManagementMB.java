package controllers;

import entityclass.Offers;
import entityclass.Vouchers;
import entityclass.Customers;
import entityclass.ProductOffers;
import entityclass.Products;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import sessionbeans.OffersFacadeLocal;
import sessionbeans.VouchersFacadeLocal;
import sessionbeans.CustomersFacadeLocal;
import sessionbeans.ProductOffersFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@Named("dealsMB")
@ViewScoped
public class DealsManagementMB implements Serializable {

    @EJB
    private OffersFacadeLocal offersFacade;

    @EJB
    private VouchersFacadeLocal vouchersFacade;

    @EJB
    private CustomersFacadeLocal customersFacade;

    @EJB
    private ProductOffersFacadeLocal productOffersFacade;

    private List<Offers> availableDeals;
    private List<Products> associatedProducts;
    private Offers selectedOfferForClaim; // Store the offer when user clicks Claim

    @PostConstruct
    public void init() {
        loadAvailableDeals();
    }

    // Prepare voucher claim - store the offer and show confirmation dialog
    public void prepareClaimVoucher(Offers deal) {
        this.selectedOfferForClaim = deal;
    }

    // Confirm and execute the voucher claim
    public void confirmClaimVoucher() {
        if (selectedOfferForClaim != null) {
            claimVoucher(selectedOfferForClaim);
            selectedOfferForClaim = null;
        }
    }

    public void loadAvailableDeals() {
        // Load all active offers
        List<Offers> allOffers = offersFacade.findAll();
        availableDeals = new ArrayList<>();

        Date currentDate = new Date();
        for (Offers offer : allOffers) {
            // Check if offer is active and within valid date range
            if ("active".equalsIgnoreCase(offer.getStatus()) &&
                offer.getStartDate() != null && offer.getEndDate() != null &&
                !currentDate.before(offer.getStartDate()) && !currentDate.after(offer.getEndDate())) {
                availableDeals.add(offer);
            }
        }
    }

    public void claimVoucher(Offers deal) {
        try {
            // Get current customer from session
            FacesContext facesContext = FacesContext.getCurrentInstance();

            // Try to get customerId from session first
            Integer customerId = (Integer) facesContext.getExternalContext().getSessionMap().get("customerId");

            // If not found in session, try to get it from currentUser
            if (customerId == null) {
                entityclass.Users currentUser = (entityclass.Users) facesContext.getExternalContext().getSessionMap().get("currentUser");
                System.out.println("DealsManagementMB.claimVoucher: currentUser=" + currentUser);
                if (currentUser != null) {
                    System.out.println("DealsManagementMB.claimVoucher: currentUser.customersList=" + currentUser.getCustomersList());
                    if (currentUser.getCustomersList() != null) {
                        System.out.println("DealsManagementMB.claimVoucher: customersList.size=" + currentUser.getCustomersList().size());
                        if (!currentUser.getCustomersList().isEmpty()) {
                            customerId = currentUser.getCustomersList().get(0).getCustomerID();
                            System.out.println("DealsManagementMB.claimVoucher: extracted customerId=" + customerId);
                        }
                    }
                }
            }

            // Debug logging
            System.out.println("DealsManagementMB.claimVoucher: final customerId=" + customerId);
            System.out.println("DealsManagementMB.claimVoucher: loggedIn=" + facesContext.getExternalContext().getSessionMap().get("loggedIn"));

            if (customerId == null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please login to claim vouchers"));
                return;
            }
            Customers customer = customersFacade.find(customerId);

            if (customer == null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Customer not found"));
                return;
            }

            // Check if the offer is of Fixed Amount type
            if (deal.getOfferType() == null || !deal.getOfferType().toLowerCase().contains("fixed")) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Only Fixed Amount offers can be claimed as vouchers"));
                return;
            }

            // Check if customer already has this voucher
            List<Vouchers> customerVouchers = vouchersFacade.findAll();
            for (Vouchers voucher : customerVouchers) {
                if (voucher.getCustomerID() != null &&
                    voucher.getCustomerID().getCustomerID().equals(customerId) &&
                    voucher.getOfferID() != null &&
                    voucher.getOfferID().getOfferID().equals(deal.getOfferID())) {
                    facesContext.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "You already have this voucher"));
                    return;
                }
            }

            // Create new voucher for customer
            Vouchers newVoucher = new Vouchers();
            newVoucher.setCustomerID(customer);
            newVoucher.setOfferID(deal);
            newVoucher.setCreatedAt(new Date());
            newVoucher.setIsUsed(false);
            newVoucher.setDiscountValue(BigDecimal.valueOf(deal.getDiscountValue()));
            newVoucher.setVoucherCode(generateUniqueVoucherCode(customerId, deal.getOfferID()));

            // Set expiry date to avoid timezone issues - use only the date part
            Date expiryDate = deal.getEndDate();
            if (expiryDate != null) {
                // Create a new date with only the date components to avoid timezone conversion issues
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(expiryDate);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                newVoucher.setExpiryDate(cal.getTime());
            } else {
                newVoucher.setExpiryDate(deal.getEndDate());
            }

            vouchersFacade.create(newVoucher);

            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Voucher claimed successfully!"));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to claim voucher: " + e.getMessage()));
        }
    }

    public List<Products> getAssociatedProducts(Offers offer) {
        if (offer == null) {
            return new ArrayList<>();
        }

        List<Products> products = new ArrayList<>();
        List<ProductOffers> productOffers = productOffersFacade.findAll();

        for (ProductOffers po : productOffers) {
            if (po.getOfferID() != null && po.getOfferID().getOfferID().equals(offer.getOfferID())) {
                if (po.getProductID() != null) {
                    products.add(po.getProductID());
                }
            }
        }

        return products;
    }

    public BigDecimal getDiscountedPrice(Products product, Offers offer) {
        if (product == null || offer == null || product.getUnitPrice() == null || offer.getDiscountValue() == null) {
            return product != null ? product.getUnitPrice() : BigDecimal.ZERO;
        }

        BigDecimal originalPrice = product.getUnitPrice();
        Integer discountValue = offer.getDiscountValue();
        String offerType = offer.getOfferType();

        if (offerType != null && offerType.toLowerCase().contains("percentage")) {
            // Percentage discount
            BigDecimal discountAmount = originalPrice.multiply(BigDecimal.valueOf(discountValue)).divide(BigDecimal.valueOf(100));
            return originalPrice.subtract(discountAmount);
        } else if (offerType != null && offerType.toLowerCase().contains("fixed")) {
            // Fixed amount discount
            BigDecimal discountAmount = BigDecimal.valueOf(discountValue);
            BigDecimal discountedPrice = originalPrice.subtract(discountAmount);
            // Ensure price doesn't go below 0
            return discountedPrice.compareTo(BigDecimal.ZERO) > 0 ? discountedPrice : BigDecimal.ZERO;
        }

        // Return original price if offer type is not recognized
        return originalPrice;
    }

    public String getDiscountDisplay(Products product, Offers offer) {
        if (product == null || offer == null || product.getUnitPrice() == null || offer.getDiscountValue() == null) {
            return "";
        }

        Integer discountValue = offer.getDiscountValue();
        String offerType = offer.getOfferType();

        if (offerType != null && offerType.toLowerCase().contains("percentage")) {
            return discountValue + "% OFF";
        } else if (offerType != null && offerType.toLowerCase().contains("fixed")) {
            return discountValue + " VND OFF";
        }

        return "";
    }

    public String productImageUrl(entityclass.Products product) {
        if (product != null && product.getProductImagesList() != null && !product.getProductImagesList().isEmpty()) {
            return product.getProductImagesList().get(0).getImageURL();
        }
        return null;
    }

    public String getProductImage(Products product) {
        if (product != null && product.getProductImagesList() != null && !product.getProductImagesList().isEmpty()) {
            return "/uploads/products/" + product.getProductImagesList().get(0).getImageURL();
        }
        return "/images/no-image.png";
    }

    private String generateUniqueVoucherCode(Integer customerId, Integer offerId) {
        try {
            String input = customerId + "-" + offerId + "-" + System.currentTimeMillis();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            // Take first 12 characters for a shorter code
            return sb.toString().substring(0, 12).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to a simple unique code if hashing fails
            return "VOUCHER-" + customerId + "-" + offerId + "-" + System.currentTimeMillis();
        }
    }

    // Getters and Setters
    public List<Offers> getAvailableDeals() {
        return availableDeals;
    }

    public void setAvailableDeals(List<Offers> availableDeals) {
        this.availableDeals = availableDeals;
    }

    public Offers getSelectedOfferForClaim() {
        return selectedOfferForClaim;
    }

    public void setSelectedOfferForClaim(Offers selectedOfferForClaim) {
        this.selectedOfferForClaim = selectedOfferForClaim;
    }
}
