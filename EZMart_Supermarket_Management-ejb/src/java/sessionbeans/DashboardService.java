package sessionbeans;

import entityclass.Orders;
import entityclass.Customers;
import entityclass.Products;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class DashboardService implements DashboardServiceLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    public BigDecimal getTotalRevenue() {
        try {
            Query query = em.createQuery(
                "SELECT SUM(o.totalAmount) FROM Orders o WHERE o.status IN ('COMPLETED', 'SHIPPING')"
            );
            BigDecimal result = (BigDecimal) query.getSingleResult();
            return result != null ? result : BigDecimal.ZERO;
        } catch (Exception e) {
            System.err.println("Error getting total revenue: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    public Long getNewOrdersCount() {
        try {
            Query query = em.createQuery(
                "SELECT COUNT(o) FROM Orders o WHERE o.status = 'NEW'"
            );
            Long result = (Long) query.getSingleResult();
            return result != null ? result : 0L;
        } catch (Exception e) {
            System.err.println("Error getting new orders count: " + e.getMessage());
            return 0L;
        }
    }

    @Override
    public Long getPendingDeliveriesCount() {
        try {
            Query query = em.createQuery(
                "SELECT COUNT(o) FROM Orders o WHERE o.status = 'SHIPPING'"
            );
            Long result = (Long) query.getSingleResult();
            return result != null ? result : 0L;
        } catch (Exception e) {
            System.err.println("Error getting pending deliveries: " + e.getMessage());
            return 0L;
        }
    }

    @Override
    public Long getActiveCustomersCount() {
        try {
            Query query = em.createQuery("SELECT COUNT(c) FROM Customers c");
            Long result = (Long) query.getSingleResult();
            return result != null ? result : 0L;
        } catch (Exception e) {
            System.err.println("Error getting active customers count: " + e.getMessage());
            return 0L;
        }
    }

    @Override
    public Long getLowStockItemsCount() {
        try {
            Query query = em.createQuery(
                "SELECT COUNT(p) FROM Products p WHERE p.stockQuantity < 10"
            );
            Long result = (Long) query.getSingleResult();
            return result != null ? result : 0L;
        } catch (Exception e) {
            System.err.println("Error getting low stock count: " + e.getMessage());
            return 0L;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OrderSummary> getRecentOrders() {
        try {
            Query query = em.createQuery(
                "SELECT o FROM Orders o ORDER BY o.orderDate DESC"
            );
            query.setMaxResults(10);
            List<?> results = query.getResultList();
            List<OrderSummary> summaries = new ArrayList<>();
            for (Object obj : results) {
                Orders order = (Orders) obj;
                Customers customer = order.getCustomerID() != null ? 
                    order.getCustomerID() : null;
                
                java.util.Date dateUtil = order.getOrderDate();
                LocalDateTime localDateTime = dateUtil != null ? 
                    LocalDateTime.ofInstant(dateUtil.toInstant(), ZoneId.systemDefault()) : 
                    LocalDateTime.now();
                
                summaries.add(new OrderSummary(
                    (long) order.getOrderID(), 
                    (long) order.getOrderID(),
                    customer != null ? customer.getFirstName() : "Unknown",
                    customer != null ? customer.getLastName() : "",
                    localDateTime,
                    order.getTotalAmount(),
                    order.getStatus()
                ));
            }
            return summaries;
        } catch (Exception e) {
            System.err.println("Error getting recent orders: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProductSales> getTopProducts() {
        try {
            Query query = em.createQuery(
                "SELECT p FROM Products p ORDER BY p.productID DESC"
            );
            query.setMaxResults(10);
            List<?> results = query.getResultList();
            List<ProductSales> productSales = new ArrayList<>();
            for (Object obj : results) {
                Products product = (Products) obj;
                String imageUrl = "";
                try {
                    Query imgQuery = em.createQuery(
                        "SELECT pi FROM ProductImages pi WHERE pi.productID.productID = :productId"
                    );
                    imgQuery.setParameter("productId", product.getProductID());
                    imgQuery.setMaxResults(1);
                    List<?> images = imgQuery.getResultList();
                    if (!images.isEmpty()) {
                        imageUrl = ((entityclass.ProductImages) images.get(0)).getImageURL();
                    }
                } catch (Exception e) {
                    // Skip image URL if error
                }
                productSales.add(new ProductSales(
                    (long) product.getProductID(),
                    product.getProductName(),
                    product.getUnitPrice(),
                    Long.valueOf(product.getStockQuantity() != null ? product.getStockQuantity() : 0),
                    imageUrl
                ));
            }
            return productSales;
        } catch (Exception e) {
            System.err.println("Error getting top products: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Inner class for Order Summary
    public static class OrderSummary {
        public Long orderId;
        public String orderNumber;
        public String customerName;
        public String lastNameCustomer;
        public LocalDateTime orderDate;
        public BigDecimal amount;
        public String status;

        public OrderSummary(Long orderId, Long orderNum, String firstName, String lastName,
                          LocalDateTime orderDate, BigDecimal amount, String status) {
            this.orderId = orderId;
            this.orderNumber = "#ORD-" + String.format("%05d", orderId);
            this.customerName = (firstName != null ? firstName : "") + 
                              (lastName != null ? " " + lastName : "");
            this.orderDate = orderDate;
            this.amount = amount;
            this.status = status;
        }

        public Long getOrderId() { return orderId; }
        public String getOrderNumber() { return orderNumber; }
        public String getCustomerName() { return customerName; }
        public LocalDateTime getOrderDate() { return orderDate; }
        public BigDecimal getAmount() { return amount; }
        public String getStatus() { return status; }
    }

    // Inner class for Product Sales
    public static class ProductSales {
        public Long productId;
        public String productName;
        public BigDecimal price;
        public Long quantitySold;
        public String imageUrl;

        public ProductSales(Long productId, String productName, BigDecimal price,
                          Long quantitySold, String imageUrl) {
            this.productId = productId;
            this.productName = productName;
            this.price = price;
            this.quantitySold = quantitySold;
            this.imageUrl = imageUrl;
        }

        public Long getProductId() { return productId; }
        public String getProductName() { return productName; }
        public BigDecimal getPrice() { return price; }
        public Long getQuantitySold() { return quantitySold; }
        public String getImageUrl() { return imageUrl; }
    }
}
