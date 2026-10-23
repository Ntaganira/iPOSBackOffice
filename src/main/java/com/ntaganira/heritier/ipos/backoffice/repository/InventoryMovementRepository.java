package com.ntaganira.heritier.ipos.backoffice.repository;

import com.ntaganira.heritier.ipos.backoffice.entity.InventoryMovement;
import com.ntaganira.heritier.ipos.backoffice.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    @Query(value = """
            select m from InventoryMovement m
            join fetch m.product p
            where (:productId is null or p.id = :productId)
              and (:type is null or m.movementType = :type)
              and (:direction is null
                   or (:direction = 'IN' and m.quantityDelta > 0)
                   or (:direction = 'OUT' and m.quantityDelta < 0))
              and m.occurredAt >= :from and m.occurredAt < :to
              and (:q = '' or lower(p.name) like :q or lower(p.code) like :q)
            order by m.occurredAt desc, m.id desc
            """,
            countQuery = """
            select count(m) from InventoryMovement m
            join m.product p
            where (:productId is null or p.id = :productId)
              and (:type is null or m.movementType = :type)
              and (:direction is null
                   or (:direction = 'IN' and m.quantityDelta > 0)
                   or (:direction = 'OUT' and m.quantityDelta < 0))
              and m.occurredAt >= :from and m.occurredAt < :to
              and (:q = '' or lower(p.name) like :q or lower(p.code) like :q)
            """)
    Page<InventoryMovement> search(@Param("productId") Long productId,
                                   @Param("type") MovementType type,
                                   @Param("direction") String direction,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to,
                                   @Param("q") String q,
                                   Pageable pageable);

    List<InventoryMovement> findByProductIdOrderByOccurredAtDesc(Long productId);

    List<InventoryMovement> findByOccurredAtBetweenOrderByOccurredAtAsc(Instant from, Instant to);

    List<InventoryMovement> findTop10ByOrderByOccurredAtDesc();

    @Query("""
            select coalesce(sum(m.quantityDelta), 0) from InventoryMovement m
            where m.product.id = :productId
            """)
    java.math.BigDecimal sumByProduct(@Param("productId") Long productId);
}
