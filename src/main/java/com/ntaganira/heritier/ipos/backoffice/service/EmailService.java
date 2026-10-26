package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : EmailService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Application service for email.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.EmailLog;
import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import com.ntaganira.heritier.ipos.backoffice.enums.EmailStatus;
import com.ntaganira.heritier.ipos.backoffice.repository.EmailLogRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.SaleRepository;
import jakarta.mail.internet.InternetAddress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    private static final int MAX_PER_MINUTE = 5;

    private final EmailLogRepository emailLogRepository;
    private final SaleRepository saleRepository;
    private final SettingsService settingsService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final EmailDeliveryService deliveryService;
    private final EmailTemplateService templateService;
    private final Map<String, Deque<Instant>> recentSends = new ConcurrentHashMap<>();

    public EmailService(EmailLogRepository emailLogRepository,
                        SaleRepository saleRepository,
                        SettingsService settingsService,
                        AuditService auditService,
                        CurrentUserService currentUser,
                        EmailDeliveryService deliveryService,
                        EmailTemplateService templateService) {
        this.emailLogRepository = emailLogRepository;
        this.saleRepository = saleRepository;
        this.settingsService = settingsService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.deliveryService = deliveryService;
        this.templateService = templateService;
    }

    @Transactional
    public EmailLog sendReceipt(Long saleId, String recipient, Long userId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new BusinessException("Sale not found."));
        String to = StringUtils.hasText(recipient) ? recipient.trim()
                : sale.getCustomer() == null ? null : sale.getCustomer().getEmail();
        if (!StringUtils.hasText(to)) {
            throw new BusinessException("Enter the recipient email address.");
        }
        validateAddress(to);
        rateLimit(to);
        ShopSetting settings = settingsService.get();
        EmailLog log = new EmailLog();
        log.setCreatedAt(Instant.now());
        log.setUserId(userId);
        log.setRecipient(to);
        log.setSubject(templateService.subject(settings, sale));
        log.setDocumentType(sale.isRefund() ? "REFUND" : "SALE");
        log.setDocumentNumber(sale.getReceiptNumber());
        log.setStatus(EmailStatus.PENDING);
        EmailLog saved = emailLogRepository.save(log);
        deliveryService.deliver(saved.getId());
        auditService.record("EMAIL_SEND", "Sale", sale.getId(), sale.getReceiptNumber(),
                null, Map.of("recipient", to, "emailLogId", saved.getId()));
        return saved;
    }

    @Transactional
    public EmailLog sendTest(String recipient) {
        if (!StringUtils.hasText(recipient)) {
            throw new BusinessException("Enter a recipient email address.");
        }
        validateAddress(recipient);
        rateLimit(recipient);
        EmailLog log = new EmailLog();
        log.setCreatedAt(Instant.now());
        log.setUserId(currentUser.user().map(u -> u.getId()).orElse(null));
        log.setRecipient(recipient.trim());
        log.setSubject("iPOS email configuration test");
        log.setDocumentType("TEST");
        log.setStatus(EmailStatus.PENDING);
        EmailLog saved = emailLogRepository.save(log);
        deliveryService.deliver(saved.getId());
        auditService.record("EMAIL_TEST", "EmailLog", saved.getId(), recipient, null,
                Map.of("recipient", recipient));
        return saved;
    }

    @Transactional
    public EmailLog resend(Long logId) {
        EmailLog log = emailLogRepository.findById(logId)
                .orElseThrow(() -> new BusinessException("Email log not found."));
        if (log.getStatus() == EmailStatus.SENT) {
            throw new BusinessException("This email was already sent. Use a new send if needed.");
        }
        log.setStatus(EmailStatus.PENDING);
        log.setErrorMessage(null);
        emailLogRepository.save(log);
        deliveryService.deliver(logId);
        return log;
    }

    @Transactional(readOnly = true)
    public Page<EmailLog> search(String q, EmailStatus status, Pageable pageable) {
        return emailLogRepository.search(StringUtils.hasText(q)
                ? "%" + q.trim().toLowerCase(Locale.ROOT) + "%" : "", status, pageable);
    }

    @Transactional(readOnly = true)
    public EmailLog get(Long id) {
        return emailLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Email log not found."));
    }

    public long failedCount() {
        return emailLogRepository.countByStatus(EmailStatus.FAILED);
    }

    private void validateAddress(String value) {
        try {
            InternetAddress address = new InternetAddress(value, true);
            address.validate();
        } catch (Exception ex) {
            throw new BusinessException("Enter a valid email address.");
        }
    }

    private void rateLimit(String recipient) {
        Deque<Instant> sends = recentSends.computeIfAbsent(recipient.toLowerCase(Locale.ROOT),
                key -> new ArrayDeque<>());
        Instant cutoff = Instant.now().minusSeconds(60);
        synchronized (sends) {
            while (!sends.isEmpty() && sends.peekFirst().isBefore(cutoff)) {
                sends.pollFirst();
            }
            if (sends.size() >= MAX_PER_MINUTE) {
                throw new BusinessException("Too many emails were sent to this address. Try again in a minute.");
            }
            sends.addLast(Instant.now());
        }
    }
}
