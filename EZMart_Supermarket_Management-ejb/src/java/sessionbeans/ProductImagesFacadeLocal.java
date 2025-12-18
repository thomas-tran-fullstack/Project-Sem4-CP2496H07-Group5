/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.ProductImages;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface ProductImagesFacadeLocal {

    void create(ProductImages productImages);

    void edit(ProductImages productImages);

    void remove(ProductImages productImages);

    ProductImages find(Object id);

    List<ProductImages> findAll();

    List<ProductImages> findRange(int[] range);

    int count();
    
}
