package controllers;

import entityclass.Customers;
import entityclass.OrderDetails;
import entityclass.Orders;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import sessionbeans.OrderDetailsFacadeLocal;
import sessionbeans.OrdersFacadeLocal;

@Named("orderHistoryMB")
@RequestScoped
public class OrderHistoryMB implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private OrdersFacadeLocal ordersFacade;

    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;

    @Inject
    private AuthController auth;

    private List<Orders> orders;
    private List<Orders> filteredOrders;
    private String selectedStatus = "all";
    private String searchTerm = "";

    @PostConstruct
    public void init() {
        loadOrders();
    }

    public void loadOrders() {
        Customers customer = auth.getCurrentCustomer();
        if (customer != null) {
            orders = ordersFacade.findByCustomerID(customer);
        } else {
            orders = new ArrayList<>();
        }
        filterOrders();
    }

    public void filterOrders() {
        filteredOrders = new ArrayList<>();
        for (Orders order : orders) {
            boolean matchesStatus = "all".equals(selectedStatus) || 
                                   (order.getStatus() != null && order.getStatus().equals(selectedStatus));
            boolean matchesSearch = searchTerm.isEmpty() || 
                                   String.valueOf(order.getOrderID()).contains(searchTerm);
            if (matchesStatus && matchesSearch) {
                filteredOrders.add(order);
            }
        }
    }

    public List<OrderDetails> getOrderItems(Orders order) {
        return orderDetailsFacade.findByOrderID(order);
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

    public void onStatusChange() {
        filterOrders();
    }

    public void onSearchChange() {
        filterOrders();
    }

    // Getters and Setters
    public List<Orders> getOrders() {
        return orders;
    }

    public void setOrders(List<Orders> orders) {
        this.orders = orders;
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
}

