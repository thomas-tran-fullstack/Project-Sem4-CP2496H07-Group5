package controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Remove leading /
        String relativePath = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        Path filePath = null;
        // Handle different upload directories
        if (relativePath.startsWith("products/")) {
            // Product images
            String fileName = relativePath.substring("products/".length());
            Path uploadPath = Paths.get(System.getProperty("user.home"), "uploads", "products");
            filePath = uploadPath.resolve(fileName);
        } else if (relativePath.startsWith("banners/")) {
    String fileName = relativePath.substring("banners/".length());
    String realPath = getServletContext()
            .getRealPath("/resources/uploads/banners/");
    if (realPath != null) {
        Path webappPath = Paths.get(realPath).resolve(fileName);
        if (Files.exists(webappPath) && Files.isRegularFile(webappPath)) {
            filePath = webappPath;
        }
    }
    if (filePath == null) {
        Path userHomePath = Paths.get(
                System.getProperty("user.home"),
                "uploads",
                "banners",
                fileName
        );
        if (Files.exists(userHomePath) && Files.isRegularFile(userHomePath)) {
            filePath = userHomePath;
        }
    }
}
else if (relativePath.startsWith("categories/")) {
            // Category images - first try webapp resources/uploads/categories, then user.home/uploads/categories
            String fileName = relativePath.substring("categories/".length());
            String realPath = getServletContext().getRealPath("/resources/uploads/categories/");
            if (realPath != null) {
                Path webappPath = Paths.get(realPath);
                filePath = webappPath.resolve(fileName);
                // If not found in webapp, try user.home
                if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                    Path userHomePath = Paths.get(System.getProperty("user.home"), "uploads", "categories");
                    filePath = userHomePath.resolve(fileName);
                }
            }
        } else if (relativePath.startsWith("payment_proofs/")) {
            // Payment proof images - first try webapp resources/uploads/payment_proofs, then user.home/uploads/payment_proofs
            String fileName = relativePath.substring("payment_proofs/".length());
            String realPath = getServletContext().getRealPath("/resources/uploads/payment_proofs/");
            if (realPath != null) {
                Path webappPath = Paths.get(realPath);
                filePath = webappPath.resolve(fileName);
                // If not found in webapp, try user.home
                if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                    Path userHomePath = Paths.get(System.getProperty("user.home"), "uploads", "payment_proofs");
                    filePath = userHomePath.resolve(fileName);
                }
            }
        } else {
            // Handle payment_proofs
            if (relativePath.startsWith("uploads/payment_proofs/")) {
                relativePath = relativePath.substring("uploads/payment_proofs/".length());
                Path uploadPath = Paths.get(System.getProperty("user.home"), "uploads", "payment_proofs");
                filePath = uploadPath.resolve(relativePath);
                if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                    // If not found, try webapp resources/uploads/payment_proofs
                    String realPath = getServletContext().getRealPath("/resources/uploads/payment_proofs/");
                    if (realPath != null) {
                        Path webappPath = Paths.get(realPath);
                        filePath = webappPath.resolve(relativePath);
                    }
                }
            } else if (relativePath.startsWith("payment_proofs/")) {
                relativePath = relativePath.substring("payment_proofs/".length());
                Path uploadPath = Paths.get(System.getProperty("user.home"), "uploads", "payment_proofs");
                filePath = uploadPath.resolve(relativePath);
                if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                    // If not found, try webapp resources/uploads/payment_proofs
                    String realPath = getServletContext().getRealPath("/resources/uploads/payment_proofs/");
                    if (realPath != null) {
                        Path webappPath = Paths.get(realPath);
                        filePath = webappPath.resolve(relativePath);
                    }
                }
            } else {
                // Direct file access for products (when pathInfo is just the filename)
                if (relativePath.startsWith("uploads/products/")) {
                    relativePath = relativePath.substring("uploads/products/".length());
                } else if (relativePath.startsWith("products/")) {
                    relativePath = relativePath.substring("products/".length());
                }
                // First try user.home/uploads/products
                Path uploadPath = Paths.get(System.getProperty("user.home"), "uploads", "products");
                filePath = uploadPath.resolve(relativePath);
                if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                    // If not found, try webapp resources/uploads/products
                    String realPath = getServletContext().getRealPath("/resources/uploads/products/");
                    if (realPath != null) {
                        Path webappPath = Paths.get(realPath);
                        filePath = webappPath.resolve(relativePath);
                    }
                }
            }
        }

        if (filePath == null || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = getServletContext().getMimeType(filePath.getFileName().toString());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            inputStream.transferTo(response.getOutputStream());
        }
    }
}
