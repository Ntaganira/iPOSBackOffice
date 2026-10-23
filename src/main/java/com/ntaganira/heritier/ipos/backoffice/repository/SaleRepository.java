package com.ntaganira.heritier.ipos.backoffice.repository;

import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import com.ntaganira.heritier.ipos.backoffice.enums.SaleStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.SaleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("""
            select s from Sale s
            left join s.customer c
            where s.saleDate >= :from and s.saleDate < :to
              and (:cashierId is null or s.cashier.id = :cashierId)
              and (:customerId is null or c.id = :customerId)
              and (:status is null or s.status = :status)
              and (:type is null or s.saleType = :type)
              and (:q = '' or lower(s.receiptNumber) like :q or lower(coalesce(c.name, '')) like :q)
              and (:paymentMethod is null or exists (
                    select 1 from SalePayment p where p.sale = s and p.paymentMethod = :paymentMethod))
            order by s.saleDate desc
            """)
    Page<Sale> search(@Param("from") Instant from, @Param("to") Instant to,
                      @Param("cashierId") Long cashierId, @Param("customerId") Long customerId,
                      @Param("status") SaleStatus status, @Param("type") SaleType type,
                      @Param("q") String q, @Param("paymentMethod") PaymentMethod paymentMethod,
                      Pageable pageable);

    List<Sale> findBySaleDateBetweenAndStatusOrderBySaleDateAsc(Instant from, Instant to, SaleStatus status);

    List<Sale> findBySaleDateBetweenOrderBySaleDateAsc(Instant from, Instant to);

    List<Sale> findTop10ByOrderBySaleDateDesc();

    List<Sale> findByStatusOrderBySaleDateDesc(SaleStatus status);

    List<Sale> findByCustomerIdOrderBySaleDateDesc(Long customerId);

    List<Sale> findByRefundOfId(Long refundOfId);

    Optional<Sale> findFirstByOrderBySaleDateDesc();

    Optional<Sale> findByReceiptNumberIgnoreCase(String receiptNumber);

    long countBySaleDateBetweenAndStatus(Instant from, Instant to, SaleStatus status);

    long countBySaleDateBetweenAndSaleType(Instant from, Instant to, SaleType type);

    @Query("select coalesce(sum(s.total), 0) from Sale s where s.saleDate >= :from and s.saleDate < :to and s.status = :status")
    BigDecimal sumTotal(@Param("from") Instant from, @Param("to") Instant to, @Param("status") SaleStatus status);

    @Query("select coalesce(sum(s.taxTotal), 0) from Sale s where s.saleDate >= :from and s.saleDate < :to and s.status = :status")
    BigDecimal sumTax(@Param("from") Instant from, @Param("to") Instant to, @Param("status") SaleStatus status);

    @Query("select coalesce(sum(s.discountTotal), 0) from Sale s where s.saleDate >= :from and s.saleDate < :to and s.status = :status")
    BigDecimal sumDiscount(@Param("from") Instant from, @Param("to") Instant to, @Param("status") SaleStatus status);

    @Query("""
            select s.saleType, coalesce(sum(s.total), 0) from Sale s
            where s.saleDate >= :from and s.saleDate < :to and s.status = 'COMPLETED'
            group by s.saleType
            """)
    List<Object[]> sumBySaleType(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select s.cashier.id, s.cashier.fullName, count(s), coalesce(sum(s.total), 0)
            from Sale s
            where s.saleDate >= :from and s.saleDate < :to and s.status = 'COMPLETED' and s.saleType = 'SALE'
            group by s.cashier.id, s.cashier.fullName
            order by sum(s.total) desc
            """)
    List<Object[]> salesByCashier(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select s from Sale s where s.status = 'COMPLETED' and s.saleDate between :from and :to
            order by s.saleDate desc
            """)
    List<Sale> findCompletedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select s from Sale s
            where s.customer.id = :customerId and s.saleType = 'SALE' and s.status = 'COMPLETED'
              and s.balanceDue > 0
            order by s.saleDate asc
            """)
    List<Sale> findOpenCreditSales(@Param("customerId") Long customerId);

    @Query("""
            select coalesce(sum(s.balanceDue), 0) from Sale s
            where s.customer.id = :customerId and s.saleType = 'SALE' and s.status = 'COMPLETED'
            """)
    BigDecimal sumBalanceByCustomer(@Param("customerId") Long customerId);
}
