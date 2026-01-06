package controllers;

import entityclass.Categories;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import sessionbeans.CategoriesFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@FacesConverter(value = "categoriesConverter", forClass = Categories.class)
public class CategoriesConverter implements Converter<Object> {

    private CategoriesFacadeLocal categoriesFacade;

    private CategoriesFacadeLocal getCategoriesFacade() {
        if (categoriesFacade == null) {
            try {
                InitialContext ctx = new InitialContext();
                categoriesFacade = (CategoriesFacadeLocal) ctx.lookup("java:global/EZMart_Supermarket_Management/EZMart_Supermarket_Management-ejb/CategoriesFacade");
            } catch (NamingException e) {
                throw new RuntimeException("Failed to lookup CategoriesFacade", e);
            }
        }
        return categoriesFacade;
    }

    @Override
    public Categories getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            Integer id = Integer.valueOf(value);
            return getCategoriesFacade().find(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Categories) {
            Categories category = (Categories) value;
            return category.getCategoryID().toString();
        }
        return value.toString();
    }
}
