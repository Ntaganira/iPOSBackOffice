package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : AppProperties.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Typed application configuration properties.
 * </pre>
 */

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String zone,
        @Valid Storage storage,
        @Valid Encryption encryption,
        @Valid Security security,
        @Valid Admin admin,
        @Valid Sync sync,
        @Valid Email email,
        @Valid Demo demo
) {

    public record Storage(@NotBlank String uploads) {
    }

    public record Encryption(@NotBlank String key) {
    }

    public record Security(@NotBlank String rememberMeKey) {
    }

    public record Admin(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String fullName,
            String email
    ) {
    }

    public record Sync(@NotBlank String token) {
    }

    public record Email(String fromDefault) {
    }

    public record Demo(boolean enabled) {
    }
}
