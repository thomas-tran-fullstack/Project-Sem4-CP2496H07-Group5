package controllers;

import entityclass.Feedbacks;
import entityclass.Customers;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import sessionbeans.FeedbacksFacadeLocal;

@Named("feedbackMB")
@ViewScoped
public class FeedbackMB implements Serializable {

    @EJB
    private FeedbacksFacadeLocal feedbacksFacade;

    @Inject
    private AuthController auth;

    private List<Feedbacks> allFeedbacks = new ArrayList<>();
    private List<Feedbacks> userFeedbacks = new ArrayList<>();
    private List<Feedbacks> openFeedbacks = new ArrayList<>();
    private List<Feedbacks> closedFeedbacks = new ArrayList<>();
    private Feedbacks newFeedback;
    private Feedbacks selectedFeedback;
    
    // Form fields
    private String subject;
    private String content;
    private String feedbackType = "GENERAL";
    private Integer rating;
    
    // Admin response fields
    private String responseText;
    private String searchTerm;
    private String currentTab = "open";

    @PostConstruct
    public void init() {
        newFeedback = new Feedbacks();
        loadFeedbacks();
    }
    
    public void checkLogin() throws IOException {
        if (!auth.isLoggedIn()) {
            FacesContext.getCurrentInstance().getExternalContext().redirect(
                FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/pages/user/login.xhtml"
            );
        }
    }

    public void loadFeedbacks() {
        try {
            allFeedbacks = feedbacksFacade.findAll();
            if (allFeedbacks == null) {
                allFeedbacks = new ArrayList<>();
            } else {
                allFeedbacks.sort((a, b) -> {
                    if (b.getCreatedAt() == null && a.getCreatedAt() == null) return 0;
                    if (b.getCreatedAt() == null) return -1;
                    if (a.getCreatedAt() == null) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
            }
            
            if (auth.getCurrentUser() != null && auth.getCurrentUser().getRole().equals("CUSTOMER")) {
                Customers customer = auth.getCurrentCustomer();
                if (customer != null) {
                    userFeedbacks = feedbacksFacade.findByCustomerID(customer);
                    if (userFeedbacks == null) {
                        userFeedbacks = new ArrayList<>();
                    } else {
                        userFeedbacks.sort((a, b) -> {
                            if (b.getCreatedAt() == null && a.getCreatedAt() == null) return 0;
                            if (b.getCreatedAt() == null) return -1;
                            if (a.getCreatedAt() == null) return 1;
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        });
                    }
                }
            }
            
            openFeedbacks = feedbacksFacade.findByStatus("OPEN");
            if (openFeedbacks == null) {
                openFeedbacks = new ArrayList<>();
            } else {
                openFeedbacks.sort((a, b) -> {
                    if (b.getCreatedAt() == null && a.getCreatedAt() == null) return 0;
                    if (b.getCreatedAt() == null) return -1;
                    if (a.getCreatedAt() == null) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
            }
            
            closedFeedbacks = feedbacksFacade.findByStatus("CLOSED");
            if (closedFeedbacks == null) {
                closedFeedbacks = new ArrayList<>();
            } else {
                closedFeedbacks.sort((a, b) -> {
                    if (b.getCreatedAt() == null && a.getCreatedAt() == null) return 0;
                    if (b.getCreatedAt() == null) return -1;
                    if (a.getCreatedAt() == null) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Initialize empty lists if error occurs
            if (allFeedbacks == null) allFeedbacks = new ArrayList<>();
            if (openFeedbacks == null) openFeedbacks = new ArrayList<>();
            if (closedFeedbacks == null) closedFeedbacks = new ArrayList<>();
            if (userFeedbacks == null) userFeedbacks = new ArrayList<>();
            addErrorMessage("Error loading feedback: " + e.getMessage());
        }
    }

    public void submitFeedback() {
        try {
            if (subject == null || subject.trim().isEmpty()) {
                addErrorMessage("Please enter a subject");
                return;
            }
            if (content == null || content.trim().isEmpty()) {
                addErrorMessage("Please enter your message");
                return;
            }
            
            Customers customer = auth.getCurrentCustomer();
            if (customer == null) {
                addErrorMessage("Please login to submit feedback");
                return;
            }

            newFeedback = new Feedbacks();
            newFeedback.setCustomerID(customer);
            newFeedback.setSubject(subject);
            newFeedback.setContent(content);
            newFeedback.setFeedbackType(feedbackType);
            newFeedback.setRating(rating);
            newFeedback.setStatus("OPEN");
            newFeedback.setCreatedAt(new Date());
            newFeedback.setUpdatedAt(new Date());

            feedbacksFacade.create(newFeedback);

            addInfoMessage("Thank you! Your feedback has been sent successfully.");
            clearForm();
            loadFeedbacks();
        } catch (Exception e) {
            addErrorMessage("Error submitting feedback: " + e.getMessage());
        }
    }

    public void selectFeedback(Feedbacks feedback) {
        selectedFeedback = feedback;
    }

    public void respondToFeedback() {
        try {
            if (selectedFeedback == null) {
                addErrorMessage("Please select feedback to reply");
                return;
            }
            if (responseText == null || responseText.trim().isEmpty()) {
                addErrorMessage("Please enter your response");
                return;
            }

            selectedFeedback.setResponse(responseText);
            selectedFeedback.setStatus("CLOSED");
            selectedFeedback.setRespondedAt(new Date());
            selectedFeedback.setUpdatedAt(new Date());

            feedbacksFacade.edit(selectedFeedback);

            addInfoMessage("Trả lời phản hồi thành công.");
            responseText = "";
            selectedFeedback = null;
            loadFeedbacks();
        } catch (Exception e) {
            addErrorMessage("Error responding to feedback: " + e.getMessage());
        }
    }

    public void deleteFeedback(Feedbacks feedback) {
        try {
            feedbacksFacade.remove(feedback);
            addInfoMessage("Xóa phản hồi thành công.");
            loadFeedbacks();
        } catch (Exception e) {
            addErrorMessage("Error deleting feedback: " + e.getMessage());
        }
    }

    public void clearForm() {
        subject = "";
        content = "";
        feedbackType = "GENERAL";
        rating = null;
        newFeedback = new Feedbacks();
    }

    public void changeTab(String tab) {
        currentTab = tab;
    }

    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", message));
    }

    private void addInfoMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", message));
    }

    // Getters and Setters
    public List<Feedbacks> getAllFeedbacks() {
        return allFeedbacks;
    }

    public void setAllFeedbacks(List<Feedbacks> allFeedbacks) {
        this.allFeedbacks = allFeedbacks;
    }

    public List<Feedbacks> getUserFeedbacks() {
        return userFeedbacks;
    }

    public void setUserFeedbacks(List<Feedbacks> userFeedbacks) {
        this.userFeedbacks = userFeedbacks;
    }

    public List<Feedbacks> getOpenFeedbacks() {
        return openFeedbacks;
    }

    public void setOpenFeedbacks(List<Feedbacks> openFeedbacks) {
        this.openFeedbacks = openFeedbacks;
    }

    public List<Feedbacks> getClosedFeedbacks() {
        return closedFeedbacks;
    }

    public void setClosedFeedbacks(List<Feedbacks> closedFeedbacks) {
        this.closedFeedbacks = closedFeedbacks;
    }

    public Feedbacks getNewFeedback() {
        return newFeedback;
    }

    public void setNewFeedback(Feedbacks newFeedback) {
        this.newFeedback = newFeedback;
    }

    public Feedbacks getSelectedFeedback() {
        return selectedFeedback;
    }

    public void setSelectedFeedback(Feedbacks selectedFeedback) {
        this.selectedFeedback = selectedFeedback;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getCurrentTab() {
        return currentTab;
    }

    public void setCurrentTab(String currentTab) {
        this.currentTab = currentTab;
    }

}
