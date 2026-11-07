package com.school.classboard.service;

import com.school.classboard.domain.TerminalDevice;
import com.school.classboard.repository.TerminalDeviceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 班牌终端设备业务逻辑。
 */
@Service
@Transactional(readOnly = true)
public class TerminalDeviceService {

    private final TerminalDeviceRepository repository;

    public TerminalDeviceService(TerminalDeviceRepository repository) {
        this.repository = repository;
    }

    public List<TerminalDevice> findAll() {
        return repository.findAll();
    }

    public Optional<TerminalDevice> findByCode(String code) {
        return repository.findByCode(code);
    }

    public boolean isCodeTaken(String code, Long excludeId) {
        return repository.findByCode(code)
                .filter(d -> !d.getId().equals(excludeId))
                .isPresent();
    }

    /** 终端上线时刷新最近在线时间 */
    @Transactional
    public void touch(Long id) {
        repository.findById(id).ifPresent(d -> d.setLastSeenAt(LocalDateTime.now()));
    }

    @Transactional
    public TerminalDevice save(TerminalDevice device) {
        return repository.save(device);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
