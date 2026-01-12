package sessionbeans;

import entityclass.Customers;
import entityclass.Offers;
import jakarta.ejb.Local;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface VoucherAutomationServiceLocal {

    void createWelcomeVoucher(Customers customer);

    void checkAndCreateLoyaltyVouchers(Customers customer);

    void createEventVouchers(Offers offer);

    void sendVoucherNotification(Integer customerId, String voucherCode, String messageType);

    double calculateTotalPurchaseAmount(Customers customer);
}
