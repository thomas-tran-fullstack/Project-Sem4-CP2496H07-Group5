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
import jakarta.servlet.http.HttpSession;
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

    @PostConstruct
    public void init() {
        loadAvailableDeals();
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
            HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);

            if (session == null || session.getAttribute("customerId") == null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please login to claim vouchers"));
                return;
            }

            Integer customerId = (Integer) session.getAttribute("customerId");
            Customers customer = customersFacade.find(customerId);

            if (customer == null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Customer not found"));
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
            newVoucher.setExpiryDate(deal.getEndDate());

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
}
