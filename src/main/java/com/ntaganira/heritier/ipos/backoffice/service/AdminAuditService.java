package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : AdminAuditService.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Cross-tenant audit queries for platform administrators.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.AuditLogView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminAuditService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditDiffService auditDiffService;

    public AdminAuditService(JdbcTemplate jdbcTemplate, AuditDiffService auditDiffService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditDiffService = auditDiffService;
    }

    public Page<AuditLogView> search(LocalDate from, LocalDate to, String q, String tenant,
                                     Pageable pageable) {
        ZoneId zone = ZoneId.systemDefault();
        List<Object> args = new ArrayList<>();
        String where = " where a.occurred_at >= ? and a.occurred_at < ?";
        args.add(Timestamp.from(from.atStartOfDay(zone).toInstant()));
        args.add(Timestamp.from(to.plusDays(1).atStartOfDay(zone).toInstant()));
        if (StringUtils.hasText(q)) {
            where += " and (lower(coalesce(a.username, '')) like ?"
                    + " or lower(coalesce(a.entity_label, '')) like ?"
                    + " or lower(coalesce(a.entity_type, '')) like ?"
                    + " or lower(coalesce(a.action, '')) like ?)";
            String query = "%" + q.trim().toLowerCase() + "%";
            args.add(query);
            args.add(query);
            args.add(query);
            args.add(query);
        }
        if (StringUtils.hasText(tenant)) {
            where += " and lower(t.code) = lower(?)";
            args.add(tenant.trim());
        }

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs a join tenants t on t.id = a.tenant_id" + where,
                Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageable.getPageSize());
        pageArgs.add(pageable.getOffset());

        List<AuditLogView> rows = jdbcTemplate.query("""
                        select a.id, a.occurred_at, a.username, a.role, a.action, a.entity_type,
                               a.entity_label, a.ip_address, a.changes_json, t.name as tenant_name
                        from audit_logs a
                        join tenants t on t.id = a.tenant_id
                        """ + where + " order by a.occurred_at desc limit ? offset ?",
                (rs, rowNum) -> new AuditLogView(
                        rs.getLong("id"),
                        toInstant(rs.getObject("occurred_at", OffsetDateTime.class)),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("action"),
                        rs.getString("entity_type"),
                        rs.getString("entity_label"),
                        rs.getString("ip_address"),
                        rs.getString("tenant_name"),
                        auditDiffService.changes(rs.getString("changes_json"))),
                pageArgs.toArray());
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    private Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
