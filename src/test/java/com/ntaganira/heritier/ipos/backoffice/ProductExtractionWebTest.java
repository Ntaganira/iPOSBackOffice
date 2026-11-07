package com.ntaganira.heritier.ipos.backoffice;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice
 * - File      : ProductExtractionWebTest.java
 * - Date      : 2026. 11. 05.
 * - User      : Heritier NTAGANIRA
 * - Desc      : MockMvc tests for the product image extraction endpoint.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.ProductExtractionResult;
import com.ntaganira.heritier.ipos.backoffice.service.ProductExtractionException;
import com.ntaganira.heritier.ipos.backoffice.service.ProductExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ProductExtractionWebTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductExtractionService extractionService;

    @Test
    void extractReturnsStructuredProduct() throws Exception {
        when(extractionService.extract(any())).thenReturn(new ProductExtractionResult(
                "Milk", "SKU-001", "MPN-7788", "5901234123457", "Beverages", new BigDecimal("2500.00"),
                java.util.List.of("Fresh milk", "UHT milk")));

        mockMvc.perform(multipart("/products/extract")
                        .file(image())
                        .with(asProductEditor())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Milk"))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.partNumber").value("MPN-7788"))
                .andExpect(jsonPath("$.barcode").value("5901234123457"))
                .andExpect(jsonPath("$.category").value("Beverages"))
                .andExpect(jsonPath("$.estimatedPrice").value(2500.00))
                .andExpect(jsonPath("$.aliases[0]").value("Fresh milk"));
    }

    @Test
    void extractReturnsReadableJsonError() throws Exception {
        when(extractionService.extract(any()))
                .thenThrow(new ProductExtractionException("No readable text was found in the image."));

        mockMvc.perform(multipart("/products/extract")
                        .file(image())
                        .with(asProductEditor())
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("No readable text was found in the image."));
    }

    @Test
    void extractRejectsRequestWithoutCsrfToken() throws Exception {
        mockMvc.perform(multipart("/products/extract")
                        .file(image())
                        .with(asProductEditor()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "label.png", "image/png", new byte[]{1, 2, 3});
    }

    private RequestPostProcessor asProductEditor() {
        return user("admin").authorities(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("PRODUCT_VIEW"),
                new SimpleGrantedAuthority("PRODUCT_CREATE"));
    }
}
