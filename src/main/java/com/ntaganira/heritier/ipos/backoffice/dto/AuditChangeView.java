package com.ntaganira.heritier.ipos.backoffice.dto;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.dto
 * - File      : AuditChangeView.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Data transfer object for audit change.
 * </pre>
 */

import lombok.Getter;

@Getter
public class AuditChangeView {

    private final String label;
    private final String beforeText;
    private final String afterText;
    private final boolean beforeEmpty;
    private final boolean afterEmpty;
    private final boolean beforeColor;
    private final boolean afterColor;
    private final boolean beforeStructured;
    private final boolean afterStructured;

    public AuditChangeView(String label, String beforeText, String afterText,
                           boolean beforeEmpty, boolean afterEmpty,
                           boolean beforeColor, boolean afterColor,
                           boolean beforeStructured, boolean afterStructured) {
        this.label = label;
        this.beforeText = beforeText;
        this.afterText = afterText;
        this.beforeEmpty = beforeEmpty;
        this.afterEmpty = afterEmpty;
        this.beforeColor = beforeColor;
        this.afterColor = afterColor;
        this.beforeStructured = beforeStructured;
        this.afterStructured = afterStructured;
    }
}
