/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.Admins;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface AdminsFacadeLocal {

    void create(Admins admins);

    void edit(Admins admins);

    void remove(Admins admins);

    Admins find(Object id);

    List<Admins> findAll();

    List<Admins> findRange(int[] range);

    int count();
    
}
