/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.PaymentProof;
import java.util.List;
import jakarta.ejb.Local;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface PaymentProofFacadeLocal {

    void create(PaymentProof paymentProof);

    void edit(PaymentProof paymentProof);

    void remove(PaymentProof paymentProof);

    PaymentProof find(Object id);

    List<PaymentProof> findAll();

    List<PaymentProof> findRange(int[] range);

    int count();
    
    PaymentProof findByOrderID(entityclass.Orders orderID);

}
