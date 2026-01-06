/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.CreditCards;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Stateless
public class CreditCardsFacade extends AbstractFacade<CreditCards> implements CreditCardsFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CreditCardsFacade() {
        super(CreditCards.class);
    }
    
    @Override
    public List<CreditCards> findByCustomer(Integer customerId) {
        // Order by IsDefault first, then by CardID (newest first) since createdAt is not present on entity
        TypedQuery<CreditCards> q = em.createQuery("SELECT c FROM CreditCards c WHERE c.customerID.customerID = :cid ORDER BY c.isDefault DESC, c.cardID DESC", CreditCards.class);
        q.setParameter("cid", customerId);
        return q.getResultList();
    }
}
