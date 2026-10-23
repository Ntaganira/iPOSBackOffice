package com.ntaganira.heritier.ipos.backoffice.entity;

import com.ntaganira.heritier.ipos.backoffice.enums.PrintFormat;
import com.ntaganira.heritier.ipos.backoffice.enums.SmtpSecurity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shop_settings")
public class ShopSetting {

    @Id
    private Integer id = 1;

    @Column(name = "shop_name", nullable = false, length = 160)
    private String shopName = "iPOS Shop";

    @Column(name = "shop_tin", length = 40)
    private String shopTin;

    @Column(length = 255)
    private String address;

    @Column(length = 40)
    private String phone;

    @Column(length = 160)
    private String email;

    @Column(name = "logo_path", length = 255)
    private String logoPath;

    @Column(nullable = false, length = 8)
    private String currency = "RWF";

    @Column(name = "receipt_footer", length = 255)
    private String receiptFooter;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_print_format", nullable = false, length = 10)
    private PrintFormat defaultPrintFormat = PrintFormat.RECEIPT;

    @Column(name = "receipt_prefix", nullable = false, length = 10)
    private String receiptPrefix = "NS-";

    @Column(name = "refund_prefix", nullable = false, length = 10)
    private String refundPrefix = "RF-";

    @Column(name = "allow_negative_stock", nullable = false)
    private boolean allowNegativeStock = false;

    @Column(name = "auto_print", nullable = false)
    private boolean autoPrint = true;

    @Column(name = "expense_approval_threshold", nullable = false)
    private BigDecimal expenseApprovalThreshold = new BigDecimal("100000.00");

    @Column(name = "auto_email_invoice", nullable = false)
    private boolean autoEmailInvoice = false;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = false;

    @Column(name = "smtp_host", length = 160)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort = 1025;

    @Column(name = "smtp_username", length = 160)
    private String smtpUsername;

    @Column(name = "smtp_password_encrypted", length = 512)
    private String smtpPasswordEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "smtp_security", nullable = false, length = 20)
    private SmtpSecurity smtpSecurity = SmtpSecurity.NONE;

    @Column(name = "email_from_name", length = 160)
    private String emailFromName;

    @Column(name = "email_from_address", length = 160)
    private String emailFromAddress;

    @Column(name = "email_reply_to", length = 160)
    private String emailReplyTo;

    @Column(name = "email_bcc_shop", nullable = false)
    private boolean emailBccShop = false;

    @Column(name = "email_subject_template", length = 255)
    private String emailSubjectTemplate;

    @Column(name = "email_body_template", columnDefinition = "text")
    private String emailBodyTemplate;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Transient
    private String smtpPassword;
}
