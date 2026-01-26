package controllers;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import sessionbeans.DashboardServiceLocal;
import sessionbeans.DashboardService.OrderSummary;
import sessionbeans.DashboardService.ProductSales;

@Named("dashboardMB")
@ViewScoped
public class DashboardMB implements Serializable {

    @Inject
    private DashboardServiceLocal dashboardService;

    private BigDecimal totalRevenue;
    private Long newOrders;
    private Long pendingDeliveries;
    private Long activeCustomers;
    private Long lowStockCount;
    private List<OrderSummary> recentOrders;
    private List<ProductSales> topProducts;

    @PostConstruct
    public void init() {
        loadDashboardData();
    }

    public void loadDashboardData() {
        try {
            // Get Total Revenue
            totalRevenue = dashboardService.getTotalRevenue();
            
            // Get New Orders
            newOrders = dashboardService.getNewOrdersCount();
            
            // Get Pending Deliveries
            pendingDeliveries = dashboardService.getPendingDeliveriesCount();
            
            // Get Active Customers
            activeCustomers = dashboardService.getActiveCustomersCount();
            
            // Get Low Stock Count
            lowStockCount = dashboardService.getLowStockItemsCount();
            
            // Get Recent Orders
            recentOrders = dashboardService.getRecentOrders();
            
            // Get Top Products
            topProducts = dashboardService.getTopProducts();
            
        } catch (Exception e) {
            System.err.println("Error loading dashboard data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Getters
    public BigDecimal getTotalRevenueValue() {
        return totalRevenue;
    }

    public Long getNewOrdersCount() {
        return newOrders;
    }

    public Long getPendingDeliveriesCount() {
        return pendingDeliveries;
    }

    public Long getActiveCustomersCount() {
        return activeCustomers;
    }

    public Long getLowStockItemsCount() {
        return lowStockCount;
    }

    public List<OrderSummary> getRecentOrdersList() {
        return recentOrders;
    }

    public List<ProductSales> getTopProductsList() {
        return topProducts;
    }
}
