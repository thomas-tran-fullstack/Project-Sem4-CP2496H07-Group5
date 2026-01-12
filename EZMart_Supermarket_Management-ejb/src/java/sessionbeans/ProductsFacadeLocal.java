/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.Products;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface ProductsFacadeLocal {

    void create(Products products);

    void edit(Products products);

    void remove(Products products);

    Products find(Object id);

    List<Products> findAll();

    List<Products> findRange(int[] range);

    int count();

    public List<Products> findByProductName(String productName);

    public List<Products> findByCategory(Integer categoryId);

    public List<Products> findByBrand(Integer brandId);

    public List<Products> findByPriceRange(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);

    public List<Products> findFiltered(String searchTerm, Integer categoryId, Integer brandId, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);

    public List<Products> findFiltered(String searchTerm, Integer categoryId, List<Integer> brandIds, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);
    public List<Products> findFiltered(String searchTerm, List<Integer> categoryIds, List<Integer> brandIds, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);

    public boolean hasProductsByCategory(Integer categoryId);

    public boolean hasProductsByBrand(Integer brandId);
}
