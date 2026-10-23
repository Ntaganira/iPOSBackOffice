package com.ntaganira.heritier.ipos.backoffice.service;

import com.ntaganira.heritier.ipos.backoffice.entity.EmailLog;
import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import com.ntaganira.heritier.ipos.backoffice.enums.EmailStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.SmtpSecurity;
import com.ntaganira.heritier.ipos.backoffice.repository.EmailLogRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.SaleRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Properties;

@Service
public class EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    private final EmailLogRepository emailLogRepository;
    private final SaleRepository saleRepository;
    private final SettingsService settingsService;
    private final EncryptionService encryptionService;
    private final PdfService pdfService;
    private final EmailTemplateService templateService;

    public EmailDeliveryService(EmailLogRepository emailLogRepository,
                                SaleRepository saleRepository,
                                SettingsService settingsService,
                                EncryptionService encryptionService,
                                PdfService pdfService,
                                EmailTemplateService templateService) {
        this.emailLogRepository = emailLogRepository;
        this.saleRepository = saleRepository;
        this.settingsService = settingsService;
        this.encryptionService = encryptionService;
        this.pdfService = pdfService;
        this.templateService = templateService;
    }

    @Async
    @Transactional
    public void deliver(Long logId) {
        EmailLog emailLog = emailLogRepository.findById(logId).orElse(null);
        if (emailLog == null) {
            return;
        }
        try {
            ShopSetting settings = settingsService.get();
            if (!settings.isEmailEnabled() || !StringUtils.hasText(settings.getSmtpHost())) {
                throw new IllegalStateException("Email is not enabled or the SMTP host is missing.");
            }
            JavaMailSenderImpl sender = sender(settings);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String from = StringUtils.hasText(settings.getEmailFromAddress())
                    ? settings.getEmailFromAddress() : settings.getSmtpUsername();
            if (!StringUtils.hasText(from)) {
                throw new IllegalStateException("Set the sender email address in Settings.");
            }
            if (StringUtils.hasText(settings.getEmailFromName())) {
                helper.setFrom(new InternetAddress(from, settings.getEmailFromName()));
            } else {
                helper.setFrom(from);
            }
            helper.setTo(emailLog.getRecipient());
            if (StringUtils.hasText(settings.getEmailReplyTo())) {
                helper.setReplyTo(settings.getEmailReplyTo());
            }
            if (settings.isEmailBccShop() && StringUtils.hasText(settings.getEmail())) {
                helper.setBcc(settings.getEmail());
            }

            if ("SALE".equals(emailLog.getDocumentType()) || "REFUND".equals(emailLog.getDocumentType())) {
                Sale sale = saleRepository.findByReceiptNumberIgnoreCase(emailLog.getDocumentNumber())
                        .orElseThrow(() -> new IllegalStateException("The receipt no longer exists."));
                String subject = templateService.subject(settings, sale);
                emailLog.setSubject(subject);
                helper.setSubject(subject);
                String body = templateService.body(settings, sale);
                helper.setText(toHtml(body), true);
                byte[] pdf = "REFUND".equals(emailLog.getDocumentType())
                        ? pdfService.receipt(sale) : pdfService.invoice(sale);
                helper.addAttachment((sale.isRefund() ? "refund-" : "invoice-")
                        + sale.getReceiptNumber() + ".pdf", () -> new java.io.ByteArrayInputStream(pdf));
            } else if ("TEST".equals(emailLog.getDocumentType())) {
                helper.setSubject(emailLog.getSubject());
                helper.setText("<p>This is a test email from iPOS. Your SMTP settings are working.</p>", true);
            } else {
                throw new IllegalStateException("Unsupported email document type.");
            }
            sender.send(message);
            emailLog.setStatus(EmailStatus.SENT);
            emailLog.setSentAt(Instant.now());
            emailLog.setErrorMessage(null);
            emailLogRepository.save(emailLog);
        } catch (Exception ex) {
            log.warn("Email delivery failed for log {}: {}", logId, ex.getMessage());
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage(truncate(ex.getMessage(), 2000));
            emailLog.setRetryCount(emailLog.getRetryCount() + 1);
            emailLogRepository.save(emailLog);
        }
    }

    private JavaMailSenderImpl sender(ShopSetting settings) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.getSmtpHost());
        sender.setPort(settings.getSmtpPort() == null ? 25 : settings.getSmtpPort());
        sender.setDefaultEncoding("UTF-8");
        if (StringUtils.hasText(settings.getSmtpUsername())) {
            sender.setUsername(settings.getSmtpUsername());
        }
        if (settings.getSmtpPasswordEncrypted() != null) {
            sender.setPassword(encryptionService.decrypt(settings.getSmtpPasswordEncrypted()));
        }
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(StringUtils.hasText(settings.getSmtpUsername())));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");
        if (settings.getSmtpSecurity() == SmtpSecurity.STARTTLS) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        if (settings.getSmtpSecurity() == SmtpSecurity.SSL) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        return sender;
    }

    private String toHtml(String text) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:14px;line-height:1.6;color:#1f2937\">"
                + escape(text).replace("\n", "<br>") + "</div>";
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "Unknown error";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
