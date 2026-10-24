package com.ntaganira.heritier.ipos.backoffice.service;

import com.ntaganira.heritier.ipos.backoffice.dto.NotificationItem;
import com.ntaganira.heritier.ipos.backoffice.enums.EmailStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.ExpenseStatus;
import com.ntaganira.heritier.ipos.backoffice.repository.EmailLogRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ExpenseRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final ProductRepository productRepository;
    private final ExpenseRepository expenseRepository;
    private final EmailLogRepository emailLogRepository;
    private final MessageSource messageSource;

    public NotificationService(ProductRepository productRepository,
                               ExpenseRepository expenseRepository,
                               EmailLogRepository emailLogRepository,
                               MessageSource messageSource) {
        this.productRepository = productRepository;
        this.expenseRepository = expenseRepository;
        this.emailLogRepository = emailLogRepository;
        this.messageSource = messageSource;
    }

    @Transactional(readOnly = true)
    public List<NotificationItem> forUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return List.of();
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        List<NotificationItem> items = new ArrayList<>();

        if (authorities.contains("STOCK_VIEW")) {
            long count = productRepository.countLowStock();
            add(items, "stock", "notification.lowStock.title", "notification.lowStock.message",
                    "/stock#low-stock", count);
        }
        if (authorities.contains("EXPENSE_VIEW")) {
            long count = expenseRepository.countByStatus(ExpenseStatus.PENDING);
            add(items, "expenses", "notification.expenses.title", "notification.expenses.message",
                    "/expenses?status=PENDING", count);
        }
        if (authorities.contains("EMAIL_LOG_VIEW")) {
            long count = emailLogRepository.countByStatus(EmailStatus.FAILED);
            add(items, "emails", "notification.emails.title", "notification.emails.message",
                    "/email-logs?status=FAILED", count);
        }
        return items;
    }

    private void add(List<NotificationItem> items, String type, String titleKey, String messageKey,
                     String url, long count) {
        if (count <= 0) {
            return;
        }
        items.add(new NotificationItem(type, message(titleKey), message(messageKey, count), url, count));
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}
