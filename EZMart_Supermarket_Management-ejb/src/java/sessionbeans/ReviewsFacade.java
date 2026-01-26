/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.Reviews;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Stateless
public class ReviewsFacade extends AbstractFacade<Reviews> implements ReviewsFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ReviewsFacade() {
        super(Reviews.class);
    }

    public List<Reviews> findPublishedReviewsByProductID(entityclass.Products product) {
        return em.createQuery("SELECT r FROM Reviews r WHERE r.productID = :product AND r.status = 'PUBLISHED'", Reviews.class)
                 .setParameter("product", product)
                 .getResultList();
    }

    public List<Reviews> findPendingReviews() {
        return em.createQuery("SELECT r FROM Reviews r WHERE r.status = 'PENDING'", Reviews.class)
                 .getResultList();
    }

    public List<Reviews> findFlaggedReviews() {
        return em.createQuery("SELECT r FROM Reviews r WHERE r.isFlagged = true", Reviews.class)
                 .getResultList();
    }

    public List<Reviews> findByProductIDAndCustomerID(entityclass.Products product, entityclass.Customers customer) {
        return em.createQuery("SELECT r FROM Reviews r WHERE r.productID = :product AND r.customerID = :customer", Reviews.class)
                 .setParameter("product", product)
                 .setParameter("customer", customer)
                 .getResultList();
    }

    @Override
    public List<Reviews> findAllReviews() {
        return em.createQuery("SELECT r FROM Reviews r ORDER BY r.createdAt DESC", Reviews.class)
                 .getResultList();
    }

    @Override
    public List<Reviews> findPublishedReviews() {
        return em.createQuery("SELECT r FROM Reviews r WHERE r.status = 'PUBLISHED' ORDER BY r.createdAt DESC", Reviews.class)
                 .getResultList();
    }

    @Override
    public List<Reviews> findByStatus(String status) {
        return em.createQuery("SELECT r FROM Reviews r WHERE r.status = :status ORDER BY r.createdAt DESC", Reviews.class)
                 .setParameter("status", status)
                 .getResultList();
    }

    @Override
    public int countByStatus(String status) {
        return em.createQuery("SELECT COUNT(r) FROM Reviews r WHERE r.status = :status", Long.class)
                 .setParameter("status", status)
                 .getSingleResult()
                 .intValue();
    }

    @Override
    public List<Reviews> searchReviews(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAllReviews();
        }
        String searchPattern = "%" + searchTerm.trim().toLowerCase() + "%";
        return em.createQuery(
            "SELECT r FROM Reviews r WHERE " +
            "LOWER(r.comment) LIKE :search OR " +
            "LOWER(r.customerID.firstName) LIKE :search OR " +
            "LOWER(r.customerID.lastName) LIKE :search OR " +
            "LOWER(r.productID.productName) LIKE :search " +
            "ORDER BY r.createdAt DESC", Reviews.class)
                 .setParameter("search", searchPattern)
                 .getResultList();
    }

}
