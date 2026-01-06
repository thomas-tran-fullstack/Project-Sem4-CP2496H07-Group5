/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.Carts;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface CartsFacadeLocal {

    void create(Carts carts);

    void edit(Carts carts);

    void remove(Carts carts);

    Carts find(Object id);

    List<Carts> findAll();

    List<Carts> findRange(int[] range);

    int count();

    List<Carts> findByCustomerID(Integer customerID);

}
