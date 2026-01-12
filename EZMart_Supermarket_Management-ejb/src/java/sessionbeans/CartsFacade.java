/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.Carts;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Stateless
public class CartsFacade extends AbstractFacade<Carts> implements CartsFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CartsFacade() {
        super(Carts.class);
    }

    @Override
    public List<Carts> findByCustomerID(Integer customerID) {
        return em.createQuery("SELECT c FROM Carts c WHERE c.customerID.customerID = :customerID", Carts.class)
                .setParameter("customerID", customerID)
                .getResultList();
    }
    
}
