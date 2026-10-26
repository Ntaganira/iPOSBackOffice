package com.ntaganira.heritier.ipos.backoffice.dto;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.dto
 * - File      : SupplierForm.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Data transfer object for supplier.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Supplier;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SupplierForm {

    private Long id;

    @NotBlank(message = "Supplier name is required.")
    @Size(max = 160, message = "Name must be 160 characters or fewer.")
    private String name;

    @Size(max = 40, message = "TIN must be 40 characters or fewer.")
    private String tin;

    @Size(max = 40, message = "Phone must be 40 characters or fewer.")
    private String phone;

    @Email(message = "Enter a valid email address.")
    @Size(max = 160, message = "Email must be 160 characters or fewer.")
    private String email;

    @Size(max = 255, message = "Address must be 255 characters or fewer.")
    private String address;

    private boolean active = true;

    public static SupplierForm from(Supplier supplier) {
        SupplierForm form = new SupplierForm();
        form.id = supplier.getId();
        form.name = supplier.getName();
        form.tin = supplier.getTin();
        form.phone = supplier.getPhone();
        form.email = supplier.getEmail();
        form.address = supplier.getAddress();
        form.active = supplier.isActive();
        return form;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTin() {
        return tin;
    }

    public void setTin(String tin) {
        this.tin = tin;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
