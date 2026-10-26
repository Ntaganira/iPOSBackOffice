package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : PurchaseRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for purchase.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Purchase;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.PurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @Query("""
            select p from Purchase p
            left join p.supplier s
            where (:q = '' or lower(p.purchaseNumber) like :q or lower(coalesce(s.name, '')) like :q)
              and (:supplierId is null or s.id = :supplierId)
              and (:status is null or p.status = :status)
              and p.purchaseDate between :from and :to
            order by p.purchaseDate desc, p.id desc
            """)
    Page<Purchase> search(@Param("q") String q, @Param("supplierId") Long supplierId,
                          @Param("status") PurchaseStatus status, @Param("from") LocalDate from,
                          @Param("to") LocalDate to, Pageable pageable);

    List<Purchase> findBySupplierIdOrderByPurchaseDateDesc(Long supplierId);

    List<Purchase> findByStatusInOrderByPurchaseDateDesc(List<PurchaseStatus> statuses);

    List<Purchase> findTop8ByOrderByPurchaseDateDesc();

    long countByStatusIn(List<PurchaseStatus> statuses);

    List<Purchase> findByPaymentStatusNotOrderByPurchaseDateDesc(PaymentStatus status);

    @Query("""
            select p from Purchase p
            where p.supplier.id = :supplierId and p.status <> 'CANCELLED' and p.amountPaid < p.total
            order by p.purchaseDate asc, p.id asc
            """)
    List<Purchase> findOpenPurchases(@Param("supplierId") Long supplierId);

    @Query("""
            select coalesce(sum(p.total - p.amountPaid), 0) from Purchase p
            where p.supplier.id = :supplierId and p.status <> 'CANCELLED'
            """)
    java.math.BigDecimal sumBalanceBySupplier(@Param("supplierId") Long supplierId);
}
