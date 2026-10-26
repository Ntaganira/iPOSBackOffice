package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : WhatsAppService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Sends receipts and notifications through WhatsApp.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.config.AppProperties;
import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.entity.SaleLine;
import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import com.ntaganira.heritier.ipos.backoffice.util.WhatsApp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class WhatsAppService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final SettingsService settingsService;
    private final ZoneId zone;

    public WhatsAppService(SettingsService settingsService, AppProperties properties) {
        this.settingsService = settingsService;
        this.zone = ZoneId.of(properties.zone());
    }

    @Transactional(readOnly = true)
    public String invoiceLink(Sale sale) {
        return invoiceLink(sale, sale.getCustomer() == null ? null : sale.getCustomer().getPhone());
    }

    @Transactional(readOnly = true)
    public String invoiceLink(Sale sale, String recipientPhone) {
        return WhatsApp.link(WhatsApp.tryNormalize(recipientPhone), invoiceMessage(sale));
    }

    @Transactional(readOnly = true)
    public String invoiceMessage(Sale sale) {
        ShopSetting shop = settingsService.get();
        StringBuilder message = new StringBuilder();
        String customerName = sale.getCustomer() == null ? null : sale.getCustomer().getName();
        message.append(StringUtils.hasText(customerName) ? "Hello " + customerName.trim() : "Hello").append(",\n\n");
        message.append(sale.isRefund() ? "Refund" : "Invoice").append(' ')
                .append(sale.getReceiptNumber()).append(" from ").append(shop.getShopName()).append(".\n");
        message.append("Date: ").append(DATE_TIME.format(sale.getSaleDate().atZone(zone))).append('\n');
        for (SaleLine line : sale.getLines()) {
            message.append("- ").append(line.getProductName())
                    .append(" x ").append(quantity(line.getQuantity()))
                    .append(": ").append(money(shop, line.getLineTotal())).append('\n');
        }
        message.append("Total: ").append(money(shop, sale.getTotal())).append('\n');
        if (sale.getBalanceDue() != null && sale.getBalanceDue().signum() > 0) {
            message.append("Balance due: ").append(money(shop, sale.getBalanceDue())).append('\n');
        }
        if (StringUtils.hasText(shop.getWhatsappNumber())) {
            message.append("\nQuestions? WhatsApp us on ").append(shop.getWhatsappNumber()).append(".\n");
        }
        message.append("\nThank you for shopping with ").append(shop.getShopName()).append('.');
        return message.toString();
    }

    private String money(ShopSetting shop, BigDecimal value) {
        return shop.getCurrency() + " " + String.format(Locale.ROOT, "%,.2f",
                value == null ? BigDecimal.ZERO : value);
    }

    private String quantity(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
