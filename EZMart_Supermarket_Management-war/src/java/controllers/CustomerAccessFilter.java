package controllers;

import entityclass.Users;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filter to restrict access to customer-only pages (like checkout) to users
 * with CUSTOMER role only
 *
 * @author TRUONG LAM
 */
public class CustomerAccessFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();

        // Check if the request is for checkout page
        if (requestURI.contains("/checkout.xhtml")) {
            HttpSession session = httpRequest.getSession(false);

            boolean hasAccess = false;

            if (session != null) {
                // Check if user is logged in and has CUSTOMER role
                Users currentUser = (Users) session.getAttribute("currentUser");
                Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");

                System.out.println("CustomerAccessFilter: session=" + session.getId());
                System.out.println("CustomerAccessFilter: currentUser=" + currentUser);
                System.out.println("CustomerAccessFilter: loggedIn=" + loggedIn);
                if (currentUser != null) {
                    System.out.println("CustomerAccessFilter: role=" + currentUser.getRole());
                }

                if (currentUser != null
                        && Boolean.TRUE.equals(loggedIn)
                        && "CUSTOMER".equalsIgnoreCase(currentUser.getRole())) {
                    hasAccess = true;
                }

            } else {
                System.out.println("CustomerAccessFilter: session is null");
            }

            if (!hasAccess) {
                System.out.println("CustomerAccessFilter: access denied, redirecting to login");
                // Redirect to login page if not authorized
                httpResponse.sendRedirect(contextPath + "/pages/user/login.xhtml");
                return;
            }
        }

        // Continue with the request
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
