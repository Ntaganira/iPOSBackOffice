package com.ntaganira.heritier.ipos.backoffice.dto;

public record NotificationItem(
        String type,
        String title,
        String message,
        String url,
        long count) {
}
