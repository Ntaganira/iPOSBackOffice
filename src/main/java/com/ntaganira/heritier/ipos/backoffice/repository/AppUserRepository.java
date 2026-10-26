package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : AppUserRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for app user.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.AppUser;
import com.ntaganira.heritier.ipos.backoffice.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmailIgnoreCase(String email);

    List<AppUser> findByEnabledTrueOrderByFullNameAsc();

    long countByRole(UserRole role);

    long countByRoleAndEnabledTrue(UserRole role);

    @Query("""
            select u from AppUser u
            where (:q = '' or lower(u.username) like :q or lower(u.fullName) like :q or lower(coalesce(u.email, '')) like :q)
              and (:role is null or u.role = :role)
            """)
    Page<AppUser> search(@Param("q") String q, @Param("role") UserRole role, Pageable pageable);
}
