/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.Orders;
import entityclass.OrderDetails;
import entityclass.Products;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Stateless
public class OrdersFacade extends AbstractFacade<Orders> implements OrdersFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public OrdersFacade() {
        super(Orders.class);
    }

    public List<Orders> findByCustomerIDAndStatus(Integer customerId, String status) {
        return em.createNamedQuery("Orders.findByCustomerIDAndStatus", Orders.class)
                .setParameter("customerID", customerId)
                .setParameter("status", status)
                .getResultList();
    }
    
    @Override
    public List<Orders> findByCustomerID(entityclass.Customers customer) {
        if (customer == null) {
            return em.createQuery("SELECT o FROM Orders o ORDER BY o.orderDate DESC", Orders.class)
                    .getResultList();
        }
        return em.createQuery("SELECT o FROM Orders o WHERE o.customerID = :customer ORDER BY o.orderDate DESC", Orders.class)
                .setParameter("customer", customer)
                .getResultList();
    }

    @Override
    public Orders findOrderWithDetails(Integer orderId) {
        try {
            return em.createQuery("SELECT o FROM Orders o LEFT JOIN FETCH o.orderDetailsList WHERE o.orderID = :orderId", Orders.class)
                    .setParameter("orderId", orderId)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

     @Override
    public void confirmOrderAndDeductStock(Integer orderId) {

        Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
        if (order == null) {
            throw new IllegalStateException("Order not found");
        }

        // tránh trừ 2 lần
        if (Boolean.TRUE.equals(order.getStockDeducted())) {
            order.setStatus("CONFIRMED");
            em.merge(order);
            return;
        }

        List<OrderDetails> details = em.createQuery(
            "SELECT od FROM OrderDetails od "
          + "JOIN FETCH od.productID "
          + "WHERE od.orderID.orderID = :oid", OrderDetails.class)
            .setParameter("oid", orderId)
            .getResultList();

        // kiểm tra & trừ stock
        for (OrderDetails item : details) {
            Products product = em.find(
                    Products.class,
                    item.getProductID().getProductID(),
                    LockModeType.PESSIMISTIC_WRITE
            );

            int required = item.getQuantity();
            int available = product.getStockQuantity();

            if (available < required) {
                throw new IllegalStateException(
                        "Insufficient stock for product: " + product.getProductName()
                );
            }

            product.setStockQuantity(available - required);
            em.merge(product);
        }

        order.setStatus("CONFIRMED");
        order.setStockDeducted(true);
        em.merge(order);
    }
}
