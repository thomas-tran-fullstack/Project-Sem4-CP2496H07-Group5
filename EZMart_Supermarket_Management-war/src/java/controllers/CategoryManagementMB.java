package controllers;

import entityclass.Categories;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import sessionbeans.CategoriesFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@Named(value = "categoryMB")
@ViewScoped
public class CategoryManagementMB implements Serializable {

    @EJB
    private CategoriesFacadeLocal categoriesFacade;

    private List<Categories> categoriesList;

    private Categories selectedCategory;

    private Categories newCategory;

    private boolean editCategoryMode = false;

    private String searchTerm;

    private Integer categoryId;

    @PostConstruct
    public void init() {
        loadCategories();
        newCategory = new Categories();
    }

    public void loadCategories() {
        categoriesList = categoriesFacade.findAll();
    }

    // Category CRUD operations
    public String addCategory() {
        try {
            newCategory.setCreatedAt(new Date());
            newCategory.setStatus("Active");
            categoriesFacade.create(newCategory);
            loadCategories();
            newCategory = new Categories();
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
        try {
            categoriesFacade.edit(selectedCategory);
            loadCategories();
            editCategoryMode = false;
            selectedCategory = null;
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
        try {
            categoriesFacade.remove(category);
            loadCategories();
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
}
