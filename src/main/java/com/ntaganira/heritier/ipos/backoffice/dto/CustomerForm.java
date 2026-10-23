package com.ntaganira.heritier.ipos.backoffice.dto;

import com.ntaganira.heritier.ipos.backoffice.entity.Customer;
import com.ntaganira.heritier.ipos.backoffice.enums.CustomerType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CustomerForm {

    private Long id;

    @NotBlank(message = "Customer name is required.")
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

    private CustomerType customerType = CustomerType.INDIVIDUAL;

    @DecimalMin(value = "0.00", message = "Credit limit cannot be negative.")
    private BigDecimal creditLimit = BigDecimal.ZERO;

    private boolean active = true;

    public static CustomerForm from(Customer customer) {
        CustomerForm form = new CustomerForm();
        form.id = customer.getId();
        form.name = customer.getName();
        form.tin = customer.getTin();
        form.phone = customer.getPhone();
        form.email = customer.getEmail();
        form.address = customer.getAddress();
        form.customerType = customer.getCustomerType();
        form.creditLimit = customer.getCreditLimit();
        form.active = customer.isActive();
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

    public CustomerType getCustomerType() {
        return customerType;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
