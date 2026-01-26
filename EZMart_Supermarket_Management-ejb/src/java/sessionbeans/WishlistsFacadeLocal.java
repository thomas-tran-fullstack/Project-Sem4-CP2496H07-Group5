/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.Wishlists;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface WishlistsFacadeLocal {

    void create(Wishlists wishlists);

    void edit(Wishlists wishlists);

    void remove(Wishlists wishlists);

    Wishlists find(Object id);

    List<Wishlists> findAll();

    List<Wishlists> findRange(int[] range);

    int count();

    Wishlists findByCustomerID(Integer customerID);

    List<Wishlists> findAllByCustomerID(Integer customerID);
}
