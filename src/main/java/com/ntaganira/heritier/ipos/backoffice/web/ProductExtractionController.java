package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : ProductExtractionController.java
 * - Date      : 2026. 11. 06.
 * - User      : Heritier NTAGANIRA
 * - Desc      : REST endpoint for extracting product details from an image.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.ProductExtractionResult;
import com.ntaganira.heritier.ipos.backoffice.service.ProductExtractionException;
import com.ntaganira.heritier.ipos.backoffice.service.ProductExtractionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/products")
@PreAuthorize("hasAuthority('PRODUCT_CREATE')")
public class ProductExtractionController {

    private final ProductExtractionService extractionService;

    public ProductExtractionController(ProductExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductExtractionResult extract(@RequestParam("image") MultipartFile image) {
        return extractionService.extract(image);
    }

    @ExceptionHandler(ProductExtractionException.class)
    public ResponseEntity<Map<String, String>> extractionError(ProductExtractionException exception) {
        return ResponseEntity.status(exception.status())
                .body(Map.of("message", exception.getMessage()));
    }
}
