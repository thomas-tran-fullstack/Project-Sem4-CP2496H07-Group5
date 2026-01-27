package controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@WebServlet(name = "ImageServlet", urlPatterns = {"/resources/*"})
public class ImageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        System.out.println("DEBUG ImageServlet: pathInfo = " + pathInfo);

        if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Remove leading /
        String relativePath = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        System.out.println("DEBUG ImageServlet: relativePath = " + relativePath);
        
        Path filePath = null;
        
        // Handle different upload directories
        if (relativePath.startsWith("uploads/products/")) {
            // Product images - /resources/uploads/products/filename
            String fileName = relativePath.substring("uploads/products/".length());
            Path uploadPath = Paths.get(System.getProperty("user.home"), "uploads", "products");
            filePath = uploadPath.resolve(fileName);
            System.out.println("DEBUG ImageServlet: Product image path = " + filePath);
        } else if (relativePath.startsWith("products/")) {
            // Product images - /resources/products/filename
            String fileName = relativePath.substring("products/".length());
            Path uploadPath = Paths.get(System.getProperty("user.home"), "uploads", "products");
            filePath = uploadPath.resolve(fileName);
        } else if (relativePath.startsWith("uploads/banners/")) {
            String fileName = relativePath.substring("uploads/banners/".length());
            Path uploadPath = Paths.get(System.getProperty("user.home"), "uploads", "banners");
            filePath = uploadPath.resolve(fileName);
        } else if (relativePath.startsWith("banners/")) {
            String fileName = relativePath.substring("banners/".length());
            String realPath = getServletContext().getRealPath("/resources/uploads/banners/");
            if (realPath != null) {
                Path webappPath = Paths.get(realPath).resolve(fileName);
                if (Files.exists(webappPath) && Files.isRegularFile(webappPath)) {
                    filePath = webappPath;
                }
            }
            if (filePath == null) {
                Path userHomePath = Paths.get(System.getProperty("user.home"), "uploads", "banners");
                filePath = userHomePath.resolve(fileName);
            }
        } else if (relativePath.startsWith("uploads/categories/")) {
            String fileName = relativePath.substring("uploads/categories/".length());
            String realPath = getServletContext().getRealPath("/resources/uploads/categories/");
            if (realPath != null) {
                Path webappPath = Paths.get(realPath);
                filePath = webappPath.resolve(fileName);
                if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                    Path userHomePath = Paths.get(System.getProperty("user.home"), "uploads", "categories");
                    filePath = userHomePath.resolve(fileName);
                }
            }
        } else if (relativePath.startsWith("categories/")) {
            String fileName = relativePath.substring("categories/".length());
            String realPath = getServletContext().getRealPath("/resources/uploads/categories/");
            if (realPath != null) {
                Path webappPath = Paths.get(realPath);
                filePath = webappPath.resolve(fileName);
                if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                    Path userHomePath = Paths.get(System.getProperty("user.home"), "uploads", "categories");
                    filePath = userHomePath.resolve(fileName);
                }
            }
        } else if (relativePath.startsWith("uploads/payment_proofs/")) {
            String fileName = relativePath.substring("uploads/payment_proofs/".length());
            String realPath = getServletContext().getRealPath("/resources/uploads/payment_proofs/");
            if (realPath != null) {
                Path webappPath = Paths.get(realPath);
                filePath = webappPath.resolve(fileName);
                if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                    Path userHomePath = Paths.get(System.getProperty("user.home"), "uploads", "payment_proofs");
                    filePath = userHomePath.resolve(fileName);
                }
            }
        } else {
            // Default: try as-is under user home/uploads
            Path uploadPath = Paths.get(System.getProperty("user.home"), "uploads");
            filePath = uploadPath.resolve(relativePath);
        }

        if (filePath == null || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            System.out.println("DEBUG ImageServlet: File not found at " + filePath);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = getServletContext().getMimeType(filePath.getFileName().toString());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        System.out.println("DEBUG ImageServlet: Serving file " + filePath + " with type " + contentType);

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            inputStream.transferTo(response.getOutputStream());
        }
    }
}
