package com.ntaganira.heritier.ipos.backoffice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntaganira.heritier.ipos.backoffice.config.AppProperties;
import com.ntaganira.heritier.ipos.backoffice.dto.CartRequest;
import com.ntaganira.heritier.ipos.backoffice.entity.Customer;
import com.ntaganira.heritier.ipos.backoffice.entity.HeldSale;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.entity.SaleLine;
import com.ntaganira.heritier.ipos.backoffice.entity.SalePayment;
import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import com.ntaganira.heritier.ipos.backoffice.enums.MovementType;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.SaleStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.SaleType;
import com.ntaganira.heritier.ipos.backoffice.repository.CustomerRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.HeldSaleRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.SaleRepository;
import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final HeldSaleRepository heldSaleRepository;
    private final StockService stockService;
    private final SettingsService settingsService;
    private final SequenceService sequenceService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public SaleService(SaleRepository saleRepository,
                       ProductRepository productRepository,
                       CustomerRepository customerRepository,
                       HeldSaleRepository heldSaleRepository,
                       StockService stockService,
                       SettingsService settingsService,
                       SequenceService sequenceService,
                       AuditService auditService,
                       CurrentUserService currentUser,
                       ObjectMapper objectMapper,
                       AppProperties properties) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.heldSaleRepository = heldSaleRepository;
        this.stockService = stockService;
        this.settingsService = settingsService;
        this.sequenceService = sequenceService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Transactional
    public Sale completeSale(CartRequest cart) {
        if (cart == null || cart.getItems().isEmpty()) {
            throw new BusinessException("Add at least one product to the cart.");
        }
        ShopSetting settings = settingsService.get();
        Customer customer = cart.getCustomerId() == null ? null
                : customerRepository.findById(cart.getCustomerId())
                .orElseThrow(() -> new BusinessException("Customer not found."));

        Sale sale = new Sale();
        sale.setReceiptNumber(sequenceService.next(settings.getReceiptPrefix()));
        sale.setSaleType(SaleType.SALE);
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setCustomer(customer);
        sale.setCashier(currentUser.user().orElse(null));
        sale.setSaleDate(Instant.now());
        sale.setNotes(StringUtils.hasText(cart.getNotes()) ? cart.getNotes().trim() : null);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        for (CartRequest.Item item : cart.getItems()) {
            if (item.getProductId() == null) {
                throw new BusinessException("One of the cart items is invalid.");
            }
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException("A product in the cart no longer exists."));
            if (!product.isActive()) {
                throw new BusinessException(product.getName() + " is no longer available.");
            }
            BigDecimal quantity = Money.quantity(item.getQuantity());
            if (quantity.signum() <= 0) {
                throw new BusinessException("Quantity for " + product.getName() + " must be greater than zero.");
            }
            BigDecimal gross = Money.multiply(product.getSellingPrice(), quantity);
            BigDecimal discount = Money.of(item.getDiscount() == null ? BigDecimal.ZERO : item.getDiscount());
            if (discount.signum() < 0) {
                throw new BusinessException("Discount cannot be negative.");
            }
            if (discount.compareTo(gross) > 0) {
                throw new BusinessException("Discount for " + product.getName() + " cannot exceed the line total.");
            }
            BigDecimal base = gross.subtract(discount);
            BigDecimal rate = product.getTaxCategory() == null || product.getTaxCategory().isExempt()
                    ? BigDecimal.ZERO : product.getTaxCategory().getRate();
            BigDecimal tax = Money.taxFromGross(base, rate);

            SaleLine line = new SaleLine();
            line.setProduct(product);
            line.setProductName(product.getName());
            line.setProductCode(product.getCode());
            line.setQuantity(quantity);
            line.setUnitPrice(Money.of(product.getSellingPrice()));
            line.setUnitCost(Money.of(product.getBuyingPrice()));
            line.setDiscount(discount);
            line.setTaxCategoryCode(product.getTaxCategory() == null ? null : product.getTaxCategory().getCode());
            line.setTaxRate(rate);
            line.setLineTotal(base);
            line.setTaxAmount(tax);
            sale.addLine(line);

            subtotal = subtotal.add(gross);
            discountTotal = discountTotal.add(discount);
            taxTotal = taxTotal.add(tax);
        }

        subtotal = Money.of(subtotal);
        discountTotal = Money.of(discountTotal);
        taxTotal = Money.of(taxTotal);
        BigDecimal total = Money.of(subtotal.subtract(discountTotal));
        sale.setSubtotal(subtotal);
        sale.setDiscountTotal(discountTotal);
        sale.setTaxTotal(taxTotal);
        sale.setTotal(total);

        BigDecimal tendered = BigDecimal.ZERO;
        BigDecimal nonCash = BigDecimal.ZERO;
        for (CartRequest.Payment payment : cart.getPayments()) {
            if (payment.getMethod() == null || payment.getMethod() == PaymentMethod.CREDIT) {
                continue;
            }
            BigDecimal amount = Money.of(payment.getAmount());
            if (amount.signum() <= 0) {
                continue;
            }
            SalePayment saved = new SalePayment();
            saved.setPaymentMethod(payment.getMethod());
            saved.setAmount(amount);
            saved.setReference(StringUtils.hasText(payment.getReference()) ? payment.getReference().trim() : null);
            sale.addPayment(saved);
            tendered = tendered.add(amount);
            if (payment.getMethod() != PaymentMethod.CASH) {
                nonCash = nonCash.add(amount);
            }
        }
        if (nonCash.compareTo(total) > 0) {
            throw new BusinessException("Non-cash payments cannot exceed the total due.");
        }
        if (tendered.compareTo(total) > 0) {
            sale.setChangeDue(Money.of(tendered.subtract(total)));
        }

        BigDecimal paid = tendered.min(total);
        BigDecimal balance = Money.of(total.subtract(paid));
        sale.setAmountPaid(Money.of(paid));
        sale.setBalanceDue(balance);
        if (balance.signum() > 0) {
            if (customer == null) {
                throw new BusinessException("Choose a customer for a credit sale.");
            }
            BigDecimal outstanding = Money.of(saleRepository.sumBalanceByCustomer(customer.getId()));
            BigDecimal limit = Money.of(customer.getCreditLimit());
            if (limit.signum() <= 0 || outstanding.add(balance).compareTo(limit) > 0) {
                throw new BusinessException("This credit sale exceeds the customer's available credit limit. "
                        + "Outstanding: " + outstanding.toPlainString() + ", limit: " + limit.toPlainString() + ".");
            }
            customer.setBalance(Money.of(customer.getBalance().add(balance)));
            customerRepository.save(customer);
        }
        sale.setPaymentStatus(balance.signum() <= 0 ? PaymentStatus.PAID
                : paid.signum() == 0 ? PaymentStatus.UNPAID : PaymentStatus.PARTIAL);
        sale.setEbmSignature(signature(sale, settings));

        Sale saved = saleRepository.save(sale);
        for (SaleLine line : saved.getLines()) {
            if (line.getProduct() != null) {
                stockService.applyMovement(line.getProduct(), MovementType.SALE, line.getQuantity().negate(),
                        line.getUnitCost(), "SALE", String.valueOf(saved.getId()), saved.getReceiptNumber());
            }
        }
        if (cart.getHoldId() != null) {
            heldSaleRepository.deleteById(cart.getHoldId());
        }
        auditService.created("Sale", saved.getId(), saved.getReceiptNumber(), Map.of(
                "receiptNumber", saved.getReceiptNumber(),
                "total", saved.getTotal(),
                "tax", saved.getTaxTotal(),
                "paymentStatus", saved.getPaymentStatus(),
                "customer", customer == null ? "Walk-in" : customer.getName(),
                "items", saved.getLines().size()));
        return saved;
    }

    @Transactional
    public Sale refund(Long saleId, String reason) {
        Sale original = sale(saleId);
        if (original.getSaleType() == SaleType.REFUND) {
            throw new BusinessException("A refund cannot be refunded.");
        }
        if (original.getStatus() != SaleStatus.COMPLETED) {
            throw new BusinessException("This sale is not eligible for a refund.");
        }
        ShopSetting settings = settingsService.get();
        Sale refund = new Sale();
        refund.setReceiptNumber(sequenceService.next(settings.getRefundPrefix()));
        refund.setSaleType(SaleType.REFUND);
        refund.setStatus(SaleStatus.COMPLETED);
        refund.setRefundOf(original);
        refund.setCustomer(original.getCustomer());
        refund.setCashier(currentUser.user().orElse(null));
        refund.setSaleDate(Instant.now());
        refund.setSubtotal(original.getSubtotal());
        refund.setDiscountTotal(original.getDiscountTotal());
        refund.setTaxTotal(original.getTaxTotal());
        refund.setTotal(original.getTotal());
        refund.setAmountPaid(original.getTotal());
        refund.setBalanceDue(BigDecimal.ZERO);
        refund.setPaymentStatus(PaymentStatus.PAID);
        refund.setNotes(StringUtils.hasText(reason) ? reason.trim() : "Refund of " + original.getReceiptNumber());
        for (SaleLine source : original.getLines()) {
            SaleLine line = new SaleLine();
            line.setProduct(source.getProduct());
            line.setProductName(source.getProductName());
            line.setProductCode(source.getProductCode());
            line.setQuantity(source.getQuantity());
            line.setUnitPrice(source.getUnitPrice());
            line.setUnitCost(source.getUnitCost());
            line.setDiscount(source.getDiscount());
            line.setTaxCategoryCode(source.getTaxCategoryCode());
            line.setTaxRate(source.getTaxRate());
            line.setLineTotal(source.getLineTotal());
            line.setTaxAmount(source.getTaxAmount());
            refund.addLine(line);
        }
        refund.setEbmSignature(signature(refund, settings));
        Sale saved = saleRepository.save(refund);

        for (SaleLine line : saved.getLines()) {
            if (line.getProduct() != null) {
                stockService.applyMovement(line.getProduct(), MovementType.REFUND, line.getQuantity(),
                        line.getUnitCost(), "REFUND", String.valueOf(saved.getId()), saved.getReceiptNumber());
            }
        }
        BigDecimal unpaid = original.getBalanceDue();
        if (original.getCustomer() != null && unpaid.signum() > 0) {
            Customer customer = original.getCustomer();
            customer.setBalance(Money.of(customer.getBalance().subtract(unpaid).max(BigDecimal.ZERO)));
            customerRepository.save(customer);
        }
        original.setStatus(SaleStatus.REFUNDED);
        saleRepository.save(original);
        auditService.created("Refund", saved.getId(), saved.getReceiptNumber(),
                Map.of("original", original.getReceiptNumber(), "total", saved.getTotal(),
                        "reason", saved.getNotes()));
        return saved;
    }

    @Transactional(readOnly = true)
    public Sale sale(Long id) {
        return saleRepository.findById(id).orElseThrow(() -> new BusinessException("Sale not found."));
    }

    @Transactional(readOnly = true)
    public Optional<Sale> byReceipt(String receiptNumber) {
        return saleRepository.findByReceiptNumberIgnoreCase(receiptNumber);
    }

    @Transactional(readOnly = true)
    public Page<Sale> search(Instant from, Instant to, Long cashierId, Long customerId,
                             SaleStatus status, SaleType type, String q, PaymentMethod method, Pageable pageable) {
        return saleRepository.search(from, to, cashierId, customerId, status, type, like(q), method, pageable);
    }

    @Transactional(readOnly = true)
    public List<Sale> recent() {
        return saleRepository.findTop10ByOrderBySaleDateDesc();
    }

    @Transactional(readOnly = true)
    public List<Sale> recentForCashier() {
        return currentUser.user()
                .map(user -> saleRepository.search(Instant.EPOCH, Instant.now().plusSeconds(1),
                                user.getId(), null, null, null, "", null,
                                org.springframework.data.domain.PageRequest.of(0, 8))
                        .getContent())
                .orElseGet(saleRepository::findTop10ByOrderBySaleDateDesc);
    }

    @Transactional(readOnly = true)
    public boolean verifySignature(Sale sale) {
        ShopSetting settings = settingsService.get();
        return signature(sale, settings).equals(sale.getEbmSignature());
    }

    @Transactional
    public HeldSale hold(CartRequest cart, String label) {
        HeldSale held = new HeldSale();
        held.setLabel(StringUtils.hasText(label) ? label.trim()
                : "Held " + LocalDate.now() + " " + Instant.now().atZone(ZoneId.systemDefault()).toLocalTime().withNano(0));
        held.setCashier(currentUser.user().orElse(null));
        if (cart.getCustomerId() != null) {
            held.setCustomer(customerRepository.findById(cart.getCustomerId()).orElse(null));
        }
        try {
            held.setCartJson(objectMapper.writeValueAsString(cart));
        } catch (Exception ex) {
            throw new BusinessException("The cart could not be held.", ex);
        }
        held.setCreatedAt(Instant.now());
        return heldSaleRepository.save(held);
    }

    @Transactional(readOnly = true)
    public List<HeldSale> heldSales() {
        return heldSaleRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public CartRequest heldCart(Long heldId) {
        HeldSale held = heldSaleRepository.findById(heldId)
                .orElseThrow(() -> new BusinessException("Held sale not found."));
        try {
            CartRequest cart = objectMapper.readValue(held.getCartJson(), CartRequest.class);
            cart.setHoldId(held.getId());
            return cart;
        } catch (Exception ex) {
            throw new BusinessException("The held cart could not be restored.", ex);
        }
    }

    @Transactional
    public void deleteHeld(Long id) {
        heldSaleRepository.deleteById(id);
    }

    public String signature(Sale sale, ShopSetting settings) {
        try {
            String canonical = String.join("|",
                    sale.getReceiptNumber(),
                    sale.getSaleType().name(),
                    String.valueOf(sale.getSaleDate().toEpochMilli()),
                    sale.getTotal().toPlainString(),
                    sale.getTaxTotal().toPlainString(),
                    settings.getShopTin() == null ? "" : settings.getShopTin(),
                    properties.encryption().key());
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign the receipt.", ex);
        }
    }

    private String like(String q) {
        return StringUtils.hasText(q) ? "%" + q.trim().toLowerCase(Locale.ROOT) + "%" : "";
    }
}
