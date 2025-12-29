/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.ProductImages;
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
public class ProductImagesFacade extends AbstractFacade<ProductImages> implements ProductImagesFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ProductImagesFacade() {
        super(ProductImages.class);
    }

    @Override
    public List<ProductImages> findByProductID(Products productID) {
        return em.createNamedQuery("ProductImages.findByProductID", ProductImages.class)
                .setParameter("productID", productID)
                .getResultList();
    }
    
}
