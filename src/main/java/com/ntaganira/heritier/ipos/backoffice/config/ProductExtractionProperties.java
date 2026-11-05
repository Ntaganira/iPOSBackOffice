package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : ProductExtractionProperties.java
 * - Date      : 2026. 11. 05.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Configuration for product image extraction.
 * </pre>
 */

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.product-extraction")
public record ProductExtractionProperties(
        @Min(1024) long maxImageBytes,
        String tesseractDataPath,
        @NotBlank String tesseractLanguages
) {
}
