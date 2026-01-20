package controllers;

import entityclass.Addresses;
import entityclass.CartItems;
import entityclass.Customers;
import entityclass.OrderDetails;
import entityclass.Orders;
import entityclass.ProductImages;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import sessionbeans.AddressesFacadeLocal;
import sessionbeans.OrderDetailsFacadeLocal;
import sessionbeans.OrdersFacadeLocal;
import sessionbeans.ProductImagesFacadeLocal;
import sessionbeans.VouchersFacadeLocal;

@Named("checkoutMB")
@SessionScoped
public class CheckoutMB implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private OrdersFacadeLocal ordersFacade;

    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;

    @EJB
    private AddressesFacadeLocal addressesFacade;

    @EJB
    private ProductImagesFacadeLocal productImagesFacade;

    @EJB
    private VouchersFacadeLocal vouchersFacade;

    @Inject
    private AuthController auth;

    @Inject
    private CartMB cartMB;

    @Inject
    private AddressController addressCtrl;

    // Checkout fields
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Addresses selectedAddress;
    private Integer selectedAddressID;
    private String shippingMethod; // "standard" or "express"
    private String paymentMethodType; // "cod" or "online"
    private String paymentMethod; // "card", "paypal", "applepay"
    private boolean saveAddress;
    private boolean billingSameAsShipping;

    // New address fields
    private String newAddressLine1;
    private String newAddressLine2;
    private String newCity;
    private String newState;
    private String newRegion;
    private String newZipCode;
    private String newCountry;

    // Payment fields
    private String cardNumber;
    private String cardholderName;
    private String expiryDate;
    private String cvv;

    // Voucher fields
    private String voucherCode;
    private entityclass.Vouchers appliedVoucher;
    private BigDecimal discountAmount;

    @PostConstruct
    public void init() {
        loadCustomerInfo();
        shippingMethod = "standard";
        paymentMethodType = "cod";
        paymentMethod = "card";
        billingSameAsShipping = true;
        saveAddress = false;
        // Clear voucher fields for fresh checkout
        voucherCode = null;
        appliedVoucher = null;
        discountAmount = null;
        // Ensure addresses are loaded for the converter
        ensureAddressesLoaded();
    }

    public void loadCustomerInfo() {
        Customers customer = auth.getCurrentCustomer();
        if (customer != null) {
            firstName = customer.getFirstName();
            lastName = customer.getLastName();
            phoneNumber = customer.getMobilePhone();

            // Load addresses
            addressCtrl.loadForCurrentUser();
            refreshSelectedAddress();
        }
    }

    /**
     * Refresh selected address to ensure it's valid based on current addresses list
     */
    private void refreshSelectedAddress() {
        // Refresh the address list first to ensure we have the latest data
        addressCtrl.loadForCurrentUser();

        List<Addresses> addresses = addressCtrl.getAddresses();
        if (addresses != null && !addresses.isEmpty()) {
            // First, try to restore selectedAddress from selectedAddressID if it's set
            if (selectedAddressID != null && selectedAddress == null) {
                for (Addresses addr : addresses) {
                    if (addr.getAddressID().equals(selectedAddressID)) {
                        selectedAddress = addr;
                        break;
                    }
                }
            }

            // If we have a selected address, check if it's still in the list
            if (selectedAddress != null) {
                boolean found = false;
                for (Addresses addr : addresses) {
                    if (addr.getAddressID().equals(selectedAddress.getAddressID())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    selectedAddress = null; // Clear invalid selection
                    selectedAddressID = null;
                }
            }

            // If no valid selection, select default or first address
            if (selectedAddress == null) {
                // Select default address if available
                for (Addresses addr : addresses) {
                    if (addr.getIsDefault() != null && addr.getIsDefault()) {
                        selectedAddress = addr;
                        selectedAddressID = addr.getAddressID();
                        break;
                    }
                }
                // If no default, select first address
                if (selectedAddress == null) {
                    selectedAddress = addresses.get(0);
                    selectedAddressID = selectedAddress.getAddressID();
                }
            } else {
                // Ensure selectedAddressID is set
                if (selectedAddressID == null) {
                    selectedAddressID = selectedAddress.getAddressID();
                }
            }
        } else {
            // No addresses available, clear selection
            selectedAddress = null;
            selectedAddressID = null;
        }
    }

    public String placeOrder() {
        try {
            // Refresh selected address to ensure it's valid
            refreshSelectedAddress();

            // Validate cart is not empty
            if (cartMB.isCartEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Your cart is empty"));
                return null;
            }

            // Validate required fields
            if (firstName == null || firstName.trim().isEmpty() ||
                lastName == null || lastName.trim().isEmpty() ||
                phoneNumber == null || phoneNumber.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please fill in all contact details"));
                return null;
            }

            // Validate address
            if (selectedAddress == null && (newAddressLine1 == null || newAddressLine1.trim().isEmpty())) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please select or enter a shipping address"));
                return null;
            }

            // Handle new address if entered
          Addresses shippingAddress = null;
if (selectedAddress != null && selectedAddress.getAddressID() != null) {
    shippingAddress = addressesFacade.find(selectedAddress.getAddressID());
}
            if (shippingAddress == null && newAddressLine1 != null && !newAddressLine1.trim().isEmpty()) {
                shippingAddress = new Addresses();
                shippingAddress.setStreet(newAddressLine1);
                shippingAddress.setHouse(newAddressLine2);
                shippingAddress.setCity(newCity);
                shippingAddress.setState(newState);
                shippingAddress.setRegion(newRegion);
                shippingAddress.setCountry(newCountry);
                shippingAddress.setCustomerID(auth.getCurrentCustomer());
                shippingAddress.setCreatedAt(new Date());

                if (saveAddress) {
                    // Unset other defaults if this is default
                    if (shippingAddress.getIsDefault() == null) {
                        shippingAddress.setIsDefault(false);
                    }
                    addressesFacade.create(shippingAddress);
                }
            }

            // Create order
            Orders order = new Orders();
            order.setCustomerID(auth.getCurrentCustomer());
            order.setOrderDate(new Date());
            order.setTotalAmount(getFinalTotal());
            order.setShippingMethod(shippingMethod);

            // Set order status and payment status based on payment method
            if ("cod".equals(paymentMethodType)) {
                order.setPaymentMethod("COD");
                order.setPaymentStatus("UNPAID");
                order.setStatus("NEW");
            } else if ("online".equals(paymentMethodType)) {
                order.setPaymentMethod("ONLINE");
                order.setPaymentStatus("WAITING");
                order.setStatus("NEW");
            } else {
                order.setPaymentMethod("ONLINE");
                order.setPaymentStatus("WAITING");
                order.setStatus("NEW");
            }

            ordersFacade.create(order);

            // Mark voucher as used immediately after order creation to prevent reuse
            if (appliedVoucher != null) {
                appliedVoucher.setIsUsed(true);
                vouchersFacade.edit(appliedVoucher);
            }

            // Create order details
            for (CartItems item : cartMB.getCartItems()) {
                OrderDetails detail = new OrderDetails();
                detail.setOrderID(order);
                detail.setProductID(item.getProductID());
                detail.setQuantity(item.getQuantity());
                detail.setUnitPrice(item.getUnitPrice());
                detail.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

                // Populate additional fields from updated entity
                detail.setCustomerName(firstName + " " + lastName);
                List<String> addressParts = new ArrayList<>();
                if (shippingAddress.getStreet() != null && !shippingAddress.getStreet().trim().isEmpty()) addressParts.add(shippingAddress.getStreet().trim());
                if (shippingAddress.getHouse() != null && !shippingAddress.getHouse().trim().isEmpty()) addressParts.add(shippingAddress.getHouse().trim());
                if (shippingAddress.getCity() != null && !shippingAddress.getCity().trim().isEmpty()) addressParts.add(shippingAddress.getCity().trim());
                if (shippingAddress.getState() != null && !shippingAddress.getState().trim().isEmpty()) addressParts.add(shippingAddress.getState().trim());
                if (shippingAddress.getRegion() != null && !shippingAddress.getRegion().trim().isEmpty()) addressParts.add(shippingAddress.getRegion().trim());
                if (shippingAddress.getCountry() != null && !shippingAddress.getCountry().trim().isEmpty()) addressParts.add(shippingAddress.getCountry().trim());
                detail.setCustomerAddress(String.join(", ", addressParts));
                detail.setCustomerPhone(phoneNumber);
                detail.setProductName(item.getProductID().getProductName());
                detail.setProductImage(productImageUrl(item.getProductID()));

                orderDetailsFacade.create(detail);
            }

            // Clear cart
            cartMB.clearCart();

            // Clear voucher fields for new checkout
            voucherCode = null;
            appliedVoucher = null;
            discountAmount = null;

            // Redirect based on payment method
            if ("cod".equals(paymentMethodType)) {
                return "/pages/user/order-success.xhtml?faces-redirect=true&orderId=" + order.getOrderID();
            } else {
                return "/pages/user/order-payment.xhtml?faces-redirect=true&orderId=" + order.getOrderID();
            }

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to place order: " + e.getMessage()));
            e.printStackTrace();
            return null;
        }
    }

    public BigDecimal getShippingCost() {
        if ("express".equals(shippingMethod)) {
            return new BigDecimal("15.00");
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTaxAmount() {
        // Simple tax calculation (8% of subtotal)
        return cartMB.getSubtotal().multiply(new BigDecimal("0.08"));
    }

    public BigDecimal getFinalTotal() {
        BigDecimal total = cartMB.getSubtotal().add(getTaxAmount()).add(getShippingCost());
        if (discountAmount != null) {
            total = total.subtract(discountAmount);
        }
        return total;
    }

    public String productImageUrl(entityclass.Products product) {
        List<ProductImages> images = productImagesFacade.findByProductID(product);
        if (images != null && !images.isEmpty()) {
            return images.get(0).getImageURL();
        }
        return null;
    }

    public void applyVoucher() {
        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please enter a voucher code"));
            return;
        }

        try {
            entityclass.Vouchers voucher = vouchersFacade.findByVoucherCode(voucherCode.trim());
            if (voucher == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid voucher code"));
                return;
            }

            // Check if voucher belongs to current customer
            if (voucher.getCustomerID() == null || !voucher.getCustomerID().equals(auth.getCurrentCustomer())) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "This voucher is not valid for your account"));
                return;
            }

            // Check if voucher is already used
            if (voucher.getIsUsed() != null && voucher.getIsUsed()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Voucher has already been used"));
                return;
            }

            // Check if voucher is expired
            Date now = new Date();
            if (voucher.getExpiryDate().before(now)) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Voucher is expired"));
                return;
            }

            // Check if voucher has a discount value (either from offer or direct)
            if (voucher.getDiscountValue() == null || voucher.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
                // If no direct discount value, check if it has an active offer
                entityclass.Offers offer = voucher.getOfferID();
                if (offer == null) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid voucher"));
                    return;
                }
                // Check if offer is active
                if (!"Active".equals(offer.getStatus())) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Voucher is inactive"));
                    return;
                }
            }

            // Calculate discount using voucher's direct discountValue field or offer's discountValue
            BigDecimal subtotal = cartMB.getSubtotal();
            discountAmount = voucher.getDiscountValue();
            if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                // Use offer's discountValue if voucher's is not set
                entityclass.Offers offer = voucher.getOfferID();
                if (offer != null && offer.getDiscountValue() != null) {
                    discountAmount = new BigDecimal(offer.getDiscountValue());
                } else {
                    discountAmount = BigDecimal.ZERO;
                }
            }

            // Ensure discount doesn't exceed subtotal
            if (discountAmount.compareTo(subtotal) > 0) {
                discountAmount = subtotal;
            }

            appliedVoucher = voucher;
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Voucher applied successfully"));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to apply voucher"));
            e.printStackTrace();
        }
    }

    public void removeVoucher() {
        appliedVoucher = null;
        discountAmount = null;
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Voucher removed"));
    }

    /**
     * Ensure addresses are loaded for the converter
     */
    private void ensureAddressesLoaded() {
        if (addressCtrl != null) {
            addressCtrl.loadForCurrentUser();
        }
    }

    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Addresses getSelectedAddress() {
        // Ensure selected address is valid before returning
        refreshSelectedAddress();
        return selectedAddress;
    }
    public void setSelectedAddress(Addresses selectedAddress) {
        this.selectedAddress = selectedAddress;
        this.selectedAddressID = (selectedAddress != null) ? selectedAddress.getAddressID() : null;
    }

    public Integer getSelectedAddressID() {
        return selectedAddressID;
    }
    public void setSelectedAddressID(Integer selectedAddressID) {
        this.selectedAddressID = selectedAddressID;
        if (selectedAddressID != null) {
            // Find the address by ID
            List<Addresses> addresses = addressCtrl.getAddresses();
            if (addresses != null) {
                for (Addresses addr : addresses) {
                    if (addr.getAddressID().equals(selectedAddressID)) {
                        this.selectedAddress = addr;
                        return;
                    }
                }
            }
        }
        this.selectedAddress = null;
    }

    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentMethodType() { return paymentMethodType; }
    public void setPaymentMethodType(String paymentMethodType) { this.paymentMethodType = paymentMethodType; }

    public boolean isSaveAddress() { return saveAddress; }
    public void setSaveAddress(boolean saveAddress) { this.saveAddress = saveAddress; }

    public boolean isBillingSameAsShipping() { return billingSameAsShipping; }
    public void setBillingSameAsShipping(boolean billingSameAsShipping) { this.billingSameAsShipping = billingSameAsShipping; }

    public String getNewAddressLine1() { return newAddressLine1; }
    public void setNewAddressLine1(String newAddressLine1) { this.newAddressLine1 = newAddressLine1; }

    public String getNewAddressLine2() { return newAddressLine2; }
    public void setNewAddressLine2(String newAddressLine2) { this.newAddressLine2 = newAddressLine2; }

    public String getNewCity() { return newCity; }
    public void setNewCity(String newCity) { this.newCity = newCity; }

    public String getNewState() { return newState; }
    public void setNewState(String newState) { this.newState = newState; }

    public String getNewRegion() { return newRegion; }
    public void setNewRegion(String newRegion) { this.newRegion = newRegion; }

    public String getNewZipCode() { return newZipCode; }
    public void setNewZipCode(String newZipCode) { this.newZipCode = newZipCode; }

    public String getNewCountry() { return newCountry; }
    public void setNewCountry(String newCountry) { this.newCountry = newCountry; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCardholderName() { return cardholderName; }
    public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public entityclass.Vouchers getAppliedVoucher() { return appliedVoucher; }
    public void setAppliedVoucher(entityclass.Vouchers appliedVoucher) { this.appliedVoucher = appliedVoucher; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
}
