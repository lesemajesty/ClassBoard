package com.school.classboard.repository;

import com.school.classboard.domain.TerminalDevice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TerminalDeviceRepository extends JpaRepository<TerminalDevice, Long> {

    Optional<TerminalDevice> findByCode(String code);

    boolean existsByCode(String code);

    /** 删除班级时解除设备绑定，避免留下指向已删除班级的孤儿记录 */
    @Modifying
    @Query("update TerminalDevice d set d.classId = null where d.classId = :classId")
    int unbindClass(@Param("classId") Long classId);
}
