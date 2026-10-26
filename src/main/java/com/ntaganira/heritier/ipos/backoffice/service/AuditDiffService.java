package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : AuditDiffService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Formats audit JSON changes for human-readable display.
 * </pre>
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntaganira.heritier.ipos.backoffice.dto.AuditChangeView;
import com.ntaganira.heritier.ipos.backoffice.dto.AuditLogView;
import com.ntaganira.heritier.ipos.backoffice.entity.AuditLog;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuditDiffService {

    private static final Set<String> ACRONYMS = Set.of(
            "api", "csv", "ebm", "id", "ip", "sku", "smtp", "tin", "url", "vat");

    private final ObjectMapper objectMapper;

    public AuditDiffService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuditLogView view(AuditLog log) {
        return new AuditLogView(log.getId(), log.getOccurredAt(), log.getUsername(), log.getRole(),
                log.getAction(), log.getEntityType(), log.getEntityLabel(), log.getIpAddress(),
                changes(log.getChangesJson()));
    }

    private List<AuditChangeView> changes(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                return List.of();
            }
            List<AuditChangeView> changes = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode detail = field.getValue();
                JsonNode before = null;
                JsonNode after = null;
                if (detail != null && detail.isObject() && (detail.has("before") || detail.has("after"))) {
                    before = detail.get("before");
                    after = detail.get("after");
                } else {
                    after = detail;
                }
                changes.add(change(field.getKey(), before, after));
            }
            return changes;
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private AuditChangeView change(String key, JsonNode before, JsonNode after) {
        return new AuditChangeView(title(key), format(before), format(after),
                isEmpty(before), isEmpty(after), isColor(before), isColor(after),
                isStructured(before), isStructured(after));
    }

    private String title(String key) {
        if (!StringUtils.hasText(key)) {
            return "Value";
        }
        String spaced = key.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                .replace('_', ' ')
                .replace('-', ' ');
        return Arrays.stream(spaced.split("\\s+"))
                .filter(word -> !word.isBlank())
                .map(this::capitalize)
                .collect(Collectors.joining(" "));
    }

    private String capitalize(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        if (ACRONYMS.contains(lower)) {
            return lower.toUpperCase(Locale.ROOT);
        }
        return word.length() == 1
                ? word.toUpperCase(Locale.ROOT)
                : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase(Locale.ROOT);
    }

    private String format(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isBoolean()) {
            return node.asBoolean() ? "Yes" : "No";
        }
        if (node.isTextual() || node.isNumber()) {
            return node.asText();
        }
        return node.toString();
    }

    private boolean isEmpty(JsonNode node) {
        return node == null || node.isNull() || (node.isTextual() && node.asText().isBlank());
    }

    private boolean isColor(JsonNode node) {
        return node != null && node.isTextual()
                && node.asText().trim().matches("^#[0-9a-fA-F]{6}$");
    }

    private boolean isStructured(JsonNode node) {
        return node != null && (node.isObject() || node.isArray());
    }
}
