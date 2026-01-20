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
import sessionbeans.OffersFacadeLocal;
import sessionbeans.ProductOffersFacadeLocal;
import entityclass.Offers;
import entityclass.ProductOffers;
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

    @EJB
    private OffersFacadeLocal offersFacade;

    @EJB
    private ProductOffersFacadeLocal productOffersFacade;

    @Inject
    private AuthController auth;

    @Inject
    private CartMB cartMB;

    private List<Products> products;
    private List<Categories> categories;
    private List<Brands> brands;
    private Products selectedProduct;
    private Products newProduct;
    private String searchTerm;
    private Part uploadedFile;
    private List<ProductImages> productImages;
    private Integer quantity = 1;
    private int selectedImageIndex = 0;
    private String selectedProductImageUrl;

    // Filter properties
    private Integer selectedCategoryId;
    private Integer selectedBrandId;
    private java.math.BigDecimal minPrice;
    private java.math.BigDecimal maxPrice;
    private List<Integer> selectedBrandIds;
    private List<Integer> selectedCategoryIds;
    private List<jakarta.faces.model.SelectItem> brandItems;
    private List<jakarta.faces.model.SelectItem> categoryItems;
    private List<jakarta.faces.model.SelectItem> sortItems;

    // Pagination properties
    private int currentPage = 1;
    private int pageSize = 24; // Changed to 24 to match the UI
    private int totalRecords;
    private int totalPages;
    private List<Products> paginatedProducts;

    // Sorting properties
    private String sortBy = "name"; // default sort
    private String sortOrder = "asc"; // asc or desc
    private String selectedSort = "name-asc";

    @PostConstruct
    public void init() {
        loadProducts();
        loadCategories();
        loadBrands();
        newProduct = new Products();
        productImages = new ArrayList<>();
        selectedBrandIds = new ArrayList<>();
        selectedCategoryIds = new ArrayList<>();
        loadBrandItems();
        loadCategoryItems();
        loadSortItems();

        // Check for search parameter in URL
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();
        String searchParam = params.get("search");
        String categoryParam = params.get("category");
        if (searchParam != null && !searchParam.trim().isEmpty()) {
            searchTerm = searchParam.trim();
        }
        if (categoryParam != null && !categoryParam.trim().isEmpty()) {
            try {
                Integer categoryId = Integer.valueOf(categoryParam.trim());
                selectedCategoryIds = new ArrayList<>();
                selectedCategoryIds.add(categoryId);
            } catch (NumberFormatException e) {
                // Invalid category ID, ignore
            }
        }
        if (searchParam != null || categoryParam != null) {
            applyFilters();
        } else {
            updatePagination();
        }
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
        // Validation
        if (newProduct.getProductName() == null || newProduct.getProductName().trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Product name is required"));
            return;
        }
        if (newProduct.getProductName().length() < 2) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Product name must be at least 2 characters"));
            return;
        }
        if (newProduct.getProductName().length() > 100) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Product name must not exceed 100 characters"));
            return;
        }
        // Check for duplicate product name
        List<Products> existingProducts = productsFacade.findByProductName(newProduct.getProductName());
        if (!existingProducts.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Product name already exists"));
            return;
        }

        if (newProduct.getUnitPrice() == null || newProduct.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Unit price must be greater than 0"));
            return;
        }

        if (newProduct.getStockQuantity() == null || newProduct.getStockQuantity() < 0) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Stock quantity must be 0 or greater"));
            return;
        }

        if (newProduct.getCategoryID() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Category is required"));
            return;
        }

        if (newProduct.getBrandID() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Brand is required"));
            return;
        }

        try {
            newProduct.setCreatedAt(new java.util.Date());
            newProduct.setStatus("Active");
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
            updatePagination();
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
        // Validation
        if (selectedProduct.getProductName() == null || selectedProduct.getProductName().trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Product name is required"));
            return;
        }
        if (selectedProduct.getProductName().length() < 2) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Product name must be at least 2 characters"));
            return;
        }
        if (selectedProduct.getProductName().length() > 100) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Product name must not exceed 100 characters"));
            return;
        }
        // Check for duplicate product name (excluding current product)
        List<Products> existingProducts = productsFacade.findByProductName(selectedProduct.getProductName());
        if (existingProducts.size() > 0 && !existingProducts.get(0).getProductID().equals(selectedProduct.getProductID())) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Product name already exists"));
            return;
        }

        if (selectedProduct.getUnitPrice() == null || selectedProduct.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Unit price must be greater than 0"));
            return;
        }

        if (selectedProduct.getStockQuantity() == null || selectedProduct.getStockQuantity() < 0) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Stock quantity must be 0 or greater"));
            return;
        }

        if (selectedProduct.getCategoryID() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Category is required"));
            return;
        }

        if (selectedProduct.getBrandID() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Brand is required"));
            return;
        }

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
                    // Set default selected image
                    if (productImages != null && !productImages.isEmpty()) {
                        selectedProductImageUrl = productImages.get(0).getImageURL();
                        selectedImageIndex = 0;
                    } else {
                        selectedProductImageUrl = null;
                        selectedImageIndex = 0;
                    }
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
        sortProducts();
        // Reset pagination after search
        currentPage = 1;
        updatePagination();
    }

    public void applyFilters() {
        products = productsFacade.findFiltered(searchTerm, selectedCategoryIds, selectedBrandIds, minPrice, maxPrice);
        sortProducts();
        // Reset pagination after applying filters
        currentPage = 1;
        updatePagination();
    }

    public void updateSelectedBrands(Integer brandId) {
        if (selectedBrandIds.contains(brandId)) {
            selectedBrandIds.remove(brandId);
        } else {
            selectedBrandIds.add(brandId);
        }
    }

    public void loadBrandItems() {
        brandItems = new ArrayList<>();
        for (Brands brand : brands) {
            brandItems.add(new jakarta.faces.model.SelectItem(brand.getBrandID(), brand.getBrandName()));
        }
    }

    public void loadCategoryItems() {
        categoryItems = new ArrayList<>();
        for (Categories category : categories) {
            categoryItems.add(new jakarta.faces.model.SelectItem(category.getCategoryID(), category.getCategoryName()));
        }
    }

    public void loadSortItems() {
        sortItems = new ArrayList<>();
        sortItems.add(new jakarta.faces.model.SelectItem("name-asc", "Name: A to Z"));
        sortItems.add(new jakarta.faces.model.SelectItem("name-desc", "Name: Z to A"));
        sortItems.add(new jakarta.faces.model.SelectItem("price-asc", "Price: Low to High"));
        sortItems.add(new jakarta.faces.model.SelectItem("price-desc", "Price: High to Low"));
        sortItems.add(new jakarta.faces.model.SelectItem("date-asc", "Date: Oldest First"));
        sortItems.add(new jakarta.faces.model.SelectItem("date-desc", "Date: Newest First"));
    }

    public void updateSelectedCategories(Integer categoryId) {
        if (selectedCategoryIds.contains(categoryId)) {
            selectedCategoryIds.remove(categoryId);
        } else {
            selectedCategoryIds.add(categoryId);
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

    private void updatePagination() {
        if (products == null) {
            totalRecords = 0;
            totalPages = 0;
            paginatedProducts = new ArrayList<>();
            return;
        }
        totalRecords = products.size();
        totalPages = (totalRecords + pageSize - 1) / pageSize;
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        if (currentPage < 1) {
            currentPage = 1;
        }
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalRecords);
        paginatedProducts = products.subList(startIndex, endIndex);
    }

    public void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }

    public void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
        }
    }

    public void goToPage(int page) {
        if (page >= 1 && page <= totalPages) {
            currentPage = page;
            updatePagination();
        }
    }

    public void sortProducts() {
        if (products == null || products.isEmpty()) {
            return;
        }
        products.sort((p1, p2) -> {
            int result = 0;
            switch (sortBy) {
                case "name":
                    result = p1.getProductName().compareToIgnoreCase(p2.getProductName());
                    break;
                case "price":
                    result = p1.getUnitPrice().compareTo(p2.getUnitPrice());
                    break;
                case "date":
                    result = p1.getCreatedAt().compareTo(p2.getCreatedAt());
                    break;
                default:
                    result = p1.getProductName().compareToIgnoreCase(p2.getProductName());
                    break;
            }
            return "desc".equals(sortOrder) ? -result : result;
        });
    }

    public void changeSort(String sortBy, String sortOrder) {
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        sortProducts();
        currentPage = 1;
        updatePagination();
    }

    public void changeSortCombined(String combined) {
        String[] parts = combined.split("-");
        if (parts.length == 2) {
            this.sortBy = parts[0];
            this.sortOrder = parts[1];
            sortProducts();
            currentPage = 1;
            updatePagination();
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

    public Integer getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(Integer selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }

    public Integer getSelectedBrandId() {
        return selectedBrandId;
    }

    public void setSelectedBrandId(Integer selectedBrandId) {
        this.selectedBrandId = selectedBrandId;
    }

    public java.math.BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(java.math.BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public java.math.BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(java.math.BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public List<Integer> getSelectedBrandIds() {
        return selectedBrandIds;
    }

    public void setSelectedBrandIds(List<Integer> selectedBrandIds) {
        this.selectedBrandIds = selectedBrandIds;
    }

    public List<jakarta.faces.model.SelectItem> getBrandItems() {
        return brandItems;
    }

    public void setBrandItems(List<jakarta.faces.model.SelectItem> brandItems) {
        this.brandItems = brandItems;
    }

    public List<jakarta.faces.model.SelectItem> getCategoryItems() {
        return categoryItems;
    }

    public void setCategoryItems(List<jakarta.faces.model.SelectItem> categoryItems) {
        this.categoryItems = categoryItems;
    }

    public List<jakarta.faces.model.SelectItem> getSortItems() {
        return sortItems;
    }

    public void setSortItems(List<jakarta.faces.model.SelectItem> sortItems) {
        this.sortItems = sortItems;
    }

    public List<Integer> getSelectedCategoryIds() {
        return selectedCategoryIds;
    }

    public void setSelectedCategoryIds(List<Integer> selectedCategoryIds) {
        this.selectedCategoryIds = selectedCategoryIds;
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
   
    public void addToCart() {
        cartMB.addToCart(selectedProduct, quantity);
    }

    public void addProductToCart(Products product) {
        // Check if user is logged in
        if (!auth.isLoggedIn()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please login to add items to cart"));
            return;
        }
        // Check if product is out of stock
        if (isOutOfStock(product)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Product is out of stock"));
            return;
        }
        cartMB.addToCart(product, 1);
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Product added to cart successfully"));
    }

    public void selectProductImage(String imageUrl) {
        this.selectedProductImageUrl = imageUrl;
        // Find the index of the selected image
        if (productImages != null) {
            for (int i = 0; i < productImages.size(); i++) {
                if (productImages.get(i).getImageURL().equals(imageUrl)) {
                    selectedImageIndex = i;
                    break;
                }
            }
        }
    }

    public int getSelectedImageIndex() {
        return selectedImageIndex;
    }

    public void setSelectedImageIndex(int selectedImageIndex) {
        this.selectedImageIndex = selectedImageIndex;
    }

    public String getSelectedProductImageUrl() {
        return selectedProductImageUrl;
    }

    public void setSelectedProductImageUrl(String selectedProductImageUrl) {
        this.selectedProductImageUrl = selectedProductImageUrl;
    }

    // Pagination getters and setters
    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<Products> getPaginatedProducts() {
        return paginatedProducts;
    }

    public void setPaginatedProducts(List<Products> paginatedProducts) {
        this.paginatedProducts = paginatedProducts;
    }

    public List<Integer> getPageNumbers() {
        List<Integer> pageNumbers = new ArrayList<>();
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, currentPage + 2);
        for (int i = startPage; i <= endPage; i++) {
            pageNumbers.add(i);
        }
        return pageNumbers;
    }

    // Discount calculation methods
    public boolean hasOffer(Products product) {
        if (product == null) {
            return false;
        }

        List<ProductOffers> productOffers = productOffersFacade.findAll();
        java.util.Date now = new java.util.Date();

        for (ProductOffers po : productOffers) {
            if (po.getProductID() != null && po.getProductID().getProductID().equals(product.getProductID())) {
                Offers offer = po.getOfferID();
                if (offer != null && "Active".equals(offer.getStatus())) {
                    if (offer.getStartDate() != null && offer.getEndDate() != null) {
                        if (now.after(offer.getStartDate()) && now.before(offer.getEndDate())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public Offers getProductOffer(Products product) {
        if (product == null) {
            return null;
        }

        List<ProductOffers> productOffers = productOffersFacade.findAll();
        java.util.Date now = new java.util.Date();

        for (ProductOffers po : productOffers) {
            if (po.getProductID() != null && po.getProductID().getProductID().equals(product.getProductID())) {
                Offers offer = po.getOfferID();
                if (offer != null && "Active".equals(offer.getStatus())) {
                    if (offer.getStartDate() != null && offer.getEndDate() != null) {
                        if (now.after(offer.getStartDate()) && now.before(offer.getEndDate())) {
                            return offer;
                        }
                    }
                }
            }
        }
        return null;
    }

    public BigDecimal getDiscountedPrice(Products product) {
        if (product == null || product.getUnitPrice() == null) {
            return BigDecimal.ZERO;
        }

        Offers offer = getProductOffer(product);
        if (offer == null) {
            return product.getUnitPrice();
        }

        if ("Percentage".equals(offer.getOfferType())) {
            if (offer.getDiscountValue() != null) {
                BigDecimal discountValue = BigDecimal.valueOf(offer.getDiscountValue());
                BigDecimal discount = product.getUnitPrice().multiply(discountValue.divide(BigDecimal.valueOf(100)));
                return product.getUnitPrice().subtract(discount);
            }
        } else if ("Fixed Amount".equals(offer.getOfferType())) {
            if (offer.getDiscountValue() != null) {
                BigDecimal discountValue = BigDecimal.valueOf(offer.getDiscountValue());
                return product.getUnitPrice().subtract(discountValue);
            }
        }

        return product.getUnitPrice();
    }

    public String getDiscountDisplay(Products product) {
        Offers offer = getProductOffer(product);
        if (offer == null) {
            return "";
        }

        if ("Percentage".equals(offer.getOfferType())) {
            return offer.getDiscountValue() + "% OFF";
        } else if ("Fixed Amount".equals(offer.getOfferType())) {
            return "$" + offer.getDiscountValue() + " OFF";
        }

        return "";
    }

    // Stock management methods
    public boolean isLowStock(Products product) {
        if (product == null || product.getStockQuantity() == null) {
            return false;
        }
        return product.getStockQuantity() <= 5;
    }

    public boolean isOutOfStock(Products product) {
        if (product == null || product.getStockQuantity() == null) {
            return false;
        }
        return product.getStockQuantity() == 0;
    }

    public String getStockStatus(Products product) {
        if (isOutOfStock(product)) {
            return "Out of Stock";
        } else if (isLowStock(product)) {
            return "Low Stock";
        } else {
            return "In Stock";
        }
    }

    public int getLowStockCount() {
        if (products == null) {
            return 0;
        }
        return (int) products.stream().filter(this::isLowStock).count();
    }

    public List<Products> getLowStockProducts() {
        if (products == null) {
            return new ArrayList<>();
        }
        return products.stream().filter(this::isLowStock).toList();
    }

    // Notification dropdown properties
    private boolean showNotificationDropdown = false;

    public void toggleNotificationDropdown() {
        showNotificationDropdown = !showNotificationDropdown;
    }

    public boolean isShowNotificationDropdown() {
        return showNotificationDropdown;
    }

    public void setShowNotificationDropdown(boolean showNotificationDropdown) {
        this.showNotificationDropdown = showNotificationDropdown;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getSelectedSort() {
        return selectedSort;
    }

    public void setSelectedSort(String selectedSort) {
        this.selectedSort = selectedSort;
        changeSortCombined(selectedSort);
    }
}
