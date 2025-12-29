package controllers;

import entityclass.Brands;
import entityclass.Categories;
import entityclass.ProductImages;
import entityclass.Products;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import sessionbeans.BrandsFacadeLocal;
import sessionbeans.CategoriesFacadeLocal;
import sessionbeans.ProductImagesFacadeLocal;
import sessionbeans.ProductsFacadeLocal;
import sessionbeans.CartsFacadeLocal;
import sessionbeans.CartItemsFacadeLocal;
import entityclass.Carts;
import entityclass.CartItems;
import jakarta.inject.Inject;

@Named("productMB")
@ViewScoped
public class ProductMB implements Serializable {

    @EJB
    private ProductsFacadeLocal productsFacade;

    @EJB
    private CategoriesFacadeLocal categoriesFacade;

    @EJB
    private BrandsFacadeLocal brandsFacade;

    @EJB
    private ProductImagesFacadeLocal productImagesFacade;

    @EJB
    private CartsFacadeLocal cartsFacade;

    @EJB
    private CartItemsFacadeLocal cartItemsFacade;

    @Inject
    private AuthController auth;

    private List<Products> products;
    private List<Categories> categories;
    private List<Brands> brands;
    private Products selectedProduct;
    private Products newProduct;
    private String searchTerm;
    private Part uploadedFile;
    private List<ProductImages> productImages;
    private Integer quantity = 1;

    @PostConstruct
    public void init() {
        loadProducts();
        loadCategories();
        loadBrands();
        newProduct = new Products();
        productImages = new ArrayList<>();
    }

    public void loadProducts() {
        products = productsFacade.findAll();
    }

    public void loadCategories() {
        categories = categoriesFacade.findAll();
    }

    public void loadBrands() {
        brands = brandsFacade.findAll();
    }

    public void createProduct() {
        try {
            newProduct.setCreatedAt(new java.util.Date());
            productsFacade.create(newProduct);

            // Handle image upload
            if (uploadedFile != null) {
                String imageUrl = saveUploadedFile(uploadedFile);
                ProductImages productImage = new ProductImages();
                productImage.setProductID(newProduct);
                productImage.setImageURL(imageUrl);
                productImagesFacade.create(productImage);
            }

            loadProducts();
            newProduct = new Products();
            uploadedFile = null;
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Product created successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to create product"));
        }
    }

    public void updateProduct() {
        try {
            productsFacade.edit(selectedProduct);

            // Handle image upload if a file is selected
            if (uploadedFile != null) {
                String imageUrl = saveUploadedFile(uploadedFile);
                ProductImages productImage = new ProductImages();
                productImage.setProductID(selectedProduct);
                productImage.setImageURL(imageUrl);
                productImagesFacade.create(productImage);
                productImages.add(productImage);
                uploadedFile = null;
            }

            loadProducts();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Product updated successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to update product"));
        }
    }

    public void deleteProduct(Products product) {
        try {
            productsFacade.remove(product);
            loadProducts();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Product deleted successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to delete product"));
        }
    }

    public void prepareEdit(Products product) {
        selectedProduct = product;
        // Load product images
        productImages = productImagesFacade.findByProductID(product);
    }

    public void loadProductById() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();
        String productIdParam = params.get("productId");
        if (productIdParam != null && !productIdParam.isEmpty()) {
            try {
                Integer productId = Integer.valueOf(productIdParam);
                selectedProduct = productsFacade.find(productId);
                if (selectedProduct != null) {
                    // Load product images
                    productImages = productImagesFacade.findByProductID(selectedProduct);
                }
            } catch (NumberFormatException e) {
                // Handle invalid productId
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid product ID"));
            }
        }
    }

    public void searchProducts() {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            products = productsFacade.findByProductName(searchTerm);
        } else {
            loadProducts();
        }
    }

    private String saveUploadedFile(Part file) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_" + file.getSubmittedFileName();
        Path uploadPath = Paths.get(System.getProperty("user.home"), "uploads", "products");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        try (InputStream input = file.getInputStream()) {
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/products/" + fileName;
        }
    }

    public void addProductImage() {
        if (uploadedFile != null && selectedProduct != null) {
            try {
                String imageUrl = saveUploadedFile(uploadedFile);
                ProductImages productImage = new ProductImages();
                productImage.setProductID(selectedProduct);
                productImage.setImageURL(imageUrl);
                productImagesFacade.create(productImage);
                productImages.add(productImage);
                uploadedFile = null;
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Image added successfully"));
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to add image"));
            }
        }
    }

    public void removeProductImage(ProductImages image) {
        try {
            productImagesFacade.remove(image);
            productImages.remove(image);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Image removed successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to remove image"));
        }
    }

    // Getters and Setters
    public List<Products> getProducts() {
        return products;
    }

    public void setProducts(List<Products> products) {
        this.products = products;
    }

    public List<Categories> getCategories() {
        return categories;
    }

    public void setCategories(List<Categories> categories) {
        this.categories = categories;
    }

    public List<Brands> getBrands() {
        return brands;
    }

    public void setBrands(List<Brands> brands) {
        this.brands = brands;
    }

    public Products getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Products selectedProduct) {
        this.selectedProduct = selectedProduct;
    }

    public Products getNewProduct() {
        return newProduct;
    }

    public void setNewProduct(Products newProduct) {
        this.newProduct = newProduct;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Part getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(Part uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public List<ProductImages> getProductImages() {
        return productImages;
    }

    public void setProductImages(List<ProductImages> productImages) {
        this.productImages = productImages;
    }

    public String productImageUrl(Products product) {
        List<ProductImages> images = productImagesFacade.findByProductID(product);
        if (images != null && !images.isEmpty()) {
            return images.get(0).getImageURL();
        }
        return null; // No image available
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void increaseQuantity() {
        if (selectedProduct != null && quantity < selectedProduct.getStockQuantity()) {
            quantity++;
        }
    }

    public void decreaseQuantity() {
        if (quantity > 1) {
            quantity--;
        }
    }

    public BigDecimal getDiscountedPrice(Products product) {
        if (product.getDiscountPercent() != null && product.getDiscountPercent() > 0) {
            BigDecimal discountAmount = product.getUnitPrice().multiply(BigDecimal.valueOf(product.getDiscountPercent()).divide(BigDecimal.valueOf(100)));
            return product.getUnitPrice().subtract(discountAmount);
        }
        return product.getUnitPrice();
    }

    public boolean hasDiscount(Products product) {
        return product.getDiscountPercent() != null && product.getDiscountPercent() > 0;
    }

    public void addToCart() {
        FacesContext fc = FacesContext.getCurrentInstance();

        // Check if user is logged in
        if (!auth.isLoggedIn() || auth.getCurrentCustomer() == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please log in to add items to cart"));
            return;
        }

        try {
            // Get current customer
            entityclass.Customers customer = auth.getCurrentCustomer();

            // Find existing cart for customer or create new one
            List<Carts> customerCarts = cartsFacade.findByCustomerID(customer.getCustomerID());
            Carts cart = null;
            if (!customerCarts.isEmpty()) {
                cart = customerCarts.get(0); // Assuming one cart per customer
            }

            if (cart == null) {
                // Create new cart
                cart = new Carts();
                cart.setCustomerID(customer);
                cart.setCreatedAt(new java.util.Date());
                cartsFacade.create(cart);
            }

            // Check if product is already in cart
            CartItems existingItem = null;
            List<CartItems> cartItems = cart.getCartItemsList();
            if (cartItems != null) {
                for (CartItems item : cartItems) {
                    if (item.getProductID().getProductID().equals(selectedProduct.getProductID())) {
                        existingItem = item;
                        break;
                    }
                }
            }

            if (existingItem != null) {
                // Update quantity
                int newQuantity = existingItem.getQuantity() + quantity;
                if (newQuantity > selectedProduct.getStockQuantity()) {
                    fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Not enough stock available"));
                    return;
                }
                existingItem.setQuantity(newQuantity);
                cartItemsFacade.edit(existingItem);
            } else {
                // Add new item
                if (quantity > selectedProduct.getStockQuantity()) {
                    fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Not enough stock available"));
                    return;
                }
                CartItems newItem = new CartItems();
                newItem.setCartID(cart);
                newItem.setProductID(selectedProduct);
                newItem.setQuantity(quantity);
                newItem.setUnitPrice(getDiscountedPrice(selectedProduct)); // Use discounted price
                cartItemsFacade.create(newItem);
            }

            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Product added to cart"));
        } catch (Exception e) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to add product to cart"));
            e.printStackTrace();
        }
    }
}
