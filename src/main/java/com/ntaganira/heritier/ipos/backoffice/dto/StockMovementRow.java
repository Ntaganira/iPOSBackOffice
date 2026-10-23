package com.ntaganira.heritier.ipos.backoffice.dto;

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
