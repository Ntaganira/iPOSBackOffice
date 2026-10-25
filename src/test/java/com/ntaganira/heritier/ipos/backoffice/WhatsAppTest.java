package com.ntaganira.heritier.ipos.backoffice;

import com.ntaganira.heritier.ipos.backoffice.util.WhatsApp;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsAppTest {

    @Test
    void localRwandanNumbersAreConvertedToInternationalFormat() {
        assertThat(WhatsApp.normalize("0788 000 000")).isEqualTo("+250788000000");
        assertThat(WhatsApp.normalize("788 000 000")).isEqualTo("+250788000000");
    }

    @Test
    void internationalNumbersAreKept() {
        assertThat(WhatsApp.normalize("+250 788 000 000")).isEqualTo("+250788000000");
        assertThat(WhatsApp.normalize("00250788000000")).isEqualTo("+250788000000");
    }

    @Test
    void invalidNumbersAreRejected() {
        assertThatThrownBy(() -> WhatsApp.normalize("123")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WhatsApp.normalize("not a phone")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clickToChatLinkCarriesTheInvoiceMessage() {
        String link = WhatsApp.link("+250788000000", "Invoice NS-000001\nTotal: 1,000.00");

        assertThat(link).startsWith("https://wa.me/250788000000?text=");
        assertThat(link).contains("Invoice%20NS-000001%0ATotal%3A%201%2C000.00");
    }

    @Test
    void linkWithoutRecipientOpensTheWhatsAppPicker() {
        assertThat(WhatsApp.link(null, "Hello")).isEqualTo("https://wa.me/?text=Hello");
    }
}
