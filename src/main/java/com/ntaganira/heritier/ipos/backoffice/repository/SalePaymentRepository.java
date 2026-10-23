package com.ntaganira.heritier.ipos.backoffice.repository;

import com.ntaganira.heritier.ipos.backoffice.entity.SalePayment;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface SalePaymentRepository extends JpaRepository<SalePayment, Long> {

    List<SalePayment> findBySaleIdOrderByIdAsc(Long saleId);

    @Query("""
            select p.paymentMethod, coalesce(sum(p.amount), 0), count(distinct p.sale.id)
            from SalePayment p
            where p.sale.saleDate >= :from and p.sale.saleDate < :to
              and p.sale.status = 'COMPLETED'
            group by p.paymentMethod
            """)
    List<Object[]> totalsByMethod(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select coalesce(sum(p.amount), 0) from SalePayment p
            where p.sale.saleDate >= :from and p.sale.saleDate < :to
              and p.sale.status = 'COMPLETED' and p.paymentMethod = :method
            """)
    BigDecimal sumByMethod(@Param("from") Instant from, @Param("to") Instant to,
                           @Param("method") PaymentMethod method);
}
