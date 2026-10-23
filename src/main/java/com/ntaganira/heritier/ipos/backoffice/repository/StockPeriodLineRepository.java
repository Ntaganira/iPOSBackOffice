package com.ntaganira.heritier.ipos.backoffice.repository;

import com.ntaganira.heritier.ipos.backoffice.entity.StockPeriodLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockPeriodLineRepository extends JpaRepository<StockPeriodLine, Long> {

    List<StockPeriodLine> findByPeriodIdOrderByIdAsc(Long periodId);
}
