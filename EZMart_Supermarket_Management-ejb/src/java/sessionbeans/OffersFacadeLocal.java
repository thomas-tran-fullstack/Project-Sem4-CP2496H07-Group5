/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.Offers;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface OffersFacadeLocal {

    void create(Offers offers);

    void edit(Offers offers);

    void remove(Offers offers);

    Offers find(Object id);

    List<Offers> findAll();

    List<Offers> findRange(int[] range);

    int count();

    List<Offers> findByOfferName(String offerName);

}
