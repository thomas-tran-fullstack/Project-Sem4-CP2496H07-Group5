package sessionbeans;

import jakarta.ejb.Local;
import java.math.BigDecimal;
import java.util.List;

@Local
public interface DashboardServiceLocal {
    BigDecimal getTotalRevenue();
    Long getNewOrdersCount();
    Long getPendingDeliveriesCount();
    Long getActiveCustomersCount();
    Long getLowStockItemsCount();
    List<DashboardService.OrderSummary> getRecentOrders();
    List<DashboardService.ProductSales> getTopProducts();
}
