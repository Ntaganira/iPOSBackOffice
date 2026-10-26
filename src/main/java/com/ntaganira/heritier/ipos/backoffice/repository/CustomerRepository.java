package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : CustomerRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for customer.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("""
            select c from Customer c
            where (:q = '' or lower(c.name) like :q or lower(coalesce(c.phone, '')) like :q
                   or lower(coalesce(c.tin, '')) like :q or lower(coalesce(c.email, '')) like :q)
            """)
    Page<Customer> search(@Param("q") String q, Pageable pageable);

    @Query("""
            select c from Customer c where c.active = true
              and (:q = '' or lower(c.name) like :q or lower(coalesce(c.phone, '')) like :q
                   or lower(coalesce(c.tin, '')) like :q)
            order by c.name asc
            """)
    List<Customer> searchForPos(@Param("q") String q, Pageable pageable);

    List<Customer> findByActiveTrueOrderByNameAsc();

    @Query(value = """
            select * from shop_customers
            where active = true
              and regexp_replace(coalesce(phone, ''), '[^0-9]', '', 'g') in (:phones)
            limit 2
            """, nativeQuery = true)
    List<Customer> findActiveByNormalizedPhone(@Param("phones") Collection<String> phones);

    @Query(value = """
            select * from shop_customers
            where active = true
              and regexp_replace(lower(coalesce(tin, '')), '[^a-z0-9]', '', 'g') = :tin
            limit 2
            """, nativeQuery = true)
    List<Customer> findActiveByNormalizedTin(@Param("tin") String tin);

    @Query("select c from Customer c where c.balance > 0 and c.active = true order by c.balance desc")
    List<Customer> findWithOutstandingBalance();
}
