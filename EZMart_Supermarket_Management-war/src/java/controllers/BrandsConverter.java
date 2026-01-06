package controllers;

import entityclass.Brands;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import sessionbeans.BrandsFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@FacesConverter(value = "brandsConverter", forClass = Brands.class)
public class BrandsConverter implements Converter<Object> {

    private BrandsFacadeLocal brandsFacade;

    private BrandsFacadeLocal getBrandsFacade() {
        if (brandsFacade == null) {
            try {
                InitialContext ctx = new InitialContext();
                brandsFacade = (BrandsFacadeLocal) ctx.lookup("java:global/EZMart_Supermarket_Management/EZMart_Supermarket_Management-ejb/BrandsFacade");
            } catch (NamingException e) {
                throw new RuntimeException("Failed to lookup BrandsFacade", e);
            }
        }
        return brandsFacade;
    }

    @Override
    public Brands getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            Integer id = Integer.valueOf(value);
            return getBrandsFacade().find(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Brands) {
            Brands brand = (Brands) value;
            return brand.getBrandID().toString();
        }
        return value.toString();
    }
}
