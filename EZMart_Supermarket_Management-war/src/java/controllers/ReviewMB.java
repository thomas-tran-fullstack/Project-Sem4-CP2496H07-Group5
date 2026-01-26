package controllers;

import entityclass.Reviews;
import entityclass.Customers;
import entityclass.Products;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import sessionbeans.ReviewsFacadeLocal;

@Named("reviewMB")
@ViewScoped
public class ReviewMB implements Serializable {

    @EJB
    private ReviewsFacadeLocal reviewsFacade;

    @Inject
    private AuthController auth;

    @Inject
    private ProductMB productMB;

    private List<Reviews> productReviews;
    private List<Reviews> pendingReviews;
    private List<Reviews> flaggedReviews;
    private List<Reviews> allReviews;
    private List<Reviews> publishedReviews;
    private List<Reviews> rejectedReviews;
    private Reviews newReview;
    private Reviews selectedReview;
    private Integer rating;
    private String comment;
    
    // Admin reply fields
    private String replyText;
    private String currentTab = "published";
    private String searchTerm;
    private String searchCustomerName;
    private String searchProductName;
    private String searchCommentKeyword;
    private String searchRating;

    @PostConstruct
    public void init() {
        newReview = new Reviews();
        // Load all published reviews by default (customer submitted reviews are published immediately)
        loadPublishedReviews();
        loadFlaggedReviews();
        loadRejectedReviews();
    }

    public void loadProductReviews(Products product) {
        if (product != null) {
            productReviews = reviewsFacade.findPublishedReviewsByProductID(product);
        }
    }

    public void loadPendingReviews() {
        // No longer used - reviews are published immediately
        pendingReviews = reviewsFacade.findPendingReviews();
    }

    public void loadFlaggedReviews() {
        // Load only flagged reviews that are still PUBLISHED (not hidden/rejected)
        List<Reviews> allFlagged = reviewsFacade.findFlaggedReviews();
        flaggedReviews = allFlagged.stream()
            .filter(r -> "PUBLISHED".equals(r.getStatus()))
            .toList();
    }
    
    public void loadRejectedReviews() {
        rejectedReviews = reviewsFacade.findByStatus("REJECTED");
    }
    
    public void loadAllReviews() {
        allReviews = reviewsFacade.findAllReviews();
    }
    
    public void loadPublishedReviews() {
        publishedReviews = reviewsFacade.findPublishedReviews();
    }
    
    public void loadReviewsByStatus(String status) {
        if ("pending".equals(status)) {
            // No longer used - reviews are published immediately
            publishedReviews = reviewsFacade.findPublishedReviews();
        } else if ("published".equals(status)) {
            publishedReviews = reviewsFacade.findPublishedReviews();
        } else if ("rejected".equals(status)) {
            rejectedReviews = reviewsFacade.findByStatus("REJECTED");
        } else if ("flagged".equals(status)) {
            flaggedReviews = reviewsFacade.findFlaggedReviews();
        } else {
            loadPublishedReviews();
        }
    }
    
    public void searchReviews() {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            allReviews = reviewsFacade.searchReviews(searchTerm);
        } else {
            loadAllReviews();
        }
    }
    
    public void advancedSearch() {
        boolean hasSearchCriteria = false;
        List<Reviews> publishedResults = reviewsFacade.findPublishedReviews();
        List<Reviews> rejectedResults = reviewsFacade.findByStatus("REJECTED");
        
        // Filter by customer name
        if (searchCustomerName != null && !searchCustomerName.trim().isEmpty()) {
            hasSearchCriteria = true;
            String customerNameLower = searchCustomerName.trim().toLowerCase();
            publishedResults = publishedResults.stream()
                .filter(r -> r.getCustomerID() != null && 
                    (getCustomerName(r).toLowerCase().contains(customerNameLower)))
                .toList();
            rejectedResults = rejectedResults.stream()
                .filter(r -> r.getCustomerID() != null && 
                    (getCustomerName(r).toLowerCase().contains(customerNameLower)))
                .toList();
        }
        
        // Filter by product name
        if (searchProductName != null && !searchProductName.trim().isEmpty()) {
            hasSearchCriteria = true;
            String productNameLower = searchProductName.trim().toLowerCase();
            publishedResults = publishedResults.stream()
                .filter(r -> r.getProductID() != null && 
                    r.getProductID().getProductName() != null &&
                    r.getProductID().getProductName().toLowerCase().contains(productNameLower))
                .toList();
            rejectedResults = rejectedResults.stream()
                .filter(r -> r.getProductID() != null && 
                    r.getProductID().getProductName() != null &&
                    r.getProductID().getProductName().toLowerCase().contains(productNameLower))
                .toList();
        }
        
        // Filter by comment keyword
        if (searchCommentKeyword != null && !searchCommentKeyword.trim().isEmpty()) {
            hasSearchCriteria = true;
            String commentLower = searchCommentKeyword.trim().toLowerCase();
            publishedResults = publishedResults.stream()
                .filter(r -> r.getComment() != null && 
                    r.getComment().toLowerCase().contains(commentLower))
                .toList();
            rejectedResults = rejectedResults.stream()
                .filter(r -> r.getComment() != null && 
                    r.getComment().toLowerCase().contains(commentLower))
                .toList();
        }
        
        // Filter by rating
        if (searchRating != null && !searchRating.trim().isEmpty()) {
            hasSearchCriteria = true;
            try {
                int ratingValue = Integer.parseInt(searchRating);
                publishedResults = publishedResults.stream()
                    .filter(r -> r.getRating() != null && r.getRating() == ratingValue)
                    .toList();
                rejectedResults = rejectedResults.stream()
                    .filter(r -> r.getRating() != null && r.getRating() == ratingValue)
                    .toList();
            } catch (NumberFormatException e) {
                // Invalid rating, ignore
            }
        }
        
        publishedReviews = publishedResults;
        flaggedReviews = publishedResults.stream()
            .filter(this::isFlagged)
            .toList();
        rejectedReviews = rejectedResults;
    }

    public void clearSearch() {
        searchCustomerName = null;
        searchProductName = null;
        searchCommentKeyword = null;
        searchRating = null;
        searchTerm = null;
        loadPublishedReviews();
        loadFlaggedReviews();
        loadRejectedReviews();
    }

    public void submitReview() {
        if (!auth.isLoggedIn()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please login to submit a review"));
            return;
        }

        if (productMB.getSelectedProduct() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No product selected"));
            return;
        }

        if (rating == null || rating < 1 || rating > 5) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please provide a rating between 1 and 5"));
            return;
        }

        if (comment == null || comment.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please provide a comment"));
            return;
        }

        // Check if user already reviewed this product
        Customers currentCustomer = auth.getCurrentCustomer();
        List<Reviews> existingReviews = reviewsFacade.findByProductIDAndCustomerID(productMB.getSelectedProduct(), currentCustomer);
        if (!existingReviews.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "You have already reviewed this product"));
            return;
        }

        try {
            newReview.setProductID(productMB.getSelectedProduct());
            newReview.setCustomerID(currentCustomer);
            newReview.setRating(rating);
            newReview.setComment(comment.trim());
            newReview.setCreatedAt(new Date());
            // Reviews are PUBLISHED immediately when customer submits
            newReview.setStatus("PUBLISHED");
            newReview.setIsFlagged(false);

            reviewsFacade.create(newReview);

            // Refresh product reviews to show the new review immediately
            loadProductReviews(productMB.getSelectedProduct());
            loadPublishedReviews();

            // Reset form
            newReview = new Reviews();
            rating = null;
            comment = null;

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Your review has been published successfully!"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to submit review"));
        }
    }

    public void approveReview(Reviews review) {
        // Reviews are already published by default, this method can be used to re-publish rejected reviews
        try {
            review.setStatus("PUBLISHED");
            reviewsFacade.edit(review);
            loadPublishedReviews();
            loadFlaggedReviews();
            loadRejectedReviews();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Review approved and published"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to approve review"));
        }
    }

    public void rejectReview(Reviews review) {
        // Reject to hide toxic/inappropriate reviews
        try {
            review.setStatus("REJECTED");
            reviewsFacade.edit(review);
            loadPublishedReviews();
            loadFlaggedReviews();
            loadRejectedReviews();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Review rejected and hidden"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to reject review"));
        }
    }

    public void replyToReview(Reviews review, String replyText) {
        if (replyText == null || replyText.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Reply cannot be empty"));
            return;
        }

        try {
            review.setReply(replyText.trim());
            review.setReplyAt(new Date());
            reviewsFacade.edit(review);
            loadPublishedReviews();
            loadFlaggedReviews();
            loadRejectedReviews();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Reply added successfully"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to add reply"));
        }
    }

    public void flagReview(Reviews review) {
        try {
            review.setIsFlagged(true);
            reviewsFacade.edit(review);
            loadPublishedReviews();
            loadFlaggedReviews();
            loadRejectedReviews();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Review flagged for moderation"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to flag review"));
        }
    }
    
    public void unflagReview(Reviews review) {
        try {
            review.setIsFlagged(false);
            reviewsFacade.edit(review);
            loadPublishedReviews();
            loadFlaggedReviews();
            loadRejectedReviews();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Review unflagged"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to unflag review"));
        }
    }

    public boolean hasUserReviewedProduct(Products product, Customers customer) {
        if (product == null || customer == null) {
            return false;
        }
        List<Reviews> reviews = reviewsFacade.findByProductIDAndCustomerID(product, customer);
        return !reviews.isEmpty();
    }

    public double getAverageRating(Products product) {
        List<Reviews> reviews = reviewsFacade.findPublishedReviewsByProductID(product);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        double sum = reviews.stream().mapToInt(Reviews::getRating).sum();
        return sum / reviews.size();
    }

    public int getReviewCount(Products product) {
        return reviewsFacade.findPublishedReviewsByProductID(product).size();
    }
    
    public int getPendingCount() {
        // No longer used - reviews are published immediately
        return 0;
    }
    
    public int getPublishedCount() {
        return reviewsFacade.countByStatus("PUBLISHED");
    }
    
    public int getRejectedCount() {
        return reviewsFacade.countByStatus("REJECTED");
    }
    
    public int getFlaggedCount() {
        return reviewsFacade.findFlaggedReviews().size();
    }
    
    public boolean isFlagged(Reviews review) {
        return review != null && review.getIsFlagged() != null && review.getIsFlagged();
    }
    
    public String getCustomerName(Reviews review) {
        if (review != null && review.getCustomerID() != null) {
            Customers c = review.getCustomerID();
            StringBuilder sb = new StringBuilder();
            if (c.getFirstName() != null) sb.append(c.getFirstName());
            if (c.getMiddleName() != null && !c.getMiddleName().isEmpty()) sb.append(" ").append(c.getMiddleName());
            if (c.getLastName() != null) sb.append(" ").append(c.getLastName());
            return sb.toString().trim();
        }
        return "Anonymous";
    }
    
    public String getStatusStyle(String status) {
        if (status == null) return "bg-gray-100 text-gray-800";
        switch (status) {
            case "PUBLISHED":
                return "bg-green-100 text-green-800";
            case "PENDING":
                return "bg-yellow-100 text-yellow-800";
            case "REJECTED":
                return "bg-red-100 text-red-800";
            default:
                return "bg-gray-100 text-gray-800";
        }
    }
    
    public String getStatusDisplay(String status) {
        if (status == null) return "Unknown";
        switch (status) {
            case "PUBLISHED": return "Published";
            case "PENDING": return "Pending Review";
            case "REJECTED": return "Rejected";
            default: return status;
        }
    }
    
    public String getStarRating(Integer rating) {
        if (rating == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= rating) {
                sb.append("★");
            } else {
                sb.append("☆");
            }
        }
        return sb.toString();
    }

    // Getters and Setters
    public List<Reviews> getProductReviews() {
        return productReviews;
    }

    public void setProductReviews(List<Reviews> productReviews) {
        this.productReviews = productReviews;
    }

    public List<Reviews> getPendingReviews() {
        return pendingReviews;
    }

    public void setPendingReviews(List<Reviews> pendingReviews) {
        this.pendingReviews = pendingReviews;
    }

    public List<Reviews> getFlaggedReviews() {
        return flaggedReviews;
    }

    public void setFlaggedReviews(List<Reviews> flaggedReviews) {
        this.flaggedReviews = flaggedReviews;
    }

    public List<Reviews> getAllReviews() {
        if (allReviews == null) {
            loadAllReviews();
        }
        return allReviews;
    }

    public void setAllReviews(List<Reviews> allReviews) {
        this.allReviews = allReviews;
    }

    public List<Reviews> getPublishedReviews() {
        return publishedReviews;
    }

    public void setPublishedReviews(List<Reviews> publishedReviews) {
        this.publishedReviews = publishedReviews;
    }

    public List<Reviews> getRejectedReviews() {
        return rejectedReviews;
    }

    public void setRejectedReviews(List<Reviews> rejectedReviews) {
        this.rejectedReviews = rejectedReviews;
    }

    public Reviews getNewReview() {
        return newReview;
    }

    public void setNewReview(Reviews newReview) {
        this.newReview = newReview;
    }

    public Reviews getSelectedReview() {
        return selectedReview;
    }

    public void setSelectedReview(Reviews selectedReview) {
        this.selectedReview = selectedReview;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public String getCurrentTab() {
        return currentTab;
    }

    public void setCurrentTab(String currentTab) {
        this.currentTab = currentTab;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
    
    public String getSearchCustomerName() {
        return searchCustomerName;
    }

    public void setSearchCustomerName(String searchCustomerName) {
        this.searchCustomerName = searchCustomerName;
    }

    public String getSearchProductName() {
        return searchProductName;
    }

    public void setSearchProductName(String searchProductName) {
        this.searchProductName = searchProductName;
    }

    public String getSearchCommentKeyword() {
        return searchCommentKeyword;
    }

    public void setSearchCommentKeyword(String searchCommentKeyword) {
        this.searchCommentKeyword = searchCommentKeyword;
    }

    public String getSearchRating() {
        return searchRating;
    }

    public void setSearchRating(String searchRating) {
        this.searchRating = searchRating;
    }
}
