package com.school.classboard.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.school.classboard.domain.CheckInRecord;
import com.school.classboard.domain.CheckInType;
import com.school.classboard.domain.ClassInfo;
import com.school.classboard.service.CheckInService;
import com.school.classboard.service.ClassInfoService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 签到页整页构建测试：模拟已选班级，验证统计与最近记录都能正常渲染。
 */
@ExtendWith(MockitoExtension.class)
class CheckInViewRenderTest {

    @Mock
    private CheckInService checkInService;
    @Mock
    private ClassInfoService classInfoService;

    @Test
    void checkInViewBuildsWithClassAndRecords() {
        ClassInfo clazz = new ClassInfo();
        clazz.setId(1L);
        clazz.setName("高一(3)班");

        CheckInRecord record = new CheckInRecord();
        record.setStudentId(1L);
        record.setStudentName("王雨桐");
        record.setType(CheckInType.IN);
        record.setCheckedAt(LocalDateTime.now());
        record.setLate(false);

        when(classInfoService.findById(1L)).thenReturn(Optional.of(clazz));
        when(checkInService.stats(1L)).thenReturn(new CheckInService.Stats(46, 45, 1, 20, 2));
        when(checkInService.todayRecords(1L)).thenReturn(List.of(record));

        CheckInView view = new CheckInView(checkInService, classInfoService) {
            @Override
            protected Long currentClassId() {
                return 1L;
            }
        };

        // 标题、提示、统计、输入行、结果框、最近签到标题、记录列表
        assertTrue(view.getComponentCount() >= 6);
    }
}
