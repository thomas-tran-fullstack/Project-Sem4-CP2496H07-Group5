package controllers;

import entityclass.Customers;
import entityclass.Users;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import sessionbeans.CustomersFacadeLocal;
import sessionbeans.UsersFacadeLocal;

/**
 *
 * @author TRUONG LAM
 */
@Named(value = "profileMB")
@ViewScoped
public class ProfileMB implements java.io.Serializable {

    @EJB
    private CustomersFacadeLocal customersFacade;

    @EJB
    private UsersFacadeLocal usersFacade;

    @Inject
    private AuthController auth;

    private Customers customer;
    private String firstName;
    private String lastName;
    private String email;
    private String mobilePhone;
    private String street;
    private String city;
    private String state;
    private String country;
    private Double latitude;
    private Double longitude;

    /**
     * Creates a new instance of ProfileMB
     */
    public ProfileMB() {
    }

    public void loadProfile() {
        if (auth != null && auth.getCurrentUser() != null) {
            Users currentUser = auth.getCurrentUser();
            customer = customersFacade.findByUserID(currentUser.getUserID());

            if (customer != null) {
                firstName = customer.getFirstName();
                lastName = customer.getLastName();
                mobilePhone = customer.getMobilePhone();
                street = customer.getStreet();
                city = customer.getCity();
                state = customer.getState();
                country = customer.getCountry();
                latitude = customer.getLatitude();
                longitude = customer.getLongitude();
            }

            email = currentUser.getEmail();
        }
    }

    public String updateProfile() {
        try {
            if (auth != null && auth.getCurrentUser() != null) {
                Users currentUser = auth.getCurrentUser();

                // Update user email
                currentUser.setEmail(email);
                usersFacade.edit(currentUser);

                // Update or create customer profile
                if (customer == null) {
                    customer = new Customers();
                    customer.setUserID(currentUser);
                }

                customer.setFirstName(firstName);
                customer.setLastName(lastName);
                customer.setMobilePhone(mobilePhone);
                customer.setStreet(street);
                customer.setCity(city);
                customer.setState(state);
                customer.setCountry(country);
                customer.setLatitude(latitude);
                customer.setLongitude(longitude);

                if (customer.getCustomerID() == null) {
                    customersFacade.create(customer);
                } else {
                    customersFacade.edit(customer);
                }

                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Profile updated successfully"));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to update profile: " + e.getMessage()));
        }

        return null;
    }

    // Getters and Setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Customers getCustomer() {
        return customer;
    }

    public void setCustomer(Customers customer) {
        this.customer = customer;
    }
}
