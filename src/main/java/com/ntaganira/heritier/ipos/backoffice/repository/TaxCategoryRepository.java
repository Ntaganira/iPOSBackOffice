package com.ntaganira.heritier.ipos.backoffice.repository;

import com.ntaganira.heritier.ipos.backoffice.entity.TaxCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxCategoryRepository extends JpaRepository<TaxCategory, String> {

    List<TaxCategory> findAllByOrderBySortOrderAsc();

    List<TaxCategory> findByActiveTrueOrderBySortOrderAsc();
}
