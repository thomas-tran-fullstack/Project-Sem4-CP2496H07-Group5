package controllers;

import entityclass.OrderDetails;
import entityclass.Orders;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import sessionbeans.OrderDetailsFacadeLocal;
import sessionbeans.OrdersFacadeLocal;

@Named("orderSuccessMB")
@RequestScoped
public class OrderSuccessMB implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private OrdersFacadeLocal ordersFacade;

    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;

    private Integer orderId;
    private Orders order;
    private List<OrderDetails> orderItems;
    private BigDecimal orderTotal;
    private String shippingMethodDisplay;
    private String deliveryEstimate;

    @PostConstruct
    public void init() {
        System.out.println("DEBUG: OrderSuccessMB.init() called");

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
                    orderTotal = order.getTotalAmount();
                    System.out.println("DEBUG: orderTotal: " + orderTotal);

                    orderItems = order.getOrderDetailsList();
                    System.out.println("DEBUG: orderItems size: " + (orderItems != null ? orderItems.size() : "null"));

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
                        } catch (Exception e) {
                            System.out.println("DEBUG: Manual fetch failed: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    shippingMethodDisplay = order.getShippingMethod() != null ? order.getShippingMethod() : "Standard Delivery";
                    deliveryEstimate = "2-3 business days";
                    System.out.println("DEBUG: shippingMethodDisplay: " + shippingMethodDisplay);
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

    public boolean isCodPayment() {
        return order != null && "COD".equalsIgnoreCase(order.getPaymentMethod());
    }

    public String getSuccessTitle() {
        return isCodPayment() ? "Order Placed Successfully!" : "Payment Proof Submitted Successfully!";
    }

    public String getSuccessMessage() {
        return isCodPayment() ?
            "Thank you for your order. We'll prepare your items and deliver them soon." :
            "Thank you for submitting your payment proof. We'll verify it and confirm your order soon.";
    }

    public String getOrderStatusText() {
        return isCodPayment() ? "Order Confirmed" : "Payment Proof Submitted";
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

    public BigDecimal getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(BigDecimal orderTotal) {
        this.orderTotal = orderTotal;
    }

    public String getShippingMethodDisplay() {
        return shippingMethodDisplay;
    }

    public void setShippingMethodDisplay(String shippingMethodDisplay) {
        this.shippingMethodDisplay = shippingMethodDisplay;
    }

    public String getDeliveryEstimate() {
        return deliveryEstimate;
    }

    public void setDeliveryEstimate(String deliveryEstimate) {
        this.deliveryEstimate = deliveryEstimate;
    }
}
