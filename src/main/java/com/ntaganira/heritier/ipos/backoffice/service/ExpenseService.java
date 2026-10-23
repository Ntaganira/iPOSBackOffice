package com.ntaganira.heritier.ipos.backoffice.service;

import com.ntaganira.heritier.ipos.backoffice.dto.ExpenseForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Expense;
import com.ntaganira.heritier.ipos.backoffice.entity.ExpenseCategory;
import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import com.ntaganira.heritier.ipos.backoffice.enums.ExpenseStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import com.ntaganira.heritier.ipos.backoffice.enums.Recurrence;
import com.ntaganira.heritier.ipos.backoffice.repository.ExpenseCategoryRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ExpenseRepository;
import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final SettingsService settingsService;
    private final StorageService storageService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public ExpenseService(ExpenseRepository expenseRepository,
                          ExpenseCategoryRepository categoryRepository,
                          SettingsService settingsService,
                          StorageService storageService,
                          AuditService auditService,
                          CurrentUserService currentUser) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.settingsService = settingsService;
        this.storageService = storageService;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public Page<Expense> search(LocalDate from, LocalDate to, Long categoryId, PaymentMethod method,
                                ExpenseStatus status, String q, Pageable pageable) {
        return expenseRepository.search(from, to, categoryId, method, status, null, like(q), pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal total(LocalDate from, LocalDate to, Long categoryId, PaymentMethod method,
                            ExpenseStatus status, String q) {
        return Money.of(expenseRepository.totalFor(from, to, categoryId, method, status, null, like(q)));
    }

    @Transactional(readOnly = true)
    public Expense get(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Expense not found."));
    }

    @Transactional(readOnly = true)
    public ExpenseForm formFor(Long id) {
        return ExpenseForm.from(get(id));
    }

    @Transactional
    public Expense create(ExpenseForm form, MultipartFile attachment) {
        Expense expense = new Expense();
        applyForm(expense, form);
        expense.setCreatedBy(currentUser.username());
        ShopSetting settings = settingsService.get();
        boolean needsApproval = expense.getAmount().compareTo(settings.getExpenseApprovalThreshold()) > 0;
        expense.setStatus(needsApproval ? ExpenseStatus.PENDING : ExpenseStatus.PAID);
        if (attachment != null && !attachment.isEmpty()) {
            expense.setAttachmentPath(storageService.saveExpenseAttachment(attachment));
            expense.setAttachmentContentType(storageService.contentType(expense.getAttachmentPath()));
        } else if (StringUtils.hasText(form.getAttachmentPath())) {
            expense.setAttachmentPath(form.getAttachmentPath());
            expense.setAttachmentContentType(form.getAttachmentContentType());
        }
        Expense saved = expenseRepository.save(expense);
        auditService.created("Expense", saved.getId(), saved.getDescription(),
                Map.of("amount", saved.getAmount(), "category", saved.getCategory().getName(),
                        "status", saved.getStatus().name(), "payee", saved.getPayee() == null ? "" : saved.getPayee()));
        return saved;
    }

    @Transactional
    public Expense update(Long id, ExpenseForm form, MultipartFile attachment) {
        Expense expense = get(id);
        if (expense.getStatus() == ExpenseStatus.VOID) {
            throw new BusinessException("A voided expense cannot be edited.");
        }
        Map<String, Object> before = snapshot(expense);
        applyForm(expense, form);
        String oldAttachment = expense.getAttachmentPath();
        if (attachment != null && !attachment.isEmpty()) {
            expense.setAttachmentPath(storageService.saveExpenseAttachment(attachment));
            expense.setAttachmentContentType(storageService.contentType(expense.getAttachmentPath()));
            if (oldAttachment != null) {
                storageService.delete(oldAttachment);
            }
        }
        Expense saved = expenseRepository.save(expense);
        auditService.updated("Expense", saved.getId(), saved.getDescription(), before, snapshot(saved));
        return saved;
    }

    @Transactional
    public Expense approve(Long id) {
        Expense expense = get(id);
        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new BusinessException("Only pending expenses can be approved.");
        }
        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setApprovedBy(currentUser.username());
        expense.setApprovedAt(Instant.now());
        Expense saved = expenseRepository.save(expense);
        auditService.record("UPDATE", "Expense", saved.getId(), saved.getDescription(),
                Map.of("status", "PENDING"), Map.of("status", "APPROVED"));
        return saved;
    }

    @Transactional
    public Expense reject(Long id, String reason) {
        Expense expense = get(id);
        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new BusinessException("Only pending expenses can be rejected.");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("Enter the reason for rejecting this expense.");
        }
        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setRejectionReason(reason.trim());
        Expense saved = expenseRepository.save(expense);
        auditService.record("UPDATE", "Expense", saved.getId(), saved.getDescription(),
                Map.of("status", "PENDING"), Map.of("status", "REJECTED", "reason", reason.trim()));
        return saved;
    }

    @Transactional
    public Expense markPaid(Long id) {
        Expense expense = get(id);
        if (expense.getStatus() != ExpenseStatus.APPROVED) {
            throw new BusinessException("Only approved expenses can be marked as paid.");
        }
        expense.setStatus(ExpenseStatus.PAID);
        Expense saved = expenseRepository.save(expense);
        auditService.record("UPDATE", "Expense", saved.getId(), saved.getDescription(),
                Map.of("status", "APPROVED"), Map.of("status", "PAID"));
        return saved;
    }

    @Transactional
    public Expense voidExpense(Long id, String reason) {
        Expense expense = get(id);
        if (expense.getStatus() == ExpenseStatus.VOID) {
            throw new BusinessException("This expense is already voided.");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("Enter the reason for voiding this expense.");
        }
        expense.setStatus(ExpenseStatus.VOID);
        expense.setVoidReason(reason.trim());
        Expense saved = expenseRepository.save(expense);
        auditService.record("UPDATE", "Expense", saved.getId(), saved.getDescription(),
                Map.of("status", expense.getStatus().name()), Map.of("status", "VOID", "reason", reason.trim()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategory> categories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategory> activeCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public ExpenseCategory saveCategory(Long id, String name, boolean active) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("Category name is required.");
        }
        ExpenseCategory category = id == null ? new ExpenseCategory() : categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Expense category not found."));
        Map<String, Object> before = id == null ? null
                : Map.of("name", category.getName(), "active", category.isActive());
        category.setName(name.trim());
        category.setActive(active);
        ExpenseCategory saved = categoryRepository.save(category);
        Map<String, Object> after = Map.of("name", saved.getName(), "active", saved.isActive());
        if (before == null) {
            auditService.created("ExpenseCategory", saved.getId(), saved.getName(), after);
        } else {
            auditService.updated("ExpenseCategory", saved.getId(), saved.getName(), before, after);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Object[]> totalsByCategory(LocalDate from, LocalDate to) {
        return expenseRepository.totalsByCategory(from, to);
    }

    @Transactional
    @Scheduled(cron = "0 10 0 * * *")
    public void materializeRecurring() {
        LocalDate today = LocalDate.now();
        List<Expense> due = expenseRepository.findByRecurringTrueAndNextOccurrenceLessThanEqualAndStatusIn(
                today, List.of(ExpenseStatus.PAID, ExpenseStatus.APPROVED));
        for (Expense template : due) {
            Expense next = new Expense();
            next.setExpenseDate(today);
            next.setCategory(template.getCategory());
            next.setDescription(template.getDescription());
            next.setAmount(template.getAmount());
            next.setTaxAmount(template.getTaxAmount());
            next.setPaymentMethod(template.getPaymentMethod());
            next.setReference(template.getReference());
            next.setPayee(template.getPayee());
            next.setNotes("Recurring expense generated from #" + template.getId());
            next.setStatus(ExpenseStatus.PAID);
            next.setRecurring(false);
            next.setCreatedBy("system");
            expenseRepository.save(next);
            template.setNextOccurrence(advance(template.getNextOccurrence(), template.getRecurrence()));
            expenseRepository.save(template);
        }
    }

    private void applyForm(Expense expense, ExpenseForm form) {
        ExpenseCategory category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new BusinessException("Choose a valid expense category."));
        expense.setExpenseDate(form.getExpenseDate());
        expense.setCategory(category);
        expense.setDescription(form.getDescription().trim());
        expense.setAmount(Money.of(form.getAmount()));
        expense.setTaxAmount(Money.of(form.getTaxAmount()));
        expense.setPaymentMethod(form.getPaymentMethod() == null ? PaymentMethod.CASH : form.getPaymentMethod());
        expense.setReference(trimToNull(form.getReference()));
        expense.setPayee(trimToNull(form.getPayee()));
        expense.setNotes(trimToNull(form.getNotes()));
        expense.setRecurring(form.isRecurring());
        expense.setRecurrence(form.isRecurring() ? (form.getRecurrence() == null ? Recurrence.MONTHLY : form.getRecurrence()) : null);
        if (form.isRecurring()) {
            expense.setNextOccurrence(form.getNextOccurrence() == null
                    ? advance(expense.getExpenseDate(), expense.getRecurrence()) : form.getNextOccurrence());
        } else {
            expense.setNextOccurrence(null);
        }
    }

    private LocalDate advance(LocalDate date, Recurrence recurrence) {
        if (date == null) {
            return LocalDate.now().plusMonths(1);
        }
        return recurrence == Recurrence.WEEKLY ? date.plusWeeks(1) : date.plusMonths(1);
    }

    private Map<String, Object> snapshot(Expense expense) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("expenseDate", expense.getExpenseDate());
        map.put("category", expense.getCategory().getName());
        map.put("description", expense.getDescription());
        map.put("amount", expense.getAmount());
        map.put("taxAmount", expense.getTaxAmount());
        map.put("paymentMethod", expense.getPaymentMethod());
        map.put("reference", expense.getReference());
        map.put("payee", expense.getPayee());
        map.put("notes", expense.getNotes());
        map.put("recurring", expense.isRecurring());
        map.put("recurrence", expense.getRecurrence());
        map.put("nextOccurrence", expense.getNextOccurrence());
        map.put("attachmentPath", expense.getAttachmentPath());
        return map;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String like(String q) {
        return StringUtils.hasText(q) ? "%" + q.trim().toLowerCase(Locale.ROOT) + "%" : "";
    }
}
