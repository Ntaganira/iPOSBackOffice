package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : HeldSaleRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for held sale.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.HeldSale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeldSaleRepository extends JpaRepository<HeldSale, Long> {

    List<HeldSale> findAllByOrderByCreatedAtDesc();

    List<HeldSale> findByCashierIdOrderByCreatedAtDesc(Long cashierId);
}
