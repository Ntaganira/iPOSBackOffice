package com.ntaganira.heritier.ipos.backoffice.repository;

import com.ntaganira.heritier.ipos.backoffice.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("select c from Customer c where c.balance > 0 and c.active = true order by c.balance desc")
    List<Customer> findWithOutstandingBalance();
}
