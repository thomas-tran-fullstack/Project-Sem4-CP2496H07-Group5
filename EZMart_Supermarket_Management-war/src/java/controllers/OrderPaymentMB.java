package controllers;

import entityclass.Orders;
import entityclass.PaymentProof;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import sessionbeans.OrdersFacadeLocal;
import sessionbeans.PaymentProofFacadeLocal;

@Named("orderPaymentMB")
@RequestScoped
public class OrderPaymentMB implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private OrdersFacadeLocal ordersFacade;

    @EJB
    private PaymentProofFacadeLocal paymentProofFacade;

    private Integer orderId;
    private BigDecimal orderTotal;
    private Part paymentProofFile;
    private String transactionId;
    private String paymentNotes;

    @PostConstruct
    public void init() {
        // Get orderId from request parameter
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String orderIdStr = facesContext.getExternalContext().getRequestParameterMap().get("orderId");
        if (orderIdStr != null) {
            try {
                orderId = Integer.parseInt(orderIdStr);
                Orders order = ordersFacade.find(orderId);
                if (order != null) {
                    orderTotal = order.getTotalAmount();
                }
            } catch (NumberFormatException e) {
                // Handle invalid orderId
            }
        }
    }

    public String submitPaymentProof() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        try {
            System.out.println("DEBUG: submitPaymentProof called");
            System.out.println("DEBUG: orderId = " + orderId);
            System.out.println("DEBUG: paymentProofFile = " + (paymentProofFile != null ? "not null" : "null"));
            if (paymentProofFile != null) {
                System.out.println("DEBUG: paymentProofFile size = " + paymentProofFile.getSize());
                System.out.println("DEBUG: paymentProofFile name = " + paymentProofFile.getSubmittedFileName());
            }

            // Try to get orderId from request parameter if not set
            if (orderId == null) {
                String orderIdStr = facesContext.getExternalContext().getRequestParameterMap().get("orderId");
                if (orderIdStr != null) {
                    try {
                        orderId = Integer.parseInt(orderIdStr);
                        System.out.println("DEBUG: Retrieved orderId from request parameter: " + orderId);
                    } catch (NumberFormatException e) {
                        System.out.println("DEBUG: Invalid orderId parameter: " + orderIdStr);
                    }
                }
            }

            // Validate required fields
            if (paymentProofFile == null || paymentProofFile.getSize() <= 0) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please upload payment proof"));
                return null;
            }

            if (orderId == null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid order - orderId is null"));
                return null;
            }

            // Find and validate order
            Orders order = ordersFacade.find(orderId);
            if (order == null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Order not found"));
                return null;
            }

            System.out.println("DEBUG: Order found: " + order.getOrderID());

            // Validate transaction ID if provided
            if (transactionId != null && transactionId.trim().isEmpty()) {
                transactionId = null; // Set to null if empty
            }

            // Save payment proof file
            String originalFileName = paymentProofFile.getSubmittedFileName();
            String fileExtension = ".jpg"; // default
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = "payment_proof_" + orderId + "_" + System.currentTimeMillis() + fileExtension;

            System.out.println("DEBUG: Generated filename: " + fileName);

            // Use webapp uploads directory
            String uploadDir = facesContext.getExternalContext().getRealPath("/uploads/payment_proofs");
            System.out.println("DEBUG: Real path for uploads: " + uploadDir);

            if (uploadDir == null) {
                // Fallback to absolute path
                uploadDir = System.getProperty("user.home") + "/uploads/payment_proofs";
                System.out.println("DEBUG: Using fallback path: " + uploadDir);
            }

            Path uploadPath = Paths.get(uploadDir);
            System.out.println("DEBUG: Creating directories at: " + uploadPath);
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(fileName);

            System.out.println("DEBUG: Saving file to: " + filePath);

            // Save file
            try (InputStream input = paymentProofFile.getInputStream()) {
                Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("DEBUG: File saved successfully");

            // Create payment proof record
            PaymentProof paymentProof = new PaymentProof();
            paymentProof.setOrderID(order);
            paymentProof.setImagePath("/uploads/payment_proofs/" + fileName); // Relative path for web access
            paymentProof.setTransactionID(transactionId);
            paymentProof.setNote(paymentNotes);
            paymentProof.setUploadedAt(new Date());
            paymentProof.setStatus("PENDING");

            System.out.println("DEBUG: Creating payment proof record");

            // Save payment proof to database
            paymentProofFacade.create(paymentProof);

            System.out.println("DEBUG: Payment proof saved to database");

            // Update order status
            order.setStatus("NEW");
            order.setPaymentStatus("PENDING");
            ordersFacade.edit(order);

            System.out.println("DEBUG: Order status updated");

            // Success message
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Payment proof submitted successfully"));

            // Clear form fields
            paymentProofFile = null;
            transactionId = null;
            paymentNotes = null;

            System.out.println("DEBUG: Redirecting to success page");

            // External redirect to avoid navigation issues with file upload
            String redirectUrl = facesContext.getExternalContext().getRequestContextPath() + "/pages/user/order-success.xhtml?orderId=" + orderId;
            System.out.println("DEBUG: Redirect URL: " + redirectUrl);
            
            try {
                facesContext.getExternalContext().redirect(redirectUrl);
            } catch (IOException e) {
                System.err.println("DEBUG: IOException during redirect: " + e.getMessage());
                e.printStackTrace();
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to redirect after submission"));
                return null;
            }
            
            // Prevent JSF navigation after redirect
            FacesContext.getCurrentInstance().responseComplete();
            return null;

        } catch (IOException e) {
            System.err.println("DEBUG: IOException in submitPaymentProof: " + e.getMessage());
            e.printStackTrace();
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to upload payment proof: " + e.getMessage()));
            return null;
        } catch (Exception e) {
            System.err.println("DEBUG: Exception in submitPaymentProof: " + e.getMessage());
            e.printStackTrace();
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to submit payment proof: " + e.getMessage()));
            return null;
        }
    }

    // Getters and Setters
    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(BigDecimal orderTotal) {
        this.orderTotal = orderTotal;
    }

    public Part getPaymentProofFile() {
        return paymentProofFile;
    }

    public void setPaymentProofFile(Part paymentProofFile) {
        this.paymentProofFile = paymentProofFile;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPaymentNotes() {
        return paymentNotes;
    }

    public void setPaymentNotes(String paymentNotes) {
        this.paymentNotes = paymentNotes;
    }
}
