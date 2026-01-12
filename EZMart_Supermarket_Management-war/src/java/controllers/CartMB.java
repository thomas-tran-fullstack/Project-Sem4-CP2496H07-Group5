package controllers;

import entityclass.CartItems;
import entityclass.Carts;
import entityclass.Customers;
import entityclass.ProductImages;
import entityclass.Products;
import entityclass.Vouchers;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import sessionbeans.ProductImagesFacadeLocal;
import sessionbeans.VouchersFacadeLocal;

@Named("cartMB")
@SessionScoped
public class CartMB implements Serializable {

    @EJB
    private ProductImagesFacadeLocal productImagesFacade;

    @EJB
    private VouchersFacadeLocal vouchersFacade;

    @Inject
    private AuthController auth;

    @Inject
    private ProductMB productMB;

    private List<CartItems> cartItems;
    private BigDecimal totalPrice;
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal totalSavings;

    // Voucher-related fields
    private String promoCode;
    private Vouchers appliedVoucher;
    private BigDecimal discountAmount;

    @PostConstruct
    public void init() {
        cartItems = new ArrayList<>();
        calculateTotal();
    }

    public void updateQuantity(CartItems item, int newQuantity) {
        if (newQuantity <= 0) {
            removeItem(item);
            return;
        }

        if (newQuantity > item.getProductID().getStockQuantity()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Not enough stock available"));
            return;
        }

        item.setQuantity(newQuantity);
        calculateTotal();
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Quantity updated"));
    }

    public void removeItem(CartItems item) {
        cartItems.remove(item);
        calculateTotal();
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Item removed from cart"));
    }

    public void clearCart() {
        cartItems.clear();
        calculateTotal();
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Cart cleared"));
    }

    public void applyVoucher() {
        try {
            if (promoCode == null || promoCode.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please enter a promo code"));
                return;
            }

            // Find voucher by code
            Vouchers voucher = vouchersFacade.findByVoucherCode(promoCode.trim().toUpperCase());
            if (voucher == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid promo code"));
                return;
            }

            // Check if voucher is already used
            if (Boolean.TRUE.equals(voucher.getIsUsed())) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "This voucher has already been used"));
                return;
            }

            // Check if voucher has expired
            if (voucher.getExpiryDate() != null && voucher.getExpiryDate().before(new Date())) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "This voucher has expired"));
                return;
            }

            // Check if voucher belongs to current customer (if it's customer-specific)
            if (voucher.getCustomerID() != null && auth.getCurrentCustomer() != null) {
                if (!voucher.getCustomerID().getCustomerID().equals(auth.getCurrentCustomer().getCustomerID())) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "This voucher is not valid for your account"));
                    return;
                }
            }

            // Apply the voucher
            appliedVoucher = voucher;
            calculateTotal();

            // Mark voucher as used
            voucher.setIsUsed(true);
            vouchersFacade.edit(voucher);

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success",
                    "Voucher applied successfully! You saved $" + discountAmount));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to apply voucher: " + e.getMessage()));
        }
    }

    public void addToCart(Products product, int quantity) {
        // Check if user is logged in
        if (!auth.isLoggedIn()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please login to add items to cart"));
            return;
        }

        // Check if product already exists in cart
        for (CartItems item : cartItems) {
            if (item.getProductID().getProductID().equals(product.getProductID())) {
                int newQuantity = item.getQuantity() + quantity;
                if (newQuantity > product.getStockQuantity()) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Not enough stock available"));
                    return;
                }
                item.setQuantity(newQuantity);
                calculateTotal();
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Product quantity updated in cart"));
                return;
            }
        }

        // Create new cart item
        CartItems newItem = new CartItems();
        newItem.setProductID(product);
        newItem.setQuantity(quantity);
        newItem.setUnitPrice(productMB.getDiscountedPrice(product));
        cartItems.add(newItem);
        calculateTotal();
    }

    private void calculateTotal() {
        totalPrice = BigDecimal.ZERO;
        BigDecimal originalTotal = BigDecimal.ZERO;

        for (CartItems item : cartItems) {
            BigDecimal discountedPrice = item.getUnitPrice();
            BigDecimal originalPrice = item.getProductID().getUnitPrice();
            BigDecimal itemTotal = discountedPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal originalItemTotal = originalPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            totalPrice = totalPrice.add(itemTotal);
            originalTotal = originalTotal.add(originalItemTotal);
        }

        subtotal = totalPrice;

        // Calculate product offer savings
        BigDecimal offerSavings = originalTotal.subtract(totalPrice);

        // Apply voucher discount
        discountAmount = BigDecimal.ZERO;
        if (appliedVoucher != null && appliedVoucher.getDiscountValue() != null) {
            discountAmount = appliedVoucher.getDiscountValue();
            // Ensure discount doesn't exceed subtotal
            if (discountAmount.compareTo(subtotal) > 0) {
                discountAmount = subtotal;
            }
        }

        shippingCost = BigDecimal.ZERO; // Free shipping for now
        taxAmount = BigDecimal.ZERO; // No tax for now
        totalAmount = subtotal.subtract(discountAmount).add(shippingCost).add(taxAmount);
        totalSavings = offerSavings.add(discountAmount); // Total savings from offers and vouchers
    }

    public String productImageUrl(Products product) {
        List<ProductImages> images = productImagesFacade.findByProductID(product);
        if (images != null && !images.isEmpty()) {
            return images.get(0).getImageURL();
        }
        return null;
    }

    public BigDecimal getItemTotal(CartItems item) {
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    // Getters and Setters
    public List<CartItems> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItems> cartItems) {
        this.cartItems = cartItems;
    }



    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public boolean isCartEmpty() {
        return cartItems == null || cartItems.isEmpty();
    }

    public int getCartItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalSavings() {
        return totalSavings;
    }

    public void setTotalSavings(BigDecimal totalSavings) {
        this.totalSavings = totalSavings;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public Vouchers getAppliedVoucher() {
        return appliedVoucher;
    }

    public void setAppliedVoucher(Vouchers appliedVoucher) {
        this.appliedVoucher = appliedVoucher;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
}
