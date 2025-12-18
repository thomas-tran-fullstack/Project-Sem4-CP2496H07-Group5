/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.Stores;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface StoresFacadeLocal {

    void create(Stores stores);

    void edit(Stores stores);

    void remove(Stores stores);

    Stores find(Object id);

    List<Stores> findAll();

    List<Stores> findRange(int[] range);

    int count();
    
}
