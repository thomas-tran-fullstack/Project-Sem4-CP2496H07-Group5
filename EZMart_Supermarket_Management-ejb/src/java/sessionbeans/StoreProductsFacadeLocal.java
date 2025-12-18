/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.StoreProducts;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface StoreProductsFacadeLocal {

    void create(StoreProducts storeProducts);

    void edit(StoreProducts storeProducts);

    void remove(StoreProducts storeProducts);

    StoreProducts find(Object id);

    List<StoreProducts> findAll();

    List<StoreProducts> findRange(int[] range);

    int count();
    
}
