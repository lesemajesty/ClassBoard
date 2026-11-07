package com.school.classboard.ui.admin;

import com.school.classboard.domain.ClassInfo;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

/**
 * 后台：班级信息管理（需要登录）。
 */
@Route(value = "admin/classes", layout = MainLayout.class)
@PageTitle("班级管理")
@PermitAll
public class ClassAdminView extends Composite<VerticalLayout> {

    private final ClassInfoService service;
    private final Grid<ClassInfo> grid = new Grid<>(ClassInfo.class, false);

    public ClassAdminView(ClassInfoService service) {
        this.service = service;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("班级管理");
        title.getStyle().set("margin", "0");

        Button add = new Button("新增班级", e -> openEditor(new ClassInfo()));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(title, add);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        root.add(header);

        grid.addColumn(ClassInfo::getName).setHeader("班级").setAutoWidth(true);
        grid.addColumn(c -> text(c.getGrade())).setHeader("年级").setAutoWidth(true);
        grid.addColumn(c -> text(c.getHeadTeacher())).setHeader("班主任").setAutoWidth(true);
        grid.addColumn(c -> text(c.getClassroom())).setHeader("教室").setAutoWidth(true);
        grid.addColumn(c -> c.getStudentCount() == null ? "" : String.valueOf(c.getStudentCount()))
                .setHeader("人数").setAutoWidth(true);
        grid.addColumn(c -> text(c.getMotto())).setHeader("班级格言").setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("操作").setAutoWidth(true);
        grid.setSizeFull();
        root.add(grid);
        root.expand(grid);

        refresh();
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private Component actions(ClassInfo clazz) {
        Button edit = new Button("编辑", e -> openEditor(clazz));
        edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        Button delete = new Button("删除", e -> confirmDelete(clazz));
        delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        return new HorizontalLayout(edit, delete);
    }

    /** 删除是不可恢复操作，先让管理员确认会连带清理哪些数据 */
    private void confirmDelete(ClassInfo clazz) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("删除班级");

        Span warning = new Span("确定删除「" + clazz.getName() + "」吗？该班的课表与班级公告会一并删除，"
                + "已绑定的班牌设备将自动解绑。此操作不可恢复。");
        warning.getStyle().set("line-height", "1.7").set("max-width", "420px");
        dialog.add(warning);

        Button cancel = new Button("取消", e -> dialog.close());
        Button confirm = new Button("确认删除", e -> {
            service.delete(clazz.getId());
            refresh();
            dialog.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private void refresh() {
        grid.setItems(service.findAll());
    }

    private void openEditor(ClassInfo clazz) {
        boolean isNew = clazz.getId() == null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "新增班级" : "编辑班级");

        TextField name = new TextField("班级名称");
        name.setPlaceholder("如：高一(3)班");
        TextField grade = new TextField("年级");
        grade.setPlaceholder("如：2025级");
        TextField headTeacher = new TextField("班主任");
        TextField classroom = new TextField("教室");
        IntegerField studentCount = new IntegerField("学生人数");
        TextField motto = new TextField("班级格言");
        motto.setWidthFull();

        VerticalLayout form = new VerticalLayout(name, grade, headTeacher, classroom, studentCount, motto);
        form.setPadding(false);
        form.setWidth("480px");

        Binder<ClassInfo> binder = new Binder<>(ClassInfo.class);
        binder.forField(name).asRequired("请填写班级名称").bind(ClassInfo::getName, ClassInfo::setName);
        binder.forField(grade).bind(ClassInfo::getGrade, ClassInfo::setGrade);
        binder.forField(headTeacher).bind(ClassInfo::getHeadTeacher, ClassInfo::setHeadTeacher);
        binder.forField(classroom).bind(ClassInfo::getClassroom, ClassInfo::setClassroom);
        binder.forField(studentCount).bind(ClassInfo::getStudentCount, ClassInfo::setStudentCount);
        binder.forField(motto).bind(ClassInfo::getMotto, ClassInfo::setMotto);
        binder.setBean(clazz);

        dialog.add(form);

        Button save = new Button("保存", e -> {
            if (binder.validate().isOk()) {
                service.save(clazz);
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
