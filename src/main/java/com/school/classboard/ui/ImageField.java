package com.school.classboard.ui;

import com.school.classboard.service.ImageStorageService;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import java.io.InputStream;

/**
 * 图片编辑控件：可以粘贴图片地址，也可以直接上传本地图片。
 * 上传成功后自动把地址填入输入框并显示预览。
 */
public class ImageField extends VerticalLayout {

    private final TextField url = new TextField("图片地址");
    private final Image preview = new Image();

    public ImageField(ImageStorageService storage) {
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        url.setWidthFull();
        url.setPlaceholder("/uploads/xxx 或 https://...（可留空）");
        url.addValueChangeListener(event -> updatePreview(event.getValue()));

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/webp", "image/gif");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setWidthFull();
        upload.addSucceededListener(event -> {
            try (InputStream in = buffer.getInputStream()) {
                String path = storage.store(in, event.getFileName(), event.getMIMEType());
                url.setValue(path);
                Notification.show("图片已上传");
            } catch (Exception ex) {
                Notification.show("上传失败：" + ex.getMessage());
            }
        });

        preview.setAlt("配图预览");
        preview.getStyle()
                .set("max-width", "260px")
                .set("max-height", "160px")
                .set("object-fit", "cover")
                .set("border-radius", "8px")
                .set("border", "1px solid #e2e8f0")
                .set("margin-top", "0.4rem");
        preview.setVisible(false);

        add(new Span("配图（可选）"), url, upload, preview);
    }

    public String getValue() {
        return url.getValue() == null || url.getValue().isBlank() ? null : url.getValue().trim();
    }

    public void setValue(String value) {
        url.setValue(value == null ? "" : value);
        updatePreview(value);
    }

    private void updatePreview(String value) {
        if (value == null || value.isBlank()) {
            preview.setVisible(false);
            return;
        }
        preview.setSrc(value);
        preview.setVisible(true);
    }
}
