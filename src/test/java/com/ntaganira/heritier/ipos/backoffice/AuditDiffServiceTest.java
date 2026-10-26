package com.ntaganira.heritier.ipos.backoffice;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice
 * - File      : AuditDiffServiceTest.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Unit tests for audit diff formatting.
 * </pre>
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntaganira.heritier.ipos.backoffice.entity.AuditLog;
import com.ntaganira.heritier.ipos.backoffice.service.AuditDiffService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditDiffServiceTest {

    private final AuditDiffService service = new AuditDiffService(new ObjectMapper());

    @Test
    void colorChangeRendersLabelAndSwatches() {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setOccurredAt(Instant.now());
        log.setUsername("admin");
        log.setRole("ADMIN");
        log.setAction("UPDATE");
        log.setEntityType("StorageLocation");
        log.setEntityLabel("Diff test");
        log.setChangesJson("{\"colorAccent\":{\"before\":\"#2563EB\",\"after\":\"#16A34A\"}}");

        List<com.ntaganira.heritier.ipos.backoffice.dto.AuditChangeView> changes =
                service.view(log).getChanges();

        assertEquals(1, changes.size());
        var change = changes.get(0);
        assertEquals("Color Accent", change.getLabel());
        assertEquals("#2563EB", change.getBeforeText());
        assertEquals("#16A34A", change.getAfterText());
        assertFalse(change.isBeforeEmpty());
        assertFalse(change.isAfterEmpty());
        assertTrue(change.isBeforeColor());
        assertTrue(change.isAfterColor());
        assertFalse(change.isBeforeStructured());
        assertFalse(change.isAfterStructured());
    }

    @Test
    void nullAndBooleanValuesRenderPlainly() {
        AuditLog log = new AuditLog();
        log.setChangesJson("{\"success\":{\"before\":null,\"after\":true},\"notes\":{\"before\":null,\"after\":\"added\"}}");

        var changes = service.view(log).getChanges();
        assertEquals(2, changes.size());

        var success = changes.get(0);
        assertEquals("Success", success.getLabel());
        assertTrue(success.isBeforeEmpty());
        assertFalse(success.isAfterEmpty());
        assertEquals("Yes", success.getAfterText());
        assertFalse(success.isAfterColor());

        var notes = changes.get(1);
        assertEquals("Notes", notes.getLabel());
        assertTrue(notes.isBeforeEmpty());
        assertFalse(notes.isAfterEmpty());
        assertEquals("added", notes.getAfterText());
    }

    @Test
    void structuredValuesAreFlagged() {
        AuditLog log = new AuditLog();
        log.setChangesJson("{\"items\":{\"before\":{\"a\":1},\"after\":[1,2]}}");

        var change = service.view(log).getChanges().get(0);
        assertEquals("Items", change.getLabel());
        assertEquals("{\"a\":1}", change.getBeforeText());
        assertEquals("[1,2]", change.getAfterText());
        assertTrue(change.isBeforeStructured());
        assertTrue(change.isAfterStructured());
    }

    @Test
    void blankJsonProducesNoChanges() {
        AuditLog log = new AuditLog();
        log.setChangesJson(null);
        assertTrue(service.view(log).getChanges().isEmpty());

        log.setChangesJson("   ");
        assertTrue(service.view(log).getChanges().isEmpty());
    }
}
