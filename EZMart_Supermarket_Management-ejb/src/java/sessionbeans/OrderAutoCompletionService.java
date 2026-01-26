package sessionbeans;

import entityclass.Orders;
import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
@Startup
public class OrderAutoCompletionService {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Resource
    TimerService timerService;

    // Auto-complete orders that have been in SHIPPING status for more than 7 days
    @Schedule(hour = "2", minute = "0", second = "0", persistent = false) // Run daily at 2 AM
    public void autoCompleteOldOrders() {
        try {
            // Calculate date 7 days ago
            long sevenDaysInMillis = TimeUnit.DAYS.toMillis(7);
            Date sevenDaysAgo = new Date(System.currentTimeMillis() - sevenDaysInMillis);

            // Find orders that are still in SHIPPING status and were shipped more than 7 days ago
            Query query = em.createQuery(
                "SELECT o FROM Orders o WHERE o.status = :status AND o.orderDate < :dateThreshold");
            query.setParameter("status", "SHIPPING");
            query.setParameter("dateThreshold", sevenDaysAgo);

            List<Orders> oldShippingOrders = query.getResultList();

            for (Orders order : oldShippingOrders) {
                // Auto-complete the order
                order.setStatus("COMPLETED");

                // For COD orders, set payment status to PAID when auto-completed
                if ("COD".equals(order.getPaymentMethod())) {
                    order.setPaymentStatus("PAID");
                }

                em.merge(order);
                System.out.println("Auto-completed order ID: " + order.getOrderID() +
                                 " - Status changed from SHIPPING to COMPLETED");
            }

            if (!oldShippingOrders.isEmpty()) {
                System.out.println("Auto-completed " + oldShippingOrders.size() + " orders");
            }

        } catch (Exception e) {
            System.err.println("Error in auto-completion service: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
