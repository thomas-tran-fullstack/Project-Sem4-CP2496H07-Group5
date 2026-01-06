/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.Users;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface UsersFacadeLocal {

    void create(Users users);

    void edit(Users users);

    void remove(Users users);

    Users find(Object id);

    List<Users> findAll();

    List<Users> findRange(int[] range);

    int count();
    
    Users findByUsername(String username);
    Users findByEmail(String email);
    Users findByIdentifierAndPassword(String identifier, String passwordHash);
    Users findByUsernameAndPassword(String username, String passwordHash);
    
}
