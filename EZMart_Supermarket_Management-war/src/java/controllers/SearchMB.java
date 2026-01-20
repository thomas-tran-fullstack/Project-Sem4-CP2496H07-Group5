package controllers;

import entityclass.Categories;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.ResourceBundle;
import sessionbeans.CategoriesFacadeLocal;

@Named("searchMB")
@ViewScoped
public class SearchMB implements Serializable {

    private String searchTerm;
    private List<Categories> categories;
    private Categories selectedCategory;
    private Integer selectedCategoryId;
    private String categoryFilter;

    @EJB
    private CategoriesFacadeLocal categoriesFacade;

    @PostConstruct
    public void init() {
        loadCategories();
    }

    public void loadCategories() {
        categories = categoriesFacade.findAll();
    }

    public String search() {
        StringBuilder redirect = new StringBuilder("products?faces-redirect=true");
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            redirect.append("&search=").append(searchTerm.trim());
        }
        if (selectedCategoryId != null && selectedCategoryId > 0) {
            redirect.append("&category=").append(selectedCategoryId);
        }
        return redirect.toString();
    }

    public List<Categories> getFilteredCategories() {
        if (categoryFilter == null || categoryFilter.trim().isEmpty()) {
            return categories;
        }
        return categories.stream()
                .filter(cat -> cat.getCategoryName().toLowerCase().contains(categoryFilter.toLowerCase()))
                .toList();
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

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

    public String getCategoryFilter() {
        return categoryFilter;
    }

    public void setCategoryFilter(String categoryFilter) {
        this.categoryFilter = categoryFilter;
    }

    public Integer getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(Integer selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }
}
