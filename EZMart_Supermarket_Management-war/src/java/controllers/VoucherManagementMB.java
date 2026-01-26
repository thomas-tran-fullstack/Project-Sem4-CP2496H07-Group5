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
    private Integer voucherToDeleteId;
    private Vouchers voucherToDelete; // For delete confirmation
    private Date startDateFilter;
    private Date endDateFilter;

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
                    .filter(v -> startDateFilter == null || (v.getExpiryDate() != null && !v.getExpiryDate().before(startDateFilter)))
                    .filter(v -> endDateFilter == null || (v.getExpiryDate() != null && !v.getExpiryDate().after(endDateFilter)))
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

    public void deleteVoucher(Vouchers voucher) {
    try {
        if (voucher == null || voucher.getVoucherID() == null) {
            return;
        }

        Vouchers v = vouchersFacade.find(voucher.getVoucherID());
        if (v != null) {
            vouchersFacade.remove(v);
            loadVouchers();
        }

        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Voucher deleted successfully"));

    } catch (Exception e) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Delete failed: " + e.getMessage()));
    }
}


    public void confirmDeleteVoucher() {
    try {
        if (voucherToDelete == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No voucher selected"));
            return;
        }

        // Check if voucher is used - prevent deletion if not used
        if (voucherToDelete.getIsUsed() == null || !voucherToDelete.getIsUsed()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot delete unused voucher. Voucher must be used before deletion."));
            return;
        }

        Vouchers managed = vouchersFacade.find(voucherToDelete.getVoucherID());
        vouchersFacade.remove(managed);

        loadVouchers();

        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Voucher deleted successfully"));

    } catch (Exception e) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
    } finally {
        voucherToDelete = null;
    }
}


    public void saveVoucher() {
        try {
            // Validate voucher code
            if (selectedVoucher.getVoucherCode() == null || selectedVoucher.getVoucherCode().trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "Voucher code is required"));
                return;
            }
            if (selectedVoucher.getVoucherCode().length() < 3 || selectedVoucher.getVoucherCode().length() > 20) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "Voucher code must be between 3 and 20 characters"));
                return;
            }

            // Validate discount value
            if (selectedVoucher.getDiscountValue() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "Discount value is required"));
                return;
            }
            if (selectedVoucher.getDiscountValue().compareTo(new BigDecimal("0.01")) < 0) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "Discount value must be at least 0.01"));
                return;
            }

            // Validate expiry date
            if (selectedVoucher.getExpiryDate() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "Expiry date is required"));
                return;
            }
            if (selectedVoucher.getExpiryDate().before(new Date())) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "Expiry date must be in the future"));
                return;
            }

            // Validate voucher type and relationships
            if ("customer".equals(voucherType)) {
                if (selectedCustomerId == null) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "Customer selection is required for customer vouchers"));
                    return;
                }
            } else if ("offer".equals(voucherType)) {
                if (selectedOfferId == null) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "Offer selection is required for offer vouchers"));
                    return;
                }
            }

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

    // Delete functionality removed as per requirements

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

                // Set expiry date to avoid timezone issues - use only the date part
                Date expiryDate = selectedOffer.getEndDate();
                if (expiryDate != null) {
                    // Create a new date with only the date components to avoid timezone conversion issues
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(expiryDate);
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    cal.set(java.util.Calendar.MINUTE, 0);
                    cal.set(java.util.Calendar.SECOND, 0);
                    cal.set(java.util.Calendar.MILLISECOND, 0);
                    selectedVoucher.setExpiryDate(cal.getTime());
                } else {
                    selectedVoucher.setExpiryDate(null);
                }
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

    public Integer getVoucherToDeleteId() {
        return voucherToDeleteId;
    }

    public void setVoucherToDeleteId(Integer voucherToDeleteId) {
        this.voucherToDeleteId = voucherToDeleteId;
    }

    // Statistics methods for admin dashboard
    public int getTotalVouchers() {
        return vouchers != null ? vouchers.size() : 0;
    }

    public int getUsedVouchers() {
        if (vouchers == null) return 0;
        return (int) vouchers.stream()
                .filter(v -> v.getIsUsed() != null && v.getIsUsed())
                .count();
    }

    public int getUnusedVouchers() {
        if (vouchers == null) return 0;
        return (int) vouchers.stream()
                .filter(v -> v.getIsUsed() == null || !v.getIsUsed())
                .count();
    }

    public int getExpiredVouchers() {
        if (vouchers == null) return 0;
        Date now = new Date();
        return (int) vouchers.stream()
                .filter(v -> v.getExpiryDate() != null && v.getExpiryDate().before(now))
                .count();
    }

    public int getActiveVouchers() {
        if (vouchers == null) return 0;
        Date now = new Date();
        return (int) vouchers.stream()
                .filter(v -> (v.getIsUsed() == null || !v.getIsUsed()) &&
                            (v.getExpiryDate() == null || v.getExpiryDate().after(now)))
                .count();
    }

    public BigDecimal getTotalDiscountValue() {
        if (vouchers == null) return BigDecimal.ZERO;
        return vouchers.stream()
                .filter(v -> v.getDiscountValue() != null)
                .map(Vouchers::getDiscountValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getUsedDiscountValue() {
        if (vouchers == null) return BigDecimal.ZERO;
        return vouchers.stream()
                .filter(v -> v.getIsUsed() != null && v.getIsUsed() && v.getDiscountValue() != null)
                .map(Vouchers::getDiscountValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getActiveVouchersCount() {
        return getActiveVouchers();
    }

    public int getExpiredVouchersCount() {
        return getExpiredVouchers();
    }

    public int getUsedVouchersPercentage() {
        if (vouchers == null || vouchers.isEmpty()) return 0;
        long usedCount = vouchers.stream()
                .filter(v -> v.getIsUsed() != null && v.getIsUsed())
                .count();
        return (int) ((usedCount * 100) / vouchers.size());
    }

    public int getUnusedVouchersPercentage() {
        if (vouchers == null || vouchers.isEmpty()) return 0;
        long unusedCount = vouchers.stream()
                .filter(v -> v.getIsUsed() == null || !v.getIsUsed())
                .count();
        return (int) ((unusedCount * 100) / vouchers.size());
    }

    public Vouchers getVoucherToDelete() {
    return voucherToDelete;
}

public void setVoucherToDelete(Vouchers voucherToDelete) {
    this.voucherToDelete = voucherToDelete;
}

public Date getStartDateFilter() {
    return startDateFilter;
}

public void setStartDateFilter(Date startDateFilter) {
    this.startDateFilter = startDateFilter;
}

public Date getEndDateFilter() {
    return endDateFilter;
}

public void setEndDateFilter(Date endDateFilter) {
    this.endDateFilter = endDateFilter;
}

}
