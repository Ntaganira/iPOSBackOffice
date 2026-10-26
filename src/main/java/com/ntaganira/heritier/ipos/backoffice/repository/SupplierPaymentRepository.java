package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : SupplierPaymentRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for supplier payment.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    List<SupplierPayment> findBySupplierIdOrderByPaidAtDesc(Long supplierId);
}
