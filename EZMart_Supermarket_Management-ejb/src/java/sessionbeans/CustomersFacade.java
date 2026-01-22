/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.Customers;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author TRUONG LAM
 */
@Stateless
public class CustomersFacade extends AbstractFacade<Customers> implements CustomersFacadeLocal {
    @Override
    public void edit(Customers customer) {
        super.edit(customer);
            Customers managed = em.merge(customer);
            em.flush();
            em.refresh(managed);
    }

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CustomersFacade() {
        super(Customers.class);
    }

    @Override
    public Customers findByUserID(Integer userID) {
        try {
            return em.createQuery("SELECT c FROM Customers c WHERE c.userID.userID = :userID", Customers.class)
                    .setParameter("userID", userID)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Customers findByMobilePhone(String mobilePhone) {
        try {
            return em.createQuery("SELECT c FROM Customers c WHERE c.mobilePhone = :mobilePhone", Customers.class)
                    .setParameter("mobilePhone", mobilePhone)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    // Update avatarUrl for a customer
    public void updateAvatar(Integer customerId, String avatarUrl) {
        Customers c = em.find(Customers.class, customerId);
        if (c != null) {
            c.setAvatarUrl(avatarUrl);
            em.merge(c);
        }
    }

}
