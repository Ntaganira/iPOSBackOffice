package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : AuditLogRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for audit log.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where a.occurredAt >= :from and a.occurredAt < :to
              and (:username = '' or a.username = :username)
              and (:action = '' or a.action = :action)
              and (:entityType = '' or a.entityType = :entityType)
              and (:q = '' or lower(coalesce(a.entityLabel, '')) like :q
                   or lower(coalesce(a.entityId, '')) like :q or lower(coalesce(a.username, '')) like :q)
            order by a.occurredAt desc
            """)
    Page<AuditLog> search(@Param("from") Instant from, @Param("to") Instant to,
                          @Param("username") String username, @Param("action") String action,
                          @Param("entityType") String entityType, @Param("q") String q,
                          Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(String entityType, String entityId);

    List<AuditLog> findTop15ByUsernameOrderByOccurredAtDesc(String username);

    List<AuditLog> findTop20ByOrderByOccurredAtDesc();

    @Query("select distinct a.action from AuditLog a order by a.action")
    List<String> distinctActions();

    @Query("select distinct a.entityType from AuditLog a order by a.entityType")
    List<String> distinctEntityTypes();
}
