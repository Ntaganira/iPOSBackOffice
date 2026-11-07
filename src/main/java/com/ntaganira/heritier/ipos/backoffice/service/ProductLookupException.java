package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : ProductLookupException.java
 * - Date      : 2026. 11. 06.
 * - User      : Heritier NTAGANIRA
 * - Desc      : HTTP-aware exception for barcode and keyword product lookups.
 * </pre>
 */

import org.springframework.http.HttpStatus;

public class ProductLookupException extends RuntimeException {

    private final HttpStatus status;

    public ProductLookupException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ProductLookupException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
