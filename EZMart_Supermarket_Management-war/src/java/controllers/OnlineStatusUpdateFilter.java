package controllers;

import entityclass.Users;
import jakarta.ejb.EJB;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import sessionbeans.UsersFacadeLocal;
import utils.OnlineUserRegistry;
import java.io.IOException;
import java.util.Date;

/**
 * Filter to maintain online status by updating lastOnlineAt timestamp on every request
 * for authenticated users. This ensures that active users show "Online" status in the 
 * user management page.
 * 
 * Also tracks online users in OnlineUserRegistry to determine if user is currently logged in.
 * 
 * @author Admin
 */
@WebFilter(urlPatterns = {"/*"})
public class OnlineStatusUpdateFilter implements Filter {

    @EJB
    private UsersFacadeLocal usersFacade;

    private static final long UPDATE_INTERVAL_MS = 10000; // Update every 10 seconds
    private static final String LAST_UPDATE_KEY = "lastOnlineStatusUpdate";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);

        // Only update if user is logged in
        if (session != null) {
            try {
                Users currentUser = (Users) session.getAttribute("currentUser");
                Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");

                // Check if user is authenticated
                if (currentUser != null && loggedIn != null && loggedIn && 
                    currentUser.getUserID() != null) {
                    
                    // Mark user as online in registry (for "Online" status determination)
                    OnlineUserRegistry.markOnline(currentUser.getUserID());
                    
                    // Check if enough time has passed since last update (to avoid excessive DB writes)
                    Long lastUpdate = (Long) session.getAttribute(LAST_UPDATE_KEY);
                    long currentTime = System.currentTimeMillis();
                    
                    if (lastUpdate == null || (currentTime - lastUpdate) >= UPDATE_INTERVAL_MS) {
                        // Update lastOnlineAt in database
                        try {
                            Users user = usersFacade.find(currentUser.getUserID());
                            if (user != null) {
                                user.setLastOnlineAt(new Date());
                                usersFacade.edit(user);
                                
                                // Update the session object as well
                                currentUser.setLastOnlineAt(new Date());
                                session.setAttribute("currentUser", currentUser);
                                
                                // Record the update time to prevent excessive updates
                                session.setAttribute(LAST_UPDATE_KEY, currentTime);
                                
                                System.out.println("OnlineStatusUpdateFilter: Updated lastOnlineAt for user " + 
                                    currentUser.getUsername() + " (ID: " + currentUser.getUserID() + ")");
                            }
                        } catch (Exception e) {
                            // Log but don't fail the request
                            System.err.println("OnlineStatusUpdateFilter: Error updating lastOnlineAt: " + 
                                e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                // Log but don't fail the request
                System.err.println("OnlineStatusUpdateFilter: Error in filter: " + e.getMessage());
            }
        }

        // Continue the filter chain
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
