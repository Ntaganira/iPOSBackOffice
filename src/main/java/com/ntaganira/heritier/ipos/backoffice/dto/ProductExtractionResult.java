package com.ntaganira.heritier.ipos.backoffice.dto;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.dto
 * - File      : ProductExtractionResult.java
 * - Date      : 2026. 11. 05.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Structured product data returned from an uploaded product image.
 * </pre>
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductExtractionResult(
        String productName,
        String sku,
        String category,
        BigDecimal estimatedPrice
) {
}
