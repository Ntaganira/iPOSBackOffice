package com.ntaganira.heritier.ipos.backoffice.dto;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.dto
 * - File      : TenantForm.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Data transfer object for creating a tenant and its first administrator.
 * </pre>
 */

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantForm {

    @NotBlank(message = "Tenant code is required.")
    @Size(max = 40, message = "Tenant code must be 40 characters or fewer.")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Use letters, numbers, hyphens or underscores.")
    private String code;

    @NotBlank(message = "Tenant name is required.")
    @Size(max = 160, message = "Tenant name must be 160 characters or fewer.")
    private String name;

    @NotBlank(message = "Tenant email is required.")
    @Email(message = "Enter a valid tenant email address.")
    @Size(max = 160, message = "Tenant email must be 160 characters or fewer.")
    private String email;

    @Size(max = 40, message = "Tenant phone must be 40 characters or fewer.")
    private String phone;

    @NotBlank(message = "Administrator full name is required.")
    @Size(max = 120, message = "Administrator full name must be 120 characters or fewer.")
    private String adminFullName;

    @NotBlank(message = "Administrator username is required.")
    @Size(min = 3, max = 50, message = "Administrator username must be between 3 and 50 characters.")
    private String adminUsername;

    @NotBlank(message = "Administrator email is required.")
    @Email(message = "Enter a valid administrator email address.")
    @Size(max = 160, message = "Administrator email must be 160 characters or fewer.")
    private String adminEmail;

    @NotBlank(message = "Temporary password is required.")
    @Size(min = 8, max = 100, message = "Temporary password must be at least 8 characters long.")
    private String adminPassword;

    @NotBlank(message = "Confirm the temporary password.")
    private String confirmPassword;
}
