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
import java.text.SimpleDateFormat;
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
        // Get orderId from request parameter
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String orderIdStr = facesContext.getExternalContext().getRequestParameterMap().get("orderId");
        if (orderIdStr != null) {
            try {
                orderId = Integer.parseInt(orderIdStr);
                order = ordersFacade.find(orderId);
                if (order != null) {
                    orderTotal = order.getTotalAmount();
                    orderItems = orderDetailsFacade.findByOrderID(order);

                    // Set shipping method display
                    shippingMethodDisplay = "Standard Delivery";
                    if ("express".equals(order.getShippingMethod())) {
                        shippingMethodDisplay = "Express Delivery";
                    }

                    // Set delivery estimate
                    deliveryEstimate = "3-5 Business Days";
                    if ("express".equals(order.getShippingMethod())) {
                        deliveryEstimate = "1-2 Business Days";
                    }
                }
            } catch (NumberFormatException e) {
                // Handle invalid orderId
            }
        }
    }

    public String getProductImage(OrderDetails item) {
        if (item != null && item.getProductImage() != null) {
            return item.getProductImage();
        }
        return "/images/no-image.png";
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
