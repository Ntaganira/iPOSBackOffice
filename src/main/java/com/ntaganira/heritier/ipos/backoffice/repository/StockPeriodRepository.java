package com.ntaganira.heritier.ipos.backoffice.repository;

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
