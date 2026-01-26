/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.WishlistItems;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface WishlistItemsFacadeLocal {

    void create(WishlistItems wishlistItems);

    void edit(WishlistItems wishlistItems);

    void remove(WishlistItems wishlistItems);

    WishlistItems find(Object id);

    List<WishlistItems> findAll();

    List<WishlistItems> findRange(int[] range);

    int count();

    List<WishlistItems> findByWishlistID(Integer wishlistID);

    WishlistItems findByWishlistIDAndProductID(Integer wishlistID, Integer productID);

    boolean deleteItemFromWishlist(Integer wishlistID, Integer productID);

    int getWishlistItemCount(Integer wishlistID);

    Double getTotalWishlistPrice(Integer wishlistID);

    int deleteOutOfStockItems(Integer wishlistID);
}
