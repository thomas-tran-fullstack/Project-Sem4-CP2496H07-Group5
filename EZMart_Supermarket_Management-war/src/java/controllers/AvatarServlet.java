package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "AvatarServlet", urlPatterns = {"/avatar"})
public class AvatarServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = req.getParameter("userId");
        String customerId = req.getParameter("customerId");
        
        if (userId == null && customerId == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // Use customerId if provided, otherwise userId
        String fileNamePrefix = "user_";
        if (customerId != null && !customerId.isEmpty()) {
            fileNamePrefix += customerId;
        } else if (userId != null && !userId.isEmpty()) {
            fileNamePrefix += userId;
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String uploadsPath = getServletContext().getRealPath("/WEB-INF/uploads/avatars");
        System.out.println("AvatarServlet: checking path 1: " + uploadsPath);
        File dir = new File(uploadsPath != null ? uploadsPath : "");
        if (!dir.exists()) {
            // Try user.home fallback FIRST (matches AvatarUploadServlet)
            uploadsPath = System.getProperty("user.home") + File.separator + ".ezmart_avatars";
            dir = new File(uploadsPath);
            System.out.println("AvatarServlet: using user.home fallback: " + uploadsPath + ", exists=" + dir.exists());
        }
        if (!dir.exists()) {
            // Try system temp as second fallback
            uploadsPath = System.getProperty("java.io.tmpdir") + File.separator + "ezmart_avatars";
            dir = new File(uploadsPath);
            System.out.println("AvatarServlet: path 1 null, using temp fallback: " + uploadsPath + ", exists=" + dir.exists());
        }
        if (!dir.exists()) {
            // directory missing, fall through to default avatar instead of 404
            System.out.println("AvatarServlet: directory still not found, serving default avatar");
            try (java.io.InputStream in = getServletContext().getResourceAsStream("/images/user.png")) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                resp.setContentType("image/png");
                resp.setHeader("Cache-Control", "public, max-age=3600"); // Cache for 1 hour for default
                try (OutputStream os = resp.getOutputStream()) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = in.read(buf)) != -1) os.write(buf, 0, r);
                }
                return;
            }
        }

        // try common extensions
        File img = null;
        for (String ext : new String[]{"jpg","png","gif"}) {
            File f = new File(dir, fileNamePrefix + "." + ext);
            System.out.println("AvatarServlet: checking for " + fileNamePrefix + "." + ext + ": " + f.exists());
            if (f.exists()) { img = f; break; }
        }
        if (img == null) {
            // Serve default avatar from /images/user.png if available
            System.out.println("AvatarServlet: no avatar found for " + fileNamePrefix + ", serving default");
            try (java.io.InputStream in = getServletContext().getResourceAsStream("/images/user.png")) {
                if (in == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                resp.setContentType("image/png");
                resp.setHeader("Cache-Control", "public, max-age=3600"); // Cache for 1 hour for default
                try (OutputStream os = resp.getOutputStream()) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = in.read(buf)) != -1) os.write(buf, 0, r);
                }
                return;
            }
        }

        String lc = img.getName().toLowerCase();
        String contentType = "image/jpeg";
        if (lc.endsWith(".png")) contentType = "image/png";
        else if (lc.endsWith(".gif")) contentType = "image/gif";

        resp.setContentType(contentType);
        resp.setHeader("Cache-Control", "public, max-age=86400");
        try (FileInputStream fis = new FileInputStream(img); OutputStream os = resp.getOutputStream()) {
            byte[] buf = new byte[4096];
            int r;
            while ((r = fis.read(buf)) != -1) os.write(buf, 0, r);
        }
    }
}
