package com.ntaganira.heritier.ipos.backoffice.service;

import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import com.ntaganira.heritier.ipos.backoffice.entity.TaxCategory;
import com.ntaganira.heritier.ipos.backoffice.enums.PrintFormat;
import com.ntaganira.heritier.ipos.backoffice.enums.SmtpSecurity;
import com.ntaganira.heritier.ipos.backoffice.repository.ShopSettingRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.TaxCategoryRepository;
import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class SettingsService {

    private final ShopSettingRepository settingsRepository;
    private final TaxCategoryRepository taxCategoryRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final StorageService storageService;

    public SettingsService(ShopSettingRepository settingsRepository,
                           TaxCategoryRepository taxCategoryRepository,
                           EncryptionService encryptionService,
                           AuditService auditService,
                           CurrentUserService currentUser,
                           StorageService storageService) {
        this.settingsRepository = settingsRepository;
        this.taxCategoryRepository = taxCategoryRepository;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public ShopSetting get() {
        return settingsRepository.findById(1).orElseGet(this::defaults);
    }

    @Transactional
    public ShopSetting getOrCreate() {
        return settingsRepository.findById(1).orElseGet(() -> settingsRepository.save(defaults()));
    }

    public List<TaxCategory> taxCategories() {
        return taxCategoryRepository.findAllByOrderBySortOrderAsc();
    }

    public List<TaxCategory> activeTaxCategories() {
        return taxCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    public TaxCategory taxCategory(String code) {
        return code == null ? null : taxCategoryRepository.findById(code).orElse(null);
    }

    @Transactional
    public ShopSetting updateGeneral(ShopSetting form) {
        ShopSetting current = getOrCreate();
        Map<String, Object> before = snapshot(current);
        current.setShopName(trim(form.getShopName(), "iPOS Shop"));
        current.setShopTin(trimToNull(form.getShopTin()));
        current.setAddress(trimToNull(form.getAddress()));
        current.setPhone(trimToNull(form.getPhone()));
        current.setEmail(trimToNull(form.getEmail()));
        current.setCurrency(trim(form.getCurrency(), "RWF").toUpperCase());
        current.setReceiptFooter(trimToNull(form.getReceiptFooter()));
        current.setDefaultPrintFormat(form.getDefaultPrintFormat() == null
                ? PrintFormat.RECEIPT : form.getDefaultPrintFormat());
        current.setReceiptPrefix(trim(form.getReceiptPrefix(), "NS-"));
        current.setRefundPrefix(trim(form.getRefundPrefix(), "RF-"));
        current.setAllowNegativeStock(form.isAllowNegativeStock());
        current.setAutoPrint(form.isAutoPrint());
        current.setExpenseApprovalThreshold(Money.of(form.getExpenseApprovalThreshold()));
        current.setUpdatedAt(Instant.now());
        current.setUpdatedBy(currentUser.username());
        ShopSetting saved = settingsRepository.save(current);
        auditService.updated("SystemSetting", saved.getId(), "Shop settings", before, snapshot(saved));
        return saved;
    }

    @Transactional
    public ShopSetting updateEmailSettings(ShopSetting form) {
        ShopSetting current = getOrCreate();
        Map<String, Object> before = snapshot(current);
        current.setAutoEmailInvoice(form.isAutoEmailInvoice());
        current.setEmailEnabled(form.isEmailEnabled());
        current.setSmtpHost(trimToNull(form.getSmtpHost()));
        current.setSmtpPort(form.getSmtpPort());
        current.setSmtpUsername(trimToNull(form.getSmtpUsername()));
        current.setSmtpSecurity(form.getSmtpSecurity() == null ? SmtpSecurity.NONE : form.getSmtpSecurity());
        current.setEmailFromName(trimToNull(form.getEmailFromName()));
        current.setEmailFromAddress(trimToNull(form.getEmailFromAddress()));
        current.setEmailReplyTo(trimToNull(form.getEmailReplyTo()));
        current.setEmailBccShop(form.isEmailBccShop());
        current.setEmailSubjectTemplate(trimToNull(form.getEmailSubjectTemplate()));
        current.setEmailBodyTemplate(form.getEmailBodyTemplate());
        if (StringUtils.hasText(form.getSmtpPassword())) {
            current.setSmtpPasswordEncrypted(encryptionService.encrypt(form.getSmtpPassword()));
        }
        current.setUpdatedAt(Instant.now());
        current.setUpdatedBy(currentUser.username());
        ShopSetting saved = settingsRepository.save(current);
        auditService.updated("SystemSetting", saved.getId(), "Email settings", before, snapshot(saved));
        return saved;
    }

    @Transactional
    public ShopSetting update(ShopSetting form) {
        ShopSetting current = getOrCreate();
        Map<String, Object> before = snapshot(current);
        current.setShopName(trim(form.getShopName(), "iPOS Shop"));
        current.setShopTin(trimToNull(form.getShopTin()));
        current.setAddress(trimToNull(form.getAddress()));
        current.setPhone(trimToNull(form.getPhone()));
        current.setEmail(trimToNull(form.getEmail()));
        current.setCurrency(trim(form.getCurrency(), "RWF").toUpperCase());
        current.setReceiptFooter(trimToNull(form.getReceiptFooter()));
        current.setDefaultPrintFormat(form.getDefaultPrintFormat() == null
                ? PrintFormat.RECEIPT : form.getDefaultPrintFormat());
        current.setReceiptPrefix(trim(form.getReceiptPrefix(), "NS-"));
        current.setRefundPrefix(trim(form.getRefundPrefix(), "RF-"));
        current.setAllowNegativeStock(form.isAllowNegativeStock());
        current.setAutoPrint(form.isAutoPrint());
        current.setExpenseApprovalThreshold(Money.of(form.getExpenseApprovalThreshold()));
        current.setAutoEmailInvoice(form.isAutoEmailInvoice());
        current.setEmailEnabled(form.isEmailEnabled());
        current.setSmtpHost(trimToNull(form.getSmtpHost()));
        current.setSmtpPort(form.getSmtpPort());
        current.setSmtpUsername(trimToNull(form.getSmtpUsername()));
        current.setSmtpSecurity(form.getSmtpSecurity() == null ? SmtpSecurity.NONE : form.getSmtpSecurity());
        current.setEmailFromName(trimToNull(form.getEmailFromName()));
        current.setEmailFromAddress(trimToNull(form.getEmailFromAddress()));
        current.setEmailReplyTo(trimToNull(form.getEmailReplyTo()));
        current.setEmailBccShop(form.isEmailBccShop());
        current.setEmailSubjectTemplate(trimToNull(form.getEmailSubjectTemplate()));
        current.setEmailBodyTemplate(form.getEmailBodyTemplate());
        if (StringUtils.hasText(form.getSmtpPassword())) {
            current.setSmtpPasswordEncrypted(encryptionService.encrypt(form.getSmtpPassword()));
        }
        current.setUpdatedAt(Instant.now());
        current.setUpdatedBy(currentUser.username());
        ShopSetting saved = settingsRepository.save(current);
        auditService.updated("SystemSetting", saved.getId(), "System settings", before, snapshot(saved));
        return saved;
    }

    @Transactional
    public ShopSetting updateLogo(MultipartFile logo) {
        ShopSetting current = getOrCreate();
        String oldLogo = current.getLogoPath();
        if (logo == null || logo.isEmpty()) {
            return current;
        }
        current.setLogoPath(storageService.saveLogo(logo));
        current.setUpdatedAt(Instant.now());
        current.setUpdatedBy(currentUser.username());
        ShopSetting saved = settingsRepository.save(current);
        if (oldLogo != null) {
            storageService.delete(oldLogo);
        }
        auditService.record("LOGO_UPDATED", "SystemSetting", saved.getId(), "Shop logo",
                Map.of("logoPath", oldLogo == null ? "" : oldLogo),
                Map.of("logoPath", saved.getLogoPath()));
        return saved;
    }

    @Transactional
    public void updateTaxCategory(String code, String name, BigDecimal rate, boolean exempt, boolean active) {
        TaxCategory category = taxCategoryRepository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Tax category not found."));
        Map<String, Object> before = Map.of(
                "name", category.getName(),
                "rate", category.getRate(),
                "exempt", category.isExempt(),
                "active", category.isActive());
        category.setName(trim(name, category.getName()));
        category.setRate(Money.of(rate));
        category.setExempt(exempt);
        category.setActive(active);
        taxCategoryRepository.save(category);
        Map<String, Object> after = Map.of(
                "name", category.getName(),
                "rate", category.getRate(),
                "exempt", category.isExempt(),
                "active", category.isActive());
        auditService.updated("TaxCategory", code, category.getCode() + " - " + category.getName(), before, after);
    }

    public String smtpPasswordPlain(ShopSetting settings) {
        return settings.getSmtpPasswordEncrypted() == null
                ? null : encryptionService.decrypt(settings.getSmtpPasswordEncrypted());
    }

    public boolean smtpConfigured(ShopSetting settings) {
        return settings.isEmailEnabled()
                && StringUtils.hasText(settings.getSmtpHost())
                && StringUtils.hasText(settings.getEmailFromAddress());
    }

    public Map<String, Object> snapshot(ShopSetting settings) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("shopName", settings.getShopName());
        map.put("shopTin", settings.getShopTin());
        map.put("address", settings.getAddress());
        map.put("phone", settings.getPhone());
        map.put("email", settings.getEmail());
        map.put("currency", settings.getCurrency());
        map.put("receiptFooter", settings.getReceiptFooter());
        map.put("defaultPrintFormat", settings.getDefaultPrintFormat());
        map.put("receiptPrefix", settings.getReceiptPrefix());
        map.put("refundPrefix", settings.getRefundPrefix());
        map.put("allowNegativeStock", settings.isAllowNegativeStock());
        map.put("autoPrint", settings.isAutoPrint());
        map.put("expenseApprovalThreshold", settings.getExpenseApprovalThreshold());
        map.put("autoEmailInvoice", settings.isAutoEmailInvoice());
        map.put("emailEnabled", settings.isEmailEnabled());
        map.put("smtpHost", settings.getSmtpHost());
        map.put("smtpPort", settings.getSmtpPort());
        map.put("smtpUsername", settings.getSmtpUsername());
        map.put("smtpPasswordEncrypted", settings.getSmtpPasswordEncrypted() == null ? null : "***");
        map.put("smtpSecurity", settings.getSmtpSecurity());
        map.put("emailFromName", settings.getEmailFromName());
        map.put("emailFromAddress", settings.getEmailFromAddress());
        map.put("emailReplyTo", settings.getEmailReplyTo());
        map.put("emailBccShop", settings.isEmailBccShop());
        map.put("emailSubjectTemplate", settings.getEmailSubjectTemplate());
        map.put("emailBodyTemplate", settings.getEmailBodyTemplate());
        return map;
    }

    private ShopSetting defaults() {
        ShopSetting settings = new ShopSetting();
        settings.setId(1);
        settings.setShopName("iPOS Shop");
        settings.setCurrency("RWF");
        settings.setReceiptFooter("Thank you for your business!");
        settings.setDefaultPrintFormat(PrintFormat.RECEIPT);
        settings.setReceiptPrefix("NS-");
        settings.setRefundPrefix("RF-");
        settings.setExpenseApprovalThreshold(new BigDecimal("100000.00"));
        settings.setSmtpPort(1025);
        settings.setSmtpSecurity(SmtpSecurity.NONE);
        settings.setEmailSubjectTemplate("Invoice {invoiceNumber} from {shopName}");
        return settings;
    }

    private String trim(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
