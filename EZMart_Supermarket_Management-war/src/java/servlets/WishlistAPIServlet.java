package servlets;

import entityclass.Products;
import entityclass.WishlistItems;
import entityclass.Wishlists;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import sessionbeans.ProductsFacadeLocal;
import sessionbeans.WishlistItemsFacadeLocal;
import sessionbeans.WishlistsFacadeLocal;
import entityclass.Customers;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * REST API Servlet for Wishlist operations
 * @author TRUONG LAM
 */
@WebServlet("/api/wishlist/*")
public class WishlistAPIServlet extends HttpServlet {

    @EJB
    private WishlistsFacadeLocal wishlistsFacade;

    @EJB
    private WishlistItemsFacadeLocal wishlistItemsFacade;

    @EJB
    private ProductsFacadeLocal productsFacade;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String pathInfo = request.getPathInfo();
            HttpSession session = request.getSession();
            Object userObj = session.getAttribute("currentCustomer");

            if (userObj == null || !(userObj instanceof Customers)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(Json.createObjectBuilder()
                        .add("status", "error")
                        .add("message", "Please log in")
                        .build().toString());
                return;
            }

            Customers customer = (Customers) userObj;
            Integer customerID = customer.getCustomerID();

            if (pathInfo == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // GET /api/wishlist/add/{productID}
            if (pathInfo.startsWith("/add/")) {
                Integer productID = Integer.parseInt(pathInfo.substring(5));
                addToWishlist(response, out, customerID, productID);
            }
            // GET /api/wishlist/remove/{productID}
            else if (pathInfo.startsWith("/remove/")) {
                Integer productID = Integer.parseInt(pathInfo.substring(8));
                removeFromWishlist(response, out, customerID, productID);
            }
            // GET /api/wishlist/check/{productID}
            else if (pathInfo.startsWith("/check/")) {
                Integer productID = Integer.parseInt(pathInfo.substring(7));
                checkInWishlist(response, out, customerID, productID);
            }
            // GET /api/wishlist/count
            else if (pathInfo.equals("/count")) {
                getWishlistCount(response, out, customerID);
            }
            // GET /api/wishlist/total
            else if (pathInfo.equals("/total")) {
                getWishlistTotal(response, out, customerID);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(Json.createObjectBuilder()
                    .add("status", "error")
                    .add("message", "Invalid product ID")
                    .build().toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(Json.createObjectBuilder()
                    .add("status", "error")
                    .add("message", "Server error: " + e.getMessage())
                    .build().toString());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String pathInfo = request.getPathInfo();
            HttpSession session = request.getSession();
            Object userObj = session.getAttribute("currentCustomer");

            if (userObj == null || !(userObj instanceof Customers)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(Json.createObjectBuilder()
                        .add("status", "error")
                        .add("message", "Please log in")
                        .build().toString());
                return;
            }

            Customers customer = (Customers) userObj;
            Integer customerID = customer.getCustomerID();

            if (pathInfo == null || pathInfo.equals("/")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // GET /api/wishlist/check/{productID}
            if (pathInfo.startsWith("/check/")) {
                Integer productID = Integer.parseInt(pathInfo.substring(7));
                checkInWishlist(response, out, customerID, productID);
            }
            // GET /api/wishlist/count
            else if (pathInfo.equals("/count")) {
                getWishlistCount(response, out, customerID);
            }
            // GET /api/wishlist/total
            else if (pathInfo.equals("/total")) {
                getWishlistTotal(response, out, customerID);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(Json.createObjectBuilder()
                    .add("status", "error")
                    .add("message", "Invalid product ID")
                    .build().toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(Json.createObjectBuilder()
                    .add("status", "error")
                    .add("message", "Server error: " + e.getMessage())
                    .build().toString());
        }
    }

    private void addToWishlist(HttpServletResponse response, PrintWriter out, Integer customerID, Integer productID) {
        try {
            Wishlists wishlist = wishlistsFacade.findByCustomerID(customerID);
            if (wishlist == null) {
                // Create new wishlist if not exists
                Customers customer = new Customers();
                customer.setCustomerID(customerID);
                wishlist = new Wishlists();
                wishlist.setCustomerID(customer);
                wishlist.setCreatedAt(new java.util.Date());
                wishlist.setUpdatedAt(new java.util.Date());
                wishlistsFacade.create(wishlist);
            }

            // Check if product already in wishlist
            WishlistItems existing = wishlistItemsFacade.findByWishlistIDAndProductID(
                    wishlist.getWishlistID(), productID);
            if (existing != null) {
                out.print(Json.createObjectBuilder()
                        .add("status", "warning")
                        .add("message", "This product is already in your wishlist")
                        .build().toString());
                return;
            }

            Products product = productsFacade.find(productID);
            if (product == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(Json.createObjectBuilder()
                        .add("status", "error")
                        .add("message", "Product does not exist")
                        .build().toString());
                return;
            }

            WishlistItems item = new WishlistItems();
            item.setWishlistID(wishlist);
            item.setProductID(product);
            item.setAddedAt(new java.util.Date());
            wishlistItemsFacade.create(item);

            out.print(Json.createObjectBuilder()
                    .add("status", "success")
                    .add("message", "Added to wishlist")
                    .add("wishlistCount", wishlistItemsFacade.getWishlistItemCount(wishlist.getWishlistID()))
                    .build().toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(Json.createObjectBuilder()
                    .add("status", "error")
                    .add("message", "Error: " + e.getMessage())
                    .build().toString());
        }
    }

    private void removeFromWishlist(HttpServletResponse response, PrintWriter out, Integer customerID, Integer productID) {
        try {
            Wishlists wishlist = wishlistsFacade.findByCustomerID(customerID);
            if (wishlist == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(Json.createObjectBuilder()
                        .add("status", "error")
                        .add("message", "Wishlist does not exist")
                        .build().toString());
                return;
            }

            boolean success = wishlistItemsFacade.deleteItemFromWishlist(
                    wishlist.getWishlistID(), productID);

            if (success) {
                out.print(Json.createObjectBuilder()
                        .add("status", "success")
                        .add("message", "Removed from wishlist")
                        .add("wishlistCount", wishlistItemsFacade.getWishlistItemCount(wishlist.getWishlistID()))
                        .build().toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(Json.createObjectBuilder()
                        .add("status", "error")
                        .add("message", "Product is not in your wishlist")
                        .build().toString());
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(Json.createObjectBuilder()
                    .add("status", "error")
                    .add("message", "Error: " + e.getMessage())
                    .build().toString());
        }
    }

    private void checkInWishlist(HttpServletResponse response, PrintWriter out, Integer customerID, Integer productID) {
        try {
            Wishlists wishlist = wishlistsFacade.findByCustomerID(customerID);
            if (wishlist == null) {
                out.print(Json.createObjectBuilder()
                        .add("status", "success")
                        .add("inWishlist", false)
                        .build().toString());
                return;
            }

            WishlistItems item = wishlistItemsFacade.findByWishlistIDAndProductID(
                    wishlist.getWishlistID(), productID);

            out.print(Json.createObjectBuilder()
                    .add("status", "success")
                    .add("inWishlist", item != null)
                    .build().toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(Json.createObjectBuilder()
                    .add("status", "error")
                    .add("message", "Error: " + e.getMessage())
                    .build().toString());
        }
    }

    private void getWishlistCount(HttpServletResponse response, PrintWriter out, Integer customerID) {
        try {
            Wishlists wishlist = wishlistsFacade.findByCustomerID(customerID);
            int count = 0;
            if (wishlist != null) {
                count = wishlistItemsFacade.getWishlistItemCount(wishlist.getWishlistID());
            }

            out.print(Json.createObjectBuilder()
                    .add("status", "success")
                    .add("count", count)
                    .build().toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(Json.createObjectBuilder()
                    .add("status", "error")
                    .add("message", "Error: " + e.getMessage())
                    .build().toString());
        }
    }

    private void getWishlistTotal(HttpServletResponse response, PrintWriter out, Integer customerID) {
        try {
            Wishlists wishlist = wishlistsFacade.findByCustomerID(customerID);
            double total = 0.0;
            int count = 0;
            if (wishlist != null) {
                total = wishlistItemsFacade.getTotalWishlistPrice(wishlist.getWishlistID());
                count = wishlistItemsFacade.getWishlistItemCount(wishlist.getWishlistID());
            }

            out.print(Json.createObjectBuilder()
                    .add("status", "success")
                    .add("total", total)
                    .add("count", count)
                    .build().toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(Json.createObjectBuilder()
                    .add("status", "error")
                    .add("message", "Error: " + e.getMessage())
                    .build().toString());
        }
    }
}
