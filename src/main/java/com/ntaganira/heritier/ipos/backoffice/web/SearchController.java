package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : SearchController.java
 * - Date      : 2026. 11. 07.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Unified inventory search by part number, barcode or QR code.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@PreAuthorize("hasAuthority('PRODUCT_VIEW')")
public class SearchController {

    private final ProductRepository productRepository;

    public SearchController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/search")
    public String search(@RequestParam(name = "q", required = false) String q, Model model) {
        String query = q == null ? "" : q.trim();
        List<Product> results = query.isEmpty()
                ? List.of()
                : productRepository.findForUnifiedSearch(query);

        model.addAttribute("query", query);
        model.addAttribute("results", results);
        return "search";
    }
}
