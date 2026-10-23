package com.ntaganira.heritier.ipos.backoffice.service;

import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ReceiptService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final SettingsService settingsService;
    private final QrCodeService qrCodeService;

    public ReceiptService(SettingsService settingsService, QrCodeService qrCodeService) {
        this.settingsService = settingsService;
        this.qrCodeService = qrCodeService;
    }

    public String qrContent(Sale sale) {
        ShopSetting settings = settingsService.get();
        return "iPOS|" + settings.getShopTin() + "|" + sale.getReceiptNumber()
                + "|" + sale.getSaleDate().atZone(ZoneId.systemDefault()).format(DATE_TIME)
                + "|" + sale.getTotal().toPlainString() + "|" + sale.getEbmSignature();
    }

    public String qrDataUri(Sale sale) {
        return qrCodeService.dataUri(qrContent(sale), 320);
    }

    public byte[] qrPng(Sale sale) {
        return qrCodeService.png(qrContent(sale), 320);
    }

    public String dateTime(Sale sale) {
        return DATE_TIME.format(sale.getSaleDate().atZone(ZoneId.systemDefault()));
    }

    public String verificationCode(Sale sale) {
        String signature = sale.getEbmSignature() == null ? "" : sale.getEbmSignature();
        return signature.length() <= 12 ? signature : signature.substring(0, 12);
    }

    public String receiptLabel(Sale sale) {
        return sale.isRefund() ? "REFUND RECEIPT" : "TAX RECEIPT";
    }

    public String title(Sale sale) {
        return receiptLabel(sale).toUpperCase(Locale.ROOT);
    }
}
