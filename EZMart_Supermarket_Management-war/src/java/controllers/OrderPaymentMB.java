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
        try {
            if (paymentProofFile == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please upload payment proof"));
                return null;
            }

            if (orderId == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid order"));
                return null;
            }

            Orders order = ordersFacade.find(orderId);
            if (order == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Order not found"));
                return null;
            }

            // Save payment proof file
            String fileName = "payment_proof_" + orderId + "_" + System.currentTimeMillis() + ".jpg";
            Path uploadPath = Paths.get(System.getProperty("user.home"), "uploads", "payment_proofs");
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(fileName);

            try (InputStream input = paymentProofFile.getInputStream()) {
                Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Create payment proof record
            PaymentProof paymentProof = new PaymentProof();
            paymentProof.setOrderID(order);
            paymentProof.setImagePath(filePath.toString());
            paymentProof.setTransactionID(transactionId);
            paymentProof.setNote(paymentNotes);
            paymentProof.setUploadedAt(new Date());
            paymentProof.setStatus("PENDING");

            paymentProofFacade.create(paymentProof);

            // Update order status
            order.setStatus("WAITING_CONFIRM");
            order.setPaymentStatus("PENDING");
            ordersFacade.edit(order);

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Payment proof submitted successfully"));

            return "/pages/user/order-success.xhtml?faces-redirect=true&orderId=" + orderId;

        } catch (IOException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to upload payment proof"));
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to submit payment proof: " + e.getMessage()));
            e.printStackTrace();
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
