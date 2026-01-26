package sessionbeans;

import entityclass.Carts;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface CartsFacadeLocal {

    void create(Carts carts);

    void edit(Carts carts);

    void remove(Carts carts);

    Carts find(Object id);

    List<Carts> findAll();

    List<Carts> findRange(int[] range);

    int count();

    List<Carts> findByCustomerID(Integer customerID);

    Carts findLatestByCustomer(Integer customerID);
}
