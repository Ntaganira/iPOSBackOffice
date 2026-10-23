package com.ntaganira.heritier.ipos.backoffice.repository;

import com.ntaganira.heritier.ipos.backoffice.entity.HeldSale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeldSaleRepository extends JpaRepository<HeldSale, Long> {

    List<HeldSale> findAllByOrderByCreatedAtDesc();

    List<HeldSale> findByCashierIdOrderByCreatedAtDesc(Long cashierId);
}
