package sessionbeans;

import entityclass.Staffs;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

/**
 * Session Bean for Staffs - provides database operations for staff members
 */
@Stateless
public class StaffsFacade extends AbstractFacade<Staffs> implements StaffsFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    public StaffsFacade() {
        super(Staffs.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public Staffs findByUserID(Object userID) {
        try {
            Query query = em.createNamedQuery("Staffs.findByUserID");
            query.setParameter("userID", userID);
            List<Staffs> results = query.getResultList();
            if (results != null && !results.isEmpty()) {
                return results.get(0);
            }
        } catch (Exception e) {
            System.out.println("StaffsFacade.findByUserID: Error - " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Staffs> findByStatus(String status) {
        try {
            Query query = em.createNamedQuery("Staffs.findByStatus");
            query.setParameter("status", status);
            return query.getResultList();
        } catch (Exception e) {
            System.out.println("StaffsFacade.findByStatus: Error - " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<Staffs> findActiveStaffs() {
        try {
            Query query = em.createNamedQuery("Staffs.findByStatus");
            query.setParameter("status", "active");
            return query.getResultList();
        } catch (Exception e) {
            System.out.println("StaffsFacade.findActiveStaffs: Error - " + e.getMessage());
            return null;
        }
    }

    /**
     * Count the number of active staff members
     */
    public long countActiveStaff() {
        try {
            return em.createQuery(
                "SELECT COUNT(s) FROM Staffs s WHERE s.status = 'ACTIVE'",
                Long.class
            ).getSingleResult();
        } catch (Exception e) {
            System.out.println("StaffsFacade.countActiveStaff: Error - " + e.getMessage());
            return 0;
        }
    }
}
