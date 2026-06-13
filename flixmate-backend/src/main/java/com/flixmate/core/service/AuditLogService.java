package com.flixmate.core.service;

import com.flixmate.core.model.AuditLog;
import com.flixmate.core.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String action, String actor, String details, String ipAddress) {
        log.info("Audit Log: action={}, actor={}, ip={}", action, actor, ipAddress);
        AuditLog audit = AuditLog.builder()
                .action(action)
                .actor(actor)
                .details(details)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(audit);
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}
