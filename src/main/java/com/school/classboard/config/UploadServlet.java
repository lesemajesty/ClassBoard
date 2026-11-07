package com.school.classboard.config;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 提供上传图片的访问：GET /uploads/{file}。
 *
 * <p>单独注册 Servlet，避免 Vaadin 的 /* 映射把静态图片请求吞掉。
 */
public class UploadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final transient Path root;

    public UploadServlet(Path root) {
        this.root = root;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String fileName = pathInfo.substring(1);
        if (!fileName.matches("[A-Za-z0-9._-]+")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path file = root.resolve(fileName).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = getServletContext().getMimeType(fileName);
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        response.setHeader("Cache-Control", "public, max-age=86400");
        response.setContentLengthLong(Files.size(file));
        Files.copy(file, response.getOutputStream());
    }
}
