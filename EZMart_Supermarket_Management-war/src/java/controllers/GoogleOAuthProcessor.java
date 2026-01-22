package controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import jakarta.ejb.EJB;
import sessionbeans.UsersFacadeLocal;
import entityclass.Users;
import services.OtpService;
import services.EmailService;

@WebServlet(name = "GoogleOAuthProcessor", urlPatterns = {"/process-google"})
public class GoogleOAuthProcessor extends HttpServlet {

    @EJB(lookup = "java:global/EZMart_Supermarket_Management/EZMart_Supermarket_Management-ejb/UsersFacade!sessionbeans.UsersFacadeLocal")
    private UsersFacadeLocal usersFacade;

    @EJB
    private OtpService otpService;

    @EJB
    private EmailService emailService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/pages/user/login.xhtml?oauth_error=session_expired");
            return;
        }

        String email = (String) session.getAttribute("googleEmail");
        String name = (String) session.getAttribute("googleFullName");

        System.out.println("GoogleOAuthProcessor: session googleEmail=" + email + ", googleFullName=" + name);

        if (email == null || name == null) {
            response.sendRedirect(request.getContextPath() + "/pages/user/login.xhtml?oauth_error=missing_info");
            return;
        }

        // Check if user exists
        Users existingUser = usersFacade.findByEmail(email);
        if (existingUser != null) {
            // mark session as logged in
            System.out.println("GoogleOAuthProcessor: existingUser found id=" + existingUser.getUserID() + ", email=" + email);
            session.setAttribute("currentUser", existingUser);
            session.setAttribute("currentUserId", existingUser.getUserID());
            if (existingUser.getCustomersList() != null && !existingUser.getCustomersList().isEmpty()) {
                session.setAttribute("currentCustomer", existingUser.getCustomersList().get(0));
                session.setAttribute("currentCustomerId", existingUser.getCustomersList().get(0).getCustomerID());
            }
            session.setAttribute("loggedIn", true);
            // Also update JSF session-scoped AuthController bean if present
            Object authBean = session.getAttribute("auth");
            if (authBean instanceof AuthController) {
                try {
                    AuthController auth = (AuthController) authBean;
                    auth.setCurrentUser(existingUser);
                    if (existingUser.getCustomersList() != null && !existingUser.getCustomersList().isEmpty()) {
                        auth.setCurrentCustomer(existingUser.getCustomersList().get(0));
                    }
                    auth.setLoggedIn(true);
                    System.out.println("GoogleOAuthProcessor: updated AuthController bean in session");
                } catch (Exception ex) {
                    System.out.println("GoogleOAuthProcessor: failed to update AuthController bean: " + ex.getMessage());
                }
            }
            System.out.println("GoogleOAuthProcessor: session attributes set currentUserId=" + existingUser.getUserID() + ", currentCustomerId=" + (existingUser.getCustomersList() != null && !existingUser.getCustomersList().isEmpty() ? existingUser.getCustomersList().get(0).getCustomerID() : "null") + ", loggedIn=true");
            // Redirect based on role
            String role = existingUser.getRole();
            if (role != null) {
                if (role.equalsIgnoreCase("admin")) {
                    response.sendRedirect(request.getContextPath() + "/pages/admin/dashboard.xhtml");
                } else if (role.equalsIgnoreCase("staff")) {
                    response.sendRedirect(request.getContextPath() + "/pages/staff/dashboard.xhtml");
                } else {
                    response.sendRedirect(request.getContextPath() + "/pages/user/index.xhtml");
                }
            } else {
                response.sendRedirect(request.getContextPath() + "/pages/user/index.xhtml");
            }
            return;
        }

        // User doesn't exist - generate OTP and redirect to verify page
        String otp = otpService.generateOtp(email);
        System.out.println("GoogleOAuthProcessor: new Google user, generated OTP for " + email + ": " + otp);
        try {
            emailService.sendOtpEmail(email, otp);
        } catch (Exception ex) {
            // ignore - dev mode prints OTP
        }

        // Ensure session keeps google info
        session.setAttribute("googleEmail", email);
        session.setAttribute("googleFullName", name);

        response.sendRedirect(request.getContextPath() + "/pages/user/verify-otp.xhtml");
    }
}
