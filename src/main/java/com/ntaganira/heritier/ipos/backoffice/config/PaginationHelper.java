package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : PaginationHelper.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Pagination helper for list pages.
 * </pre>
 */

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("paginationHelper")
public class PaginationHelper {

    public List<Integer> window(int current, int total, int span) {
        List<Integer> pages = new ArrayList<>();
        if (total <= 0) {
            return pages;
        }
        int start = Math.max(0, current - span);
        int end = Math.min(total - 1, current + span);
        for (int i = start; i <= end; i++) {
            pages.add(i);
        }
        return pages;
    }
}
