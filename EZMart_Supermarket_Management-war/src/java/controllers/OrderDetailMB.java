package controllers;

import entityclass.OrderDetails;
import entityclass.Orders;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import sessionbeans.OrderDetailsFacadeLocal;
import sessionbeans.OrdersFacadeLocal;

@Named("orderDetailMB")
@RequestScoped
public class OrderDetailMB implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private OrdersFacadeLocal ordersFacade;

    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;

    private Integer orderId;
    private Orders order;
    private List<OrderDetails> orderItems;

    @PostConstruct
    public void init() {
        System.out.println("DEBUG: OrderDetailMB.init() called");

        // Get orderId from request parameter
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String orderIdStr = facesContext.getExternalContext().getRequestParameterMap().get("orderId");
        System.out.println("DEBUG: orderIdStr from request: " + orderIdStr);

        if (orderIdStr != null && !orderIdStr.isEmpty()) {
            try {
                orderId = Integer.parseInt(orderIdStr);
                System.out.println("DEBUG: Parsed orderId: " + orderId);

                // Fetch order with details using the new method
                try {
                    order = ordersFacade.findOrderWithDetails(orderId);
                    System.out.println("DEBUG: Order found: " + (order != null ? order.getOrderID() : "null"));
                } catch (Exception e) {
                    System.out.println("DEBUG: Error fetching order: " + e.getMessage());
                    e.printStackTrace();
                    order = null;
                }

                if (order != null) {
                    orderItems = order.getOrderDetailsList();
                    System.out.println("DEBUG: orderItems size from order object: " + (orderItems != null ? orderItems.size() : "null"));

                    if (orderItems != null && !orderItems.isEmpty()) {
                        for (OrderDetails item : orderItems) {
                            System.out.println("DEBUG: OrderDetail - productName: " + item.getProductName() +
                                ", productImage: " + item.getProductImage() +
                                ", quantity: " + item.getQuantity() +
                                ", totalPrice: " + item.getTotalPrice());
                        }
                    } else {
                        System.out.println("DEBUG: orderItems is null or empty, trying manual fetch");
                        // Fallback: manually fetch order details using facade
                        try {
                            orderItems = orderDetailsFacade.findByOrderID(order);
                            System.out.println("DEBUG: Manual fetch returned: " + (orderItems != null ? orderItems.size() : "null") + " items");
                            if (orderItems != null) {
                                for (OrderDetails item : orderItems) {
                                    System.out.println("DEBUG: Manual fetched OrderDetail - productName: " + item.getProductName() +
                                        ", productImage: " + item.getProductImage() +
                                        ", quantity: " + item.getQuantity() +
                                        ", totalPrice: " + item.getTotalPrice());
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("DEBUG: Manual fetch failed: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("DEBUG: Order is null, not setting any values");
                }
            } catch (NumberFormatException e) {
                System.out.println("DEBUG: Invalid orderId format: " + orderIdStr);
            }
        } else {
            System.out.println("DEBUG: orderIdStr is null or empty");
        }
    }

    public String getProductImage(OrderDetails item) {
        // First try to use the denormalized productImage field from OrderDetails
        if (item.getProductImage() != null && !item.getProductImage().isEmpty()) {
            return "/uploads/products/" + item.getProductImage();
        }

        // Fallback to productID relationship if productImage is empty
        if (item.getProductID() != null && item.getProductID().getProductImagesList() != null && !item.getProductID().getProductImagesList().isEmpty()) {
            return "/uploads/products/" + item.getProductID().getProductImagesList().get(0).getImageURL();
        }

        return "/images/no-image.png";
    }

    public String productImageUrl(entityclass.Products product) {
        if (product != null && product.getProductImagesList() != null && !product.getProductImagesList().isEmpty()) {
            return product.getProductImagesList().get(0).getImageURL();
        }
        return null;
    }

    public String getProductName(OrderDetails item) {
        // First try to use the denormalized productName field from OrderDetails
        if (item.getProductName() != null && !item.getProductName().isEmpty()) {
            return item.getProductName();
        }

        // Fallback to productID relationship
        if (item.getProductID() != null) {
            return item.getProductID().getProductName();
        }

        return "Unknown Product";
    }

    public String getShippingMethodDisplay() {
        return order != null && order.getShippingMethod() != null ? order.getShippingMethod() : "Standard Delivery";
    }

    public String getDeliveryEstimate() {
        return "2-3 business days";
    }

    public BigDecimal getOrderTotal() {
        return order != null ? order.getTotalAmount() : BigDecimal.ZERO;
    }

    public void markOrderAsReceived() {
        if (order != null && "SHIPPING".equals(order.getStatus())) {
            order.setStatus("COMPLETED");
            // For COD orders, set payment status to PAID when completed
            if ("COD".equals(order.getPaymentMethod())) {
                order.setPaymentStatus("PAID");
            }
            ordersFacade.edit(order);
            // Refresh order from database
            order = ordersFacade.findOrderWithDetails(orderId);
            orderItems = order.getOrderDetailsList();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Order marked as received successfully!"));
        }
    }

    public boolean canMarkAsReceived() {
        return order != null && "SHIPPING".equals(order.getStatus());
    }

    // Getters and Setters
    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Orders getOrder() {
        return order;
    }

    public void setOrder(Orders order) {
        this.order = order;
    }

    public List<OrderDetails> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderDetails> orderItems) {
        this.orderItems = orderItems;
    }
}
