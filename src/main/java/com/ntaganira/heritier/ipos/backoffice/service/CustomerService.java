package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : CustomerService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Application service for customer.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.CustomerForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Customer;
import com.ntaganira.heritier.ipos.backoffice.entity.CustomerPayment;
import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.enums.CustomerType;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentStatus;
import com.ntaganira.heritier.ipos.backoffice.repository.CustomerPaymentRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.CustomerRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.SaleRepository;
import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerPaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerPaymentRepository paymentRepository,
                           SaleRepository saleRepository,
                           AuditService auditService,
                           CurrentUserService currentUser) {
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.saleRepository = saleRepository;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public Page<Customer> search(String q, Pageable pageable) {
        return customerRepository.search(like(q), pageable);
    }

    @Transactional(readOnly = true)
    public List<Customer> forPos(String q, int limit) {
        return customerRepository.searchForPos(like(q), PageRequest.of(0, Math.max(1, Math.min(limit, 50))));
    }

    @Transactional(readOnly = true)
    public List<Customer> active() {
        return customerRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Customer> lookup(String phone, String tin) {
        List<String> phones = phoneVariants(phone);
        String normalizedTin = normalizeTin(tin);
        if (phones.isEmpty() && normalizedTin == null) {
            return Optional.empty();
        }
        Map<Long, Customer> matches = new LinkedHashMap<>();
        if (!phones.isEmpty()) {
            customerRepository.findActiveByNormalizedPhone(phones)
                    .forEach(customer -> matches.put(customer.getId(), customer));
        }
        if (normalizedTin != null) {
            customerRepository.findActiveByNormalizedTin(normalizedTin)
                    .forEach(customer -> matches.put(customer.getId(), customer));
        }
        if (matches.size() > 1) {
            throw new BusinessException("More than one customer matches this phone or TIN. Search for the customer instead.");
        }
        return matches.values().stream().findFirst();
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Customer not found."));
    }

    @Transactional(readOnly = true)
    public CustomerForm formFor(Long id) {
        return CustomerForm.from(get(id));
    }

    @Transactional
    public Customer create(CustomerForm form) {
        Customer customer = new Customer();
        customer.setName(form.getName().trim());
        applyForm(customer, form);
        customer.setBalance(BigDecimal.ZERO);
        Customer saved = customerRepository.save(customer);
        auditService.created("Customer", saved.getId(), saved.getName(), snapshot(saved));
        return saved;
    }

    @Transactional
    public Customer update(Long id, CustomerForm form) {
        Customer customer = get(id);
        Map<String, Object> before = snapshot(customer);
        customer.setName(form.getName().trim());
        applyForm(customer, form);
        Customer saved = customerRepository.save(customer);
        auditService.updated("Customer", saved.getId(), saved.getName(), before, snapshot(saved));
        return saved;
    }

    @Transactional
    public void toggleActive(Long id) {
        Customer customer = get(id);
        Map<String, Object> before = Map.of("active", customer.isActive());
        customer.setActive(!customer.isActive());
        customerRepository.save(customer);
        auditService.updated("Customer", customer.getId(), customer.getName(), before,
                Map.of("active", customer.isActive()));
    }

    public BigDecimal outstanding(Customer customer) {
        return Money.of(saleRepository.sumBalanceByCustomer(customer.getId()));
    }

    @Transactional(readOnly = true)
    public List<Sale> creditSales(Long customerId) {
        return saleRepository.findByCustomerIdOrderBySaleDateDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<CustomerPayment> payments(Long customerId) {
        return paymentRepository.findByCustomerIdOrderByPaidAtDesc(customerId);
    }

    @Transactional
    public CustomerPayment recordPayment(Long customerId, BigDecimal amount, PaymentMethod method, String reference) {
        Customer customer = get(customerId);
        BigDecimal value = Money.of(amount);
        if (value.signum() <= 0) {
            throw new BusinessException("Enter a payment amount greater than zero.");
        }
        BigDecimal balance = saleRepository.sumBalanceByCustomer(customerId);
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (value.compareTo(balance) > 0) {
            throw new BusinessException("The payment exceeds the outstanding balance of "
                    + Money.of(balance).toPlainString() + ".");
        }
        CustomerPayment payment = new CustomerPayment();
        payment.setCustomer(customer);
        payment.setAmount(value);
        payment.setPaymentMethod(method == null ? PaymentMethod.CASH : method);
        payment.setReference(StringUtils.hasText(reference) ? reference.trim() : null);
        payment.setPaidAt(Instant.now());
        payment.setCreatedBy(currentUser.username());
        CustomerPayment saved = paymentRepository.save(payment);

        BigDecimal remaining = value;
        for (Sale sale : saleRepository.findOpenCreditSales(customerId)) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal applied = remaining.min(sale.getBalanceDue());
            sale.setAmountPaid(Money.of(sale.getAmountPaid().add(applied)));
            sale.setBalanceDue(Money.of(sale.getBalanceDue().subtract(applied)));
            sale.setPaymentStatus(sale.getBalanceDue().signum() <= 0
                    ? PaymentStatus.PAID : PaymentStatus.PARTIAL);
            saleRepository.save(sale);
            remaining = remaining.subtract(applied);
        }
        customer.setBalance(Money.of(balance.subtract(value).max(BigDecimal.ZERO)));
        customerRepository.save(customer);
        auditService.created("CustomerPayment", saved.getId(),
                "Payment from " + customer.getName(), Map.of("amount", value, "method", payment.getPaymentMethod()));
        return saved;
    }

    public BigDecimal totalOutstanding() {
        return customerRepository.findWithOutstandingBalance().stream()
                .map(Customer::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void applyForm(Customer customer, CustomerForm form) {
        customer.setTin(trimToNull(form.getTin()));
        customer.setPhone(trimToNull(form.getPhone()));
        customer.setEmail(trimToNull(form.getEmail()));
        customer.setAddress(trimToNull(form.getAddress()));
        customer.setCustomerType(form.getCustomerType() == null ? CustomerType.INDIVIDUAL : form.getCustomerType());
        customer.setCreditLimit(Money.of(form.getCreditLimit()));
        customer.setActive(form.isActive());
    }

    private Map<String, Object> snapshot(Customer customer) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("name", customer.getName());
        map.put("tin", customer.getTin());
        map.put("phone", customer.getPhone());
        map.put("email", customer.getEmail());
        map.put("address", customer.getAddress());
        map.put("customerType", customer.getCustomerType());
        map.put("creditLimit", customer.getCreditLimit());
        map.put("active", customer.isActive());
        return map;
    }

    private List<String> phoneVariants(String phone) {
        if (!StringUtils.hasText(phone)) {
            return List.of();
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        variants.add(digits);
        if (digits.length() == 9) {
            variants.add("0" + digits);
            variants.add("250" + digits);
        } else if (digits.length() == 10 && digits.startsWith("0")) {
            variants.add(digits.substring(1));
            variants.add("250" + digits.substring(1));
        } else if (digits.length() == 12 && digits.startsWith("250")) {
            variants.add("0" + digits.substring(3));
            variants.add(digits.substring(3));
        }
        return new ArrayList<>(variants);
    }

    private String normalizeTin(String tin) {
        if (!StringUtils.hasText(tin)) {
            return null;
        }
        String normalized = tin.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String like(String q) {
        return StringUtils.hasText(q) ? "%" + q.trim().toLowerCase(Locale.ROOT) + "%" : "";
    }
}
