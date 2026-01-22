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

        // Only support userId (AvatarUploadServlet saves as user_<userId>.*)
        if (userId == null || userId.trim().isEmpty()) {
            serveDefault(resp);
            return;
        }
        userId = userId.trim();

        String fileNamePrefix = "user_" + userId;

        // Look for avatar files in multiple candidate directories.
        // Prefer a persistent user.home folder first (matches AvatarUploadServlet).
        java.util.List<File> candidateDirs = new java.util.ArrayList<>();
        candidateDirs.add(new File(System.getProperty("user.home") + File.separator + ".ezmart_avatars"));

        String realPath = getServletContext().getRealPath("/WEB-INF/uploads/avatars");
        if (realPath != null) {
            candidateDirs.add(new File(realPath));
        }
        candidateDirs.add(new File(System.getProperty("java.io.tmpdir") + File.separator + "ezmart_avatars"));

        File img = null;
        for (File dir : candidateDirs) {
            if (dir == null || !dir.exists() || !dir.isDirectory()) {
                continue;
            }

            // try common extensions
            for (String ext : new String[]{"jpg", "jpeg", "png", "gif"}) {
                File f = new File(dir, fileNamePrefix + "." + ext);
                if (f.exists() && f.isFile()) {
                    img = f;
                    break;
                }
            }
            if (img != null) {
                break;
            }
        }

        if (img == null) {
            serveDefault(resp);
            return;
        }

        // Serve found image
        String lc = img.getName().toLowerCase();
        String contentType = "image/jpeg";
        if (lc.endsWith(".png")) {
            contentType = "image/png";
        } else if (lc.endsWith(".gif")) {
            contentType = "image/gif";
        }

        resp.setContentType(contentType);
        resp.setHeader("Cache-Control", "public, max-age=86400");

        try (FileInputStream fis = new FileInputStream(img); OutputStream os = resp.getOutputStream()) {
            byte[] buf = new byte[4096];
            int r;
            while ((r = fis.read(buf)) != -1) {
                os.write(buf, 0, r);
            }
        }
    }

    private void serveDefault(HttpServletResponse resp) throws IOException {
        // Serve default avatar from /images/user.png (inside web root)
        try (java.io.InputStream in = getServletContext().getResourceAsStream("/images/user.png")) {
            if (in == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            resp.setContentType("image/png");
            resp.setHeader("Cache-Control", "public, max-age=3600");
            try (OutputStream os = resp.getOutputStream()) {
                byte[] buf = new byte[4096];
                int r;
                while ((r = in.read(buf)) != -1) {
                    os.write(buf, 0, r);
                }
            }
        }
    }
}
