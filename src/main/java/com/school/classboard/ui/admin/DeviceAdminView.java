package com.school.classboard.ui.admin;

import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.TerminalDevice;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.service.TerminalDeviceService;
import com.school.classboard.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台：班牌终端设备管理。在这里登记设备并绑定班级，终端访问 /terminal/设备编码 即自动进入对应班级。
 */
@Route(value = "admin/devices", layout = MainLayout.class)
@PageTitle("设备管理")
@PermitAll
public class DeviceAdminView extends Composite<VerticalLayout> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TerminalDeviceService deviceService;
    private final ClassInfoService classInfoService;
    private final Grid<TerminalDevice> grid = new Grid<>(TerminalDevice.class, false);

    private Map<Long, ClassInfo> classById = Map.of();

    public DeviceAdminView(TerminalDeviceService deviceService, ClassInfoService classInfoService) {
        this.deviceService = deviceService;
        this.classInfoService = classInfoService;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("设备管理");
        title.getStyle().set("margin", "0");

        Button add = new Button("新增设备", e -> openEditor(new TerminalDevice()));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(title, add);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        root.add(header);

        grid.addColumn(TerminalDevice::getCode).setHeader("设备编码").setAutoWidth(true);
        grid.addColumn(d -> text(d.getName())).setHeader("设备名称").setAutoWidth(true);
        grid.addColumn(d -> text(d.getLocation())).setHeader("安装位置").setAutoWidth(true);
        grid.addColumn(this::boundClassName).setHeader("绑定班级").setAutoWidth(true);
        grid.addColumn(d -> "/terminal/" + text(d.getCode())).setHeader("终端地址").setAutoWidth(true);
        grid.addComponentColumn(this::statusBadge).setHeader("状态").setAutoWidth(true);
        grid.addColumn(d -> d.getLastSeenAt() == null ? "从未" : d.getLastSeenAt().format(FMT))
                .setHeader("最近在线").setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("操作").setAutoWidth(true);
        grid.setSizeFull();
        root.add(grid);
        root.expand(grid);

        refresh();
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String boundClassName(TerminalDevice device) {
        if (device.getClassId() == null) {
            return "未绑定";
        }
        ClassInfo clazz = classById.get(device.getClassId());
        return clazz == null ? "已删除的班级" : clazz.getName();
    }

    private Component statusBadge(TerminalDevice device) {
        Span span = new Span(device.isEnabled() ? "启用" : "停用");
        span.getStyle()
                .set("font-size", "0.8rem")
                .set("padding", "0.15rem 0.5rem")
                .set("border-radius", "999px")
                .set("background", device.isEnabled() ? "#dcfce7" : "#fee2e2")
                .set("color", device.isEnabled() ? "#166534" : "#991b1b");
        return span;
    }

    private Component actions(TerminalDevice device) {
        Button edit = new Button("编辑", e -> openEditor(device));
        edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        Button delete = new Button("删除", e -> {
            deviceService.delete(device.getId());
            refresh();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        return new HorizontalLayout(edit, delete);
    }

    private void refresh() {
        List<ClassInfo> classes = classInfoService.findAll();
        classById = classes.stream().collect(Collectors.toMap(ClassInfo::getId, Function.identity()));
        grid.setItems(deviceService.findAll());
    }

    private void openEditor(TerminalDevice device) {
        boolean isNew = device.getId() == null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "新增设备" : "编辑设备");

        TextField code = new TextField("设备编码");
        code.setPlaceholder("如：CB-A301-01，终端地址即 /terminal/该编码");
        code.setWidthFull();
        TextField name = new TextField("设备名称");
        name.setPlaceholder("如：教学楼A-301班牌");
        TextField location = new TextField("安装位置");
        location.setPlaceholder("如：教学楼A-301门口");

        List<ClassInfo> classes = classInfoService.findAll();
        Map<Long, ClassInfo> byId = classes.stream()
                .collect(Collectors.toMap(ClassInfo::getId, Function.identity()));

        ComboBox<ClassInfo> classSelect = new ComboBox<>("绑定班级");
        classSelect.setItems(classes);
        classSelect.setItemLabelGenerator(ClassInfo::getName);
        classSelect.setClearButtonVisible(true);
        classSelect.setWidthFull();

        Checkbox enabled = new Checkbox("启用该设备");

        VerticalLayout form = new VerticalLayout(code, name, location, classSelect, enabled);
        form.setPadding(false);
        form.setWidth("500px");

        Binder<TerminalDevice> binder = new Binder<>(TerminalDevice.class);
        binder.forField(code)
                .asRequired("请填写设备编码")
                .withValidator(c -> c == null || c.isBlank() || !deviceService.isCodeTaken(c.trim(), device.getId()),
                        "该设备编码已被占用")
                .bind(TerminalDevice::getCode, TerminalDevice::setCode);
        binder.forField(name).bind(TerminalDevice::getName, TerminalDevice::setName);
        binder.forField(location).bind(TerminalDevice::getLocation, TerminalDevice::setLocation);
        binder.forField(enabled).bind(TerminalDevice::isEnabled, TerminalDevice::setEnabled);

        binder.setBean(device);
        classSelect.setValue(device.getClassId() == null ? null : byId.get(device.getClassId()));
        dialog.add(form);

        Button save = new Button("保存", e -> {
            if (binder.validate().isOk()) {
                ClassInfo selected = classSelect.getValue();
                device.setClassId(selected == null ? null : selected.getId());
                device.setCode(device.getCode() == null ? null : device.getCode().trim());
                deviceService.save(device);
                refresh();
                dialog.close();
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("取消", e -> dialog.close());
        dialog.getFooter().add(cancel, save);

        dialog.open();
    }
}
