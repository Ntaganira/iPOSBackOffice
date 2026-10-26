package com.ntaganira.heritier.ipos.backoffice.dto;

import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class AuditLogView {

    private final Long id;
    private final Instant occurredAt;
    private final String username;
    private final String role;
    private final String action;
    private final String entityType;
    private final String entityLabel;
    private final String ipAddress;
    private final List<AuditChangeView> changes;

    public AuditLogView(Long id, Instant occurredAt, String username, String role, String action,
                        String entityType, String entityLabel, String ipAddress,
                        List<AuditChangeView> changes) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.username = username;
        this.role = role;
        this.action = action;
        this.entityType = entityType;
        this.entityLabel = entityLabel;
        this.ipAddress = ipAddress;
        this.changes = changes;
    }
}
