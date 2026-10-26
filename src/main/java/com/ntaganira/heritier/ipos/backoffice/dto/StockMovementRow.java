package com.ntaganira.heritier.ipos.backoffice.dto;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.dto
 * - File      : StockMovementRow.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Data transfer object for stock movement row.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.enums.MovementType;

import java.math.BigDecimal;
import java.time.Instant;

public record StockMovementRow(
        Long id,
        Instant occurredAt,
        String productName,
        String productCode,
        MovementType type,
        BigDecimal quantityDelta,
        BigDecimal unitCost,
        String referenceType,
        String referenceId,
        String note,
        String createdBy) {
}
