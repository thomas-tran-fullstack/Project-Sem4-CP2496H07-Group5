/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.Orders;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
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
    
}
