package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : StockPeriodRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for stock period.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.StockPeriod;
import com.ntaganira.heritier.ipos.backoffice.enums.StockPeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockPeriodRepository extends JpaRepository<StockPeriod, Long> {

    List<StockPeriod> findAllByOrderByPeriodStartDesc();

    Optional<StockPeriod> findFirstByStatusOrderByPeriodStartDesc(StockPeriodStatus status);

    boolean existsByStatus(StockPeriodStatus status);
}
