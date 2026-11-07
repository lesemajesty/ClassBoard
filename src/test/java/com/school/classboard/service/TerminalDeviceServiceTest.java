package com.school.classboard.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.classboard.domain.TerminalDevice;
import com.school.classboard.repository.TerminalDeviceRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TerminalDeviceServiceTest {

    @Mock
    private TerminalDeviceRepository repository;

    @InjectMocks
    private TerminalDeviceService service;

    @Test
    void codeIsFreeWhenNoDeviceUsesIt() {
        when(repository.findByCode("CB-A301-01")).thenReturn(Optional.empty());

        assertFalse(service.isCodeTaken("CB-A301-01", null));
    }

    @Test
    void codeIsTakenByAnotherDevice() {
        when(repository.findByCode("CB-A301-01")).thenReturn(Optional.of(device(2L, "CB-A301-01")));

        assertTrue(service.isCodeTaken("CB-A301-01", 1L));
    }

    @Test
    void codeIsFreeForTheDeviceThatOwnsIt() {
        when(repository.findByCode("CB-A301-01")).thenReturn(Optional.of(device(1L, "CB-A301-01")));

        assertFalse(service.isCodeTaken("CB-A301-01", 1L));
    }

    @Test
    void touchRefreshesLastSeenAt() {
        TerminalDevice device = device(1L, "CB-A301-01");
        when(repository.findById(1L)).thenReturn(Optional.of(device));

        service.touch(1L);

        assertNotNull(device.getLastSeenAt());
    }

    private TerminalDevice device(Long id, String code) {
        TerminalDevice device = new TerminalDevice();
        device.setId(id);
        device.setCode(code);
        return device;
    }
}
