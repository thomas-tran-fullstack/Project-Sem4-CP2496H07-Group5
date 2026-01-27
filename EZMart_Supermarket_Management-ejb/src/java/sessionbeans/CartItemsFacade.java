package sessionbeans;

import entityclass.CartItems;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class CartItemsFacade extends AbstractFacade<CartItems> implements CartItemsFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CartItemsFacade() {
        super(CartItems.class);
    }

    @Override
    public List<CartItems> findByCartId(Integer cartId) {
        if (cartId == null) return java.util.Collections.emptyList();

        return em.createQuery(
                "SELECT ci FROM CartItems ci " +
                "JOIN FETCH ci.productID p " +
                "WHERE ci.cartID.cartID = :cartId " +
                "ORDER BY ci.cartItemID DESC",
                CartItems.class
        )
        .setParameter("cartId", cartId)
        .getResultList();
    }

    @Override
    public CartItems findByCartAndProduct(Integer cartId, Integer productId) {
        if (cartId == null || productId == null) return null;

        List<CartItems> list = em.createQuery(
                "SELECT ci FROM CartItems ci " +
                "WHERE ci.cartID.cartID = :cartId " +
                "AND ci.productID.productID = :productId",
                CartItems.class
        )
        .setParameter("cartId", cartId)
        .setParameter("productId", productId)
        .setMaxResults(1)
        .getResultList();

        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    @Override
    public int deleteByCartId(Integer cartId) {
        if (cartId == null) return 0;

        return em.createQuery(
                "DELETE FROM CartItems ci WHERE ci.cartID.cartID = :cartId"
        )
        .setParameter("cartId", cartId)
        .executeUpdate();
    }
}
