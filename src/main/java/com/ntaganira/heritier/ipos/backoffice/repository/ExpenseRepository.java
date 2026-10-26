package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : ExpenseRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for expense.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Expense;
import com.ntaganira.heritier.ipos.backoffice.enums.ExpenseStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query(value = """
            select e from Expense e
            join fetch e.category c
            where e.expenseDate between :from and :to
              and (:categoryId is null or c.id = :categoryId)
              and (:paymentMethod is null or e.paymentMethod = :paymentMethod)
              and (:status is null or e.status = :status)
              and (:createdBy is null or e.createdBy = :createdBy)
              and (:q = '' or lower(e.description) like :q or lower(coalesce(e.reference, '')) like :q
                   or lower(coalesce(e.payee, '')) like :q)
            order by e.expenseDate desc, e.id desc
            """,
            countQuery = """
            select count(e) from Expense e
            join e.category c
            where e.expenseDate between :from and :to
              and (:categoryId is null or c.id = :categoryId)
              and (:paymentMethod is null or e.paymentMethod = :paymentMethod)
              and (:status is null or e.status = :status)
              and (:createdBy is null or e.createdBy = :createdBy)
              and (:q = '' or lower(e.description) like :q or lower(coalesce(e.reference, '')) like :q
                   or lower(coalesce(e.payee, '')) like :q)
            """)
    Page<Expense> search(@Param("from") LocalDate from, @Param("to") LocalDate to,
                         @Param("categoryId") Long categoryId,
                         @Param("paymentMethod") PaymentMethod paymentMethod,
                         @Param("status") ExpenseStatus status,
                         @Param("createdBy") String createdBy,
                         @Param("q") String q, Pageable pageable);

    @Query("""
            select coalesce(sum(e.amount), 0) from Expense e
            where e.expenseDate between :from and :to
              and (:categoryId is null or e.category.id = :categoryId)
              and (:paymentMethod is null or e.paymentMethod = :paymentMethod)
              and (:status is null or e.status = :status)
              and (:createdBy is null or e.createdBy = :createdBy)
              and (:q = '' or lower(e.description) like :q or lower(coalesce(e.reference, '')) like :q
                   or lower(coalesce(e.payee, '')) like :q)
            """)
    BigDecimal totalFor(@Param("from") LocalDate from, @Param("to") LocalDate to,
                        @Param("categoryId") Long categoryId,
                        @Param("paymentMethod") PaymentMethod paymentMethod,
                        @Param("status") ExpenseStatus status,
                        @Param("createdBy") String createdBy,
                        @Param("q") String q);

    @Query("""
            select e.category.name, coalesce(sum(e.amount), 0), count(e)
            from Expense e
            where e.expenseDate between :from and :to and e.status in ('PAID', 'APPROVED')
            group by e.category.name order by sum(e.amount) desc
            """)
    List<Object[]> totalsByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select e.paymentMethod, coalesce(sum(e.amount), 0)
            from Expense e
            where e.expenseDate between :from and :to and e.status in ('PAID', 'APPROVED')
              and e.paymentMethod = :method
            group by e.paymentMethod
            """)
    List<Object[]> totalByMethod(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                 @Param("method") PaymentMethod method);

    @Query("""
            select coalesce(sum(e.amount), 0) from Expense e
            where e.expenseDate between :from and :to
              and e.paymentMethod = 'CASH' and e.status in ('PAID', 'APPROVED')
            """)
    BigDecimal sumApprovedCash(@Param("from") LocalDate from, @Param("to") LocalDate to);
    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.expenseDate between :from and :to and e.status in ('PAID', 'APPROVED')")
    BigDecimal totalApproved(@Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByStatus(ExpenseStatus status);

    List<Expense> findTop10ByOrderByCreatedAtDesc();

    List<Expense> findByRecurringTrueAndNextOccurrenceLessThanEqualAndStatusIn(
            LocalDate date, List<ExpenseStatus> statuses);
}
