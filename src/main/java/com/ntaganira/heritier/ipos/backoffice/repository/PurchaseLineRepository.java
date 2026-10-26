package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : PurchaseLineRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for purchase line.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.PurchaseLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseLineRepository extends JpaRepository<PurchaseLine, Long> {

    List<PurchaseLine> findByPurchaseIdOrderByIdAsc(Long purchaseId);
}
