package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : TenantRepository.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for tenants.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Tenant> findByEnabledTrueOrderByNameAsc();

    List<Tenant> findAllByOrderByNameAsc();

    @Query("""
            select t from Tenant t
            where (:q = '' or lower(t.name) like :q or lower(t.code) like :q
                   or lower(coalesce(t.email, '')) like :q)
            """)
    Page<Tenant> search(@Param("q") String q, Pageable pageable);
}
