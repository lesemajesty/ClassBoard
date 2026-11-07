package com.school.classboard.ui.admin;

import com.school.classboard.domain.ClassInfo;
import com.school.classboard.domain.LeaveRecord;
import com.school.classboard.domain.Student;
import com.school.classboard.service.ClassInfoService;
import com.school.classboard.service.LeaveService;
import com.school.classboard.service.StudentService;
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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 后台：学生请假登记。请假期间签到统计的“应到”会自动扣除。
 */
@Route(value = "admin/leaves", layout = MainLayout.class)
@PageTitle("请假登记")
@PermitAll
public class LeaveAdminView extends Composite<VerticalLayout> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final LeaveService leaveService;
    private final ClassInfoService classInfoService;
    private final StudentService studentService;

    private final ComboBox<ClassInfo> classSelect = new ComboBox<>("班级");
    private final Grid<LeaveRecord> grid = new Grid<>(LeaveRecord.class, false);
    private ClassInfo currentClass;

    public LeaveAdminView(LeaveService leaveService,
                          ClassInfoService classInfoService,
                          StudentService studentService) {
        this.leaveService = leaveService;
        this.classInfoService = classInfoService;
        this.studentService = studentService;

        VerticalLayout root = getContent();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        H2 title = new H2("请假登记");
        title.getStyle().set("margin", "0");

        Button add = new Button("新增请假", e -> openEditor());
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

        Span hint = new Span("请假期间该学生的签到“应到”会自动扣除；如需提前返校，删除对应记录即可。");
        hint.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");
        root.add(hint);

        grid.addColumn(LeaveRecord::getStudentName).setHeader("学生").setAutoWidth(true);
        grid.addColumn(LeaveRecord::getType).setHeader("类型").setAutoWidth(true);
        grid.addColumn(r -> r.getStartDate().format(FMT) + " 至 " + r.getEndDate().format(FMT))
                .setHeader("请假时间").setAutoWidth(true);
        grid.addColumn(r -> r.getReason() == null ? "" : r.getReason())
                .setHeader("事由").setAutoWidth(true);
        grid.addColumn(r -> r.isApproved() ? "已批准" : "待批").setHeader("状态").setAutoWidth(true);
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

    private Component actions(LeaveRecord record) {
        Button delete = new Button("删除", e -> {
            leaveService.delete(record.getId());
            refresh();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        return new HorizontalLayout(delete);
    }

    private void refresh() {
        grid.setItems(currentClass == null ? List.of() : leaveService.forClass(currentClass.getId()));
    }

    private void openEditor() {
        if (currentClass == null) {
            return;
        }
        List<Student> students = studentService.findEnabledByClass(currentClass.getId());
        if (students.isEmpty()) {
            Notification.show("当前班级还没有学生，请先在「学生管理」中添加");
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("新增请假 · " + currentClass.getName());
        dialog.setWidth("460px");

        ComboBox<Student> student = new ComboBox<>("学生");
        student.setItems(students);
        student.setItemLabelGenerator(s -> s.getName() + "（" + s.getCardNo() + "）");
        student.setWidthFull();

        ComboBox<String> type = new ComboBox<>("类型");
        type.setItems("病假", "事假", "公假");
        type.setValue("事假");

        DatePicker start = new DatePicker("开始日期");
        start.setValue(LocalDate.now());
        DatePicker end = new DatePicker("结束日期");
        end.setValue(LocalDate.now());

        TextArea reason = new TextArea("事由");
        reason.setWidthFull();
        reason.setHeight("80px");

        VerticalLayout form = new VerticalLayout(student, type, start, end, reason);
        form.setPadding(false);
        form.setWidthFull();
        dialog.add(form);

        Button save = new Button("保存", e -> {
            if (student.getValue() == null || start.getValue() == null || end.getValue() == null) {
                Notification.show("请选择学生和请假日期");
                return;
            }
            try {
                leaveService.create(currentClass.getId(), student.getValue().getId(),
                        type.getValue(), start.getValue(), end.getValue(), reason.getValue());
                refresh();
                dialog.close();
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("取消", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }
}
