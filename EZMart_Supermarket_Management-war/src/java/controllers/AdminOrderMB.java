package controllers;

import entityclass.Customers;
import entityclass.OrderDetails;
import entityclass.Orders;
import entityclass.PaymentProof;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import sessionbeans.OrderDetailsFacadeLocal;
import sessionbeans.OrdersFacadeLocal;
import sessionbeans.PaymentProofFacadeLocal;
import sessionbeans.ProductsFacadeLocal;

@Named("adminOrderMB")
@ViewScoped
public class AdminOrderMB implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private OrdersFacadeLocal ordersFacade;

    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;

    @EJB
    private PaymentProofFacadeLocal paymentProofFacade;

    @EJB
    private ProductsFacadeLocal productsFacade;

    private List<Orders> allOrders;
    private List<Orders> filteredOrders;
    private String selectedStatus = "all";
    private String searchTerm = "";
    private Orders selectedOrder;
    private String newStatus;
    private String cancelReason;

    @PostConstruct
    public void init() {
        loadAllOrders();
    }

    public void loadAllOrders() {
        allOrders = ordersFacade.findAll();
        filterOrders();
    }

    public void filterOrders() {
        filteredOrders = new ArrayList<>();
        for (Orders order : allOrders) {
            boolean matchesStatus = "all".equals(selectedStatus) || 
                                   (order.getStatus() != null && order.getStatus().equals(selectedStatus));
            boolean matchesSearch = searchTerm.isEmpty() || 
                                   String.valueOf(order.getOrderID()).contains(searchTerm);
            if (matchesStatus && matchesSearch) {
                filteredOrders.add(order);
            }
        }
    }

    public void selectOrderForView(Orders order) {
        this.selectedOrder = order;
    }

    public void selectOrderForUpdate(Orders order) {
        this.selectedOrder = order;
        this.newStatus = order.getStatus();
    }

    public void confirmOrder(Orders order) {
        this.selectedOrder = order;
        this.newStatus = "CONFIRMED";
        updateOrderStatus();
    }

    public void shipOrder(Orders order) {
        this.selectedOrder = order;
        this.newStatus = "SHIPPING";
        updateOrderStatus();
    }

    public void completeOrder(Orders order) {
        this.selectedOrder = order;
        this.newStatus = "COMPLETED";
        // For COD orders, set payment status to PAID when completed
        if ("COD".equals(order.getPaymentMethod())) {
            order.setPaymentStatus("PAID");
        }
        updateOrderStatus();
    }

    public void selectOrderForCancel(Orders order) {
        this.selectedOrder = order;
        this.newStatus = "CANCELLED";
    }

    public List<OrderDetails> getOrderItems(Orders order) {
        return orderDetailsFacade.findByOrderID(order);
    }

    public entityclass.PaymentProof getPaymentProof(Orders order) {
        if (order != null) {
            return paymentProofFacade.findByOrderID(order);
        }
        return null;
    }

    public BigDecimal getOrderItemsTotal(Orders order) {
        List<OrderDetails> items = orderDetailsFacade.findByOrderID(order);
        BigDecimal total = BigDecimal.ZERO;
        for (OrderDetails item : items) {
            total = total.add(item.getTotalPrice());
        }
        return total;
    }

    public String getStatusStyle(String status) {
        if (status == null) return "bg-gray-100 text-gray-800";
        switch (status) {
            case "NEW": return "bg-blue-100 text-blue-800";
            case "CONFIRMED": return "bg-yellow-100 text-yellow-800";
            case "SHIPPING": return "bg-purple-100 text-purple-800";
            case "COMPLETED": return "bg-green-100 text-green-800";
            case "CANCELLED": return "bg-red-100 text-red-800";
            case "WAITING_CONFIRM": return "bg-orange-100 text-orange-800";
            default: return "bg-gray-100 text-gray-800";
        }
    }

    public String getStatusDisplay(String status) {
        if (status == null) return "Unknown";
        switch (status) {
            case "NEW": return "New";
            case "CONFIRMED": return "Confirmed";
            case "SHIPPING": return "Shipping";
            case "COMPLETED": return "Completed";
            case "CANCELLED": return "Cancelled";
            case "WAITING_CONFIRM": return "Waiting for Confirmation";
            default: return status;
        }
    }

    public String getPaymentStatusDisplay(String status) {
        if (status == null) return "Unknown";
        switch (status) {
            case "PAID": return "Paid";
            case "UNPAID": return "Unpaid";
            case "WAITING": return "Waiting";
            case "PENDING": return "Pending Confirmation";
            default: return status;
        }
    }

    public String getCustomerName(Orders order) {
        List<OrderDetails> orderDetails = getOrderItems(order);
        if (orderDetails != null && !orderDetails.isEmpty()) {
            String name = orderDetails.get(0).getCustomerName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        return "Guest";
    }

    public String getCustomerPhone(Orders order) {
        List<OrderDetails> orderDetails = getOrderItems(order);
        if (orderDetails != null && !orderDetails.isEmpty()) {
            String phone = orderDetails.get(0).getCustomerPhone();
            if (phone != null && !phone.isEmpty()) {
                return phone;
            }
        }
        return "N/A";
    }

    public String getCustomerAddress(Orders order) {
        List<OrderDetails> orderDetails = getOrderItems(order);
        if (orderDetails != null && !orderDetails.isEmpty()) {
            String address = orderDetails.get(0).getCustomerAddress();
            if (address != null && !address.isEmpty()) {
                return address;
            }
        }
        return "N/A";
    }

    public String getProductImage(OrderDetails item) {
        // First try to use the denormalized productImage field from OrderDetails
        if (item.getProductImage() != null && !item.getProductImage().isEmpty()) {
            return item.getProductImage();
        }

        // Fallback to productID relationship if productImage is empty
        if (item.getProductID() != null && item.getProductID().getProductImagesList() != null && !item.getProductID().getProductImagesList().isEmpty()) {
            return item.getProductID().getProductImagesList().get(0).getImageURL();
        }

        return null;
    }

    public void approvePaymentProof(Orders order) {
        if (order != null) {
            PaymentProof paymentProof = getPaymentProof(order);
            if (paymentProof != null) {
                paymentProof.setStatus("APPROVED");
                paymentProof.setVerifiedAt(new java.util.Date());
                // Set verifiedBy if you have admin user context
                paymentProofFacade.edit(paymentProof);

                // Update order payment status
                order.setPaymentStatus("PAID");
                ordersFacade.edit(order);

                loadAllOrders();
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Payment proof approved and payment status updated to PAID"));
            }
        }
    }

    public void rejectPaymentProof(Orders order) {
        if (order != null) {
            PaymentProof paymentProof = getPaymentProof(order);
            if (paymentProof != null) {
                paymentProof.setStatus("REJECTED");
                paymentProof.setVerifiedAt(new java.util.Date());
                // Set verifiedBy if you have admin user context
                paymentProofFacade.edit(paymentProof);

                // Update order status and payment status
                order.setStatus("WAITING_CONFIRM");
                order.setPaymentStatus("PENDING");
                ordersFacade.edit(order);

                loadAllOrders();
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Payment Rejected", "Payment proof rejected. Customer can resubmit."));
            }
        }
    }

    public void updateOrderStatus() {
        if (selectedOrder == null || newStatus == null) return;

        // Validate cancel reason when cancelling
        if ("CANCELLED".equals(newStatus)) {
            if (cancelReason == null || cancelReason.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Validation Error", "Cancel reason is required when cancelling an order"));
                return;
            }
        }

        try {
            // Nếu admin confirm → trừ stock
            if ("CONFIRMED".equals(newStatus)) {
                ordersFacade.confirmOrderAndDeductStock(
                        selectedOrder.getOrderID()
                );
            } else if ("CANCELLED".equals(newStatus)) {
                // Set cancel reason when cancelling
                selectedOrder.setStatus(newStatus);
                selectedOrder.setCancelReason(cancelReason.trim());
                ordersFacade.edit(selectedOrder);
            } else {
                selectedOrder.setStatus(newStatus);
                ordersFacade.edit(selectedOrder);
            }

            // Clear cancel reason after successful update
            cancelReason = null;

            loadAllOrders();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Success", "Order updated successfully"));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", e.getMessage()));
        }
    }


    public void onStatusChange() {
        filterOrders();
    }

    public void onSearchChange() {
        filterOrders();
    }

    // Getters and Setters
    public List<Orders> getAllOrders() {
        return allOrders;
    }

    public void setAllOrders(List<Orders> allOrders) {
        this.allOrders = allOrders;
    }

    public List<Orders> getFilteredOrders() {
        return filteredOrders;
    }

    public void setFilteredOrders(List<Orders> filteredOrders) {
        this.filteredOrders = filteredOrders;
    }

    public String getSelectedStatus() {
        return selectedStatus;
    }

    public void setSelectedStatus(String selectedStatus) {
        this.selectedStatus = selectedStatus;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Orders getSelectedOrder() {
        return selectedOrder;
    }

    public void setSelectedOrder(Orders selectedOrder) {
        this.selectedOrder = selectedOrder;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
    
