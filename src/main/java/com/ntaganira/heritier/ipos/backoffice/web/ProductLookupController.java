package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : ProductLookupController.java
 * - Date      : 2026. 11. 06.
 * - User      : Heritier NTAGANIRA
 * - Desc      : REST endpoints for barcode, QR-code and keyword product lookups.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.BarcodeLookupResult;
import com.ntaganira.heritier.ipos.backoffice.dto.ProductDetail;
import com.ntaganira.heritier.ipos.backoffice.service.ProductLookupException;
import com.ntaganira.heritier.ipos.backoffice.service.ProductLookupService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products/lookup")
@PreAuthorize("hasAnyAuthority('PRODUCT_VIEW','PRODUCT_CREATE')")
public class ProductLookupController {

    private final ProductLookupService lookupService;

    public ProductLookupController(ProductLookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("/barcode")
    public BarcodeLookupResult lookupBarcode(@RequestParam String code) {
        return lookupService.lookup(code);
    }

    @GetMapping("/search")
    public List<ProductDetail> search(@RequestParam String q,
                                      @RequestParam(defaultValue = "10") int limit) {
        return lookupService.search(q, limit);
    }

    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BarcodeLookupResult scan(@RequestParam("image") MultipartFile image) {
        return lookupService.scan(image);
    }

    @ExceptionHandler(ProductLookupException.class)
    public ResponseEntity<Map<String, String>> lookupError(ProductLookupException exception) {
        return ResponseEntity.status(exception.status())
                .body(Map.of("message", exception.getMessage()));
    }
}
