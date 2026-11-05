package com.ntaganira.heritier.ipos.backoffice.dto;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.dto
 * - File      : AdminUserView.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Minimal cross-tenant user row for platform administration.
 * </pre>
 */

public record AdminUserView(
        Long id,
        String fullName,
        String username,
        String email,
        String role
) {
}
