package com.ntaganira.heritier.ipos.backoffice.repository;

import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByCodeIgnoreCase(String code);

    Optional<Product> findByBarcode(String barcode);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByBarcode(String barcode);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    @Query("""
            select p from Product p
            left join p.category c
            left join p.location loc
            where (:q = '' or lower(p.name) like :q or lower(p.code) like :q
                   or lower(coalesce(p.barcode, '')) like :q)
              and (:categoryId is null or c.id = :categoryId)
              and (:active is null or p.active = :active)
              and (:locationId is null or loc.id = :locationId)
            order by p.name asc
            """)
    Page<Product> search(@Param("q") String q, @Param("categoryId") Long categoryId,
                         @Param("active") Boolean active, @Param("locationId") Long locationId,
                         Pageable pageable);

    @Query("""
            select p from Product p
            where p.active = true
              and (:q = '' or lower(p.name) like :q or lower(p.code) like :q
                   or lower(coalesce(p.barcode, '')) like :q)
            order by p.name asc
            """)
    List<Product> searchForPos(@Param("q") String q, Pageable pageable);

    @Query("select p from Product p where p.active = true and p.stockQuantity <= p.reorderLevel order by p.stockQuantity asc")
    List<Product> findLowStock();

    @Query("select count(p) from Product p where p.active = true and p.stockQuantity <= p.reorderLevel")
    long countLowStock();

    long countByActiveTrueAndStockQuantityLessThanEqual(BigDecimal level);

    @Query("select coalesce(sum(p.stockQuantity), 0) from Product p where p.active = true")
    BigDecimal sumStockQuantity();

    @Query("select coalesce(sum(p.stockQuantity * p.buyingPrice), 0) from Product p where p.active = true")
    BigDecimal sumStockValue();

    @Query("""
            select p.location.id as locationId, count(p) as productCount,
                   coalesce(sum(p.stockQuantity), 0) as totalQuantity
            from Product p
            where p.location is not null
            group by p.location.id
            """)
    List<LocationUsage> locationUsage();

    List<Product> findByActiveTrueOrderByNameAsc();

    long countByActiveTrue();

    interface LocationUsage {

        Long getLocationId();

        long getProductCount();

        BigDecimal getTotalQuantity();
    }
}
