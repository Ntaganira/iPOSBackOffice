package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : SupplierRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for supplier.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    @Query("""
            select s from Supplier s
            where (:q = '' or lower(s.name) like :q or lower(coalesce(s.phone, '')) like :q
                   or lower(coalesce(s.tin, '')) like :q)
            """)
    Page<Supplier> search(@Param("q") String q, Pageable pageable);

    List<Supplier> findByActiveTrueOrderByNameAsc();
}
