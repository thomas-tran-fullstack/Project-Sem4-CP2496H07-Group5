package sessionbeans;

import entityclass.Staffs;
import jakarta.ejb.Local;
import java.util.List;

/**
 * Local interface for StaffsFacade
 */
@Local
public interface StaffsFacadeLocal {

    void create(Staffs staffs);

    void edit(Staffs staffs);

    void remove(Staffs staffs);

    Staffs find(Object id);

    List<Staffs> findAll();

    List<Staffs> findRange(int[] range);

    int count();

    Staffs findByUserID(Object userID);

    List<Staffs> findByStatus(String status);

    List<Staffs> findActiveStaffs();

    /**
     * Count the number of active staff members
     */
    long countActiveStaff();
}
