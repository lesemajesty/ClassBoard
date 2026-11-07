package com.school.classboard.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 图片上传：保存到本地目录，返回可直接访问的 /uploads/xxx 地址。
 */
@Service
public class ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");

    private final Path root;

    public ImageStorageService(@Value("${classboard.upload-dir:uploads}") String dir) {
        try {
            this.root = Paths.get(dir).toAbsolutePath().normalize();
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建图片上传目录", e);
        }
    }

    public Path getRoot() {
        return root;
    }

    /**
     * 保存图片并返回访问路径。
     *
     * @param in           文件流
     * @param originalName 原始文件名，仅用于取扩展名
     * @param contentType  浏览器上报的 MIME 类型
     */
    public String store(InputStream in, String originalName, String contentType) {
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("只支持图片文件");
        }
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 png / jpg / jpeg / gif / webp 格式");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Files.copy(in, root.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("图片保存失败", e);
        }
        return "/uploads/" + fileName;
    }

    private String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
