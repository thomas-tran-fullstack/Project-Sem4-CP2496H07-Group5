package controllers;

import entityclass.Categories;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import sessionbeans.CategoriesFacadeLocal;
import sessionbeans.ProductsFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@Named(value = "categoryMB")
@ViewScoped
public class CategoryManagementMB implements Serializable {

    @EJB
    private CategoriesFacadeLocal categoriesFacade;

    @EJB
    private ProductsFacadeLocal productsFacade;

    private List<Categories> categoriesList;

    private Categories selectedCategory;

    private Categories newCategory;

    private boolean editCategoryMode = false;

    private String searchTerm;

    private Integer categoryId;

    private Part uploadedFile;

    // Pagination properties
    private int currentPage = 1;
    private int pageSize = 10;
    private int totalRecords;
    private int totalPages;
    private List<Categories> paginatedCategories;

    @PostConstruct
    public void init() {
        loadCategories();
        newCategory = new Categories();
        updatePagination();
    }

    public void loadCategories() {
        categoriesList = categoriesFacade.findAll();
    }

    // Category CRUD operations
    public String addCategory() {
        // Validation
        if (newCategory.getCategoryName() == null || newCategory.getCategoryName().trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Category name is required"));
            return null;
        }
        if (newCategory.getCategoryName().length() < 2) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Category name must be at least 2 characters"));
            return null;
        }
        if (newCategory.getCategoryName().length() > 50) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Category name must not exceed 50 characters"));
            return null;
        }
        // Check for duplicate category name
        List<Categories> existingCategories = categoriesFacade.findByCategoryName(newCategory.getCategoryName());
        if (!existingCategories.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Category name already exists"));
            return null;
        }

        try {
            // Handle image upload if a file is selected
            if (uploadedFile != null) {
                String imageUrl = saveUploadedFile(uploadedFile);
                newCategory.setImageURL(imageUrl);
            }

            newCategory.setCreatedAt(new Date());
            newCategory.setStatus("Active");

            categoriesFacade.create(newCategory);
            loadCategories();
            updatePagination();
            newCategory = new Categories();
            uploadedFile = null;
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Category added successfully"));
            return "categoriesmanage?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to add category"));
            return null;
        }
    }

    public String editCategory(Categories category) {
        selectedCategory = category;
        editCategoryMode = true;
        return "editcategory?faces-redirect=true";
    }

    public void editCategoryById(Integer categoryId) {
        selectedCategory = categoriesFacade.find(categoryId);
        editCategoryMode = true;
    }

    public void loadCategoryForEdit() {
        if (categoryId != null) {
            selectedCategory = categoriesFacade.find(categoryId);
        }
    }

    public String updateCategory() {
        // Validation
        if (selectedCategory.getCategoryName() == null || selectedCategory.getCategoryName().trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Category name is required"));
            return null;
        }
        if (selectedCategory.getCategoryName().length() < 2) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Category name must be at least 2 characters"));
            return null;
        }
        if (selectedCategory.getCategoryName().length() > 50) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Category name must not exceed 50 characters"));
            return null;
        }
        // Check for duplicate category name (excluding current category)
        List<Categories> existingCategories = categoriesFacade.findByCategoryName(selectedCategory.getCategoryName());
        if (existingCategories.size() > 0 && !existingCategories.get(0).getCategoryID().equals(selectedCategory.getCategoryID())) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Category name already exists"));
            return null;
        }

        try {
            // Handle image upload if a file is selected
            if (uploadedFile != null) {
                String imageUrl = saveUploadedFile(uploadedFile);
                selectedCategory.setImageURL(imageUrl);
            }

            categoriesFacade.edit(selectedCategory);
            loadCategories();
            editCategoryMode = false;
            selectedCategory = null;
            uploadedFile = null;
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Category updated successfully"));
            return "categoriesmanage?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to update category"));
            return null;
        }
    }

    public void deleteCategory(Categories category) {
        // Check if category has associated products
        if (productsFacade.hasProductsByCategory(category.getCategoryID())) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot delete category because it has associated products"));
            return;
        }

        try {
            categoriesFacade.remove(category);
            loadCategories();
            updatePagination();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Category deleted successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to delete category"));
        }
    }

    public void cancelEditCategory() {
        editCategoryMode = false;
        selectedCategory = null;
    }

    // Getters and Setters
    public List<Categories> getCategoriesList() {
        return categoriesList;
    }

    public void setCategoriesList(List<Categories> categoriesList) {
        this.categoriesList = categoriesList;
    }

    public Categories getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(Categories selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public Categories getNewCategory() {
        return newCategory;
    }

    public void setNewCategory(Categories newCategory) {
        this.newCategory = newCategory;
    }

    public boolean isEditCategoryMode() {
        return editCategoryMode;
    }

    public void setEditCategoryMode(boolean editCategoryMode) {
        this.editCategoryMode = editCategoryMode;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public void searchCategories() {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            categoriesList = categoriesFacade.findByCategoryName(searchTerm);
        } else {
            loadCategories();
        }
        // Reset pagination after search
        currentPage = 1;
        updatePagination();
    }

    // Pagination methods
    public void updatePagination() {
        if (categoriesList != null) {
            totalRecords = categoriesList.size();
            totalPages = (int) Math.ceil((double) totalRecords / pageSize);
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }
            if (currentPage < 1) {
                currentPage = 1;
            }
            int startIndex = (currentPage - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalRecords);
            paginatedCategories = categoriesList.subList(startIndex, endIndex);
        } else {
            totalRecords = 0;
            totalPages = 0;
            paginatedCategories = null;
        }
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

    public List<Categories> getPaginatedCategories() {
        return paginatedCategories;
    }

    public void setPaginatedCategories(List<Categories> paginatedCategories) {
        this.paginatedCategories = paginatedCategories;
    }

    private String saveUploadedFile(Part file) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_" + file.getSubmittedFileName();
        // Save to user home directory so images persist across deployments
        Path uploadPath = Paths.get(System.getProperty("user.home"), "uploads", "categories");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        try (InputStream input = file.getInputStream()) {
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
            return fileName; // Return just filename, ImageServlet will handle the path
        }
    }

    // Getters and Setters for uploadedFile
    public Part getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(Part uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

}
