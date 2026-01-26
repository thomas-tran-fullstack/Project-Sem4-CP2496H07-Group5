/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.Feedbacks;
import entityclass.Customers;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

/**
 *
 * @author EZMart
 */
@Stateless
public class FeedbacksFacade extends AbstractFacade<Feedbacks> implements FeedbacksFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public FeedbacksFacade() {
        super(Feedbacks.class);
    }

    @Override
    public List<Feedbacks> findByCustomerID(Customers customer) {
        Query query = em.createNamedQuery("Feedbacks.findByCustomerID");
        query.setParameter("customerID", customer);
        return query.getResultList();
    }

    @Override
    public List<Feedbacks> findByStatus(String status) {
        Query query = em.createNamedQuery("Feedbacks.findByStatus");
        query.setParameter("status", status);
        return query.getResultList();
    }

    @Override
    public List<Feedbacks> findByFeedbackType(String feedbackType) {
        Query query = em.createNamedQuery("Feedbacks.findByFeedbackType");
        query.setParameter("feedbackType", feedbackType);
        return query.getResultList();
    }

}
