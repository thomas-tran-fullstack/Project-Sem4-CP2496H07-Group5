/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.CreditCards;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface CreditCardsFacadeLocal {

    void create(CreditCards creditCards);

    void edit(CreditCards creditCards);

    void remove(CreditCards creditCards);

    CreditCards find(Object id);

    List<CreditCards> findAll();

    List<CreditCards> findRange(int[] range);

    int count();
    
    List<CreditCards> findByCustomer(Integer customerId);}