package com.ntaganira.heritier.ipos.backoffice.dto;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.dto
 * - File      : ImportForm.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Data transfer object for import.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.ImportBatch;
import com.ntaganira.heritier.ipos.backoffice.entity.ImportItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ImportForm {

    private Long id;
    private String referenceNo;
    private Long supplierId;
    private String originCountry;
    private String currency = "USD";
    private BigDecimal exchangeRate = BigDecimal.ONE;
    private BigDecimal customsDuty = BigDecimal.ZERO;
    private BigDecimal taxes = BigDecimal.ZERO;
    private BigDecimal freight = BigDecimal.ZERO;
    private BigDecimal otherCosts = BigDecimal.ZERO;
    private LocalDate importDate = LocalDate.now();
    private String notes;
    private List<Line> lines = new ArrayList<>();

    public static ImportForm from(ImportBatch batch) {
        ImportForm form = new ImportForm();
        form.id = batch.getId();
        form.referenceNo = batch.getReferenceNo();
        form.supplierId = batch.getSupplier() == null ? null : batch.getSupplier().getId();
        form.originCountry = batch.getOriginCountry();
        form.currency = batch.getCurrency();
        form.exchangeRate = batch.getExchangeRate();
        form.customsDuty = batch.getCustomsDuty();
        form.taxes = batch.getTaxes();
        form.freight = batch.getFreight();
        form.otherCosts = batch.getOtherCosts();
        form.importDate = batch.getImportDate();
        form.notes = batch.getNotes();
        for (ImportItem item : batch.getItems()) {
            Line line = new Line();
            line.productId = item.getProduct().getId();
            line.quantity = item.getQuantity();
            line.unitCostForeign = item.getUnitCostForeign();
            form.lines.add(line);
        }
        return form;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public BigDecimal getCustomsDuty() {
        return customsDuty;
    }

    public void setCustomsDuty(BigDecimal customsDuty) {
        this.customsDuty = customsDuty;
    }

    public BigDecimal getTaxes() {
        return taxes;
    }

    public void setTaxes(BigDecimal taxes) {
        this.taxes = taxes;
    }

    public BigDecimal getFreight() {
        return freight;
    }

    public void setFreight(BigDecimal freight) {
        this.freight = freight;
    }

    public BigDecimal getOtherCosts() {
        return otherCosts;
    }

    public void setOtherCosts(BigDecimal otherCosts) {
        this.otherCosts = otherCosts;
    }

    public LocalDate getImportDate() {
        return importDate;
    }

    public void setImportDate(LocalDate importDate) {
        this.importDate = importDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines == null ? new ArrayList<>() : lines;
    }

    public static class Line {
        private Long productId;
        private BigDecimal quantity = BigDecimal.ONE;
        private BigDecimal unitCostForeign = BigDecimal.ZERO;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitCostForeign() {
            return unitCostForeign;
        }

        public void setUnitCostForeign(BigDecimal unitCostForeign) {
            this.unitCostForeign = unitCostForeign;
        }
    }
}
