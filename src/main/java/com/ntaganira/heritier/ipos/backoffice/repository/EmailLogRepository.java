package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : EmailLogRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for email log.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.EmailLog;
import com.ntaganira.heritier.ipos.backoffice.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    @Query("""
            select e from EmailLog e
            where (:q = '' or lower(e.recipient) like :q or lower(coalesce(e.subject, '')) like :q
                   or lower(coalesce(e.documentNumber, '')) like :q)
              and (:status is null or e.status = :status)
            order by e.createdAt desc
            """)
    Page<EmailLog> search(@Param("q") String q, @Param("status") EmailStatus status, Pageable pageable);

    long countByStatus(EmailStatus status);
}
