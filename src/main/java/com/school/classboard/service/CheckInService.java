package com.school.classboard.service;

import com.school.classboard.domain.CheckInRecord;
import com.school.classboard.domain.CheckInType;
import com.school.classboard.domain.Student;
import com.school.classboard.repository.CheckInRecordRepository;
import com.school.classboard.repository.StudentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 校园卡签到：学生刷卡一次记为到校，再刷一次记为离校；超过配置时间视为迟到。
 */
@Service
@Transactional(readOnly = true)
public class CheckInService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 刷卡结果，直接给班牌做反馈 */
    public record Result(boolean ok, String title, String detail, CheckInRecord record) {
    }

    /** 班级当日签到统计 */
    public record Stats(long expected, long present, long late, long left, long leave) {
    }

    private final StudentRepository studentRepository;
    private final CheckInRecordRepository recordRepository;
    private final LeaveService leaveService;
    private final LocalTime lateTime;

    public CheckInService(StudentRepository studentRepository,
                          CheckInRecordRepository recordRepository,
                          LeaveService leaveService,
                          @Value("${classboard.checkin.late-time:07:30}") String lateTime) {
        this.studentRepository = studentRepository;
        this.recordRepository = recordRepository;
        this.leaveService = leaveService;
        this.lateTime = LocalTime.parse(lateTime);
    }

    @Transactional
    public Result checkIn(Long classId, String cardNo) {
        if (classId == null) {
            return fail("未选择班级", "请先在登录页选择班级");
        }
        if (cardNo == null || cardNo.isBlank()) {
            return fail("请刷卡", "把校园卡放在读卡器上，或手动输入卡号");
        }

        Student student = studentRepository.findByCardNo(cardNo.trim()).orElse(null);
        if (student == null) {
            return fail("卡号未登记", "请联系班主任在后台「学生管理」中登记校园卡");
        }
        if (!student.isEnabled()) {
            return fail("校园卡已停用", student.getName() + " 的校园卡已被停用");
        }
        if (!classId.equals(student.getClassId())) {
            return fail("不是本班学生", student.getName() + " 不属于当前班级");
        }

        LocalDate today = LocalDate.now();
        if (leaveService.isOnLeave(student.getId(), today)) {
            return fail("今日请假", student.getName() + " 今天在请假名单中，无需签到");
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<CheckInRecord> last = recordRepository
                .findTopByStudentIdAndCheckedAtAfterOrderByCheckedAtDesc(student.getId(), now.toLocalDate().atStartOfDay());

        CheckInType type;
        boolean late = false;
        if (last.isEmpty()) {
            type = CheckInType.IN;
            late = now.toLocalTime().isAfter(lateTime);
        } else if (last.get().getType() == CheckInType.IN) {
            type = CheckInType.OUT;
        } else {
            return fail("今日已完成签到签退", student.getName() + "，明天再来吧");
        }

        CheckInRecord record = new CheckInRecord();
        record.setStudentId(student.getId());
        record.setStudentName(student.getName());
        record.setClassId(student.getClassId());
        record.setType(type);
        record.setCheckedAt(now);
        record.setLate(late);
        recordRepository.save(record);

        String title = type == CheckInType.IN ? (late ? "迟到签到" : "签到成功") : "签退成功";
        return new Result(true, title, student.getName() + " · " + now.format(TIME_FMT), record);
    }

    public Stats stats(Long classId) {
        if (classId == null) {
            return new Stats(0, 0, 0, 0, 0);
        }
        List<CheckInRecord> records = todayRecords(classId);
        long registered = studentRepository.findByClassIdAndEnabledTrueOrderByNameAsc(classId).size();
        long leave = leaveService.todayForClass(classId).stream()
                .map(com.school.classboard.domain.LeaveRecord::getStudentId)
                .distinct()
                .count();
        long expected = Math.max(0, registered - leave);
        Set<Long> present = records.stream()
                .filter(r -> r.getType() == CheckInType.IN)
                .map(CheckInRecord::getStudentId)
                .collect(Collectors.toSet());
        long late = records.stream()
                .filter(r -> r.getType() == CheckInType.IN && r.isLate())
                .count();
        Set<Long> left = records.stream()
                .filter(r -> r.getType() == CheckInType.OUT)
                .map(CheckInRecord::getStudentId)
                .collect(Collectors.toSet());
        return new Stats(expected, present.size(), late, left.size(), leave);
    }

    public List<CheckInRecord> todayRecords(Long classId) {
        return records(LocalDate.now(), classId);
    }

    public List<CheckInRecord> records(LocalDate date, Long classId) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        return classId == null
                ? recordRepository.findByCheckedAtBetweenOrderByCheckedAtDesc(from, to)
                : recordRepository.findByClassIdAndCheckedAtBetweenOrderByCheckedAtDesc(classId, from, to);
    }

    private Result fail(String title, String detail) {
        return new Result(false, title, detail, null);
    }
}
