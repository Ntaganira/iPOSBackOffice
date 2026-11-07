package com.ntaganira.heritier.ipos.backoffice.dto;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.dto
 * - File      : BarcodeLookupResult.java
 * - Date      : 2026. 11. 06.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Result of decoding and looking up a barcode or QR code.
 * </pre>
 */

public record BarcodeLookupResult(
        String code,
        boolean found,
        ProductDetail product,
        String message
) {
}
