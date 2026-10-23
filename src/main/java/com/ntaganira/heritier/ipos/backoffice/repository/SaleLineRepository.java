package com.ntaganira.heritier.ipos.backoffice.repository;

import com.ntaganira.heritier.ipos.backoffice.entity.SaleLine;
import com.ntaganira.heritier.ipos.backoffice.enums.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SaleLineRepository extends JpaRepository<SaleLine, Long> {

    List<SaleLine> findBySaleIdOrderByIdAsc(Long saleId);

    @Query("""
            select l from SaleLine l
            join fetch l.sale s
            where s.saleDate >= :from and s.saleDate < :to and s.status = :status
            """)
    List<SaleLine> findForPeriod(@Param("from") Instant from, @Param("to") Instant to,
                                 @Param("status") SaleStatus status);

    @Query("""
            select l from SaleLine l
            join fetch l.sale s
            join fetch l.product p
            where s.saleDate >= :from and s.saleDate < :to and s.status = :status
            """)
    List<SaleLine> findForPeriodWithProduct(@Param("from") Instant from, @Param("to") Instant to,
                                            @Param("status") SaleStatus status);

    @Query("select l from SaleLine l where l.product.id = :productId order by l.id desc")
    List<SaleLine> findByProduct(@Param("productId") Long productId);
}
