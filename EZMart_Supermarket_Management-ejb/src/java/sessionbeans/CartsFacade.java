package sessionbeans;

import entityclass.Carts;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

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
        return em.createQuery(
                "SELECT c FROM Carts c WHERE c.customerID.customerID = :customerID ORDER BY c.createdAt DESC, c.cartID DESC",
                Carts.class
        )
        .setParameter("customerID", customerID)
        .getResultList();
    }

    @Override
    public Carts findLatestByCustomer(Integer customerID) {
        List<Carts> list = em.createQuery(
                "SELECT c FROM Carts c WHERE c.customerID.customerID = :customerID ORDER BY c.createdAt DESC, c.cartID DESC",
                Carts.class
        )
        .setParameter("customerID", customerID)
        .setMaxResults(1)
        .getResultList();

        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }
}
