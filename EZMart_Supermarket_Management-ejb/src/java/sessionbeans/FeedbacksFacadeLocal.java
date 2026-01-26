/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.Feedbacks;
import entityclass.Customers;
import java.util.List;
import jakarta.ejb.Local;

/**
 *
 * @author EZMart
 */
@Local
public interface FeedbacksFacadeLocal {

    void create(Feedbacks feedbacks);

    void edit(Feedbacks feedbacks);

    void remove(Feedbacks feedbacks);

    Feedbacks find(Object id);

    List<Feedbacks> findAll();

    List<Feedbacks> findRange(int[] range);

    int count();

    List<Feedbacks> findByCustomerID(Customers customer);

    List<Feedbacks> findByStatus(String status);

    List<Feedbacks> findByFeedbackType(String feedbackType);

}
