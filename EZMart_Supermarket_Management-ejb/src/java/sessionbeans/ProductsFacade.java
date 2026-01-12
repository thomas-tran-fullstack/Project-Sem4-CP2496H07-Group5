/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.Products;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Stateless
public class ProductsFacade extends AbstractFacade<Products> implements ProductsFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ProductsFacade() {
        super(Products.class);
    }

    public List<Products> findByProductName(String productName) {
        return em.createNamedQuery("Products.findByProductName", Products.class)
                .setParameter("productName", "%" + productName + "%")
                .getResultList();
    }

    @Override
    public List<Products> findByCategory(Integer categoryId) {
        return em.createNamedQuery("Products.findByCategoryID", Products.class)
                .setParameter("categoryID", categoryId)
                .getResultList();
    }

    @Override
    public List<Products> findByBrand(Integer brandId) {
        return em.createNamedQuery("Products.findByBrandID", Products.class)
                .setParameter("brandID", brandId)
                .getResultList();
    }

    @Override
    public List<Products> findByPriceRange(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        return em.createNamedQuery("Products.findByPriceRange", Products.class)
                .setParameter("minPrice", minPrice)
                .setParameter("maxPrice", maxPrice)
                .getResultList();
    }

    @Override
    public List<Products> findFiltered(String searchTerm, Integer categoryId, Integer brandId, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        StringBuilder queryStr = new StringBuilder("SELECT p FROM Products p WHERE 1=1");

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            queryStr.append(" AND LOWER(p.productName) LIKE LOWER(:searchTerm)");
        }
        if (categoryId != null) {
            queryStr.append(" AND p.categoryID.categoryID = :categoryId");
        }
        if (brandId != null) {
            queryStr.append(" AND p.brandID.brandID = :brandId");
        }
        if (minPrice != null) {
            queryStr.append(" AND p.unitPrice >= :minPrice");
        }
        if (maxPrice != null) {
            queryStr.append(" AND p.unitPrice <= :maxPrice");
        }

        jakarta.persistence.Query query = em.createQuery(queryStr.toString(), Products.class);

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            query.setParameter("searchTerm", "%" + searchTerm + "%");
        }
        if (categoryId != null) {
            query.setParameter("categoryId", categoryId);
        }
        if (brandId != null) {
            query.setParameter("brandId", brandId);
        }
        if (minPrice != null) {
            query.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }

        return query.getResultList();
    }

    public List<Products> findFiltered(String searchTerm, Integer categoryId, List<Integer> brandIds, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        StringBuilder queryStr = new StringBuilder("SELECT p FROM Products p WHERE 1=1");

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            queryStr.append(" AND LOWER(p.productName) LIKE LOWER(:searchTerm)");
        }
        if (categoryId != null) {
            queryStr.append(" AND p.categoryID.categoryID = :categoryId");
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            queryStr.append(" AND p.brandID.brandID IN :brandIds");
        }
        if (minPrice != null) {
            queryStr.append(" AND p.unitPrice >= :minPrice");
        }
        if (maxPrice != null) {
            queryStr.append(" AND p.unitPrice <= :maxPrice");
        }

        jakarta.persistence.Query query = em.createQuery(queryStr.toString(), Products.class);

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            query.setParameter("searchTerm", "%" + searchTerm + "%");
        }
        if (categoryId != null) {
            query.setParameter("categoryId", categoryId);
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            query.setParameter("brandIds", brandIds);
        }
        if (minPrice != null) {
            query.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }

        return query.getResultList();
    }

    public List<Products> findFiltered(String searchTerm, List<Integer> categoryIds, List<Integer> brandIds, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        StringBuilder queryStr = new StringBuilder("SELECT p FROM Products p WHERE 1=1");

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            queryStr.append(" AND LOWER(p.productName) LIKE LOWER(:searchTerm)");
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            queryStr.append(" AND p.categoryID.categoryID IN :categoryIds");
        }

        if (brandIds != null && !brandIds.isEmpty()) {
            queryStr.append(" AND p.brandID.brandID IN :brandIds");
        }

        if (minPrice != null) {
            queryStr.append(" AND p.unitPrice >= :minPrice");
        }

        if (maxPrice != null) {
            queryStr.append(" AND p.unitPrice <= :maxPrice");
        }

        jakarta.persistence.Query query = em.createQuery(queryStr.toString(), Products.class);

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            query.setParameter("searchTerm", "%" + searchTerm + "%");
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            query.setParameter("categoryIds", categoryIds);
        }

        if (brandIds != null && !brandIds.isEmpty()) {
            query.setParameter("brandIds", brandIds);
        }

        if (minPrice != null) {
            query.setParameter("minPrice", minPrice);
        }

        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }

        return query.getResultList();
    }

    public boolean hasProductsByCategory(Integer categoryId) {
        List<Products> products = findByCategory(categoryId);
        return products.stream().anyMatch(p -> "Active".equals(p.getStatus()));
    }

    public boolean hasProductsByBrand(Integer brandId) {
        List<Products> products = findByBrand(brandId);
        return products.stream().anyMatch(p -> "Active".equals(p.getStatus()));
    }
}
