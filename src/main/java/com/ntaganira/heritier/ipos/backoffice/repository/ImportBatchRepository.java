package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : ImportBatchRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for import batch.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.ImportBatch;
import com.ntaganira.heritier.ipos.backoffice.enums.ImportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    @Query("""
            select i from ImportBatch i
            left join i.supplier s
            where (:q = '' or lower(i.referenceNo) like :q or lower(coalesce(s.name, '')) like :q
                   or lower(coalesce(i.originCountry, '')) like :q)
              and (:status is null or i.status = :status)
              and i.importDate between :from and :to
            order by i.importDate desc, i.id desc
            """)
    Page<ImportBatch> search(@Param("q") String q, @Param("status") ImportStatus status,
                             @Param("from") LocalDate from, @Param("to") LocalDate to,
                             Pageable pageable);

    boolean existsByReferenceNoIgnoreCase(String referenceNo);
}
