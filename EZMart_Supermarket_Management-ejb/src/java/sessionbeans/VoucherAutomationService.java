package sessionbeans;

import entityclass.Customers;
import entityclass.Offers;
import entityclass.Orders;
import entityclass.Vouchers;
import java.util.List;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 *
 * @author TRUONG LAM
 */
@Stateless
public class VoucherAutomationService implements VoucherAutomationServiceLocal {

    @EJB
    private VouchersFacadeLocal vouchersFacade;

    @EJB
    private CustomersFacadeLocal customersFacade;

    @EJB
    private OffersFacadeLocal offersFacade;

    @EJB
    private OrdersFacadeLocal ordersFacade;

    @Override
    public void createWelcomeVoucher(Customers customer) {
        try {
            // Create welcome offer if not exists
            Offers welcomeOffer = createOrGetWelcomeOffer();

            Vouchers welcomeVoucher = new Vouchers();
            welcomeVoucher.setVoucherCode(generateVoucherCode());
            welcomeVoucher.setCustomerID(customer);
            welcomeVoucher.setOfferID(welcomeOffer);
            welcomeVoucher.setIsUsed(false);
            welcomeVoucher.setCreatedAt(new Date());

            // Set expiry date to 30 days from now
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 30);
            welcomeVoucher.setExpiryDate(cal.getTime());

            vouchersFacade.create(welcomeVoucher);

            // Send notification
            sendVoucherNotification(customer.getCustomerID(), welcomeVoucher.getVoucherCode(), "welcome");

        } catch (Exception e) {
            System.err.println("Error creating welcome voucher: " + e.getMessage());
        }
    }

    @Override
    public void checkAndCreateLoyaltyVouchers(Customers customer) {
        try {
            double totalPurchase = calculateTotalPurchaseAmount(customer);

            // Loyalty tiers
            if (totalPurchase >= 5000000) { // 5 million VND
                createLoyaltyVoucher(customer, "LOYALTY_5M", 50000); // 50k discount
            } else if (totalPurchase >= 2000000) { // 2 million VND
                createLoyaltyVoucher(customer, "LOYALTY_2M", 20000); // 20k discount
            } else if (totalPurchase >= 1000000) { // 1 million VND
                createLoyaltyVoucher(customer, "LOYALTY_1M", 10000); // 10k discount
            }

        } catch (Exception e) {
            System.err.println("Error checking loyalty vouchers: " + e.getMessage());
        }
    }

    @Override
    public void createEventVouchers(Offers offer) {
        try {
            if (!offer.getVoucherEnabled()) {
                return; // Only create vouchers for offers that have voucher enabled
            }

            List<Customers> allCustomers = customersFacade.findAll();

            for (Customers customer : allCustomers) {
                // Check if customer already has voucher for this offer
                boolean hasVoucher = vouchersFacade.findByCustomerID(customer.getCustomerID())
                    .stream()
                    .anyMatch(v -> v.getOfferID() != null && v.getOfferID().getOfferID().equals(offer.getOfferID()));

                if (!hasVoucher) {
                    Vouchers eventVoucher = new Vouchers();
                    eventVoucher.setVoucherCode(generateVoucherCode());
                    eventVoucher.setCustomerID(customer);
                    eventVoucher.setOfferID(offer);
                    eventVoucher.setIsUsed(false);
                    eventVoucher.setCreatedAt(new Date());
                    eventVoucher.setExpiryDate(offer.getEndDate());

                    vouchersFacade.create(eventVoucher);

                    // Send notification
                    sendVoucherNotification(customer.getCustomerID(), eventVoucher.getVoucherCode(), "event");
                }
            }

        } catch (Exception e) {
            System.err.println("Error creating event vouchers: " + e.getMessage());
        }
    }

    @Override
    public void sendVoucherNotification(Integer customerId, String voucherCode, String messageType) {
        try {
            String message = createNotificationMessage(voucherCode, messageType);

            // TODO: Send via WebSocket to specific user
            // websocket.NotificationWebSocket.sendToUser(customerId.toString(), message);
            System.out.println("Notification to customer " + customerId + ": " + message);

        } catch (Exception e) {
            System.err.println("Error sending voucher notification: " + e.getMessage());
        }
    }

    @Override
    public double calculateTotalPurchaseAmount(Customers customer) {
        try {
            List<Orders> completedOrders = ordersFacade.findByCustomerIDAndStatus(customer.getCustomerID(), "Completed");

            double totalAmount = 0.0;
            for (Orders order : completedOrders) {
                if (order.getTotalAmount() != null) {
                    totalAmount += order.getTotalAmount().doubleValue();
                }
            }

            return totalAmount;

        } catch (Exception e) {
            System.err.println("Error calculating total purchase amount: " + e.getMessage());
            return 0.0;
        }
    }

    private Offers createOrGetWelcomeOffer() {
        try {
            // Try to find existing welcome offer
            List<Offers> offers = offersFacade.findAll();
            for (Offers offer : offers) {
                if ("Welcome Voucher".equals(offer.getOfferName())) {
                    return offer;
                }
            }

            // Create new welcome offer
            Offers welcomeOffer = new Offers();
            welcomeOffer.setOfferName("Welcome Voucher");
            welcomeOffer.setOfferType("Discount");
            welcomeOffer.setDiscountValue(10); // 10% discount
            welcomeOffer.setStartDate(new Date());

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, 1); // Valid for 1 year
            welcomeOffer.setEndDate(cal.getTime());

            welcomeOffer.setStatus("Active");
            welcomeOffer.setVoucherEnabled(true);

            offersFacade.create(welcomeOffer);
            return welcomeOffer;

        } catch (Exception e) {
            System.err.println("Error creating/getting welcome offer: " + e.getMessage());
            return null;
        }
    }

    private void createLoyaltyVoucher(Customers customer, String voucherType, int discountValue) {
        try {
            // Check if customer already has this type of loyalty voucher
            boolean hasVoucher = vouchersFacade.findByCustomerID(customer.getCustomerID())
                .stream()
                .anyMatch(v -> v.getVoucherCode().startsWith(voucherType));

            if (!hasVoucher) {
                Offers loyaltyOffer = createOrGetLoyaltyOffer(voucherType, discountValue);

                Vouchers loyaltyVoucher = new Vouchers();
                loyaltyVoucher.setVoucherCode(voucherType + "_" + generateVoucherCode().substring(0, 4));
                loyaltyVoucher.setCustomerID(customer);
                loyaltyVoucher.setOfferID(loyaltyOffer);
                loyaltyVoucher.setIsUsed(false);
                loyaltyVoucher.setCreatedAt(new Date());

                // Set expiry date to 90 days from now
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, 90);
                loyaltyVoucher.setExpiryDate(cal.getTime());

                vouchersFacade.create(loyaltyVoucher);

                // Send notification
                sendVoucherNotification(customer.getCustomerID(), loyaltyVoucher.getVoucherCode(), "loyalty");
            }

        } catch (Exception e) {
            System.err.println("Error creating loyalty voucher: " + e.getMessage());
        }
    }

    private Offers createOrGetLoyaltyOffer(String voucherType, int discountValue) {
        try {
            String offerName = "Loyalty Voucher " + voucherType.replace("LOYALTY_", "");

            // Try to find existing loyalty offer
            List<Offers> offers = offersFacade.findAll();
            for (Offers offer : offers) {
                if (offerName.equals(offer.getOfferName())) {
                    return offer;
                }
            }

            // Create new loyalty offer
            Offers loyaltyOffer = new Offers();
            loyaltyOffer.setOfferName(offerName);
            loyaltyOffer.setOfferType("Fixed Discount");
            loyaltyOffer.setDiscountValue(discountValue);
            loyaltyOffer.setStartDate(new Date());

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, 1); // Valid for 1 year
            loyaltyOffer.setEndDate(cal.getTime());

            loyaltyOffer.setStatus("Active");
            loyaltyOffer.setVoucherEnabled(true);

            offersFacade.create(loyaltyOffer);
            return loyaltyOffer;

        } catch (Exception e) {
            System.err.println("Error creating/getting loyalty offer: " + e.getMessage());
            return null;
        }
    }

    private String generateVoucherCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();

        // Generate 8-character code
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }

    private String createNotificationMessage(String voucherCode, String messageType) {
        switch (messageType) {
            case "welcome":
                return "Chào mừng bạn đến với EZMart! Voucher chào mừng: " + voucherCode + " (Giảm 10%)";
            case "loyalty":
                return "Cảm ơn bạn đã tin tưởng EZMart! Voucher tri ân: " + voucherCode;
            case "event":
                return "EZMart có chương trình khuyến mãi đặc biệt! Voucher: " + voucherCode;
            default:
                return "Bạn có voucher mới: " + voucherCode;
        }
    }
}
