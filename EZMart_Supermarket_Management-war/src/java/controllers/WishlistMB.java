/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controllers;

import entityclass.Customers;
import entityclass.Products;
import entityclass.ProductImages;
import entityclass.WishlistItems;
import entityclass.Wishlists;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PreRenderViewEvent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import sessionbeans.CustomersFacadeLocal;
import sessionbeans.ProductsFacadeLocal;
import sessionbeans.ProductImagesFacadeLocal;
import sessionbeans.WishlistItemsFacadeLocal;
import sessionbeans.WishlistsFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@Named("wishlistMB")
@SessionScoped
public class WishlistMB implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private WishlistsFacadeLocal wishlistsFacade;

    @EJB
    private WishlistItemsFacadeLocal wishlistItemsFacade;

    @EJB
    private CustomersFacadeLocal customersFacade;

    @EJB
    private ProductsFacadeLocal productsFacade;

    @EJB
    private ProductImagesFacadeLocal productImagesFacade;

    private Wishlists currentWishlist;
    private List<WishlistItems> wishlistItems;
    private Integer customerID;
    private BigDecimal totalWishlistPrice;
    private String sortBy = "dateAdded"; // dateAdded, price, name

    @Inject
    private CartMB cartMB;

    @Inject
    private AuthController auth;

    public WishlistMB() {
    }

    @PostConstruct
    public void init() {
        try {
            // Just checking if we can load initial state
            loadWishlist();
        } catch (Exception e) {
            System.err.println("Error in WishlistMB init: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called before rendering wishlist page
     */
    public void onPreRender(PreRenderViewEvent event) {
        try {
            if (!FacesContext.getCurrentInstance().isPostback()) {
                loadWishlist();
            }
        } catch (Exception e) {
            System.err.println("Error in WishlistMB onPreRender: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tải wishlist của khách hàng hiện tại
     */
    public void loadWishlist() {
        System.out.println("DEBUG: loadWishlist called");
        try {
            if (auth == null) {
                System.out.println("DEBUG: Auth bean is null");
                return;
            }

            Customers customer = auth.getCurrentCustomer();
            System.out.println("DEBUG: Current customer: " + (customer != null ? customer.getCustomerID() : "null"));

            if (customer != null) {
                customerID = customer.getCustomerID();
                // Ensure facade is injected
                if (wishlistsFacade != null) {
                    currentWishlist = wishlistsFacade.findByCustomerID(customerID);
                    System.out.println("DEBUG: Found wishlist: "
                            + (currentWishlist != null ? currentWishlist.getWishlistID() : "null"));

                    if (currentWishlist == null) {
                        System.out.println("DEBUG: Creating new wishlist");
                        createNewWishlist();
                    } else {
                        System.out.println("DEBUG: Loading wishlist items");
                        loadWishlistItems();
                    }
                } else {
                    System.err.println("DEBUG: wishlistsFacade is null");
                }
            } else {
                // Reset if logged out
                System.out.println("DEBUG: Customer is null, resetting");
                customerID = null;
                currentWishlist = null;
                wishlistItems = null;
                totalWishlistPrice = BigDecimal.ZERO;
            }
        } catch (Exception e) {
            System.err.println("Error in loadWishlist: " + e.getMessage());
            e.printStackTrace();
            showMessage(FacesMessage.SEVERITY_ERROR, "Load Error", e.getMessage());
        }
    }

    /**
     * Tạo wishlist mới cho khách hàng
     */
    private void createNewWishlist() {
        try {
            System.out.println("createNewWishlist - customerID: " + customerID);
            Customers customer = customersFacade.find(customerID);
            System.out.println(
                    "createNewWishlist - Found customer: " + (customer != null ? customer.getCustomerID() : "null"));
            if (customer != null) {
                currentWishlist = new Wishlists();
                currentWishlist.setCustomerID(customer);
                currentWishlist.setName("My Wishlist");
                currentWishlist.setIsDefault(true);
                currentWishlist.setCreatedAt(new Date());
                currentWishlist.setUpdatedAt(new Date());
                wishlistsFacade.create(currentWishlist);
                System.out.println("createNewWishlist - Created wishlist with ID: " + currentWishlist.getWishlistID());
                loadWishlistItems();
            } else {
                System.out.println("createNewWishlist - Customer not found");
            }
        } catch (Exception e) {
            System.err.println("createNewWishlist - Error: " + e.getMessage());
            e.printStackTrace();
            showMessage(FacesMessage.SEVERITY_ERROR, "Lỗi", "Không thể tạo wishlist: " + e.getMessage());
        }
    }

    /**
     * Tải các sản phẩm trong wishlist
     */
    public void loadWishlistItems() {
        System.out.println("===== loadWishlistItems START =====");
        try {
            System.out.println("CurrentWishlist: " + currentWishlist);
            if (currentWishlist != null) {
                System.out.println("Wishlist ID: " + currentWishlist.getWishlistID());
                wishlistItems = wishlistItemsFacade.findByWishlistID(currentWishlist.getWishlistID());
                System.out
                        .println("Retrieved wishlistItems: " + (wishlistItems != null ? wishlistItems.size() : "null"));

                if (wishlistItems == null) {
                    System.out.println("WishlistItems is null, creating empty ArrayList");
                    wishlistItems = new ArrayList<>();
                } else {
                    // Ensure product images are loaded for each item
                    for (WishlistItems item : wishlistItems) {
                        System.out.println("  - Item: " + item.getWishlistItemID() + ", ProductID: "
                                + (item.getProductID() != null ? item.getProductID().getProductID() : "null"));
                        
                        // Load product images if not already loaded
                        if (item.getProductID() != null && 
                            (item.getProductID().getProductImagesList() == null || 
                             item.getProductID().getProductImagesList().isEmpty())) {
                            try {
                                List<entityclass.ProductImages> images = productImagesFacade.findByProductID(item.getProductID());
                                if (images != null && !images.isEmpty()) {
                                    item.getProductID().setProductImagesList(images);
                                }
                            } catch (Exception e) {
                                System.err.println("Error loading product images for product " + item.getProductID().getProductID() + ": " + e.getMessage());
                            }
                        }
                    }
                }

                // Backup for search
                this.allWishlistItems = new ArrayList<>(wishlistItems);

                calculateTotalPrice();
                sortWishlist();
            } else {
                System.out.println("CurrentWishlist is null");
            }
            System.out.println("===== loadWishlistItems END =====");
        } catch (Exception e) {
            System.err.println("Error in loadWishlistItems: " + e.getMessage());
            e.printStackTrace();
            showMessage(FacesMessage.SEVERITY_ERROR, "Items Error", e.getMessage());
        }
    }

    /**
     * Ensure customerID is set from session
     */
    private void ensureCustomerID() {
        if (auth != null && auth.getCurrentCustomer() != null) {
            customerID = auth.getCurrentCustomer().getCustomerID();
        }
    }

    /**
     * Reinitialize wishlist after login (called from AuthController)
     */
    /**
     * Reinitialize wishlist after login (called from AuthController)
     */
    public void reinitialize() {
        loadWishlist();
    }

    /**
     * Initialize session with specific customer (explicit pass from AuthController)
     */
    public void initSession(Customers customer) {
        if (customer != null) {
            this.customerID = customer.getCustomerID();
            System.out.println("DEBUG: initSession with customerID: " + customerID);

            // Execute load logic directly without relying on auth bean callback immediately
            // to ensure state is set correctly
            try {
                if (wishlistsFacade != null) {
                    currentWishlist = wishlistsFacade.findByCustomerID(customerID);
                    System.out.println("DEBUG: initSession found wishlist: "
                            + (currentWishlist != null ? currentWishlist.getWishlistID() : "null"));

                    if (currentWishlist == null) {
                        createNewWishlist();
                    } else {
                        loadWishlistItems();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in initSession: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Thêm sản phẩm vào wishlist
     */
    public void addToWishlist(Integer productID) {
        try {
            // Ensure customerID is set
            ensureCustomerID();

            if (customerID == null) {
                showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please log in to use wishlist");
                return;
            }

            if (currentWishlist == null) {
                loadWishlist();
            }

            // Check if product is already in wishlist
            WishlistItems existingItem = wishlistItemsFacade.findByWishlistIDAndProductID(
                    currentWishlist.getWishlistID(), productID);

            if (existingItem != null) {
                showMessage(FacesMessage.SEVERITY_WARN, "Notice", "This product is already in your wishlist");
                return;
            }

            Products product = productsFacade.find(productID);
            if (product == null) {
                showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Product does not exist");
                return;
            }

            WishlistItems wishlistItem = new WishlistItems();
            wishlistItem.setWishlistID(currentWishlist);
            wishlistItem.setProductID(product);
            wishlistItem.setQuantity(1); // Default quantity
            wishlistItem.setNote(null); // No note by default
            wishlistItem.setAddedAt(new Date());

            wishlistItemsFacade.create(wishlistItem);
            // Reload to refresh state
            currentWishlist = wishlistsFacade.findByCustomerID(customerID);
            loadWishlistItems();

            showMessage(FacesMessage.SEVERITY_INFO, "Success",
                    "Added \"" + product.getProductName() + "\" to wishlist");
        } catch (Exception e) {
            showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot add product: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Xóa sản phẩm khỏi wishlist
     */
    public void removeFromWishlist(Integer productID) {
        try {
            // Ensure customerID is set
            ensureCustomerID();

            if (currentWishlist != null) {
                Products product = productsFacade.find(productID);
                boolean success = wishlistItemsFacade.deleteItemFromWishlist(
                        currentWishlist.getWishlistID(), productID);

                if (success) {
                    // Reload to refresh state
                    currentWishlist = wishlistsFacade.findByCustomerID(customerID);
                    loadWishlistItems();
                    showMessage(FacesMessage.SEVERITY_INFO, "Success",
                            "Removed \"" + (product != null ? product.getProductName() : "product")
                                    + "\" from wishlist");
                } else {
                    showMessage(FacesMessage.SEVERITY_WARN, "Notice", "Product is not in your wishlist");
                }
            }
        } catch (Exception e) {
            showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot remove product: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Kiểm tra sản phẩm có trong wishlist không
     */
    public boolean isProductInWishlist(Integer productID) {
        // Ensure wishlist is loaded
        if (customerID == null) {
            ensureCustomerID();
        }
        if (customerID != null && currentWishlist == null) {
            loadWishlist();
        }
        if (currentWishlist == null) {
            return false;
        }
        return wishlistItemsFacade.findByWishlistIDAndProductID(
                currentWishlist.getWishlistID(), productID) != null;
    }

    /**
     * Xóa tất cả sản phẩm khỏi wishlist
     */
    public void clearWishlist() {
        try {
            if (currentWishlist != null && wishlistItems != null) {
                for (WishlistItems item : wishlistItems) {
                    wishlistItemsFacade.remove(item);
                }
                loadWishlistItems();
                showMessage(FacesMessage.SEVERITY_INFO, "Success", "Cleared all products from wishlist");
            }
        } catch (Exception e) {
            showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot clear wishlist: " + e.getMessage());
        }
    }

    /**
     * Lấy số lượng sản phẩm trong wishlist
     */
    public int getWishlistItemCount() {
        // Ensure wishlist items are loaded
        if (customerID == null) {
            ensureCustomerID();
        }
        if (customerID != null && currentWishlist == null) {
            loadWishlist();
        }
        return wishlistItems != null ? wishlistItems.size() : 0;
    }

    /**
     * Thêm toàn bộ wishlist vào giỏ hàng
     */
    public void addWishlistToCart() {
        try {
            if (wishlistItems == null || wishlistItems.isEmpty()) {
                showMessage(FacesMessage.SEVERITY_WARN, "Notice", "Your wishlist is empty");
                return;
            }

            int addedCount = 0;
            int failedCount = 0;

            for (WishlistItems item : wishlistItems) {
                if (item.getProductID().getStockQuantity() > 0) {
                    try {
                        if (cartMB != null) {
                            cartMB.addToCart(item.getProductID(), 1);
                            addedCount++;
                        }
                    } catch (Exception e) {
                        failedCount++;
                    }
                } else {
                    failedCount++;
                }
            }

            String message = "Added " + addedCount + " products to cart";
            if (failedCount > 0) {
                message += " (" + failedCount + " out of stock)";
            }
            showMessage(FacesMessage.SEVERITY_INFO, "Success", message);
        } catch (Exception e) {
            showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot add to cart: " + e.getMessage());
        }
    }

    /**
     * Tính tổng giá tiền wishlist
     */
    private void calculateTotalPrice() {
        totalWishlistPrice = BigDecimal.ZERO;
        if (wishlistItems != null && !wishlistItems.isEmpty()) {
            for (WishlistItems item : wishlistItems) {
                Object priceObj = item.getProductID().getUnitPrice();
                if (priceObj != null) {
                    BigDecimal price;
                    if (priceObj instanceof BigDecimal) {
                        price = (BigDecimal) priceObj;
                    } else if (priceObj instanceof Double) {
                        price = BigDecimal.valueOf((Double) priceObj);
                    } else if (priceObj instanceof Integer) {
                        price = BigDecimal.valueOf((Integer) priceObj);
                    } else {
                        price = new BigDecimal(priceObj.toString());
                    }
                    totalWishlistPrice = totalWishlistPrice.add(price);
                }
            }
        }
    }

    /**
     * Sắp xếp wishlist items
     */
    public void sortWishlist() {
        if (wishlistItems == null || wishlistItems.isEmpty()) {
            return;
        }

        switch (sortBy) {
            case "price":
                Collections.sort(wishlistItems, new Comparator<WishlistItems>() {
                    @Override
                    public int compare(WishlistItems a, WishlistItems b) {
                        Double priceA = convertToDouble(a.getProductID().getUnitPrice());
                        Double priceB = convertToDouble(b.getProductID().getUnitPrice());
                        return Double.compare(priceA, priceB);
                    }
                });
                break;
            case "priceDesc":
                Collections.sort(wishlistItems, new Comparator<WishlistItems>() {
                    @Override
                    public int compare(WishlistItems a, WishlistItems b) {
                        Double priceA = convertToDouble(a.getProductID().getUnitPrice());
                        Double priceB = convertToDouble(b.getProductID().getUnitPrice());
                        return Double.compare(priceB, priceA);
                    }
                });
                break;
            case "name":
                Collections.sort(wishlistItems, new Comparator<WishlistItems>() {
                    @Override
                    public int compare(WishlistItems a, WishlistItems b) {
                        return a.getProductID().getProductName()
                                .compareTo(b.getProductID().getProductName());
                    }
                });
                break;
            case "dateAdded":
            default:
                Collections.sort(wishlistItems, new Comparator<WishlistItems>() {
                    @Override
                    public int compare(WishlistItems a, WishlistItems b) {
                        return b.getAddedAt().compareTo(a.getAddedAt());
                    }
                });
                break;
        }
    }

    /**
     * Lấy số sản phẩm còn hàng trong wishlist
     */
    public int getAvailableItemCount() {
        int count = 0;
        if (wishlistItems != null) {
            for (WishlistItems item : wishlistItems) {
                if (item.getProductID().getStockQuantity() > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Kiểm tra xem có sản phẩm nào hết hàng không
     */
    public boolean hasOutOfStockItems() {
        if (wishlistItems == null) {
            return false;
        }
        for (WishlistItems item : wishlistItems) {
            if (item.getProductID().getStockQuantity() <= 0) {
                return true;
            }
        }
        return false;
    }

    // Getters and Setters
    public Wishlists getCurrentWishlist() {
        return currentWishlist;
    }

    public void setCurrentWishlist(Wishlists currentWishlist) {
        this.currentWishlist = currentWishlist;
    }

    public List<WishlistItems> getWishlistItems() {
        // Ensure customer is logged in and wishlist is loaded
        if (customerID == null) {
            ensureCustomerID();
        }
        if (customerID != null && currentWishlist == null) {
            loadWishlist();
        }
        return wishlistItems;
    }

    public void setWishlistItems(List<WishlistItems> wishlistItems) {
        this.wishlistItems = wishlistItems;
    }

    public Integer getCustomerID() {
        return customerID;
    }

    public void setCustomerID(Integer customerID) {
        this.customerID = customerID;
    }

    public BigDecimal getTotalWishlistPrice() {
        // Ensure wishlist items are loaded before calculating
        if (customerID == null) {
            ensureCustomerID();
        }
        if (customerID != null && currentWishlist == null) {
            loadWishlist();
        }
        if (totalWishlistPrice == null) {
            calculateTotalPrice();
        }
        return totalWishlistPrice;
    }

    public void setTotalWishlistPrice(BigDecimal totalWishlistPrice) {
        this.totalWishlistPrice = totalWishlistPrice;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * Get product image URL for display
     */
    public String getProductImageUrl(Products product) {
        if (product == null) {
            return "https://via.placeholder.com/300x300?text=No+Product";
        }

        try {
            List<ProductImages> images = productImagesFacade.findByProductID(product);
            if (images != null && !images.isEmpty()) {
                String imageUrl = images.get(0).getImageURL();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    return imageUrl;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting product image: " + e.getMessage());
        }

        // Return a default image URL
        return "https://via.placeholder.com/300x300?text=No+Image";
    }

    /**
     * Hiển thị thông báo FacesMessage
     */
    private void showMessage(FacesMessage.Severity severity, String summary, String detail) {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(severity, summary, detail));
            }
        } catch (Exception e) {
            System.err.println("Error showing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Search functionality
    private String searchKeyword;
    private List<WishlistItems> allWishlistItems;

    public void searchWishlist() {
        if (allWishlistItems == null) {
            // If allItems not yet backed up (e.g. first load), back it up
            if (this.wishlistItems != null) {
                this.allWishlistItems = new ArrayList<>(this.wishlistItems);
            } else {
                return;
            }
        }

        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            // Restore full list
            if (this.allWishlistItems != null) {
                this.wishlistItems = new ArrayList<>(this.allWishlistItems);
            }
        } else {
            // Filter
            String keyword = searchKeyword.toLowerCase().trim();
            this.wishlistItems = new ArrayList<>();
            for (WishlistItems item : allWishlistItems) {
                if (item.getProductID().getProductName().toLowerCase().contains(keyword)) {
                    this.wishlistItems.add(item);
                }
            }
        }
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    /**
     * Get recommended products (random 4 products)
     */
    public List<Products> getRecommendedProducts() {
        try {
            if (productsFacade != null) {
                // Get all active products
                List<Products> allProducts = productsFacade.findAll();
                if (allProducts != null && !allProducts.isEmpty()) {
                    // Shuffle to randomize
                    Collections.shuffle(allProducts);
                    // Return first 4
                    return allProducts.subList(0, Math.min(allProducts.size(), 4));
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting recommendations: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Chuyển đổi Object price thành Double
     */
    private Double convertToDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
