package controllers;

import entityclass.CommunityPosts;
import entityclass.Customers;
import entityclass.PostComments;
import entityclass.PostLikes;
import entityclass.CommentLikes;
import entityclass.PostShares;
import entityclass.UserFollows;
import entityclass.Notifications;
import entityclass.Users;
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
import sessionbeans.CommunityPostsFacadeLocal;
import sessionbeans.PostCommentsFacadeLocal;
import sessionbeans.PostLikesFacadeLocal;
import sessionbeans.CommentLikesFacadeLocal;
import sessionbeans.PostSharesFacadeLocal;
import sessionbeans.UserFollowsFacadeLocal;
import sessionbeans.NotificationsFacadeLocal;

@Named("communityMB")
@ViewScoped
public class CommunityMB implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== EJB INJECTIONS ==========
    @EJB
    private CommunityPostsFacadeLocal postsFacade;

    @EJB
    private PostCommentsFacadeLocal commentsFacade;

    @EJB
    private PostLikesFacadeLocal likesFacade;

    @EJB
    private CommentLikesFacadeLocal commentLikesFacade;

    @EJB
    private PostSharesFacadeLocal sharesFacade;

    @EJB
    private UserFollowsFacadeLocal followsFacade;

    @EJB
    private NotificationsFacadeLocal notificationsFacade;

    @Inject
    private AuthController auth;

    // ========== DATA LISTS ==========
    private List<CommunityPosts> allPosts = new ArrayList<>();
    private List<CommunityPosts> approvedPosts = new ArrayList<>();
    private List<CommunityPosts> pendingPosts = new ArrayList<>();
    private List<CommunityPosts> userPosts = new ArrayList<>();
    private List<PostComments> postComments = new ArrayList<>();
    private List<Notifications> notifications = new ArrayList<>();

    // ========== SELECTED ITEMS ==========
    private CommunityPosts selectedPost;
    private PostComments selectedComment;

    // ========== FORM FIELDS - POST ==========
    private String title = "";
    private String content = "";
    private String coverImageUrl = "";
    private jakarta.servlet.http.Part imageFile; // File upload part
    private String visibility = "PUBLIC";

    // ... (rest of the fields)

    // ========== POST OPERATIONS ==========
    public void submitPost() {
        try {
            // Validation
            if (title == null || title.trim().isEmpty()) {
                addErrorMessage("Please enter a title");
                return;
            }
            if (content == null || content.trim().isEmpty()) {
                addErrorMessage("Please enter content");
                return;
            }

            Users user = auth.getCurrentUser();
            Customers customer = auth.getCurrentCustomer();

            if (user == null) {
                addErrorMessage("Please log in");
                return;
            }

            // Handle File Upload
            if (imageFile != null) {
                String fileName = getFileName(imageFile);
                if (fileName != null && !fileName.isEmpty()) {
                    // Save file logic
                    try {
                        String uploadPath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("")
                                + java.io.File.separator + "resources" + java.io.File.separator + "images"
                                + java.io.File.separator + "community";
                        java.io.File uploadDir = new java.io.File(uploadPath);
                        if (!uploadDir.exists()) {
                            uploadDir.mkdirs();
                        }

                        String uniqueFileName = java.util.UUID.randomUUID().toString() + "_" + fileName;
                        java.nio.file.Path targetPath = java.nio.file.Paths.get(uploadPath, uniqueFileName);

                        try (java.io.InputStream input = imageFile.getInputStream()) {
                            java.nio.file.Files.copy(input, targetPath,
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }

                        // Set the relative URL for the entity
                        coverImageUrl = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath()
                                + "/resources/images/community/" + uniqueFileName;
                    } catch (IOException e) {
                        e.printStackTrace();
                        addErrorMessage("Failed to upload image: " + e.getMessage());
                        return;
                    }
                }
            }

            CommunityPosts post = new CommunityPosts();
            post.setTitle(title);
            post.setContent(content);
            post.setCoverImageUrl(coverImageUrl);
            post.setVisibility(visibility != null ? visibility : "PUBLIC");
            post.setStatus("PENDING");
            post.setAuthorUserID(user.getUserID());
            post.setCustomerID(customer != null ? customer.getCustomerID() : null);
            post.setSubmittedAt(new Date());
            post.setUpdatedAt(new Date());

            // Generate slug
            String slug = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("-+$", "");
            post.setSlug(slug);

            postsFacade.create(post);
            addInfoMessage("Post submitted successfully! Waiting for admin approval.");
            clearForm();
            loadAllData();
        } catch (Exception e) {
            addErrorMessage("Error submitting post: " + e.getMessage());
        }
    }

    private String getFileName(jakarta.servlet.http.Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    public jakarta.servlet.http.Part getImageFile() {
        return imageFile;
    }

    public void setImageFile(jakarta.servlet.http.Part imageFile) {
        this.imageFile = imageFile;
    }

    // ========== FORM FIELDS - COMMENT ==========
    private String commentText = "";

    // ========== FORM FIELDS - MODERATION ==========
    private String rejectReason = "";

    // ========== UI CONTROL ==========
    private String currentTab = "browse";
    private String searchTerm = "";
    private String statusFilter = "";
    private int currentPage = 0;
    private int pageSize = 10;
    private boolean isAdminView = false;
    private boolean showSidebarForm = false; // Toggle for sidebar create form
    private boolean showComments = true; // Toggle for comments section

    public boolean isShowComments() {
        return showComments;
    }

    public void setShowComments(boolean showComments) {
        this.showComments = showComments;
    }

    public void toggleComments() {
        this.showComments = !this.showComments;
    }

    // ========== POST CONSTRUCT ==========
    @PostConstruct
    public void init() {
        loadAllData();
    }

    // ========== UI ACTIONS ==========
    public void toggleSidebarForm() {
        this.showSidebarForm = !this.showSidebarForm;
        if (!this.showSidebarForm) {
            clearForm();
        }
    }

    public boolean isShowSidebarForm() {
        return showSidebarForm;
    }

    public void setShowSidebarForm(boolean showSidebarForm) {
        this.showSidebarForm = showSidebarForm;
    }

    // ========== AUTHENTICATION CHECKS ==========
    public void checkLogin() throws IOException {
        if (!auth.isLoggedIn()) {
            FacesContext.getCurrentInstance().getExternalContext().redirect(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath()
                            + "/pages/user/login.xhtml");
        }
    }

    public void checkAdminLogin() throws IOException {
        Users user = auth.getCurrentUser();
        if (user == null || !user.getRole().equals("ADMIN")) {
            FacesContext.getCurrentInstance().getExternalContext().redirect(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath()
                            + "/pages/admin/login.xhtml");
        } else {
            // Admin is valid, set admin view and default tab
            isAdminView = true;
            currentTab = "pending";
            // Load data if not already loaded
            if (allPosts == null || allPosts.isEmpty()) {
                loadAllData();
            }
        }
    }

    private boolean isAdminLoggedIn() {
        Users user = auth.getCurrentUser();
        return user != null && "ADMIN".equals(user.getRole());
    }

    // ========== DATA LOADING ==========
    public void loadAllData() {
        try {
            loadApprovedPosts();
            loadPendingPosts();
            loadUserPosts();
            loadAllPosts();
            loadNotifications();
        } catch (Exception e) {
            addErrorMessage("Error loading data: " + e.getMessage());
        }
    }

    private void loadApprovedPosts() {
        try {
            approvedPosts = postsFacade.findApprovedPosts();
            if (approvedPosts == null) {
                approvedPosts = new ArrayList<>();
            }
        } catch (Exception e) {
            approvedPosts = new ArrayList<>();
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("Error loading approved posts: " + msg);
            e.printStackTrace();
        }
    }

    private void loadPendingPosts() {
        try {
            pendingPosts = postsFacade.findPendingPosts();
            if (pendingPosts == null) {
                pendingPosts = new ArrayList<>();
            }
        } catch (Exception e) {
            pendingPosts = new ArrayList<>();
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("Error loading pending posts: " + msg);
            e.printStackTrace();
        }
    }

    private void loadUserPosts() {
        try {
            Users user = auth.getCurrentUser();
            if (user != null && "CUSTOMER".equals(user.getRole())) {
                Customers customer = auth.getCurrentCustomer();
                if (customer != null) {
                    userPosts = postsFacade.findByCustomerID(customer.getCustomerID());
                    if (userPosts == null) {
                        userPosts = new ArrayList<>();
                    }
                } else {
                    userPosts = new ArrayList<>();
                }
            } else {
                userPosts = new ArrayList<>();
            }
        } catch (Exception e) {
            userPosts = new ArrayList<>();
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("Error loading user posts: " + msg);
            e.printStackTrace();
        }
    }

    private void loadAllPosts() {
        try {
            allPosts = postsFacade.findAll();
            if (allPosts == null) {
                allPosts = new ArrayList<>();
            }
        } catch (Exception e) {
            allPosts = new ArrayList<>();
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("Error loading all posts: " + msg);
            e.printStackTrace();
        }
    }

    public void loadComments(CommunityPosts post) {
        try {
            if (post != null && post.getPostID() != null) {
                postComments = commentsFacade.findByPostID(post.getPostID());
                if (postComments == null) {
                    postComments = new ArrayList<>();
                }
            } else {
                postComments = new ArrayList<>();
            }
        } catch (Exception e) {
            postComments = new ArrayList<>();
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("Error loading comments: " + msg);
            e.printStackTrace();
        }
    }

    public void loadNotifications() {
        try {
            Users user = auth.getCurrentUser();
            if (user != null) {
                notifications = notificationsFacade.findUnread(user.getUserID());
                if (notifications == null) {
                    notifications = new ArrayList<>();
                }
            } else {
                notifications = new ArrayList<>();
            }
        } catch (Exception e) {
            notifications = new ArrayList<>();
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("Error loading notifications: " + msg);
            e.printStackTrace();
        }
    }

    // ========== POST OPERATIONS ==========

    public void deletePost(CommunityPosts post) {
        try {
            if (post == null) {
                addErrorMessage("Invalid post");
                return;
            }
            postsFacade.remove(post);
            addInfoMessage("Post deleted successfully");
            loadAllData();
        } catch (Exception e) {
            addErrorMessage("Error deleting post: " + e.getMessage());
        }
    }

    // ========== MODERATION OPERATIONS ==========
    public void approvePost(CommunityPosts post) {
        try {
            if (post == null) {
                addErrorMessage("Invalid post");
                return;
            }

            Users admin = auth.getCurrentUser();
            if (admin == null) {
                addErrorMessage("Please log in");
                return;
            }

            post.setStatus("APPROVED");
            post.setApprovedAt(new Date());
            post.setApprovedByUserID(admin.getUserID());
            post.setUpdatedAt(new Date());

            postsFacade.edit(post);

            // Create notification
            Notifications notif = new Notifications();
            notif.setRecipientUserID(post.getAuthorUserID());
            notif.setActorUserID(admin.getUserID());
            notif.setNotificationType("POST_APPROVED");
            notif.setPostID(post.getPostID());
            notif.setMessage("Your post has been approved");
            notif.setIsRead(false);
            notif.setCreatedAt(new Date());
            notificationsFacade.create(notif);

            addInfoMessage("Post approved successfully");
            loadAllData();
        } catch (Exception e) {
            addErrorMessage("Error approving post: " + e.getMessage());
        }
    }

    public void rejectPost(CommunityPosts post) {
        try {
            if (post == null) {
                addErrorMessage("Invalid post");
                return;
            }

            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                addErrorMessage("Please enter a rejection reason");
                return;
            }

            Users admin = auth.getCurrentUser();
            if (admin == null) {
                addErrorMessage("Please log in");
                return;
            }

            post.setStatus("REJECTED");
            post.setRejectedAt(new Date());
            post.setRejectedByUserID(admin.getUserID());
            post.setRejectReason(rejectReason);
            post.setUpdatedAt(new Date());

            postsFacade.edit(post);

            // Create notification
            Notifications notif = new Notifications();
            notif.setRecipientUserID(post.getAuthorUserID());
            notif.setActorUserID(admin.getUserID());
            notif.setNotificationType("POST_REJECTED");
            notif.setPostID(post.getPostID());
            notif.setMessage("Your post has been rejected. Reason: " + rejectReason);
            notif.setIsRead(false);
            notif.setCreatedAt(new Date());
            notificationsFacade.create(notif);

            addInfoMessage("Post rejected successfully");
            rejectReason = "";
            loadAllData();
        } catch (Exception e) {
            addErrorMessage("Error rejecting post: " + e.getMessage());
        }
    }

    public void hidePost(CommunityPosts post) {
        try {
            if (post == null) {
                addErrorMessage("Invalid post");
                return;
            }
            post.setStatus("HIDDEN");
            post.setUpdatedAt(new Date());
            postsFacade.edit(post);
            addInfoMessage("Post hidden successfully");
            loadAllData();
        } catch (Exception e) {
            addErrorMessage("Error hiding post: " + e.getMessage());
        }
    }

    public void unhidePost(CommunityPosts post) {
        try {
            if (post == null) {
                addErrorMessage("Invalid post");
                return;
            }
            post.setStatus("APPROVED");
            post.setUpdatedAt(new Date());
            postsFacade.edit(post);
            addInfoMessage("Post restored successfully");
            loadAllData();
        } catch (Exception e) {
            addErrorMessage("Error restoring post: " + e.getMessage());
        }
    }

    // ========== LIKE OPERATIONS ==========
    public void likePost(CommunityPosts post) {
        try {
            Users user = auth.getCurrentUser();
            Customers customer = auth.getCurrentCustomer();

            if (user == null) {
                addErrorMessage("Please log in");
                return;
            }

            if (post == null) {
                addErrorMessage("Invalid post");
                return;
            }

            if (likesFacade.userLikedPost(user.getUserID(), post.getPostID())) {
                // Unlike
                likesFacade.unlikePost(user.getUserID(), post.getPostID());
                post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                addInfoMessage("Unliked post");
            } else {
                // Like
                likesFacade.likePost(post.getPostID(), user.getUserID(),
                        customer != null ? customer.getCustomerID() : null);
                post.setLikeCount(post.getLikeCount() + 1);

                // Create notification
                if (!post.getAuthorUserID().equals(user.getUserID())) {
                    Notifications notif = new Notifications();
                    notif.setRecipientUserID(post.getAuthorUserID());
                    notif.setActorUserID(user.getUserID());
                    notif.setNotificationType("POST_LIKE");
                    notif.setPostID(post.getPostID());
                    notif.setMessage(user.getUsername() + " liked your post");
                    notif.setIsRead(false);
                    notif.setCreatedAt(new Date());
                    notificationsFacade.create(notif);
                }

                addInfoMessage("Liked post");
            }

            postsFacade.edit(post);
            loadAllData();
        } catch (Exception e) {
            addErrorMessage("Error liking post: " + e.getMessage());
        }
    }

    public boolean isLikedByCurrentUser(CommunityPosts post) {
        try {
            Users user = auth.getCurrentUser();
            if (user == null || post == null) {
                return false;
            }
            return likesFacade.userLikedPost(user.getUserID(), post.getPostID());
        } catch (Exception e) {
            return false;
        }
    }

    // ========== COMMENT OPERATIONS ==========
    public void submitComment(CommunityPosts post) {
        try {
            if (post == null || post.getPostID() == null) {
                addErrorMessage("Invalid post");
                return;
            }

            if (commentText == null || commentText.trim().isEmpty()) {
                addErrorMessage("Please enter a comment");
                return;
            }

            Users user = auth.getCurrentUser();
            Customers customer = auth.getCurrentCustomer();

            if (user == null) {
                addErrorMessage("Please log in");
                return;
            }

            PostComments comment = new PostComments();
            comment.setPostID(post.getPostID());
            comment.setAuthorUserID(user.getUserID());
            comment.setCustomerID(customer != null ? customer.getCustomerID() : null);
            comment.setContent(commentText);
            comment.setCreatedAt(new Date());
            comment.setUpdatedAt(new Date());
            comment.setIsDeleted(false);
            comment.setLikeCount(0L);

            commentsFacade.create(comment);

            // Update post comment count
            post.setCommentCount(post.getCommentCount() + 1);
            postsFacade.edit(post);

            // Create notification
            if (!post.getAuthorUserID().equals(user.getUserID())) {
                Notifications notif = new Notifications();
                notif.setRecipientUserID(post.getAuthorUserID());
                notif.setActorUserID(user.getUserID());
                notif.setNotificationType("POST_COMMENT");
                notif.setPostID(post.getPostID());
                notif.setMessage(user.getUsername() + " commented on your post");
                notif.setIsRead(false);
                notif.setCreatedAt(new Date());
                notificationsFacade.create(notif);
            }

            addInfoMessage("Comment posted successfully");
            commentText = "";
            loadComments(post);
            loadAllData();
        } catch (Exception e) {
            addErrorMessage("Error posting comment: " + e.getMessage());
        }
    }

    public void deleteComment(PostComments comment, CommunityPosts post) {
        try {
            Users user = auth.getCurrentUser();
            if (user == null || !user.getUserID().equals(comment.getAuthorUserID())) {
                addErrorMessage("You can only delete your own comments");
                return;
            }

            commentsFacade.deleteComment(comment.getCommentID());
            if (post != null) {
                post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
                postsFacade.edit(post);
            }

            addInfoMessage("Comment deleted successfully");
            loadComments(post);
            loadAllData();
        } catch (Exception e) {
            addErrorMessage("Error deleting comment: " + e.getMessage());
        }
    }

    // ========== FOLLOW OPERATIONS ==========
    public void followUser(CommunityPosts post) {
        try {
            Users currentUser = auth.getCurrentUser();
            Customers currentCustomer = auth.getCurrentCustomer();

            if (currentUser == null) {
                addErrorMessage("Please log in");
                return;
            }

            if (post == null) {
                addErrorMessage("Invalid post");
                return;
            }

            Users targetUser = post.getAuthorUser();
            Customers targetCustomer = post.getCustomer();

            if (currentUser.getUserID().equals(targetUser.getUserID())) {
                addErrorMessage("You cannot follow yourself");
                return;
            }

            if (followsFacade.isFollowing(currentUser.getUserID(), targetUser.getUserID())) {
                followsFacade.unfollowUser(currentUser.getUserID(), targetUser.getUserID());
                addInfoMessage("Unfollowed successfully");
            } else {
                followsFacade.followUser(
                        currentUser.getUserID(),
                        currentCustomer != null ? currentCustomer.getCustomerID() : null,
                        targetUser.getUserID(),
                        targetCustomer != null ? targetCustomer.getCustomerID() : null);

                // Create notification
                Notifications notif = new Notifications();
                notif.setRecipientUserID(targetUser.getUserID());
                notif.setActorUserID(currentUser.getUserID());
                notif.setNotificationType("USER_FOLLOW");
                notif.setMessage(currentUser.getUsername() + " started following you");
                notif.setIsRead(false);
                notif.setCreatedAt(new Date());
                notificationsFacade.create(notif);

                addInfoMessage("Followed user successfully");
            }

            loadAllData();
        } catch (Exception e) {
            addErrorMessage("Error following user: " + e.getMessage());
        }
    }

    public boolean isFollowingAuthor(CommunityPosts post) {
        try {
            Users currentUser = auth.getCurrentUser();
            if (currentUser == null || post == null) {
                return false;
            }
            return followsFacade.isFollowing(currentUser.getUserID(), post.getAuthorUserID());
        } catch (Exception e) {
            return false;
        }
    }

    // ========== SHARE OPERATIONS ==========
    public void sharePost(CommunityPosts post, Boolean facebook, Boolean twitter, Boolean whatsapp, String message) {
        try {
            Users user = auth.getCurrentUser();
            Customers customer = auth.getCurrentCustomer();

            if (user == null) {
                addErrorMessage("Please log in");
                return;
            }

            if (post == null) {
                addErrorMessage("Invalid post");
                return;
            }

            sharesFacade.sharePost(
                    post.getPostID(),
                    user.getUserID(),
                    customer != null ? customer.getCustomerID() : null,
                    message,
                    facebook != null ? facebook : false,
                    twitter != null ? twitter : false,
                    whatsapp != null ? whatsapp : false);

            // Create notification
            if (!post.getAuthorUserID().equals(user.getUserID())) {
                Notifications notif = new Notifications();
                notif.setRecipientUserID(post.getAuthorUserID());
                notif.setActorUserID(user.getUserID());
                notif.setNotificationType("POST_SHARE");
                notif.setPostID(post.getPostID());
                notif.setMessage(user.getUsername() + " shared your post");
                notif.setIsRead(false);
                notif.setCreatedAt(new Date());
                notificationsFacade.create(notif);
            }

            addInfoMessage("Post shared successfully");
            loadAllData();
        } catch (Exception e) {
            addErrorMessage("Error sharing post: " + e.getMessage());
        }
    }

    public Long getPostShareCount(CommunityPosts post) {
        try {
            if (post == null || post.getPostID() == null) {
                return 0L;
            }
            return sharesFacade.countByPostID(post.getPostID());
        } catch (Exception e) {
            return 0L;
        }
    }

    // ========== NOTIFICATION OPERATIONS ==========
    public Long getUnreadNotificationCount() {
        try {
            Users user = auth.getCurrentUser();
            if (user == null) {
                return 0L;
            }
            return notificationsFacade.countUnread(user.getUserID());
        } catch (Exception e) {
            return 0L;
        }
    }

    public void markNotificationAsRead(Notifications notif) {
        try {
            if (notif != null) {
                notificationsFacade.markAsRead(notif.getNotificationID());
                loadNotifications();
            }
        } catch (Exception e) {
            addErrorMessage("Error marking notification: " + e.getMessage());
        }
    }

    // ========== UI NAVIGATION ==========
    public void selectPost(CommunityPosts post) {
        selectedPost = post;
        if (post != null) {
            loadComments(post);
            showComments = true; // Default to showing comments when opening post
        }
    }

    public void changeTab(String tab) {
        this.currentTab = tab;
        this.currentPage = 0;
        clearForm();
        selectedPost = null;
    }

    public void clearForm() {
        title = "";
        content = "";
        coverImageUrl = "";
        visibility = "PUBLIC";
        rejectReason = "";
        commentText = "";
    }

    // ========== MESSAGE METHODS ==========
    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", message));
    }

    private void addInfoMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", message));
    }

    // ========== GETTERS AND SETTERS ==========
    public List<CommunityPosts> getAllPosts() {
        return allPosts;
    }

    public List<CommunityPosts> getApprovedPosts() {
        return approvedPosts;
    }

    public List<CommunityPosts> getPendingPosts() {
        return pendingPosts;
    }

    public List<CommunityPosts> getUserPosts() {
        return userPosts;
    }

    public List<PostComments> getPostComments() {
        return postComments;
    }

    public List<Notifications> getNotifications() {
        return notifications;
    }

    public CommunityPosts getSelectedPost() {
        return selectedPost;
    }

    public void setSelectedPost(CommunityPosts selectedPost) {
        this.selectedPost = selectedPost;
    }

    public PostComments getSelectedComment() {
        return selectedComment;
    }

    public void setSelectedComment(PostComments selectedComment) {
        this.selectedComment = selectedComment;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
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

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
