package com.school.classboard.repository;

import com.school.classboard.domain.Student;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByClassIdOrderByNameAsc(Long classId);

    List<Student> findByClassIdAndEnabledTrueOrderByNameAsc(Long classId);

    Optional<Student> findByCardNo(String cardNo);

    boolean existsByCardNo(String cardNo);

    boolean existsByCardNoAndIdNot(String cardNo, Long id);
}
