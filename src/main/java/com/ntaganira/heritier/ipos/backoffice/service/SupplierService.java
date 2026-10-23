package com.ntaganira.heritier.ipos.backoffice.service;

import com.ntaganira.heritier.ipos.backoffice.dto.SupplierForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Purchase;
import com.ntaganira.heritier.ipos.backoffice.entity.Supplier;
import com.ntaganira.heritier.ipos.backoffice.entity.SupplierPayment;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentStatus;
import com.ntaganira.heritier.ipos.backoffice.repository.PurchaseRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.SupplierPaymentRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.SupplierRepository;
import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierPaymentRepository paymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public SupplierService(SupplierRepository supplierRepository,
                           SupplierPaymentRepository paymentRepository,
                           PurchaseRepository purchaseRepository,
                           AuditService auditService,
                           CurrentUserService currentUser) {
        this.supplierRepository = supplierRepository;
        this.paymentRepository = paymentRepository;
        this.purchaseRepository = purchaseRepository;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public Page<Supplier> search(String q, Pageable pageable) {
        return supplierRepository.search(like(q), pageable);
    }

    @Transactional(readOnly = true)
    public List<Supplier> active() {
        return supplierRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Supplier get(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Supplier not found."));
    }

    @Transactional(readOnly = true)
    public SupplierForm formFor(Long id) {
        return SupplierForm.from(get(id));
    }

    @Transactional
    public Supplier create(SupplierForm form) {
        Supplier supplier = new Supplier();
        supplier.setName(form.getName().trim());
        applyForm(supplier, form);
        Supplier saved = supplierRepository.save(supplier);
        auditService.created("Supplier", saved.getId(), saved.getName(), snapshot(saved));
        return saved;
    }

    @Transactional
    public Supplier update(Long id, SupplierForm form) {
        Supplier supplier = get(id);
        Map<String, Object> before = snapshot(supplier);
        supplier.setName(form.getName().trim());
        applyForm(supplier, form);
        Supplier saved = supplierRepository.save(supplier);
        auditService.updated("Supplier", saved.getId(), saved.getName(), before, snapshot(saved));
        return saved;
    }

    @Transactional
    public void toggleActive(Long id) {
        Supplier supplier = get(id);
        Map<String, Object> before = Map.of("active", supplier.isActive());
        supplier.setActive(!supplier.isActive());
        supplierRepository.save(supplier);
        auditService.updated("Supplier", supplier.getId(), supplier.getName(), before,
                Map.of("active", supplier.isActive()));
    }

    public BigDecimal outstanding(Supplier supplier) {
        return Money.of(purchaseRepository.sumBalanceBySupplier(supplier.getId()));
    }

    @Transactional(readOnly = true)
    public List<Purchase> purchases(Long supplierId) {
        return purchaseRepository.findBySupplierIdOrderByPurchaseDateDesc(supplierId);
    }

    @Transactional(readOnly = true)
    public List<SupplierPayment> payments(Long supplierId) {
        return paymentRepository.findBySupplierIdOrderByPaidAtDesc(supplierId);
    }

    @Transactional
    public SupplierPayment recordPayment(Long supplierId, BigDecimal amount, PaymentMethod method, String reference) {
        Supplier supplier = get(supplierId);
        BigDecimal value = Money.of(amount);
        if (value.signum() <= 0) {
            throw new BusinessException("Enter a payment amount greater than zero.");
        }
        BigDecimal balance = purchaseRepository.sumBalanceBySupplier(supplierId);
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (value.compareTo(balance) > 0) {
            throw new BusinessException("The payment exceeds the outstanding balance of "
                    + Money.of(balance).toPlainString() + ".");
        }
        SupplierPayment payment = new SupplierPayment();
        payment.setSupplier(supplier);
        payment.setAmount(value);
        payment.setPaymentMethod(method == null ? PaymentMethod.CASH : method);
        payment.setReference(StringUtils.hasText(reference) ? reference.trim() : null);
        payment.setPaidAt(Instant.now());
        payment.setCreatedBy(currentUser.username());
        SupplierPayment saved = paymentRepository.save(payment);

        BigDecimal remaining = value;
        for (Purchase purchase : purchaseRepository.findOpenPurchases(supplierId)) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal applied = remaining.min(purchase.getBalance());
            purchase.setAmountPaid(Money.of(purchase.getAmountPaid().add(applied)));
            purchase.setPaymentStatus(purchase.getBalance().signum() <= 0
                    ? PaymentStatus.PAID : PaymentStatus.PARTIAL);
            purchaseRepository.save(purchase);
            remaining = remaining.subtract(applied);
        }
        auditService.created("SupplierPayment", saved.getId(),
                "Payment to " + supplier.getName(), Map.of("amount", value, "method", payment.getPaymentMethod()));
        return saved;
    }

    private void applyForm(Supplier supplier, SupplierForm form) {
        supplier.setTin(trimToNull(form.getTin()));
        supplier.setPhone(trimToNull(form.getPhone()));
        supplier.setEmail(trimToNull(form.getEmail()));
        supplier.setAddress(trimToNull(form.getAddress()));
        supplier.setActive(form.isActive());
    }

    private Map<String, Object> snapshot(Supplier supplier) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("name", supplier.getName());
        map.put("tin", supplier.getTin());
        map.put("phone", supplier.getPhone());
        map.put("email", supplier.getEmail());
        map.put("address", supplier.getAddress());
        map.put("active", supplier.isActive());
        return map;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String like(String q) {
        return StringUtils.hasText(q) ? "%" + q.trim().toLowerCase(Locale.ROOT) + "%" : "";
    }
}
