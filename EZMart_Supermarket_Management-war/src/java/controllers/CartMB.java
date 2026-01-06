package controllers;

import entityclass.CartItems;
import entityclass.Carts;
import entityclass.Customers;
import entityclass.ProductImages;
import entityclass.Products;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import sessionbeans.CartItemsFacadeLocal;
import sessionbeans.CartsFacadeLocal;
import sessionbeans.ProductImagesFacadeLocal;

@Named("cartMB")
@ViewScoped
public class CartMB implements Serializable {

    @EJB
    private CartsFacadeLocal cartsFacade;

    @EJB
    private CartItemsFacadeLocal cartItemsFacade;

    @EJB
    private ProductImagesFacadeLocal productImagesFacade;

    @Inject
    private AuthController auth;

    private List<CartItems> cartItems;
    private Carts currentCart;
    private BigDecimal totalPrice;

    @PostConstruct
    public void init() {
        loadCart();
    }

    public void loadCart() {
        if (auth.isLoggedIn() && auth.getCurrentCustomer() != null) {
            Customers customer = auth.getCurrentCustomer();
            List<Carts> customerCarts = cartsFacade.findByCustomerID(customer.getCustomerID());
            if (!customerCarts.isEmpty()) {
                currentCart = customerCarts.get(0); // Assuming one cart per customer
                cartItems = currentCart.getCartItemsList();
                if (cartItems == null) {
                    cartItems = new ArrayList<>();
                }
                calculateTotal();
            } else {
                cartItems = new ArrayList<>();
                totalPrice = BigDecimal.ZERO;
            }
        } else {
            cartItems = new ArrayList<>();
            totalPrice = BigDecimal.ZERO;
        }
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
        cartItemsFacade.edit(item);
        calculateTotal();
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Quantity updated"));
    }

    public void removeItem(CartItems item) {
        try {
            cartItemsFacade.remove(item);
            cartItems.remove(item);
            calculateTotal();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Item removed from cart"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to remove item"));
        }
    }

    public void clearCart() {
        try {
            for (CartItems item : cartItems) {
                cartItemsFacade.remove(item);
            }
            cartItems.clear();
            totalPrice = BigDecimal.ZERO;
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Cart cleared"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to clear cart"));
        }
    }

    private void calculateTotal() {
        totalPrice = BigDecimal.ZERO;
        for (CartItems item : cartItems) {
            BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalPrice = totalPrice.add(itemTotal);
        }
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

    public Carts getCurrentCart() {
        return currentCart;
    }

    public void setCurrentCart(Carts currentCart) {
        this.currentCart = currentCart;
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
}
