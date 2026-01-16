package controllers;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Diagnostic servlet to debug session attributes
 */
@WebServlet(name = "DiagnosticServlet", urlPatterns = {"/diagnostic/session"})
public class DiagnosticServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObjectBuilder json = Json.createObjectBuilder();
        
        HttpSession session = request.getSession(false);
        
        if (session == null) {
            json.add("hasSession", false);
            json.add("sessionId", "null");
            response.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(json.build().toString());
            }
            return;
        }

        json.add("hasSession", true);
        json.add("sessionId", session.getId());
        
        JsonArrayBuilder attributes = Json.createArrayBuilder();
        java.util.Enumeration<String> names = session.getAttributeNames();
        
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Object value = session.getAttribute(name);
            
            JsonObjectBuilder attr = Json.createObjectBuilder();
            attr.add("name", name);
            attr.add("type", value != null ? value.getClass().getSimpleName() : "null");
            attr.add("value", value != null ? value.toString() : "null");
            
            // Special handling for known attributes
            if ("currentCustomerId".equals(name) && value != null) {
                attr.add("isCurrentCustomerId", true);
            }
            if ("currentCustomer".equals(name) && value != null) {
                attr.add("isCurrentCustomer", true);
            }
            if ("currentUser".equals(name) && value != null) {
                attr.add("isCurrentUser", true);
            }
            
            attributes.add(attr.build());
        }
        
        json.add("attributes", attributes.build());
        
        response.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.build().toString());
        }
    }
}
