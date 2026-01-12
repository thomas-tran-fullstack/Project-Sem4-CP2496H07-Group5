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
 * Filter to restrict access to admin pages to users with ADMIN role only
 * @author TRUONG LAM
 */
public class AdminAccessFilter implements Filter {

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

        // Check if the request is for admin pages
        if (requestURI.startsWith(contextPath + "/pages/admin/")) {
            HttpSession session = httpRequest.getSession(false);

            boolean hasAccess = false;

            if (session != null) {
                // Check if user is logged in and has ADMIN role
                Users currentUser = (Users) session.getAttribute("currentUser");
                Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");

                if (currentUser != null && loggedIn != null && loggedIn &&
                    "ADMIN".equalsIgnoreCase(currentUser.getRole())) {
                    hasAccess = true;
                }
            }

            if (!hasAccess) {
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
