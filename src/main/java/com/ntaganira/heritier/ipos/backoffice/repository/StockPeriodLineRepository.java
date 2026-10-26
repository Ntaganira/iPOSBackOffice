package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : StockPeriodLineRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for stock period line.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.StockPeriodLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockPeriodLineRepository extends JpaRepository<StockPeriodLine, Long> {

    List<StockPeriodLine> findByPeriodIdOrderByIdAsc(Long periodId);
}
