package controllers;

import entityclass.Addresses;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 *
 * @author TRUONG LAM
 */
@FacesConverter(value = "addressesConverter", forClass = Addresses.class)
public class AddressesConverter implements Converter<Object> {

    @Override
    public Addresses getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            Integer id = Integer.valueOf(value);

            // Get the AddressController from the session
            AddressController addressCtrl = (AddressController) context.getApplication()
                .evaluateExpressionGet(context, "#{addressCtrl}", AddressController.class);

            if (addressCtrl != null) {
                // Use the AddressController's addresses list to find the address
                for (Addresses addr : addressCtrl.getAddresses()) {
                    if (addr.getAddressID().equals(id)) {
                        return addr;
                    }
                }
            }

            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Addresses) {
            Addresses address = (Addresses) value;
            return address.getAddressID().toString();
        }
        return value.toString();
    }
}
