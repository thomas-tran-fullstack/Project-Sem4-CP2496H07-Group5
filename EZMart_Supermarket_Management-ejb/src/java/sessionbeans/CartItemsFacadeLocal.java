/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.CartItems;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface CartItemsFacadeLocal {

    void create(CartItems cartItems);

    void edit(CartItems cartItems);

    void remove(CartItems cartItems);

    CartItems find(Object id);

    List<CartItems> findAll();

    List<CartItems> findRange(int[] range);

    int count();
    
}
