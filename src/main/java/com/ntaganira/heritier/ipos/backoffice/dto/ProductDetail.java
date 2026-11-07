package com.ntaganira.heritier.ipos.backoffice.dto;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.dto
 * - File      : ProductDetail.java
 * - Date      : 2026. 11. 06.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Complete product data returned by barcode and keyword lookups.
 * </pre>
 */

import java.math.BigDecimal;
import java.util.List;

public record ProductDetail(
        Long id,
        String productName,
        String sku,
        String partNumber,
        String barcode,
        String category,
        BigDecimal estimatedPrice,
        BigDecimal buyingPrice,
        String unit,
        String location,
        String taxCategory,
        BigDecimal stockQuantity,
        BigDecimal reorderLevel,
        boolean active,
        String imagePath,
        List<String> aliases
) {
}
