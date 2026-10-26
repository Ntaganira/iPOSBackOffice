package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : PdfService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Generates PDF receipts, invoices and reports.
 * </pre>
 */

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.entity.SaleLine;
import com.ntaganira.heritier.ipos.backoffice.entity.SalePayment;
import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    private final SettingsService settingsService;
    private final ReceiptService receiptService;
    private final StorageService storageService;

    public PdfService(SettingsService settingsService,
                      ReceiptService receiptService,
                      StorageService storageService) {
        this.settingsService = settingsService;
        this.receiptService = receiptService;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public byte[] receipt(Sale sale) {
        ShopSetting settings = settingsService.get();
        Rectangle size = new Rectangle(226, 820);
        return build(sale, settings, size, 10, true);
    }

    @Transactional(readOnly = true)
    public byte[] invoice(Sale sale) {
        ShopSetting settings = settingsService.get();
        return build(sale, settings, PageSize.A4, 36, false);
    }

    private byte[] build(Sale sale, ShopSetting settings, Rectangle pageSize, float margin, boolean narrow) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(pageSize, margin, margin, margin, margin);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            Font small = new Font(Font.HELVETICA, narrow ? 7 : 9);
            Font tiny = new Font(Font.HELVETICA, narrow ? 6 : 8);
            Font bold = new Font(Font.HELVETICA, narrow ? 8 : 10, Font.BOLD);
            Font title = new Font(Font.HELVETICA, narrow ? 11 : 18, Font.BOLD);
            Font shopFont = new Font(Font.HELVETICA, narrow ? 13 : 20, Font.BOLD);
            Font center = new Font(Font.HELVETICA, narrow ? 8 : 10);
            center.setColor(Color.DARK_GRAY);

            addLogo(document, settings, narrow);

            Paragraph shop = new Paragraph(settings.getShopName(), shopFont);
            shop.setAlignment(Element.ALIGN_CENTER);
            document.add(shop);
            if (settings.getAddress() != null) {
                document.add(centered(settings.getAddress(), tiny));
            }
            if (settings.getPhone() != null || settings.getEmail() != null || settings.getWhatsappNumber() != null) {
                StringBuilder contact = new StringBuilder();
                appendContact(contact, settings.getPhone());
                if (settings.getWhatsappNumber() != null) {
                    appendContact(contact, "WhatsApp " + settings.getWhatsappNumber());
                }
                appendContact(contact, settings.getEmail());
                document.add(centered(contact.toString(), tiny));
            }
            if (settings.getShopTin() != null) {
                document.add(centered("TIN: " + settings.getShopTin(), small));
            }
            if (narrow) {
                document.add(centered("------------------------------------------------", tiny));
            } else {
                document.add(new Paragraph(" "));
            }

            Paragraph label = new Paragraph(receiptService.receiptLabel(sale), title);
            label.setAlignment(Element.ALIGN_CENTER);
            document.add(label);
            document.add(centered("Receipt No: " + sale.getReceiptNumber(), bold));
            document.add(centered(receiptService.dateTime(sale), small));
            if (sale.getCashier() != null) {
                document.add(centered("Served by: " + sale.getCashier().getFullName(), small));
            }
            if (sale.getCustomer() != null) {
                document.add(centered("Customer: " + sale.getCustomer().getName()
                        + (sale.getCustomer().getTin() == null ? "" : " (TIN " + sale.getCustomer().getTin() + ")"), small));
            }
            if (sale.getRefundOf() != null) {
                document.add(centered("Original receipt: " + sale.getRefundOf().getReceiptNumber(), small));
            }
            document.add(narrow ? centered("------------------------------------------------", tiny) : new Paragraph(" "));

            PdfPTable items = new PdfPTable(narrow ? new float[]{47, 11, 19, 23} : new float[]{36, 11, 11, 16, 10, 16});
            items.setWidthPercentage(100);
            addHeader(items, narrow ? new String[]{"Item", "Qty", "Price", "Total"}
                    : new String[]{"Item", "Code", "Qty", "Unit price", "VAT", "Total"}, tiny);
            Map<String, BigDecimal[]> taxSummary = new TreeMap<>();
            for (SaleLine line : sale.getLines()) {
                if (narrow) {
                    items.addCell(text(line.getProductName(), small));
                    items.addCell(text(plain(line.getQuantity()), small, Element.ALIGN_RIGHT));
                    items.addCell(text(money(line.getUnitPrice()), small, Element.ALIGN_RIGHT));
                    items.addCell(text(money(line.getLineTotal()), small, Element.ALIGN_RIGHT));
                } else {
                    items.addCell(text(line.getProductName(), small));
                    items.addCell(text(line.getProductCode() == null ? "" : line.getProductCode(), small));
                    items.addCell(text(plain(line.getQuantity()), small, Element.ALIGN_RIGHT));
                    items.addCell(text(money(line.getUnitPrice()), small, Element.ALIGN_RIGHT));
                    items.addCell(text((line.getTaxCategoryCode() == null ? "-" : line.getTaxCategoryCode())
                            + " " + plain(line.getTaxRate()) + "%", small, Element.ALIGN_RIGHT));
                    items.addCell(text(money(line.getLineTotal()), small, Element.ALIGN_RIGHT));
                }
                String code = line.getTaxCategoryCode() == null ? "A" : line.getTaxCategoryCode();
                BigDecimal[] totals = taxSummary.computeIfAbsent(code, key -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                totals[0] = totals[0].add(line.getLineTotal());
                totals[1] = totals[1].add(line.getTaxAmount());
            }
            document.add(items);

            document.add(new Paragraph(" "));
            PdfPTable totals = new PdfPTable(new float[]{62, 38});
            totals.setWidthPercentage(100);
            totalRow(totals, "Subtotal", money(sale.getSubtotal()), small, bold);
            if (sale.getDiscountTotal().signum() > 0) {
                totalRow(totals, "Discount", "-" + money(sale.getDiscountTotal()), small, bold);
            }
            totalRow(totals, "VAT included", money(sale.getTaxTotal()), small, bold);
            totalRow(totals, "TOTAL " + settings.getCurrency(), money(sale.getTotal()), bold, bold);
            document.add(totals);

            if (!taxSummary.isEmpty() && !narrow) {
                document.add(new Paragraph("VAT breakdown", bold));
                PdfPTable taxTable = new PdfPTable(new float[]{30, 40, 30});
                taxTable.setWidthPercentage(100);
                addHeader(taxTable, new String[]{"Category", "Taxable amount", "VAT"}, tiny);
                for (Map.Entry<String, BigDecimal[]> entry : taxSummary.entrySet()) {
                    taxTable.addCell(text(entry.getKey(), small));
                    taxTable.addCell(text(money(entry.getValue()[0]), small, Element.ALIGN_RIGHT));
                    taxTable.addCell(text(money(entry.getValue()[1]), small, Element.ALIGN_RIGHT));
                }
                document.add(taxTable);
            }

            if (!sale.getPayments().isEmpty()) {
                document.add(new Paragraph(" "));
                PdfPTable payments = new PdfPTable(new float[]{45, 55});
                payments.setWidthPercentage(100);
                addHeader(payments, new String[]{"Payment", "Amount"}, tiny);
                for (SalePayment payment : sale.getPayments()) {
                    payments.addCell(text(payment.getPaymentMethod().name().replace('_', ' '), small));
                    payments.addCell(text(money(payment.getAmount()), small, Element.ALIGN_RIGHT));
                }
                document.add(payments);
            }
            if (sale.getChangeDue().signum() > 0) {
                document.add(centered("Change: " + settings.getCurrency() + " " + money(sale.getChangeDue()), small));
            }
            if (sale.getBalanceDue().signum() > 0) {
                document.add(centered("Balance due: " + settings.getCurrency() + " " + money(sale.getBalanceDue()), bold));
                document.add(centered("Payment status: " + sale.getPaymentStatus().name(), small));
            }

            if (narrow) {
                document.add(centered("------------------------------------------------", tiny));
            } else {
                document.add(new Paragraph(" "));
            }
            Paragraph footer = new Paragraph(settings.getReceiptFooter() == null
                    ? "Thank you for your business!" : settings.getReceiptFooter(), small);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            document.add(centered("EBM verification code: " + receiptService.verificationCode(sale), tiny));
            Paragraph signature = new Paragraph("Signature: " + safe(sale.getEbmSignature()), tiny);
            signature.setAlignment(Element.ALIGN_CENTER);
            document.add(signature);

            byte[] qr = receiptService.qrPng(sale);
            Image image = Image.getInstance(qr);
            image.scaleToFit(narrow ? 90 : 110, narrow ? 90 : 110);
            image.setAlignment(Element.ALIGN_CENTER);
            document.add(image);

            Paragraph powered = new Paragraph("Powered by iPOS", tiny);
            powered.setAlignment(Element.ALIGN_CENTER);
            document.add(powered);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            if (document.isOpen()) {
                document.close();
            }
            throw new BusinessException("Could not generate the PDF document.", ex);
        }
    }

    private Paragraph centered(String text, Font font) {
        Paragraph paragraph = new Paragraph(text == null ? "" : text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    private void addLogo(Document document, ShopSetting settings, boolean narrow) {
        if (settings.getLogoPath() == null) {
            return;
        }
        try {
            Resource resource = storageService.load(settings.getLogoPath());
            if (resource == null || !resource.exists()) {
                return;
            }
            byte[] bytes;
            try (InputStream in = resource.getInputStream()) {
                bytes = in.readAllBytes();
            }
            Image logo = Image.getInstance(bytes);
            logo.scaleToFit(narrow ? 90 : 120, narrow ? 70 : 90);
            logo.setAlignment(Element.ALIGN_CENTER);
            document.add(logo);
            document.add(new Paragraph(" "));
        } catch (Exception ex) {
            log.warn("Could not render the shop logo on the PDF: {}", ex.getMessage());
        }
    }

    private void appendContact(StringBuilder contact, String value) {
        if (value == null) {
            return;
        }
        if (contact.length() > 0) {
            contact.append("  |  ");
        }
        contact.append(value);
    }

    private void addHeader(PdfPTable table, String[] values, Font font) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, new Font(Font.HELVETICA, font.getSize(), Font.BOLD)));
            cell.setBackgroundColor(new Color(240, 240, 240));
            cell.setBorder(Rectangle.BOTTOM);
            cell.setPadding(3);
            table.addCell(cell);
        }
    }

    private PdfPCell text(String value, Font font) {
        return text(value, font, Element.ALIGN_LEFT);
    }

    private PdfPCell text(String value, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private void totalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell left = text(label, labelFont, Element.ALIGN_RIGHT);
        PdfPCell right = text(value, valueFont, Element.ALIGN_RIGHT);
        table.addCell(left);
        table.addCell(right);
    }

    private String money(BigDecimal value) {
        return String.format("%,.2f", value == null ? BigDecimal.ZERO : value);
    }

    private String plain(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    public Map<String, Object> receiptSummary(Sale sale) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("receiptNumber", sale.getReceiptNumber());
        model.put("total", sale.getTotal());
        model.put("tax", sale.getTaxTotal());
        model.put("verification", receiptService.verificationCode(sale));
        model.put("qr", receiptService.qrDataUri(sale));
        return model;
    }
}
