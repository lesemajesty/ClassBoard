package com.school.classboard.ui;

import com.school.classboard.domain.CheckInRecord;
import com.school.classboard.domain.ClassInfo;
import com.school.classboard.service.CheckInService;
import com.school.classboard.service.ClassInfoService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.format.DateTimeFormatter;

/**
 * 校园卡刷卡签到页：读卡器模拟键盘输入卡号并回车，也可用屏幕输入框手动输入。
 * 第一次刷卡为到校，第二次为离校。
 */
@Route(value = "checkin", layout = MainLayout.class)
@PageTitle("校园卡签到")
@AnonymousAllowed
public class CheckInView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final CheckInService checkInService;
    private final ClassInfoService classInfoService;
    private final Long classId;

    private final TextField cardInput = new TextField();
    private final Div statsRow = new Div();
    private final Div resultBox = new Div();
    private final Span resultTitle = new Span();
    private final Span resultDetail = new Span();
    private final VerticalLayout recentList = new VerticalLayout();

    public CheckInView(CheckInService checkInService, ClassInfoService classInfoService) {
        this.checkInService = checkInService;
        this.classInfoService = classInfoService;
        this.classId = currentClassId();

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        if (classId == null) {
            return;
        }

        String className = classInfoService.findById(classId).map(ClassInfo::getName).orElse("班级");
        H2 title = new H2("校园卡签到 · " + className);
        title.getStyle().set("margin", "0");

        Span hint = new Span("把校园卡放在读卡器上，或输入卡号后按回车；第一次刷卡签到，第二次刷卡签退。");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)");

        cardInput.setPlaceholder("刷卡 / 输入卡号");
        cardInput.setWidth("360px");
        cardInput.setClearButtonVisible(true);
        cardInput.getElement().setAttribute("autocomplete", "off");

        Button confirm = new Button("确认", e -> submit());
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout inputRow = new HorizontalLayout(cardInput, confirm);
        inputRow.setAlignItems(FlexComponent.Alignment.END);
        inputRow.setSpacing(true);

        statsRow.getStyle()
                .set("display", "flex")
                .set("gap", "0.75rem")
                .set("flex-wrap", "wrap");

        resultTitle.getStyle().set("font-size", "1.6rem").set("font-weight", "700");
        resultDetail.getStyle().set("font-size", "1rem");
        resultBox.add(resultTitle, resultDetail);
        resultBox.getStyle()
                .set("padding", "1rem 1.5rem")
                .set("border-radius", "12px")
                .set("min-width", "320px");
        resultBox.setVisible(false);

        recentList.setPadding(false);
        recentList.setSpacing(true);

        H3 recentTitle = new H3("最近签到");
        recentTitle.getStyle().set("margin", "0.5rem 0 0");

        add(title, hint, statsRow, inputRow, resultBox, recentTitle, recentList);
        // 读卡器等价于“输入卡号 + 回车”，直接监听输入框的回车键
        cardInput.addKeyDownListener(Key.ENTER, event -> submit());

        refresh();
        addAttachListener(e -> cardInput.focus());
    }

    /** 当前班级 id；测试中可由子类覆盖 */
    protected Long currentClassId() {
        return ClassSession.getClassId();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (ClassSession.getClassId() == null) {
            event.rerouteTo(LoginView.class);
        }
    }

    private void submit() {
        if (classId == null) {
            return;
        }
        CheckInService.Result result = checkInService.checkIn(classId, cardInput.getValue());
        cardInput.clear();

        resultTitle.setText(result.title());
        resultDetail.setText(result.detail());
        resultBox.setVisible(true);
        resultBox.getStyle()
                .set("background", result.ok() ? "#dcfce7" : "#fee2e2")
                .set("color", result.ok() ? "#166534" : "#991b1b");

        refresh();
        cardInput.focus();
    }

    private void refresh() {
        if (classId == null) {
            return;
        }
        CheckInService.Stats stats = checkInService.stats(classId);
        statsRow.removeAll();
        statsRow.add(statChip("应到", String.valueOf(stats.expected())),
                statChip("实到", String.valueOf(stats.present())),
                statChip("迟到", String.valueOf(stats.late())),
                statChip("请假", String.valueOf(stats.leave())),
                statChip("已签退", String.valueOf(stats.left())));

        recentList.removeAll();
        var records = checkInService.todayRecords(classId);
        if (records.isEmpty()) {
            recentList.add(new Span("今天还没有签到记录"));
        } else {
            records.stream().limit(8).forEach(record -> recentList.add(recordRow(record)));
        }
    }

    private Div statChip(String label, String value) {
        Span number = new Span(value);
        number.getStyle().set("font-size", "1.5rem").set("font-weight", "700").set("color", "#1d4ed8");
        Span text = new Span(label);
        text.getStyle().set("font-size", "0.85rem").set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout content = new VerticalLayout(number, text);
        content.setPadding(false);
        content.setSpacing(false);

        Div chip = new Div(content);
        chip.getStyle()
                .set("background", "#eff6ff")
                .set("border-radius", "12px")
                .set("padding", "0.6rem 1.2rem")
                .set("min-width", "96px")
                .set("text-align", "center");
        return chip;
    }

    private HorizontalLayout recordRow(CheckInRecord record) {
        Span time = new Span(record.getCheckedAt().format(TIME_FMT));
        time.getStyle().set("color", "var(--lumo-secondary-text-color)").set("min-width", "80px");

        Span name = new Span(record.getStudentName());
        name.getStyle().set("font-weight", "600");

        Span type = new Span(record.getType().getLabel());
        type.getStyle()
                .set("font-size", "0.8rem")
                .set("padding", "0.1rem 0.5rem")
                .set("border-radius", "999px")
                .set("background", record.getType() == com.school.classboard.domain.CheckInType.IN ? "#dbeafe" : "#f1f5f9")
                .set("color", record.getType() == com.school.classboard.domain.CheckInType.IN ? "#1d4ed8" : "#475569");

        HorizontalLayout row = new HorizontalLayout(time, name, type);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        if (record.isLate()) {
            Span late = new Span("迟到");
            late.getStyle().set("color", "#dc2626").set("font-size", "0.8rem");
            row.add(late);
        }
        row.getStyle().set("padding", "0.4rem 0.2rem").set("border-bottom", "1px solid #e2e8f0");
        return row;
    }
}
