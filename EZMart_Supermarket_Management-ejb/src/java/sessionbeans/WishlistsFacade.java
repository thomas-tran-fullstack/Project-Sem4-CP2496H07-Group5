/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.Wishlists;
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
public class WishlistsFacade extends AbstractFacade<Wishlists> implements WishlistsFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public WishlistsFacade() {
        super(Wishlists.class);
    }

    public Wishlists findByCustomerID(Integer customerID) {
        try {
            TypedQuery<Wishlists> query = em.createNamedQuery("Wishlists.findByCustomerIDAndDefault", Wishlists.class);
            query.setParameter("customerID", customerID);
            return query.getSingleResult();
        } catch (Exception e) {
            // If default wishlist not found, try to find any wishlist
            try {
                TypedQuery<Wishlists> query = em.createNamedQuery("Wishlists.findByCustomerID", Wishlists.class);
                query.setParameter("customerID", customerID);
                List<Wishlists> results = query.getResultList();
                return results != null && !results.isEmpty() ? results.get(0) : null;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public List<Wishlists> findAllByCustomerID(Integer customerID) {
        try {
            TypedQuery<Wishlists> query = em.createNamedQuery("Wishlists.findByCustomerID", Wishlists.class);
            query.setParameter("customerID", customerID);
            return query.getResultList();
        } catch (Exception e) {
            return null;
        }
    }

    public Wishlists findDefaultWishlistByCustomerID(Integer customerID) {
        try {
            TypedQuery<Wishlists> query = em.createNamedQuery("Wishlists.findByCustomerIDAndDefault", Wishlists.class);
            query.setParameter("customerID", customerID);
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
}
