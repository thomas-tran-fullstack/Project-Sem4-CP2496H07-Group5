package controllers;

import entityclass.Vouchers;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import sessionbeans.VouchersFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@Named("customerVoucherMB")
@ViewScoped
public class CustomerVoucherMB implements Serializable {

    @EJB
    private VouchersFacadeLocal vouchersFacade;

    private List<Vouchers> myVouchers;
    private List<Vouchers> activeVouchers;
    private List<Vouchers> usedVouchers;
    private List<Vouchers> expiredVouchers;
    private String searchCode;

    @PostConstruct
    public void init() {
        loadMyVouchers();
    }

    public void loadMyVouchers() {
        // Get current customer from session/auth
        FacesContext context = FacesContext.getCurrentInstance();
        Integer customerId = (Integer) context.getExternalContext().getSessionMap().get("customerId");

        if (customerId != null) {
            List<Vouchers> allVouchers = vouchersFacade.findByCustomerID(customerId);
            myVouchers = allVouchers;

            Date now = new Date();
            activeVouchers = allVouchers.stream()
                    .filter(v -> !v.getIsUsed() && (v.getExpiryDate() == null || v.getExpiryDate().after(now)))
                    .collect(Collectors.toList());

            usedVouchers = allVouchers.stream()
                    .filter(v -> v.getIsUsed())
                    .collect(Collectors.toList());

            expiredVouchers = allVouchers.stream()
                    .filter(v -> !v.getIsUsed() && v.getExpiryDate() != null && v.getExpiryDate().before(now))
                    .collect(Collectors.toList());
        }
    }

    public void searchVouchers() {
        if (searchCode != null && !searchCode.trim().isEmpty()) {
            String code = searchCode.trim().toUpperCase();
            myVouchers = myVouchers.stream()
                    .filter(v -> v.getVoucherCode().toUpperCase().contains(code))
                    .collect(Collectors.toList());
        } else {
            loadMyVouchers();
        }
    }

    public boolean isVoucherValid(Vouchers voucher) {
        if (voucher.getIsUsed()) {
            return false;
        }
        if (voucher.getExpiryDate() != null && voucher.getExpiryDate().before(new Date())) {
            return false;
        }
        return true;
    }

    public String getVoucherStatus(Vouchers voucher) {
        if (voucher.getIsUsed()) {
            return "Used";
        }
        if (voucher.getExpiryDate() != null && voucher.getExpiryDate().before(new Date())) {
            return "Expired";
        }
        return "Active";
    }

    public String getStatusColor(Vouchers voucher) {
        String status = getVoucherStatus(voucher);
        switch (status) {
            case "Active":
                return "success";
            case "Used":
                return "secondary";
            case "Expired":
                return "danger";
            default:
                return "primary";
        }
    }

    // Getters and Setters
    public List<Vouchers> getMyVouchers() {
        return myVouchers;
    }

    public void setMyVouchers(List<Vouchers> myVouchers) {
        this.myVouchers = myVouchers;
    }

    public List<Vouchers> getActiveVouchers() {
        return activeVouchers;
    }

    public void setActiveVouchers(List<Vouchers> activeVouchers) {
        this.activeVouchers = activeVouchers;
    }

    public List<Vouchers> getUsedVouchers() {
        return usedVouchers;
    }

    public void setUsedVouchers(List<Vouchers> usedVouchers) {
        this.usedVouchers = usedVouchers;
    }

    public List<Vouchers> getExpiredVouchers() {
        return expiredVouchers;
    }

    public void setExpiredVouchers(List<Vouchers> expiredVouchers) {
        this.expiredVouchers = expiredVouchers;
    }

    public String getSearchCode() {
        return searchCode;
    }

    public void setSearchCode(String searchCode) {
        this.searchCode = searchCode;
    }
}
