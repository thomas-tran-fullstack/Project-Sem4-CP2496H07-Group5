package sessionbeans;

import entityclass.Addresses;
import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Stateless
public class AddressesFacade implements AddressesFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    public void create(Addresses address) { em.persist(address); }

    @Override
    public void edit(Addresses address) { em.merge(address); }

    @Override
    public void remove(Addresses address) { em.remove(em.merge(address)); }

    @Override
    public Addresses find(Object id) { return em.find(Addresses.class, id); }

    @Override
    public List<Addresses> findAll() { return em.createQuery("SELECT a FROM Addresses a", Addresses.class).getResultList(); }

    @Override
    public List<Addresses> findByCustomer(Integer customerId) {
        TypedQuery<Addresses> q = em.createQuery("SELECT a FROM Addresses a WHERE a.customerID.customerID = :cid ORDER BY a.isDefault DESC, a.createdAt DESC", Addresses.class);
        q.setParameter("cid", customerId);
        return q.getResultList();
    }
}
