/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.Reviews;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface ReviewsFacadeLocal {

    void create(Reviews reviews);

    void edit(Reviews reviews);

    void remove(Reviews reviews);

    Reviews find(Object id);

    List<Reviews> findAll();

    List<Reviews> findRange(int[] range);

    int count();
    
}
