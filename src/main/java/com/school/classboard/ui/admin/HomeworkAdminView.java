package com.school.classboard.ui.admin;

import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.Homework;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.service.HomeworkService;
import com.school.classboard.ui.ClassSession;
import com.school.classboard.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 后台：班级作业管理。
 */
@Route(value = "admin/homework", layout = MainLayout.class)
@PageTitle("作业管理")
@PermitAll
public class HomeworkAdminView extends Composite<VerticalLayout> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final HomeworkService homeworkService;
    private final ClassInfoService classInfoService;

    private final ComboBox<ClassInfo> classSelect = new ComboBox<>("班级");
    private final Grid<Homework> grid = new Grid<>(Homework.class, false);
    private ClassInfo currentClass;

    public HomeworkAdminView(HomeworkService homeworkService, ClassInfoService classInfoService) {
        this.homeworkService = homeworkService;
        this.classInfoService = classInfoService;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("作业管理");
        title.getStyle().set("margin", "0");

        Button add = new Button("布置作业", e -> openEditor(new Homework()));
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

        grid.addColumn(h -> h.getDueDate().format(FMT)).setHeader("截止日期").setAutoWidth(true);
        grid.addColumn(Homework::getSubject).setHeader("科目").setAutoWidth(true);
        grid.addColumn(h -> h.getContent() == null ? "" : h.getContent())
                .setHeader("作业内容").setAutoWidth(true);
        grid.addColumn(h -> h.getTeacher() == null ? "" : h.getTeacher())
                .setHeader("教师").setAutoWidth(true);
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
        }
        refresh();
    }

    private Component actions(Homework homework) {
        Button edit = new Button("编辑", e -> openEditor(homework));
        edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        Button delete = new Button("删除", e -> {
            homeworkService.delete(homework.getId());
            refresh();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        return new HorizontalLayout(edit, delete);
    }

    private void refresh() {
        grid.setItems(currentClass == null ? List.of() : homeworkService.forClass(currentClass.getId()));
    }

    private void openEditor(Homework homework) {
        if (currentClass == null) {
            return;
        }
        boolean isNew = homework.getId() == null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "布置作业" : "编辑作业");
        dialog.setWidth("480px");

        ComboBox<String> subject = new ComboBox<>("科目");
        subject.setItems("语文", "数学", "英语", "物理", "化学", "生物",
                "历史", "地理", "政治", "信息技术");
        subject.setAllowCustomValue(true);
        subject.setValue(homework.getSubject() == null ? "语文" : homework.getSubject());

        TextArea content = new TextArea("作业内容");
        content.setWidthFull();
        content.setHeight("120px");
        content.setValue(homework.getContent() == null ? "" : homework.getContent());

        TextField teacher = new TextField("教师");
        teacher.setValue(homework.getTeacher() == null ? "" : homework.getTeacher());

        DatePicker dueDate = new DatePicker("截止日期");
        dueDate.setValue(homework.getDueDate() == null ? LocalDate.now() : homework.getDueDate());

        VerticalLayout form = new VerticalLayout(subject, content, teacher, dueDate);
        form.setPadding(false);
        form.setWidthFull();
        dialog.add(form);

        Button save = new Button("保存", e -> {
            if (subject.getValue() == null || subject.getValue().isBlank() || content.isEmpty()) {
                Notification.show("请填写科目和作业内容");
                return;
            }
            homework.setClassId(currentClass.getId());
            homework.setSubject(subject.getValue().trim());
            homework.setContent(content.getValue());
            homework.setTeacher(teacher.getValue());
            homework.setDueDate(dueDate.getValue());
            homeworkService.save(homework);
            refresh();
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("取消", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }
}
