package controllers;

import entityclass.Categories;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import sessionbeans.CategoriesFacadeLocal;

@Named("categoryUserMB")
@ViewScoped
public class CategoryMB implements Serializable {

    @EJB
    private CategoriesFacadeLocal categoriesFacade;

    private List<Categories> categories;
    private Categories selectedCategory;
    private Categories newCategory;
    private String searchTerm;

    @PostConstruct
    public void init() {
        loadCategories();
        newCategory = new Categories();
    }

    public void loadCategories() {
        categories = categoriesFacade.findAll();
    }

    public void createCategory() {
        try {
            categoriesFacade.create(newCategory);
            loadCategories();
            newCategory = new Categories();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Category created successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to create category"));
        }
    }

    public void updateCategory() {
        try {
            categoriesFacade.edit(selectedCategory);
            loadCategories();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Category updated successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to update category"));
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

    public void prepareEdit(Categories category) {
        selectedCategory = category;
    }

    public void searchCategories() {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            categories = categoriesFacade.findByCategoryName(searchTerm);
        } else {
            loadCategories();
        }
    }

    // Getters and Setters
    public List<Categories> getCategories() {
        return categories;
    }

    public void setCategories(List<Categories> categories) {
        this.categories = categories;
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

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
}
