package com.ntaganira.heritier.ipos.backoffice;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice
 * - File      : ProductLookupWebTest.java
 * - Date      : 2026. 11. 06.
 * - User      : Heritier NTAGANIRA
 * - Desc      : MockMvc tests for barcode, QR-code and alias product lookups.
 * </pre>
 */

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.ntaganira.heritier.ipos.backoffice.entity.Category;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.entity.Unit;
import com.ntaganira.heritier.ipos.backoffice.repository.CategoryRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ProductLookupWebTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Test
    void looksUpProductByBarcodeAndSku() throws Exception {
        Product product = product();

        mockMvc.perform(get("/products/lookup/barcode")
                        .param("code", product.getBarcode())
                        .with(asProductEditor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.product.productName").value(product.getName()))
                .andExpect(jsonPath("$.product.sku").value(product.getCode()))
                .andExpect(jsonPath("$.product.partNumber").value(product.getPartNumber()))
                .andExpect(jsonPath("$.product.aliases[0]").value("Alias " + product.getCode()));

        mockMvc.perform(get("/products/lookup/barcode")
                        .param("code", product.getCode().toLowerCase())
                        .with(asProductEditor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id").value(product.getId()));

        mockMvc.perform(get("/products/lookup/barcode")
                        .param("code", product.getPartNumber().toLowerCase())
                        .with(asProductEditor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id").value(product.getId()));
    }

    @Test
    void searchesProductsByAliasFragment() throws Exception {
        Product product = product();
        String aliasFragment = product.getAliases().get(1).getAlias();

        mockMvc.perform(get("/products/lookup/search")
                        .param("q", aliasFragment)
                        .with(asProductEditor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId()))
                .andExpect(jsonPath("$[0].productName").value(product.getName()))
                .andExpect(jsonPath("$[0].partNumber").value(product.getPartNumber()))
                .andExpect(jsonPath("$[0].aliases[0]").value("Alias " + product.getCode()));

        mockMvc.perform(get("/products/lookup/search")
                        .param("q", product.getPartNumber())
                        .with(asProductEditor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId()));
    }

    @Test
    void scansQrCodeAndReturnsProduct() throws Exception {
        Product product = product();
        MockMultipartFile image = qrImage(product.getBarcode());

        mockMvc.perform(multipart("/products/lookup/scan")
                        .file(image)
                        .with(asProductEditor())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.code").value(product.getBarcode()))
                .andExpect(jsonPath("$.product.productName").value(product.getName()));
    }

    @Test
    void returnsFoundFalseForUnknownCode() throws Exception {
        mockMvc.perform(get("/products/lookup/barcode")
                        .param("code", "UNKNOWN-" + UUID.randomUUID())
                        .with(asProductEditor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.product").doesNotExist());
    }

    private Product product() {
        Category category = categoryRepository.findAll().get(0);
        Unit unit = unitRepository.findAll().get(0);
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Product product = new Product();
        product.setCode("LOOK-" + suffix);
        product.setBarcode(String.valueOf(9_000_000_000_000_000L + Math.abs(suffix.hashCode())));
        product.setPartNumber("PN-" + suffix);
        product.setName("Lookup product " + suffix);
        product.setCategory(category);
        product.setUnit(unit);
        product.setBuyingPrice(new BigDecimal("100.00"));
        product.setSellingPrice(new BigDecimal("150.00"));
        product.setActive(true);
        product.replaceAliases(List.of("Alias " + product.getCode(), "Search " + suffix));
        return productRepository.saveAndFlush(product);
    }

    private MockMultipartFile qrImage(String value) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 360, 360);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("image", "barcode.png", "image/png", output.toByteArray());
    }

    private RequestPostProcessor asProductEditor() {
        return user("admin").authorities(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("PRODUCT_VIEW"),
                new SimpleGrantedAuthority("PRODUCT_CREATE"));
    }
}
