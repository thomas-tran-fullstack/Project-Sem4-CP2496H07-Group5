package controllers;

import entityclass.CartItems;
import entityclass.Carts;
import entityclass.Customers;
import entityclass.ProductImages;
import entityclass.Products;
import entityclass.Vouchers;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sessionbeans.CartItemsFacadeLocal;
import sessionbeans.CartsFacadeLocal;
import sessionbeans.ProductImagesFacadeLocal;
import sessionbeans.ProductsFacadeLocal;
import sessionbeans.VouchersFacadeLocal;

@Named("cartMB")
@SessionScoped
public class CartMB implements Serializable {

    @EJB
    private ProductImagesFacadeLocal productImagesFacade;

    @EJB
    private VouchersFacadeLocal vouchersFacade;

    @EJB
    private ProductsFacadeLocal productsFacade;

    @EJB
    private CartsFacadeLocal cartsFacade;

    @EJB
    private CartItemsFacadeLocal cartItemsFacade;

    @Inject
    private AuthController auth;

    @Inject
    private ProductMB productMB;

    private Carts currentCart;
    private List<CartItems> cartItems;

    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal totalSavings;

    private String promoCode;
    private Vouchers appliedVoucher;
    private BigDecimal discountAmount;

    @PostConstruct
    public void init() {
        cartItems = new ArrayList<>();
        reloadCartFromDatabase(); // ✅ init cũng load nếu đã login
    }

    // ===================== DB LOAD =====================
    public void reloadCartFromDatabase() {
        currentCart = null;
        cartItems = new ArrayList<>();

        // reset voucher in memory if logged out (tránh hiển thị sai)
        if (!auth.isLoggedIn() || auth.getCurrentCustomer() == null) {
            appliedVoucher = null;
            promoCode = null;
            discountAmount = BigDecimal.ZERO;
            calculateTotal();
            return;
        }

        Customers customer = auth.getCurrentCustomer();
        currentCart = cartsFacade.findLatestByCustomer(customer.getCustomerID());

        if (currentCart != null && currentCart.getCartID() != null) {
            cartItems = cartItemsFacade.findByCartId(currentCart.getCartID());
        }

        calculateTotal();
    }

    private Carts ensureCart() {
        if (!auth.isLoggedIn() || auth.getCurrentCustomer() == null) return null;

        if (currentCart != null && currentCart.getCartID() != null) return currentCart;

        Customers customer = auth.getCurrentCustomer();
        currentCart = cartsFacade.findLatestByCustomer(customer.getCustomerID());

        if (currentCart == null) {
            Carts c = new Carts();
            c.setCustomerID(customer);
            c.setCreatedAt(new Date());
            cartsFacade.create(c);

            // ✅ reload lại để chắc chắn có ID
            currentCart = cartsFacade.findLatestByCustomer(customer.getCustomerID());
        }

        return currentCart;
    }

    // ===================== CART ACTIONS =====================
    public void addToCart(Products product, int quantity) {
        if (product == null || product.getProductID() == null || quantity <= 0) return;

        if (!auth.isLoggedIn() || auth.getCurrentCustomer() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please login to add items to cart"));
            return;
        }

        Carts cart = ensureCart();
        if (cart == null || cart.getCartID() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot create cart"));
            return;
        }

        // refresh product from DB (stock/price mới nhất)
        Products fresh = productsFacade.find(product.getProductID());
        if (fresh != null) product = fresh;

        if (product.getStockQuantity() != null && product.getStockQuantity() <= 0) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Product is out of stock"));
            return;
        }

        CartItems existing = cartItemsFacade.findByCartAndProduct(cart.getCartID(), product.getProductID());
        BigDecimal nowPrice = productMB.getDiscountedPrice(product);

        if (existing != null) {
            int newQty = (existing.getQuantity() != null ? existing.getQuantity() : 0) + quantity;

            if (product.getStockQuantity() != null && newQty > product.getStockQuantity()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Not enough stock available"));
                return;
            }

            existing.setQuantity(newQty);
            existing.setUnitPrice(nowPrice);
            existing.setProductID(product);
            cartItemsFacade.edit(existing);

        } else {
            if (product.getStockQuantity() != null && quantity > product.getStockQuantity()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Not enough stock available"));
                return;
            }

            CartItems ci = new CartItems();
            ci.setCartID(cart);
            ci.setProductID(product);
            ci.setQuantity(quantity);
            ci.setUnitPrice(nowPrice);
            cartItemsFacade.create(ci);
        }

        reloadCartFromDatabase();
    }

    public void updateQuantity(CartItems item, int newQuantity) {
        if (item == null || item.getCartItemID() == null) return;

        if (newQuantity <= 0) {
            removeItem(item);
            return;
        }

        CartItems dbItem = cartItemsFacade.find(item.getCartItemID());
        if (dbItem == null || dbItem.getProductID() == null || dbItem.getProductID().getProductID() == null) {
            reloadCartFromDatabase();
            return;
        }

        Products product = productsFacade.find(dbItem.getProductID().getProductID());
        if (product == null || product.getStockQuantity() == null) {
            // product bị xóa hoặc lỗi stock => remove item
            cartItemsFacade.remove(dbItem);
            reloadCartFromDatabase();
            return;
        }

        if (newQuantity > product.getStockQuantity()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Not enough stock available"));
            return;
        }

        dbItem.setProductID(product);
        dbItem.setQuantity(newQuantity);
        dbItem.setUnitPrice(productMB.getDiscountedPrice(product));
        cartItemsFacade.edit(dbItem);

        reloadCartFromDatabase();
    }

    public void removeItem(CartItems item) {
        if (item == null || item.getCartItemID() == null) return;

        CartItems dbItem = cartItemsFacade.find(item.getCartItemID());
        if (dbItem != null) cartItemsFacade.remove(dbItem);

        reloadCartFromDatabase();
    }

    public void clearCart() {
        if (!auth.isLoggedIn() || auth.getCurrentCustomer() == null) {
            cartItems = new ArrayList<>();
            appliedVoucher = null;
            promoCode = null;
            discountAmount = BigDecimal.ZERO;
            calculateTotal();
            return;
        }

        Carts cart = ensureCart();
        if (cart == null || cart.getCartID() == null) return;

        cartItemsFacade.deleteByCartId(cart.getCartID());

        appliedVoucher = null;
        promoCode = null;
        discountAmount = BigDecimal.ZERO;

        reloadCartFromDatabase();
    }

    // ===================== VALIDATE STOCK (CHUẨN DB) =====================
    /**
     * Kiểm tra lại tồn kho + giá ngay trước checkout.
     * - nếu product bị xóa / hết hàng => remove khỏi cart
     * - nếu qty > stock => giảm qty = stock
     * - nếu giá/offer đổi => cập nhật unitPrice
     *
     * @return true nếu KHÔNG thay đổi gì; false nếu có tự động chỉnh sửa (cart bị modified)
     */
    public boolean validateCartStock() {
        if (!auth.isLoggedIn() || auth.getCurrentCustomer() == null) return true;

        Carts cart = ensureCart();
        if (cart == null || cart.getCartID() == null) return true;

        boolean modified = false;

        List<CartItems> dbItems = cartItemsFacade.findByCartId(cart.getCartID());
        if (dbItems == null) dbItems = new ArrayList<>();

        List<CartItems> removeList = new ArrayList<>();

        for (CartItems ci : dbItems) {
            if (ci == null || ci.getCartItemID() == null || ci.getProductID() == null || ci.getProductID().getProductID() == null) {
                modified = true;
                removeList.add(ci);
                continue;
            }

            Products p = productsFacade.find(ci.getProductID().getProductID());
            if (p == null || p.getStockQuantity() == null) {
                modified = true;
                removeList.add(ci);
                continue;
            }

            // out of stock
            if (p.getStockQuantity() <= 0) {
                modified = true;
                removeList.add(ci);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning",
                                p.getProductName() + " is out of stock and has been removed from your cart"));
                continue;
            }

            boolean changedRow = false;

            // qty > stock => reduce
            if (ci.getQuantity() == null || ci.getQuantity() <= 0) {
                ci.setQuantity(1);
                changedRow = true;
                modified = true;
            } else if (ci.getQuantity() > p.getStockQuantity()) {
                ci.setQuantity(p.getStockQuantity());
                changedRow = true;
                modified = true;
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning",
                                "Quantity for " + p.getProductName() + " has been adjusted to available stock (" + p.getStockQuantity() + ")"));
            }

            // update price if changed
            BigDecimal nowPrice = productMB.getDiscountedPrice(p);
            if (ci.getUnitPrice() == null || ci.getUnitPrice().compareTo(nowPrice) != 0) {
                ci.setUnitPrice(nowPrice);
                changedRow = true;
                modified = true;
            }

            // keep product reference fresh
            ci.setProductID(p);

            if (changedRow) cartItemsFacade.edit(ci);
        }

        // remove invalid/out of stock
        for (CartItems r : removeList) {
            if (r == null) continue;
            if (r.getCartItemID() != null) {
                CartItems db = cartItemsFacade.find(r.getCartItemID());
                if (db != null) cartItemsFacade.remove(db);
            }
        }

        if (modified) {
            reloadCartFromDatabase();
        } else {
            // vẫn reload nhẹ để totals chắc chắn đúng
            reloadCartFromDatabase();
        }

        return !modified;
    }

    // ===================== TOTALS =====================
    private void calculateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal originalTotal = BigDecimal.ZERO;

        if (cartItems != null) {
            for (CartItems item : cartItems) {
                if (item == null || item.getProductID() == null) continue;

                BigDecimal discountedPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal originalPrice = item.getProductID().getUnitPrice() != null
                        ? item.getProductID().getUnitPrice() : BigDecimal.ZERO;

                int q = item.getQuantity() != null ? item.getQuantity() : 0;
                BigDecimal qty = BigDecimal.valueOf(q);

                total = total.add(discountedPrice.multiply(qty));
                originalTotal = originalTotal.add(originalPrice.multiply(qty));
            }
        }

        subtotal = total;

        BigDecimal offerSavings = originalTotal.subtract(total);
        if (offerSavings.compareTo(BigDecimal.ZERO) < 0) offerSavings = BigDecimal.ZERO;

        discountAmount = BigDecimal.ZERO;
        if (appliedVoucher != null && appliedVoucher.getDiscountValue() != null) {
            discountAmount = appliedVoucher.getDiscountValue();
            if (discountAmount.compareTo(subtotal) > 0) discountAmount = subtotal;
        }

        shippingCost = BigDecimal.ZERO;
        // Tax is calculated on (subtotal - discount) to avoid overcharging when a voucher is applied.
        BigDecimal taxableBase = subtotal.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        if (taxableBase.compareTo(BigDecimal.ZERO) < 0) taxableBase = BigDecimal.ZERO;
        taxAmount = taxableBase.multiply(new BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP);
        totalAmount = taxableBase.add(shippingCost).add(taxAmount);

        totalSavings = offerSavings.add(discountAmount);
        if (totalSavings.compareTo(BigDecimal.ZERO) < 0) totalSavings = BigDecimal.ZERO;
    }

    // ===================== UI HELPERS =====================
    public String productImageUrl(Products product) {
        if (product == null) return null;
        List<ProductImages> images = productImagesFacade.findByProductID(product);
        return (images != null && !images.isEmpty()) ? images.get(0).getImageURL() : null;
    }

    public BigDecimal getItemTotal(CartItems item) {
        if (item == null) return BigDecimal.ZERO;
        BigDecimal up = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        int qty = item.getQuantity() != null ? item.getQuantity() : 0;
        return up.multiply(BigDecimal.valueOf(qty));
    }

    // ===================== GETTERS =====================
    public boolean isCartEmpty() { return cartItems == null || cartItems.isEmpty(); }
    public int getCartItemCount() { return cartItems != null ? cartItems.size() : 0; }

    public List<CartItems> getCartItems() { return cartItems; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getTotalSavings() { return totalSavings; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public Vouchers getAppliedVoucher() { return appliedVoucher; }
    public BigDecimal getDiscountAmount() { return discountAmount; }

    public Carts getCurrentCart() { return currentCart; }
}
