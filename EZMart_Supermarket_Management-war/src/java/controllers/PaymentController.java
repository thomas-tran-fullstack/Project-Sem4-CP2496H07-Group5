package controllers;

import entityclass.CreditCards;
import entityclass.Customers;
import java.io.Serializable;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.faces.context.FacesContext;
import sessionbeans.CreditCardsFacadeLocal;

@Named("paymentCtrl")
@SessionScoped
public class PaymentController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private CreditCardsFacadeLocal creditCardsFacade;
    
    @Inject
    private AuthController authController;

    private List<CreditCards> cards = new ArrayList<>();
    private CreditCards editingCard = new CreditCards();
    // transient UI-only fields
    private String editingCardHolderName;
    private String expiryMonth;
    private String expiryYear;

    @PostConstruct
    public void init() {
        loadForCurrentUser();
    }

    public void loadForCurrentUser() {
        try {
            // Get current customer via AuthController
            if (authController == null) {
                authController = (AuthController) FacesContext.getCurrentInstance()
                    .getApplication().evaluateExpressionGet(FacesContext.getCurrentInstance(), "#{auth}", AuthController.class);
            }
            
            if (authController != null && authController.getCurrentCustomer() != null) {
                Integer cid = authController.getCurrentCustomer().getCustomerID();
                if (cid != null) {
                    cards = creditCardsFacade.findByCustomer(cid);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading payment cards: " + e.getMessage());
            cards = new ArrayList<>();
        }
    }

    /**
     * Verify that the card belongs to the current user
     */
    private boolean isCardOwnedByCurrentUser(CreditCards card) {
        if (card == null || card.getCustomerID() == null) return false;
        if (authController == null || authController.getCurrentCustomer() == null) return false;
        return card.getCustomerID().getCustomerID().equals(authController.getCurrentCustomer().getCustomerID());
    }

    /**
     * Validate card fields based on payment type
     */
    private boolean validateCard(CreditCards card) {
        if (card == null) return false;

        // Validate card type
        String cardType = card.getCardType();
        if (cardType == null || cardType.isEmpty() || cardType.length() > 20) {
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Payment method type is required", null
            ));
            return false;
        }

        // Different validation rules based on payment type
        if ("VISA".equalsIgnoreCase(cardType) || "MASTERCARD".equalsIgnoreCase(cardType)) {
            // Credit card validation
            return validateCreditCard(card);
        } else if ("MOMO".equalsIgnoreCase(cardType)) {
            // MOMO validation - no card number/expiry required
            return validateMomoCard(card);
        } else if ("PAYPAL".equalsIgnoreCase(cardType)) {
            // PayPal validation - no card number/expiry required
            return validatePayPalCard(card);
        } else {
            // Unknown payment type
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Unsupported payment method type", null
            ));
            return false;
        }
    }

    /**
     * Validate credit card fields
     */
    private boolean validateCreditCard(CreditCards card) {
        // Validate card number
        String cardNumber = card.getCardNumber();
        if (cardNumber == null || cardNumber.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Card number is required", null
            ));
            return false;
        }

        // Remove non-digits for validation
        String digitsOnly = cardNumber.replaceAll("\\D", "");
        if (digitsOnly.length() < 13 || digitsOnly.length() > 19) {
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Card number must be between 13 and 19 digits", null
            ));
            return false;
        }

        // Validate Luhn checksum
        if (!luhnCheck(cardNumber)) {
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Invalid card number (Luhn check failed)", null
            ));
            return false;
        }

        // Validate expiry date
        String expiry = card.getCardExpiry();
        if (expiry == null || expiry.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Expiry date is required (MM/YY format)", null
            ));
            return false;
        }

        if (!isValidExpiryDate(expiry)) {
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Card is expired or invalid expiry date format (use MM/YY)", null
            ));
            return false;
        }

        return true;
    }

    /**
     * Validate MOMO payment method
     */
    private boolean validateMomoCard(CreditCards card) {
        // MOMO doesn't require card number or expiry date
        // The card number field might be used to store phone number or other identifier
        // For now, just ensure card type is set (already validated above)
        return true;
    }

    /**
     * Validate PayPal payment method
     */
    private boolean validatePayPalCard(CreditCards card) {
        // PayPal doesn't require card number or expiry date
        // The card number field might be used to store email or other identifier
        // For now, just ensure card type is set (already validated above)
        return true;
    }

    /**
     * Validate expiry date format (MM/YY) and check if card is not expired
     */
    private boolean isValidExpiryDate(String expiry) {
        try {
            // Expected format: MM/YY
            String[] parts = expiry.split("/");
            if (parts.length != 2) return false;
            
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            
            if (month < 1 || month > 12) return false;
            
            // Assume 2000s for 2-digit year
            if (year < 100) year += 2000;
            
            // Check if card is not expired
            YearMonth expireDate = YearMonth.of(year, month);
            YearMonth today = YearMonth.now();
            
            return expireDate.isAfter(today);
        } catch (Exception e) {
            return false;
        }
    }

    public List<CreditCards> getCards() { return cards; }
    public CreditCards getEditingCard() { return editingCard; }
    public void setEditingCard(CreditCards c) { this.editingCard = c; }
    public String getEditingCardHolderName() { return editingCardHolderName; }
    public void setEditingCardHolderName(String v) { this.editingCardHolderName = v; }
    public String getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(String m) { this.expiryMonth = m; }
    public String getExpiryYear() { return expiryYear; }
    public void setExpiryYear(String y) { this.expiryYear = y; }

    public String startAdd() {
        editingCard = new CreditCards();
        editingCardHolderName = "";
        expiryMonth = "";
        expiryYear = "";
        return null;
    }

    public String startEdit(CreditCards c) {
        if (c != null && isCardOwnedByCurrentUser(c)) {
            editingCard = c;
            // populate transient fields from stored cardExpiry (format MM/YY)
            String ce = c.getCardExpiry();
            if (ce != null && ce.contains("/")) {
                String[] parts = ce.split("/");
                if (parts.length == 2) {
                    expiryMonth = parts[0];
                    expiryYear = parts[1];
                } else {
                    expiryMonth = "";
                    expiryYear = "";
                }
            } else {
                expiryMonth = "";
                expiryYear = "";
            }
            editingCardHolderName = ""; // not stored on entity currently
        }
        return null;
    }

    public String save() {
        try {
            // Assemble expiry into card before validation
            if (expiryMonth != null && expiryYear != null && !expiryMonth.isEmpty() && !expiryYear.isEmpty()) {
                editingCard.setCardExpiry(expiryMonth + "/" + expiryYear);
            }

            // Validate input
            if (!validateCard(editingCard)) {
                return null;
            }
            
            // Ensure current user is authenticated
            if (authController == null || authController.getCurrentCustomer() == null) {
                FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    "You must be logged in to manage payment methods", null
                ));
                return null;
            }
            
            Customers cust = authController.getCurrentCustomer();
            
            if (editingCard.getCardID() == null) {
                // Creating new card
                editingCard.setCustomerID(cust);
                creditCardsFacade.create(editingCard);
                FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                    "Card added successfully", null
                ));
            } else {
                // Updating card - verify ownership
                if (!isCardOwnedByCurrentUser(editingCard)) {
                    FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                        jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                        "You cannot modify this card", null
                    ));
                    return null;
                }
                creditCardsFacade.edit(editingCard);
                FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                    "Card updated successfully", null
                ));
            }
            
            loadForCurrentUser();
        } catch (Exception e) {
            System.err.println("Error saving payment card: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Failed to save card", null
            ));
        }
        return null;
    }

    public String delete(CreditCards c) {
        try {
            // Verify ownership before deletion
            if (c != null && isCardOwnedByCurrentUser(c)) {
                creditCardsFacade.remove(c);
                loadForCurrentUser();
                FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                    "Card removed successfully", null
                ));
            } else {
                FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    "You cannot delete this card", null
                ));
            }
        } catch (Exception e) {
            System.err.println("Error deleting card: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Failed to delete card", null
            ));
        }
        return null;
    }

    public String setDefault(CreditCards c) {
        try {
            if (c == null || !isCardOwnedByCurrentUser(c)) {
                FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    "Invalid card", null
                ));
                return null;
            }
            
            if (authController == null || authController.getCurrentCustomer() == null) return null;
            
            Integer cid = authController.getCurrentCustomer().getCustomerID();
            List<CreditCards> list = creditCardsFacade.findByCustomer(cid);
            
            // Unset other defaults for this customer
            for (CreditCards cc : list) {
                if (cc.getIsDefault() != null && cc.getIsDefault()) {
                    cc.setIsDefault(false);
                    creditCardsFacade.edit(cc);
                }
            }
            
            c.setIsDefault(true);
            creditCardsFacade.edit(c);
            loadForCurrentUser();
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_INFO,
                "Default payment method updated", null
            ));
        } catch (Exception e) {
            System.err.println("Error setting default card: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new jakarta.faces.application.FacesMessage(
                jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                "Failed to update default card", null
            ));
        }
        return null;
    }

    /**
     * Luhn algorithm to validate card numbers
     */
    private boolean luhnCheck(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) return false;
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() < 13 || digits.length() > 19) return false;
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(digits.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) n = (n % 10) + 1;
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10) == 0;
    }
}
