package controllers;

import entityclass.Customers;
import entityclass.OrderDetails;
import entityclass.Orders;
import entityclass.PaymentProof;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
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

@Named("adminOrderMB")
@RequestScoped
public class AdminOrderMB implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private OrdersFacadeLocal ordersFacade;

    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;

    @EJB
    private PaymentProofFacadeLocal paymentProofFacade;

    private List<Orders> allOrders;
    private List<Orders> filteredOrders;
    private String selectedStatus = "all";
    private String searchTerm = "";
    private Orders selectedOrder;
    private String newStatus;

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
            case "PROCESSING": return "bg-yellow-100 text-yellow-800";
            case "SHIPPED": return "bg-purple-100 text-purple-800";
            case "DELIVERED": return "bg-green-100 text-green-800";
            case "CANCELLED": return "bg-red-100 text-red-800";
            case "WAITING_CONFIRM": return "bg-orange-100 text-orange-800";
            default: return "bg-gray-100 text-gray-800";
        }
    }

    public String getStatusDisplay(String status) {
        if (status == null) return "Unknown";
        switch (status) {
            case "NEW": return "New";
            case "PROCESSING": return "Processing";
            case "SHIPPED": return "Shipped";
            case "DELIVERED": return "Delivered";
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
        if (order.getCustomerID() != null) {
            Customers customer = order.getCustomerID();
            StringBuilder nameBuilder = new StringBuilder();
            if (customer.getFirstName() != null && !customer.getFirstName().isEmpty()) {
                nameBuilder.append(customer.getFirstName());
            }
            if (customer.getMiddleName() != null && !customer.getMiddleName().isEmpty()) {
                if (nameBuilder.length() > 0) nameBuilder.append(" ");
                nameBuilder.append(customer.getMiddleName());
            }
            if (customer.getLastName() != null && !customer.getLastName().isEmpty()) {
                if (nameBuilder.length() > 0) nameBuilder.append(" ");
                nameBuilder.append(customer.getLastName());
            }
            if (nameBuilder.length() > 0) {
                return nameBuilder.toString();
            }
            // Fallback to username from Users entity
            if (customer.getUserID() != null && customer.getUserID().getUsername() != null) {
                return customer.getUserID().getUsername();
            }
        }
        return "Guest";
    }

    public void updateOrderStatus() {
        if (selectedOrder != null && newStatus != null) {
            selectedOrder.setStatus(newStatus);
            ordersFacade.edit(selectedOrder);
            loadAllOrders();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Order status updated successfully"));
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
}

