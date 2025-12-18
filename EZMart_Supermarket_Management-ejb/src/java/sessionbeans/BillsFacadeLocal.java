/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.Bills;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface BillsFacadeLocal {

    void create(Bills bills);

    void edit(Bills bills);

    void remove(Bills bills);

    Bills find(Object id);

    List<Bills> findAll();

    List<Bills> findRange(int[] range);

    int count();
    
}
