package controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ejb.EJB;
import entityclass.Users;
import sessionbeans.UsersFacadeLocal;

@WebServlet(name = "UserServlet", urlPatterns = {"/resources/api/users"})
public class UserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private UsersFacadeLocal usersFacade;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        
        try {
            String role = request.getParameter("role");

            if (role != null && !role.isEmpty()) {
                // Get users by role
                List<Users> users = usersFacade.findUsersByRole(role);
                
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Users user : users) {
                    JsonObject userJson = Json.createObjectBuilder()
                        .add("id", user.getUserID())
                        .add("name", user.getUsername() != null ? user.getUsername() : user.getEmail())
                        .add("email", user.getEmail())
                        .add("role", user.getRole())
                        .build();
                    arrayBuilder.add(userJson);
                }

                try (PrintWriter out = response.getWriter()) {
                    out.print(arrayBuilder.build().toString());
                }
            } else {
                // Get all users (limited)
                List<Users> users = usersFacade.findAll();
                
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                int count = 0;
                for (Users user : users) {
                    if (count >= 50) break; // Limit to 50 users
                    
                    JsonObject userJson = Json.createObjectBuilder()
                        .add("id", user.getUserID())
                        .add("name", user.getUsername() != null ? user.getUsername() : user.getEmail())
                        .add("email", user.getEmail())
                        .add("role", user.getRole())
                        .build();
                    arrayBuilder.add(userJson);
                    count++;
                }

                try (PrintWriter out = response.getWriter()) {
                    out.print(arrayBuilder.build().toString());
                }
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = response.getWriter()) {
                JsonObject error = Json.createObjectBuilder()
                    .add("error", e.getMessage())
                    .build();
                out.print(error.toString());
            }
        }
    }
}
