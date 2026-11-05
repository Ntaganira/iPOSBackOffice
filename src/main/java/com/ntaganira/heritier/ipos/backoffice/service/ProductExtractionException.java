package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : ProductExtractionException.java
 * - Date      : 2026. 11. 05.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Raised when product image extraction cannot be completed.
 * </pre>
 */

import org.springframework.http.HttpStatus;

public class ProductExtractionException extends RuntimeException {

    private final HttpStatus status;

    public ProductExtractionException(String message) {
        this(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public ProductExtractionException(String message, Throwable cause) {
        this(message, cause, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public ProductExtractionException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ProductExtractionException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
