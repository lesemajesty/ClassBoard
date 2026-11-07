package com.school.classboard.repository;

import com.school.classboard.domain.ClassInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassInfoRepository extends JpaRepository<ClassInfo, Long> {
}
