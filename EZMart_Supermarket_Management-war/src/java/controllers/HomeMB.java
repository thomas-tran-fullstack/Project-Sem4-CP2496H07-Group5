package controllers;

import entityclass.Categories;
import entityclass.Customers;
import entityclass.Offers;
import entityclass.ProductImages;
import entityclass.ProductOffers;
import entityclass.Products;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import sessionbeans.CategoriesFacadeLocal;
import sessionbeans.OffersFacadeLocal;
import sessionbeans.ProductImagesFacadeLocal;
import sessionbeans.ProductOffersFacadeLocal;
import sessionbeans.ProductsFacadeLocal;

@Named("homeMB")
@ViewScoped
public class HomeMB implements Serializable {

    @EJB
    private CategoriesFacadeLocal categoriesFacade;

    @EJB
    private ProductsFacadeLocal productsFacade;

    @EJB
    private ProductImagesFacadeLocal productImagesFacade;

    @EJB
    private OffersFacadeLocal offersFacade;

    @EJB
    private ProductOffersFacadeLocal productOffersFacade;

    private List<Categories> categories;
    private List<Products> dailyDeals;
    private List<Offers> activeOffers;

    @PostConstruct
    public void init() {
        loadCategories();
        loadDailyDeals();
        loadActiveOffers();
    }

    public void loadCategories() {
        categories = categoriesFacade.findAll().stream()
                .filter(cat -> "Active".equalsIgnoreCase(cat.getStatus()))
                .limit(6) // Limit to 6 categories for display
                .collect(Collectors.toList());
    }

    public void loadDailyDeals() {
        // Get products that have active offers
        List<ProductOffers> activeProductOffers = productOffersFacade.findAll().stream()
                .filter(po -> {
                    Offers offer = po.getOfferID();
                    return offer != null && "Active".equalsIgnoreCase(offer.getStatus()) &&
                           isOfferActive(offer);
                })
                .collect(Collectors.toList());

        dailyDeals = activeProductOffers.stream()
                .map(ProductOffers::getProductID)
                .filter(product -> "Active".equalsIgnoreCase(product.getStatus()) && product.getStockQuantity() > 0)
                .distinct()
                .limit(4) // Limit to 4 products for display
                .collect(Collectors.toList());
    }

    public void loadActiveOffers() {
        activeOffers = offersFacade.findAll().stream()
                .filter(offer -> "Active".equalsIgnoreCase(offer.getStatus()) && isOfferActive(offer))
                .limit(2) // Limit to 2 offers for banners
                .collect(Collectors.toList());
    }

    private boolean isOfferActive(Offers offer) {
        Date now = new Date();
        return offer.getStartDate() != null && offer.getEndDate() != null &&
               now.after(offer.getStartDate()) && now.before(offer.getEndDate());
    }

    // Helper methods for displaying data
    public String getCategoryImageUrl(Categories category) {
        if (category.getImageURL() != null && !category.getImageURL().isEmpty()) {
            return category.getImageURL();
        }
        // Return a default image URL or placeholder
        return "https://via.placeholder.com/64x64?text=" + category.getCategoryName().substring(0, 1);
    }

    public String getProductImageUrl(Products product) {
        List<ProductImages> images = productImagesFacade.findByProductID(product);
        if (images != null && !images.isEmpty()) {
            return images.get(0).getImageURL();
        }
        // Return a default image URL
        return "https://via.placeholder.com/300x300?text=No+Image";
    }

    public String getTimeAgo(Date date) {
        if (date == null) return "";
        long diff = new Date().getTime() - date.getTime();
        long days = diff / (1000 * 60 * 60 * 24);
        if (days > 0) {
            return days + " days ago";
        }
        long hours = diff / (1000 * 60 * 60);
        if (hours > 0) {
            return hours + " hours ago";
        }
        long minutes = diff / (1000 * 60);
        return minutes + " minutes ago";
    }

    public String getDiscountDisplay(Products product) {
        ProductOffers po = productOffersFacade.findAll().stream()
                .filter(p -> p.getProductID().equals(product))
                .filter(p -> "Active".equalsIgnoreCase(p.getOfferID().getStatus()))
                .filter(p -> isOfferActive(p.getOfferID()))
                .findFirst().orElse(null);
        if (po != null) {
            Offers offer = po.getOfferID();
            String type = offer.getOfferType();
            Integer value = offer.getDiscountValue();
            if ("Percentage".equalsIgnoreCase(type)) {
                return value + "% OFF";
            } else if ("Fixed".equalsIgnoreCase(type)) {
                return "$" + value + " OFF";
            }
        }
        return "";
    }

    public double getDiscountedPrice(Products product) {
        BigDecimal original = product.getUnitPrice();
        if (original == null) return 0.0;
        ProductOffers po = productOffersFacade.findAll().stream()
                .filter(p -> p.getProductID().equals(product))
                .filter(p -> "Active".equalsIgnoreCase(p.getOfferID().getStatus()))
                .filter(p -> isOfferActive(p.getOfferID()))
                .findFirst().orElse(null);
        if (po != null) {
            Offers offer = po.getOfferID();
            String type = offer.getOfferType();
            Integer value = offer.getDiscountValue();
            if ("Percentage".equalsIgnoreCase(type)) {
                return original.doubleValue() * (1 - value / 100.0);
            } else if ("Fixed".equalsIgnoreCase(type)) {
                return original.doubleValue() - value;
            }
        }
        return original.doubleValue();
    }

    public boolean hasOffer(Products product) {
        return productOffersFacade.findAll().stream()
                .filter(p -> p.getProductID().equals(product))
                .filter(p -> "Active".equalsIgnoreCase(p.getOfferID().getStatus()))
                .filter(p -> isOfferActive(p.getOfferID()))
                .findFirst().isPresent();
    }

    // Getters
    public List<Categories> getCategories() {
        return categories;
    }

    public List<Products> getDailyDeals() {
        return dailyDeals;
    }

    public List<Offers> getActiveOffers() {
        return activeOffers;
    }
}
