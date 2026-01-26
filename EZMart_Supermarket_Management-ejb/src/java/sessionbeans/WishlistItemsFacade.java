/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.WishlistItems;
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
public class WishlistItemsFacade extends AbstractFacade<WishlistItems> implements WishlistItemsFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public WishlistItemsFacade() {
        super(WishlistItems.class);
    }

    public List<WishlistItems> findByWishlistID(Integer wishlistID) {
        try {
            TypedQuery<WishlistItems> query = em.createNamedQuery("WishlistItems.findByWishlistID", WishlistItems.class);
            query.setParameter("wishlistID", wishlistID);
            return query.getResultList();
        } catch (Exception e) {
            return null;
        }
    }

    public WishlistItems findByWishlistIDAndProductID(Integer wishlistID, Integer productID) {
        try {
            TypedQuery<WishlistItems> query = em.createNamedQuery("WishlistItems.findByWishlistIDAndProductID", WishlistItems.class);
            query.setParameter("wishlistID", wishlistID);
            query.setParameter("productID", productID);
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean deleteItemFromWishlist(Integer wishlistID, Integer productID) {
        try {
            WishlistItems item = findByWishlistIDAndProductID(wishlistID, productID);
            if (item != null) {
                remove(item);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy số lượng sản phẩm trong wishlist
     */
    public int getWishlistItemCount(Integer wishlistID) {
        try {
            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(w) FROM WishlistItems w WHERE w.wishlistID.wishlistID = :wishlistID",
                    Long.class
            );
            query.setParameter("wishlistID", wishlistID);
            return query.getSingleResult().intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Tính tổng giá tiền wishlist
     */
    public Double getTotalWishlistPrice(Integer wishlistID) {
        try {
            TypedQuery<Double> query = em.createQuery(
                    "SELECT SUM(p.unitPrice) FROM WishlistItems w " +
                    "JOIN w.productID p WHERE w.wishlistID.wishlistID = :wishlistID",
                    Double.class
            );
            query.setParameter("wishlistID", wishlistID);
            Double result = query.getSingleResult();
            return result != null ? result : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Xóa các item hết hàng
     */
    public int deleteOutOfStockItems(Integer wishlistID) {
        try {
            int count = em.createQuery(
                    "DELETE FROM WishlistItems w WHERE w.wishlistID.wishlistID = :wishlistID " +
                    "AND w.productID.stockQuantity <= 0"
            ).setParameter("wishlistID", wishlistID).executeUpdate();
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Cập nhật số lượng sản phẩm trong wishlist
     */
    public void updateQuantity(Integer wishlistItemID, Integer quantity) {
        try {
            WishlistItems item = find(wishlistItemID);
            if (item != null && quantity != null && quantity > 0) {
                item.setQuantity(quantity);
                edit(item);
            }
        } catch (Exception e) {
            // Handle error
        }
    }

    /**
     * Cập nhật ghi chú sản phẩm trong wishlist
     */
    public void updateNote(Integer wishlistItemID, String note) {
        try {
            WishlistItems item = find(wishlistItemID);
            if (item != null) {
                item.setNote(note);
                edit(item);
            }
        } catch (Exception e) {
            // Handle error
        }
    }

    /**
     * Thêm sản phẩm hoặc cập nhật số lượng nếu đã tồn tại
     */
    public WishlistItems addOrUpdateItem(Integer wishlistID, Integer productID, Integer quantity, String note) {
        try {
            WishlistItems existingItem = findByWishlistIDAndProductID(wishlistID, productID);
            if (existingItem != null) {
                // Cập nhật số lượng
                if (quantity != null && quantity > 0) {
                    existingItem.setQuantity(existingItem.getQuantity() + quantity);
                }
                if (note != null && !note.isEmpty()) {
                    existingItem.setNote(note);
                }
                existingItem.setAddedAt(new java.util.Date());
                edit(existingItem);
                return existingItem;
            } else {
                // Tạo mục mới
                WishlistItems newItem = new WishlistItems();
                // Set wishlistID - you'll need to set this from the caller
                // newItem.setWishlistID(wishlist);
                // newItem.setProductID(product);
                newItem.setQuantity(quantity != null && quantity > 0 ? quantity : 1);
                newItem.setNote(note);
                newItem.setAddedAt(new java.util.Date());
                create(newItem);
                return newItem;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
