/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.PaymentProof;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author TRUONG LAM
 */
@Stateless
public class PaymentProofFacade extends AbstractFacade<PaymentProof> implements PaymentProofFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PaymentProofFacade() {
        super(PaymentProof.class);
    }

    @Override
    public PaymentProof findByOrderID(entityclass.Orders orderID) {
        try {
            return em.createQuery("SELECT p FROM PaymentProof p WHERE p.orderID = :orderID", PaymentProof.class)
                    .setParameter("orderID", orderID)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

}
