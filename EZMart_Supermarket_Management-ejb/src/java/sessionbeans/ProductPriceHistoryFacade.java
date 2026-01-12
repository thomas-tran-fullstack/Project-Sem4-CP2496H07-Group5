/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.ProductPriceHistory;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.Date;
import java.util.List;

@Stateless
public class ProductPriceHistoryFacade extends AbstractFacade<ProductPriceHistory> implements ProductPriceHistoryFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ProductPriceHistoryFacade() {
        super(ProductPriceHistory.class);
    }

    @Override
    public List<ProductPriceHistory> findByProductID(Object productID) {
        Query query = em.createNamedQuery("ProductPriceHistory.findByProductID");
        query.setParameter("productID", productID);
        return query.getResultList();
    }

    @Override
    public List<ProductPriceHistory> findByDateRange(Date startDate, Date endDate) {
        Query query = em.createNamedQuery("ProductPriceHistory.findByDateRange");
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.getResultList();
    }
}
