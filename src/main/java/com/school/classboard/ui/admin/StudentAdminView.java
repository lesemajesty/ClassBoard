package com.school.classboard.ui.admin;

import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.Student;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.service.StudentService;
import com.school.classboard.ui.ClassSession;
import com.school.classboard.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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

/**
 * 后台：学生与校园卡管理。
 */
@Route(value = "admin/students", layout = MainLayout.class)
@PageTitle("学生管理")
@PermitAll
public class StudentAdminView extends Composite<VerticalLayout> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StudentService service;
    private final ClassInfoService classInfoService;
    private final ComboBox<ClassInfo> classSelect = new ComboBox<>("班级");
    private final Grid<Student> grid = new Grid<>(Student.class, false);

    private ClassInfo currentClass;

    public StudentAdminView(StudentService service, ClassInfoService classInfoService) {
        this.service = service;
        this.classInfoService = classInfoService;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("学生管理");
        title.getStyle().set("margin", "0");

        Button add = new Button("新增学生", e -> openEditor(new Student()));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        classSelect.setWidth("220px");
        classSelect.setItemLabelGenerator(ClassInfo::getName);
        classSelect.addValueChangeListener(e -> {
            currentClass = e.getValue();
            refresh();
        });

        HorizontalLayout toolbar = new HorizontalLayout(classSelect, add);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout header = new HorizontalLayout(title, toolbar);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        root.add(header);

        grid.addColumn(Student::getName).setHeader("姓名").setAutoWidth(true);
        grid.addColumn(Student::getCardNo).setHeader("校园卡号").setAutoWidth(true);
        grid.addColumn(s -> s.getBirthday() == null ? "" : s.getBirthday().format(DATE_FMT))
                .setHeader("生日").setAutoWidth(true);
        grid.addComponentColumn(this::statusBadge).setHeader("状态").setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("操作").setAutoWidth(true);
        grid.setSizeFull();
        root.add(grid);
        root.expand(grid);

        List<ClassInfo> classes = classInfoService.findAll();
        classSelect.setItems(classes);
        if (!classes.isEmpty()) {
            Long sessionClassId = ClassSession.getClassId();
            ClassInfo initial = classes.stream()
                    .filter(c -> c.getId().equals(sessionClassId))
                    .findFirst()
                    .orElse(classes.get(0));
            classSelect.setValue(initial);
            currentClass = initial;
            refresh();
        }
    }

    private Component statusBadge(Student student) {
        Span span = new Span(student.isEnabled() ? "正常" : "停用");
        span.getStyle()
                .set("font-size", "0.8rem")
                .set("padding", "0.15rem 0.5rem")
                .set("border-radius", "999px")
                .set("background", student.isEnabled() ? "#dcfce7" : "#fee2e2")
                .set("color", student.isEnabled() ? "#166534" : "#991b1b");
        return span;
    }

    private Component actions(Student student) {
        Button edit = new Button("编辑", e -> openEditor(student));
        edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        Button delete = new Button("删除", e -> confirmDelete(student));
        delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        return new HorizontalLayout(edit, delete);
    }

    private void confirmDelete(Student student) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("删除学生");
        dialog.add(new Span("确定删除「" + student.getName() + "」吗？历史签到记录会保留，但该校园卡将无法再签到。"));
        Button cancel = new Button("取消", e -> dialog.close());
        Button confirm = new Button("确认删除", e -> {
            service.delete(student.getId());
            refresh();
            dialog.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private void refresh() {
        grid.setItems(currentClass == null ? List.of() : service.findByClass(currentClass.getId()));
    }

    private void openEditor(Student student) {
        if (currentClass == null) {
            return;
        }
        boolean isNew = student.getId() == null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "新增学生" : "编辑学生");
        dialog.setWidth("440px");

        TextField name = new TextField("姓名");
        name.setPlaceholder("如：王雨桐");
        TextField cardNo = new TextField("校园卡号");
        cardNo.setPlaceholder("刷卡时读到的号码");
        DatePicker birthday = new DatePicker("生日");
        Checkbox enabled = new Checkbox("启用校园卡");

        VerticalLayout form = new VerticalLayout(name, cardNo, birthday, enabled);
        form.setPadding(false);
        form.setWidthFull();

        Binder<Student> binder = new Binder<>(Student.class);
        binder.forField(name).asRequired("请填写姓名").bind(Student::getName, Student::setName);
        binder.forField(cardNo)
                .asRequired("请填写校园卡号")
                .withValidator(value -> value == null || value.isBlank()
                        || !service.isCardTaken(value.trim(), student.getId()), "该卡号已被占用")
                .bind(Student::getCardNo, Student::setCardNo);
        binder.forField(birthday).bind(Student::getBirthday, Student::setBirthday);
        binder.forField(enabled).bind(Student::isEnabled, Student::setEnabled);
        binder.setBean(student);
        dialog.add(form);

        Button save = new Button("保存", e -> {
            if (!binder.validate().isOk()) {
                return;
            }
            student.setClassId(currentClass.getId());
            student.setCardNo(cardNo.getValue() == null ? null : cardNo.getValue().trim());
            service.save(student);
            refresh();
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("取消", e -> dialog.close());
        dialog.getFooter().add(cancel, save);

        dialog.open();
    }
}
