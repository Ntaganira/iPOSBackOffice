package com.ntaganira.heritier.ipos.backoffice.dto;

import com.ntaganira.heritier.ipos.backoffice.entity.Expense;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import com.ntaganira.heritier.ipos.backoffice.enums.Recurrence;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseForm {

    private Long id;

    @NotNull(message = "Expense date is required.")
    private LocalDate expenseDate = LocalDate.now();

    @NotNull(message = "Choose an expense category.")
    private Long categoryId;

    @NotBlank(message = "Description is required.")
    @Size(max = 255, message = "Description must be 255 characters or fewer.")
    private String description;

    @NotNull(message = "Amount is required.")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
    private BigDecimal amount;

    @DecimalMin(value = "0.00", message = "Tax cannot be negative.")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Size(max = 80, message = "Reference must be 80 characters or fewer.")
    private String reference;

    @Size(max = 160, message = "Payee must be 160 characters or fewer.")
    private String payee;

    @Size(max = 500, message = "Notes must be 500 characters or fewer.")
    private String notes;

    private boolean recurring;
    private Recurrence recurrence;
    private LocalDate nextOccurrence;
    private String attachmentPath;
    private String attachmentContentType;

    public static ExpenseForm from(Expense expense) {
        ExpenseForm form = new ExpenseForm();
        form.id = expense.getId();
        form.expenseDate = expense.getExpenseDate();
        form.categoryId = expense.getCategory() == null ? null : expense.getCategory().getId();
        form.description = expense.getDescription();
        form.amount = expense.getAmount();
        form.taxAmount = expense.getTaxAmount();
        form.paymentMethod = expense.getPaymentMethod();
        form.reference = expense.getReference();
        form.payee = expense.getPayee();
        form.notes = expense.getNotes();
        form.recurring = expense.isRecurring();
        form.recurrence = expense.getRecurrence();
        form.nextOccurrence = expense.getNextOccurrence();
        form.attachmentPath = expense.getAttachmentPath();
        form.attachmentContentType = expense.getAttachmentContentType();
        return form;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getPayee() {
        return payee;
    }

    public void setPayee(String payee) {
        this.payee = payee;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(Recurrence recurrence) {
        this.recurrence = recurrence;
    }

    public LocalDate getNextOccurrence() {
        return nextOccurrence;
    }

    public void setNextOccurrence(LocalDate nextOccurrence) {
        this.nextOccurrence = nextOccurrence;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public String getAttachmentContentType() {
        return attachmentContentType;
    }

    public void setAttachmentContentType(String attachmentContentType) {
        this.attachmentContentType = attachmentContentType;
    }
}
