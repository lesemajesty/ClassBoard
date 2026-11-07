package com.school.classboard.service;

import com.school.classboard.domain.Student;
import com.school.classboard.repository.StudentRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学生与校园卡管理。
 */
@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    public List<Student> findByClass(Long classId) {
        return repository.findByClassIdOrderByNameAsc(classId);
    }

    public List<Student> findEnabledByClass(Long classId) {
        return repository.findByClassIdAndEnabledTrueOrderByNameAsc(classId);
    }

    public Optional<Student> findByCardNo(String cardNo) {
        return repository.findByCardNo(cardNo);
    }

    public boolean isCardTaken(String cardNo, Long excludeId) {
        if (cardNo == null || cardNo.isBlank()) {
            return false;
        }
        String value = cardNo.trim();
        return excludeId == null ? repository.existsByCardNo(value)
                : repository.existsByCardNoAndIdNot(value, excludeId);
    }

    /** 今天过生日的学生（只比较月日） */
    public List<Student> birthdaysToday(Long classId) {
        LocalDate today = LocalDate.now();
        return findByClass(classId).stream()
                .filter(Student::isEnabled)
                .filter(s -> s.getBirthday() != null
                        && s.getBirthday().getMonthValue() == today.getMonthValue()
                        && s.getBirthday().getDayOfMonth() == today.getDayOfMonth())
                .toList();
    }

    @Transactional
    public Student save(Student student) {
        return repository.save(student);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
