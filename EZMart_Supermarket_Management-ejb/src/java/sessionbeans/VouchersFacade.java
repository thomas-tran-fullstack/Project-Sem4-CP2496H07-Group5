/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.Vouchers;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Stateless
public class VouchersFacade extends AbstractFacade<Vouchers> implements VouchersFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public VouchersFacade() {
        super(Vouchers.class);
    }

    @Override
    public List<Vouchers> findByCustomerID(Object customerID) {
        Query query = em.createNamedQuery("Vouchers.findByCustomerID");
        query.setParameter("customerID", customerID);
        return query.getResultList();
    }

    @Override
    public List<Vouchers> findByOfferID(Object offerID) {
        Query query = em.createNamedQuery("Vouchers.findByOfferID");
        query.setParameter("offerID", offerID);
        return query.getResultList();
    }

    @Override
    public List<Vouchers> findUnusedByCustomer(Object customerID) {
        Query query = em.createNamedQuery("Vouchers.findUnusedByCustomer");
        query.setParameter("customerID", customerID);
        query.setParameter("currentDate", new java.util.Date());
        return query.getResultList();
    }

    @Override
    public Vouchers findByVoucherCode(String voucherCode) {
        Query query = em.createNamedQuery("Vouchers.findByVoucherCode");
        query.setParameter("voucherCode", voucherCode);
        List<Vouchers> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }
}
