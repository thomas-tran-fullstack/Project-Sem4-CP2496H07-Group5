/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.ProductOffers;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface ProductOffersFacadeLocal {

    void create(ProductOffers productOffers);

    void edit(ProductOffers productOffers);

    void remove(ProductOffers productOffers);

    ProductOffers find(Object id);

    List<ProductOffers> findAll();

    List<ProductOffers> findRange(int[] range);

    int count();
    
}
