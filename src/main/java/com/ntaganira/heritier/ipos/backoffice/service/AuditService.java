package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : AuditService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Records and queries administrative audit events.
 * </pre>
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ntaganira.heritier.ipos.backoffice.entity.AuditLog;
import com.ntaganira.heritier.ipos.backoffice.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;
    private final CurrentUserService currentUser;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, CurrentUserService currentUser, ObjectMapper objectMapper) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(String action, String entityType, Object entityId, String label,
                       Map<String, Object> before, Map<String, Object> after) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId == null ? null : String.valueOf(entityId));
            entry.setEntityLabel(truncate(label, 255));
            entry.setChangesJson(changesJson(before, after));
            entry.setUserId(currentUser.user().map(u -> u.getId()).orElse(null));
            entry.setUsername(currentUser.username());
            entry.setRole(currentUser.role());
            entry.setIpAddress(currentUser.clientIp());
            entry.setUserAgent(truncate(currentUser.userAgent(), 255));
            repository.save(entry);
        } catch (RuntimeException ex) {
            log.warn("Could not write audit entry for {} {}: {}", action, entityType, ex.getMessage());
        }
    }

    @Transactional
    public void recordAs(Long userId, String username, String role, String action,
                         String entityType, Object entityId, String label, Map<String, Object> details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId == null ? null : String.valueOf(entityId));
            entry.setEntityLabel(truncate(label, 255));
            entry.setChangesJson(changesJson(null, details));
            entry.setUserId(userId);
            entry.setUsername(username);
            entry.setRole(role);
            entry.setIpAddress(currentUser.clientIp());
            entry.setUserAgent(truncate(currentUser.userAgent(), 255));
            repository.save(entry);
        } catch (RuntimeException ex) {
            log.warn("Could not write audit entry for {} {}: {}", action, entityType, ex.getMessage());
        }
    }

    @Transactional
    public void created(String entityType, Object id, String label, Map<String, Object> values) {
        record("CREATE", entityType, id, label, null, values);
    }

    @Transactional
    public void updated(String entityType, Object id, String label,
                        Map<String, Object> before, Map<String, Object> after) {
        record("UPDATE", entityType, id, label, before, after);
    }

    @Transactional
    public void deleted(String entityType, Object id, String label, Map<String, Object> values) {
        record("DELETE", entityType, id, label, values, null);
    }

    public String changesJson(Map<String, Object> before, Map<String, Object> after) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            Set<String> keys = new LinkedHashSet<>();
            if (before != null) {
                keys.addAll(before.keySet());
            }
            if (after != null) {
                keys.addAll(after.keySet());
            }
            for (String key : keys) {
                Object oldValue = before == null ? null : before.get(key);
                Object newValue = after == null ? null : after.get(key);
                if (sameValue(oldValue, newValue)) {
                    continue;
                }
                ObjectNode change = root.putObject(key);
                change.set("before", objectMapper.valueToTree(maskValue(key, oldValue)));
                change.set("after", objectMapper.valueToTree(maskValue(key, newValue)));
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            log.warn("Could not serialize audit changes: {}", ex.getMessage());
            return "{}";
        }
    }

    public Map<String, Object> snapshot(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    private boolean sameValue(Object a, Object b) {
        if (a instanceof BigDecimal left && b instanceof BigDecimal right) {
            return left.compareTo(right) == 0;
        }
        return Objects.equals(normalize(a), normalize(b));
    }

    private Object normalize(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Object maskValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        String lower = key == null ? "" : key.toLowerCase();
        if (lower.contains("password") || lower.contains("secret") || lower.contains("token")
                || lower.contains("hash")) {
            return "***";
        }
        return value;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
