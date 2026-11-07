package com.ntaganira.heritier.ipos.backoffice.dto;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.dto
 * - File      : ProductForm.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Data transfer object for product.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductForm {

    private Long id;

    @Size(max = 40, message = "Product code must be 40 characters or fewer.")
    private String code;

    @Size(max = 60, message = "Barcode must be 60 characters or fewer.")
    private String barcode;

    @Size(max = 80, message = "Part number must be 80 characters or fewer.")
    private String partNumber;

    @Size(max = 160, message = "Product name must be 160 characters or fewer.")
    private String name;

    @Size(max = 2000, message = "Aliases must be 2000 characters or fewer.")
    private String aliases;

    @NotNull(message = "Choose a category.")
    private Long categoryId;

    @NotNull(message = "Choose a unit.")
    private Long unitId;

    private Long locationId;

    @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Location accent must be a hex value such as #268071.")
    private String locationColor;

    @NotBlank(message = "Choose a tax category.")
    private String taxCategoryCode;

    @NotNull(message = "Buying price is required.")
    @DecimalMin(value = "0.00", message = "Buying price cannot be negative.")
    private BigDecimal buyingPrice = BigDecimal.ZERO;

    @NotNull(message = "Selling price is required.")
    @DecimalMin(value = "0.00", message = "Selling price cannot be negative.")
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @NotNull(message = "Reorder level is required.")
    @DecimalMin(value = "0.000", message = "Reorder level cannot be negative.")
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @DecimalMin(value = "0.000", message = "Opening quantity cannot be negative.")
    private BigDecimal openingQuantity = BigDecimal.ZERO;

    private boolean active = true;

    private String imagePath;

    public static ProductForm from(Product product) {
        ProductForm form = new ProductForm();
        form.id = product.getId();
        form.code = product.getCode();
        form.barcode = product.getBarcode();
        form.partNumber = product.getPartNumber();
        form.name = product.getName();
        form.aliases = product.getAliases().stream()
                .map(alias -> alias.getAlias())
                .collect(java.util.stream.Collectors.joining(", "));
        form.categoryId = product.getCategory() == null ? null : product.getCategory().getId();
        form.unitId = product.getUnit() == null ? null : product.getUnit().getId();
        form.locationId = product.getLocation() == null ? null : product.getLocation().getId();
        form.locationColor = product.getLocation() == null ? null : product.getLocation().getColorAccent();
        form.taxCategoryCode = product.getTaxCategory() == null ? "A" : product.getTaxCategory().getCode();
        form.buyingPrice = product.getBuyingPrice();
        form.sellingPrice = product.getSellingPrice();
        form.reorderLevel = product.getReorderLevel();
        form.openingQuantity = BigDecimal.ZERO;
        form.active = product.isActive();
        form.imagePath = product.getImagePath();
        return form;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAliases() {
        return aliases;
    }

    public void setAliases(String aliases) {
        this.aliases = aliases;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationColor() {
        return locationColor;
    }

    public void setLocationColor(String locationColor) {
        this.locationColor = locationColor;
    }

    public String getTaxCategoryCode() {
        return taxCategoryCode;
    }

    public void setTaxCategoryCode(String taxCategoryCode) {
        this.taxCategoryCode = taxCategoryCode;
    }

    public BigDecimal getBuyingPrice() {
        return buyingPrice;
    }

    public void setBuyingPrice(BigDecimal buyingPrice) {
        this.buyingPrice = buyingPrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public BigDecimal getOpeningQuantity() {
        return openingQuantity;
    }

    public void setOpeningQuantity(BigDecimal openingQuantity) {
        this.openingQuantity = openingQuantity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
