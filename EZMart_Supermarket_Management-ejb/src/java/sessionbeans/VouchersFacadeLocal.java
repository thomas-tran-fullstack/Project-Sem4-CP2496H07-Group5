/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.Vouchers;
import java.util.List;
import jakarta.ejb.Local;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface VouchersFacadeLocal {

    void create(Vouchers vouchers);

    void edit(Vouchers vouchers);

    void remove(Vouchers vouchers);

    Vouchers find(Object id);

    List<Vouchers> findAll();

    List<Vouchers> findRange(int[] range);

    int count();

    List<Vouchers> findByCustomerID(Object customerID);

    List<Vouchers> findByOfferID(Object offerID);

    List<Vouchers> findUnusedByCustomer(Object customerID);

    Vouchers findByVoucherCode(String voucherCode);
}
