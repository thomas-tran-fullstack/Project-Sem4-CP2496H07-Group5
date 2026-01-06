package sessionbeans;

import entityclass.Addresses;
import java.util.List;
import jakarta.ejb.Local;

@Local
public interface AddressesFacadeLocal {
    void create(Addresses address);
    void edit(Addresses address);
    void remove(Addresses address);
    Addresses find(Object id);
    List<Addresses> findAll();
    List<Addresses> findByCustomer(Integer customerId);
}
