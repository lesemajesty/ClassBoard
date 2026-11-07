package com.school.classboard.service;

import com.school.classboard.domain.ClassInfo;
import com.school.classboard.repository.AnnouncementRepository;
import com.school.classboard.repository.ClassInfoRepository;
import com.school.classboard.repository.CourseRepository;
import com.school.classboard.repository.TerminalDeviceRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 班级信息业务逻辑。
 */
@Service
@Transactional(readOnly = true)
public class ClassInfoService {

    private final ClassInfoRepository repository;
    private final CourseRepository courseRepository;
    private final AnnouncementRepository announcementRepository;
    private final TerminalDeviceRepository terminalDeviceRepository;

    public ClassInfoService(ClassInfoRepository repository,
                            CourseRepository courseRepository,
                            AnnouncementRepository announcementRepository,
                            TerminalDeviceRepository terminalDeviceRepository) {
        this.repository = repository;
        this.courseRepository = courseRepository;
        this.announcementRepository = announcementRepository;
        this.terminalDeviceRepository = terminalDeviceRepository;
    }

    public List<ClassInfo> findAll() {
        return repository.findAll();
    }

    public Optional<ClassInfo> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public ClassInfo save(ClassInfo classInfo) {
        return repository.save(classInfo);
    }

    /**
     * 删除班级并清理关联数据：课表与班级定向公告一并删除，已绑定设备自动解绑。
     * 不这样做会留下指向已删除班级的孤儿记录，后台会显示「已删除的班级」。
     */
    @Transactional
    public void delete(Long id) {
        courseRepository.deleteByClassId(id);
        announcementRepository.deleteByClassId(id);
        terminalDeviceRepository.unbindClass(id);
        repository.deleteById(id);
    }
}
