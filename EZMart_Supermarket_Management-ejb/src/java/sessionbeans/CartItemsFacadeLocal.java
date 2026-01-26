package sessionbeans;

import entityclass.CartItems;
import java.util.List;
import jakarta.ejb.Local;


@Local
public interface CartItemsFacadeLocal {

    void create(CartItems cartItems);

    void edit(CartItems cartItems);

    void remove(CartItems cartItems);

    CartItems find(Object id);

    List<CartItems> findAll();

    List<CartItems> findRange(int[] range);

    int count();

    List<CartItems> findByCartId(Integer cartId);

    CartItems findByCartAndProduct(Integer cartId, Integer productId);

    int deleteByCartId(Integer cartId);
}
