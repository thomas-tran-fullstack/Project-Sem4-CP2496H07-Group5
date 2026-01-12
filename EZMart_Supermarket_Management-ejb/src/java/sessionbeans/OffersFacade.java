/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.Offers;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Stateless
public class OffersFacade extends AbstractFacade<Offers> implements OffersFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public OffersFacade() {
        super(Offers.class);
    }

    @Override
    public List<Offers> findByOfferName(String offerName) {
        return em.createQuery("SELECT o FROM Offers o WHERE o.offerName LIKE :offerName", Offers.class)
                .setParameter("offerName", "%" + offerName + "%")
                .getResultList();
    }

}
