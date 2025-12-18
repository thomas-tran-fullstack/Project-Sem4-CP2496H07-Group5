/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.Brands;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface BrandsFacadeLocal {

    void create(Brands brands);

    void edit(Brands brands);

    void remove(Brands brands);

    Brands find(Object id);

    List<Brands> findAll();

    List<Brands> findRange(int[] range);

    int count();
    
}
