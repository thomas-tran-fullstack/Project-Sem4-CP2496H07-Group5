/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controllers;

import entityclass.Customers;
import entityclass.Offers;
import entityclass.Vouchers;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import sessionbeans.CustomersFacadeLocal;
import sessionbeans.OffersFacadeLocal;
import sessionbeans.VouchersFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@Named(value = "voucherManagementMB")
@ViewScoped
public class VoucherManagementMB implements Serializable {

    @EJB
    private VouchersFacadeLocal vouchersFacade;

    @EJB
    private CustomersFacadeLocal customersFacade;

    @EJB
    private OffersFacadeLocal offersFacade;

    private List<Vouchers> vouchers;
    private List<Customers> customers;
    private List<Offers> offers;

    private Vouchers selectedVoucher;
    private Integer selectedCustomerId;
    private Integer selectedOfferId;
    private Boolean selectedStatus;
    private String searchCode;
    private String voucherType; // "offer" or "customer"
    private List<Offers> eligibleOffers; // Filtered offers for voucher type
    private String selectedType; // For filtering: "offer", "customer", "general"

    @PostConstruct
    public void init() {
        loadVouchers();
        loadCustomers();
        loadOffers();
        selectedVoucher = new Vouchers();
    }

    public void loadVouchers() {
        vouchers = vouchersFacade.findAll().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void loadCustomers() {
        customers = customersFacade.findAll();
    }

    public void loadOffers() {
        offers = offersFacade.findAll();
    }

    public void searchVouchers() {
        List<Vouchers> allVouchers = vouchersFacade.findAll().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Apply filters
        if (searchCode != null && !searchCode.trim().isEmpty()) {
            // Search by voucher code
            Vouchers voucher = vouchersFacade.findByVoucherCode(searchCode.trim());
            vouchers = voucher != null ? List.of(voucher) : List.of();
        } else {
            vouchers = allVouchers.stream()
                    .filter(v -> selectedType == null || "All Types".equals(selectedType) ||
                             ("offer".equals(selectedType) && v.getOfferID() != null) ||
                             ("customer".equals(selectedType) && v.getCustomerID() != null) ||
                             ("general".equals(selectedType) && v.getOfferID() == null && v.getCustomerID() == null))
                    .filter(v -> selectedCustomerId == null || (v.getCustomerID() != null && v.getCustomerID().getCustomerID().equals(selectedCustomerId)))
                    .filter(v -> selectedStatus == null || v.getIsUsed().equals(selectedStatus))
                    .collect(Collectors.toList());
        }
    }

    public void prepareCreate() {
        selectedVoucher = new Vouchers();
        selectedVoucher.setCreatedAt(new Date());
        selectedVoucher.setIsUsed(false);
        selectedCustomerId = null;
        selectedOfferId = null;
        voucherType = "customer"; // Default to customer type
        loadEligibleOffers();
    }

    public void prepareEdit(Vouchers voucher) {
        if (voucher == null) {
            return;
        }
        selectedVoucher = voucher;
        selectedCustomerId = voucher.getCustomerID() != null ? voucher.getCustomerID().getCustomerID() : null;
        selectedOfferId = voucher.getOfferID() != null ? voucher.getOfferID().getOfferID() : null;
        // Determine voucher type based on existing data
        if (selectedOfferId != null) {
            voucherType = "offer";
        } else if (selectedCustomerId != null) {
            voucherType = "customer";
        } else {
            voucherType = "offer"; // Default
        }
        loadEligibleOffers();
    }

    public void saveVoucher() {
        try {
            // Set relationships
            if (selectedCustomerId != null) {
                Customers customer = customersFacade.find(selectedCustomerId);
                selectedVoucher.setCustomerID(customer);
            }

            if (selectedOfferId != null) {
                Offers offer = offersFacade.find(selectedOfferId);
                selectedVoucher.setOfferID(offer);
            }

            // Generate voucher code if creating new and user didn't provide one
            if (selectedVoucher.getVoucherID() == null) {
                if (selectedVoucher.getVoucherCode() == null || selectedVoucher.getVoucherCode().trim().isEmpty()) {
                    selectedVoucher.setVoucherCode(generateVoucherCode());
                }
                selectedVoucher.setCreatedAt(new Date());
            }

            if (selectedVoucher.getVoucherID() == null) {
                vouchersFacade.create(selectedVoucher);
            } else {
                vouchersFacade.edit(selectedVoucher);
            }

            loadVouchers();
            selectedVoucher = new Vouchers();

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success",
                    selectedVoucher.getVoucherID() == null ? "Voucher created successfully" : "Voucher updated successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                    "Failed to save voucher: " + e.getMessage()));
        }
    }

    public void deleteVoucher(Vouchers voucher) {
        try {
            vouchersFacade.remove(voucher);
            loadVouchers();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Voucher deleted successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to delete voucher: " + e.getMessage()));
        }
    }

    public void generateVouchersForOffer(Offers offer, List<Customers> targetCustomers) {
        try {
            for (Customers customer : targetCustomers) {
                Vouchers voucher = new Vouchers();
                voucher.setVoucherCode(generateVoucherCode());
                voucher.setOfferID(offer);
                voucher.setCustomerID(customer);
                voucher.setExpiryDate(offer.getEndDate());
                voucher.setIsUsed(false);
                voucher.setCreatedAt(new Date());

                vouchersFacade.create(voucher);
            }

            loadVouchers();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success",
                    "Generated " + targetCustomers.size() + " vouchers for offer: " + offer.getOfferName()));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                    "Failed to generate vouchers: " + e.getMessage()));
        }
    }

    public void loadEligibleOffers() {
        System.out.println("Loading eligible offers. Total offers: " + offers.size());
        if ("offer".equals(voucherType)) {
            // Filter offers to only fixed amount with VoucherEnabled = true
            eligibleOffers = offers.stream()
                    .filter(offer -> "Fixed Amount".equals(offer.getOfferType()) && Boolean.TRUE.equals(offer.getVoucherEnabled()))
                    .collect(Collectors.toList());
            System.out.println("Eligible offers for voucher: " + eligibleOffers.size());
        } else {
            eligibleOffers = List.of(); // No offers for customer type
        }
    }

    public void onVoucherTypeChange() {
        // Reset selections when type changes
        selectedCustomerId = null;
        selectedOfferId = null;
        selectedVoucher.setDiscountValue(null); // Reset discount value
        selectedVoucher.setExpiryDate(null); // Reset expiry date
        loadEligibleOffers();
    }

    public void onOfferChange() {
        if (selectedOfferId != null) {
            Offers selectedOffer = eligibleOffers.stream()
                    .filter(offer -> offer.getOfferID().equals(selectedOfferId))
                    .findFirst()
                    .orElse(null);
            if (selectedOffer != null) {
                // Convert Integer to BigDecimal for discount value
                selectedVoucher.setDiscountValue(selectedOffer.getDiscountValue() != null ?
                    new BigDecimal(selectedOffer.getDiscountValue()) : null);
                selectedVoucher.setExpiryDate(selectedOffer.getEndDate());
            }
        } else {
            selectedVoucher.setDiscountValue(null);
            selectedVoucher.setExpiryDate(null);
        }
    }


    private String generateVoucherCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();

        // Generate 8-character code
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }

    // Getters and Setters
    public List<Vouchers> getVouchers() {
        return vouchers;
    }

    public void setVouchers(List<Vouchers> vouchers) {
        this.vouchers = vouchers;
    }

    public List<Customers> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customers> customers) {
        this.customers = customers;
    }

    public List<Offers> getOffers() {
        return offers;
    }

    public void setOffers(List<Offers> offers) {
        this.offers = offers;
    }

    public Vouchers getSelectedVoucher() {
        return selectedVoucher;
    }

    public void setSelectedVoucher(Vouchers selectedVoucher) {
        this.selectedVoucher = selectedVoucher;
    }

    public Integer getSelectedCustomerId() {
        return selectedCustomerId;
    }

    public void setSelectedCustomerId(Integer selectedCustomerId) {
        this.selectedCustomerId = selectedCustomerId;
    }

    public Integer getSelectedOfferId() {
        return selectedOfferId;
    }

    public void setSelectedOfferId(Integer selectedOfferId) {
        this.selectedOfferId = selectedOfferId;
    }

    public Boolean getSelectedStatus() {
        return selectedStatus;
    }

    public void setSelectedStatus(Boolean selectedStatus) {
        this.selectedStatus = selectedStatus;
    }

    public String getSearchCode() {
        return searchCode;
    }

    public void setSearchCode(String searchCode) {
        this.searchCode = searchCode;
    }

    public String getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(String voucherType) {
        this.voucherType = voucherType;
    }

    public List<Offers> getEligibleOffers() {
        return eligibleOffers;
    }

    public void setEligibleOffers(List<Offers> eligibleOffers) {
        this.eligibleOffers = eligibleOffers;
    }

    public String getSelectedType() {
        return selectedType;
    }

    public void setSelectedType(String selectedType) {
        this.selectedType = selectedType;
    }
}
