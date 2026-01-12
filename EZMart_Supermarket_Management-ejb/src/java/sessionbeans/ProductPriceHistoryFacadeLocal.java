/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.ProductPriceHistory;
import java.util.Date;
import java.util.List;
import jakarta.ejb.Local;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface ProductPriceHistoryFacadeLocal {

    void create(ProductPriceHistory productPriceHistory);

    void edit(ProductPriceHistory productPriceHistory);

    void remove(ProductPriceHistory productPriceHistory);

    ProductPriceHistory find(Object id);

    List<ProductPriceHistory> findAll();

    List<ProductPriceHistory> findRange(int[] range);

    int count();

    List<ProductPriceHistory> findByProductID(Object productID);

    List<ProductPriceHistory> findByDateRange(Date startDate, Date endDate);
}
