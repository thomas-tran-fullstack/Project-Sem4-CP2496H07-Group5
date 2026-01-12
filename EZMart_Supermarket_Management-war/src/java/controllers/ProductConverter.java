package controllers;

import entityclass.Products;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import sessionbeans.ProductsFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@FacesConverter(value = "productConverter")
public class ProductConverter implements Converter<Products> {

    @Inject
    private ProductsFacadeLocal productsFacade;

    @Override
    public Products getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            Integer productId = Integer.valueOf(value);
            return productsFacade.find(productId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Products value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value.getProductID());
    }
}
