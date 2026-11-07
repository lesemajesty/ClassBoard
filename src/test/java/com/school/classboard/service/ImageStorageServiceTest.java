package com.school.classboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesImageAndReturnsPublicUrl() throws Exception {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        byte[] bytes = {(byte) 0x89, 'P', 'N', 'G'};

        String url = service.store(new ByteArrayInputStream(bytes), "photo.png", "image/png");

        assertTrue(url.startsWith("/uploads/"));
        Path saved = tempDir.resolve(url.substring("/uploads/".length()));
        assertTrue(Files.exists(saved));
        assertEquals(bytes.length, Files.size(saved));
    }

    @Test
    void rejectsNonImageContentType() {
        ImageStorageService service = new ImageStorageService(tempDir.toString());

        assertThrows(IllegalArgumentException.class,
                () -> service.store(new ByteArrayInputStream(new byte[]{1}), "a.txt", "text/plain"));
    }

    @Test
    void rejectsUnsupportedImageExtension() {
        ImageStorageService service = new ImageStorageService(tempDir.toString());

        assertThrows(IllegalArgumentException.class,
                () -> service.store(new ByteArrayInputStream(new byte[]{1}), "a.bmp", "image/bmp"));
    }
}
