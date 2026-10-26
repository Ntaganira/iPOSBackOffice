package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : TaxCategoryRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for tax category.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.TaxCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxCategoryRepository extends JpaRepository<TaxCategory, String> {

    List<TaxCategory> findAllByOrderBySortOrderAsc();

    List<TaxCategory> findByActiveTrueOrderBySortOrderAsc();
}
