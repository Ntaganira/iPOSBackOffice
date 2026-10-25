package com.ntaganira.heritier.ipos.backoffice;

import com.ntaganira.heritier.ipos.backoffice.dto.CartRequest;
import com.ntaganira.heritier.ipos.backoffice.entity.Category;
import com.ntaganira.heritier.ipos.backoffice.entity.Customer;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.entity.StorageLocation;
import com.ntaganira.heritier.ipos.backoffice.entity.Unit;
import com.ntaganira.heritier.ipos.backoffice.enums.CustomerType;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.CategoryRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.CustomerRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.PermissionRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.StorageLocationRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.UnitRepository;
import com.ntaganira.heritier.ipos.backoffice.service.SaleService;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc
class WebPagesTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private StorageLocationRepository storageLocationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SaleService saleService;

    @Test
    void loginPageRenders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString("iPOS")));
    }

    @Test
    void protectedPagesRedirectToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/imports"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @ParameterizedTest
    @MethodSource("adminPages")
    void authenticatedPagesRender(String route) throws Exception {
        mockMvc.perform(get(route).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<th:"))));
    }

    @Test
    void dashboardContainsChartData() throws Exception {
        mockMvc.perform(get("/dashboard").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("const trendLabels = [")));
    }

    @Test
    void headerShowsLowStockNotification() throws Exception {
        Product product = new Product();
        product.setCode("NOTIF-" + UUID.randomUUID().toString().substring(0, 8));
        product.setName("Notification test product");
        product.setBuyingPrice(new BigDecimal("1.00"));
        product.setSellingPrice(new BigDecimal("2.00"));
        product.setReorderLevel(new BigDecimal("5"));
        product.setStockQuantity(BigDecimal.ONE);
        productRepository.save(product);

        mockMvc.perform(get("/dashboard").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"notif-btn\"")))
                .andExpect(content().string(containsString("id=\"notif-panel\"")))
                .andExpect(content().string(containsString("notif-item-stock")))
                .andExpect(content().string(containsString("Products at or below reorder level:")))
                .andExpect(content().string(containsString("/stock#low-stock")));
    }

    @Test
    void errorPagesUseAppShell() throws Exception {
        mockMvc.perform(get("/403").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(view().name("error/403"))
                .andExpect(content().string(containsString("sidebar-brand")))
                .andExpect(content().string(containsString("error-card")))
                .andExpect(content().string(containsString("Back to dashboard")));
    }

    @Test
    void notFoundPageUsesAppShell() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(org.springframework.http.MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/this-page-does-not-exist")
                        .with(asAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("sidebar-brand")))
                .andExpect(content().string(containsString("error-card")))
                .andExpect(content().string(containsString("Page not found")));
    }

    @Test
    void salesListShowsCheckoutWithoutDateFilter() throws Exception {
        Product product = productRepository.findAll().stream()
                .filter(item -> item.getStockQuantity().signum() > 0)
                .findFirst().orElseThrow();

        CartRequest cart = new CartRequest();
        CartRequest.Item item = new CartRequest.Item();
        item.setProductId(product.getId());
        item.setQuantity(BigDecimal.ONE);
        item.setDiscount(BigDecimal.ZERO);
        cart.setItems(List.of(item));

        CartRequest.Payment payment = new CartRequest.Payment();
        payment.setMethod(PaymentMethod.CASH);
        payment.setAmount(product.getSellingPrice());
        cart.setPayments(List.of(payment));

        Sale sale = saleService.completeSale(cart);

        mockMvc.perform(get("/sales").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(sale.getReceiptNumber())));
    }

    @Test
    void shopSettingsExposeWhatsAppNumber() throws Exception {
        mockMvc.perform(get("/settings/shop").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"whatsappNumber\"")))
                .andExpect(content().string(containsString("WhatsApp number")));
    }

    @Test
    void whatsappInvoiceRedirectsToClickToChat() throws Exception {
        Product product = productRepository.findAll().stream()
                .filter(item -> item.getStockQuantity().signum() > 0)
                .findFirst().orElseThrow();

        CartRequest cart = new CartRequest();
        CartRequest.Item item = new CartRequest.Item();
        item.setProductId(product.getId());
        item.setQuantity(BigDecimal.ONE);
        item.setDiscount(BigDecimal.ZERO);
        cart.setItems(List.of(item));

        CartRequest.Payment payment = new CartRequest.Payment();
        payment.setMethod(PaymentMethod.CASH);
        payment.setAmount(product.getSellingPrice());
        cart.setPayments(List.of(payment));

        Sale sale = saleService.completeSale(cart);

        mockMvc.perform(get("/sales/" + sale.getId() + "/whatsapp")
                        .param("phone", "0788 000 000")
                        .with(asAdmin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://wa.me/250788000000?text=")));
    }

    @Test
    void saleDetailOffersWhatsAppPdfShare() throws Exception {
        Sale sale = completedSale();

        mockMvc.perform(get("/sales/" + sale.getId()).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-whatsapp-pdf")))
                .andExpect(content().string(containsString("Share receipt PDF")))
                .andExpect(content().string(containsString(
                        "data-pdf-url=\"/sales/" + sale.getId() + "/pdf?format=receipt\"")))
                .andExpect(content().string(containsString(
                        "Receipt-" + sale.getReceiptNumber() + ".pdf")));
    }

    @Test
    void receiptPreviewOffersWhatsAppPdfShare() throws Exception {
        Sale sale = completedSale();

        mockMvc.perform(get("/sales/" + sale.getId() + "/receipt").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("WhatsApp PDF")))
                .andExpect(content().string(containsString("data-whatsapp-status")))
                .andExpect(content().string(containsString("/js/whatsapp-share.js")));
    }

    @Test
    void embeddedReceiptCanRenderInsidePosPreview() throws Exception {
        Sale sale = completedSale();

        mockMvc.perform(get("/sales/" + sale.getId() + "/receipt")
                        .param("embedded", "true")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andExpect(content().string(containsString("receipt-paper")))
                .andExpect(content().string(not(containsString("print-toolbar no-print"))));
    }

    @Test
    void receiptPdfIsDownloadableAsAnAttachment() throws Exception {
        Sale sale = completedSale();

        mockMvc.perform(get("/sales/" + sale.getId() + "/pdf")
                        .param("format", "receipt")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString(
                        "Receipt-" + sale.getReceiptNumber() + ".pdf")));
    }

    @Test
    void posReceiptDialogOffersWhatsAppPdfShare() throws Exception {
        mockMvc.perform(get("/pos").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"share-receipt-btn\"")))
                .andExpect(content().string(containsString("data-whatsapp-pdf")));
    }

    @Test
    void heldSaleUsesModalInsteadOfBrowserPrompt() throws Exception {
        mockMvc.perform(get("/pos").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"hold-modal\"")))
                .andExpect(content().string(containsString("id=\"hold-form\"")))
                .andExpect(content().string(containsString("id=\"hold-label\"")))
                .andExpect(content().string(containsString("id=\"hold-save-btn\"")))
                .andExpect(content().string(not(containsString("window.prompt"))));
    }

    @Test
    void posQuickCustomerStartsWithPhoneAndTin() throws Exception {
        String html = mockMvc.perform(get("/pos").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"customer-lookup-status\"")))
                .andReturn().getResponse().getContentAsString();

        int phone = html.indexOf("id=\"qc-phone\"");
        int tin = html.indexOf("id=\"qc-tin\"");
        int name = html.indexOf("id=\"qc-name\"");
        assertTrue(phone > 0 && tin > phone && name > tin,
                "Phone and TIN should appear before the customer name field");
    }

    @Test
    void posCustomerLookupFindsCustomerByFormattedPhone() throws Exception {
        Customer customer = savedLookupCustomer();

        mockMvc.perform(get("/pos/customers/lookup")
                        .param("phone", "+250 " + customer.getPhone().substring(1))
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found", is(true)))
                .andExpect(jsonPath("$.customer.id").value(customer.getId().intValue()))
                .andExpect(jsonPath("$.customer.name").value(customer.getName()))
                .andExpect(jsonPath("$.customer.phone").value(customer.getPhone()))
                .andExpect(jsonPath("$.customer.tin").value(customer.getTin()))
                .andExpect(jsonPath("$.customer.email").value(customer.getEmail()))
                .andExpect(jsonPath("$.customer.address").value(customer.getAddress()))
                .andExpect(jsonPath("$.customer.customerType").value("COMPANY"));
    }

    @Test
    void posCustomerLookupFindsCustomerByFormattedTin() throws Exception {
        Customer customer = savedLookupCustomer();

        mockMvc.perform(get("/pos/customers/lookup")
                        .param("tin", customer.getTin().substring(0, 3) + " " + customer.getTin().substring(3))
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found", is(true)))
                .andExpect(jsonPath("$.customer.id").value(customer.getId().intValue()));
    }

    @Test
    void posCustomerLookupReturnsNoMatchForUnknownIdentifier() throws Exception {
        mockMvc.perform(get("/pos/customers/lookup")
                        .param("tin", "ZZ" + randomSixDigits() + "ZZ")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found", is(false)))
                .andExpect(jsonPath("$.customer").doesNotExist());
    }

    @Test
    void posCustomerLookupRejectsAmbiguousPhone() throws Exception {
        String phone = "0789" + randomSixDigits();
        savedLookupCustomer(phone, "9" + randomSixDigits() + "00");
        savedLookupCustomer(phone, "9" + randomSixDigits() + "00");

        mockMvc.perform(get("/pos/customers/lookup")
                        .param("phone", phone)
                        .with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("More than one customer")));
    }

    @Test
    void userEditFormRenders() throws Exception {
        Long adminId = userRepository.findByUsername("admin").orElseThrow().getId();
        mockMvc.perform(get("/users/" + adminId + "/edit").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(view().name("users/form"))
                .andExpect(content().string(containsString("user-form-layout")))
                .andExpect(content().string(containsString("user-reset-card")))
                .andExpect(content().string(containsString("data-confirm=")));
    }

    @Test
    void taxEditorRendersStyledInputs() throws Exception {
        mockMvc.perform(get("/settings/tax").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("tax-input")))
                .andExpect(content().string(containsString("tax-rate-field")))
                .andExpect(content().string(containsString("switch-track")));
    }

    @Test
    void pagesRenderInFrenchAndKinyarwanda() throws Exception {
        mockMvc.perform(get("/imports").param("lang", "fr").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Importations")));
        mockMvc.perform(get("/imports").param("lang", "rw").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ibiva hanze")));
    }

    @Test
    void csvExportsAreDownloadable() throws Exception {
        mockMvc.perform(get("/reports/sales.csv")
                        .param("from", "2020-01-01")
                        .param("to", "2030-12-31")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("Receipt,Date,Type,Status")));
        mockMvc.perform(get("/products/export").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    void pagesRequirePermissions() throws Exception {
        mockMvc.perform(get("/users").with(user("viewer")
                        .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
        mockMvc.perform(get("/imports").with(user("viewer")
                        .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
    }

    @Test
    void storageLocationsMapProductsAndFilterStock() throws Exception {
        Category category = categoryRepository.findAll().get(0);
        Unit unit = unitRepository.findAll().get(0);
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String locationCode = "RACK-" + suffix;
        String productCode = "LOC-" + suffix;

        mockMvc.perform(post("/stock/locations").with(asAdmin()).with(csrf())
                        .param("code", locationCode)
                        .param("name", "Rack " + suffix)
                        .param("type", "RACK")
                        .param("description", "Aisle 2")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/stock/locations"));

        StorageLocation location = storageLocationRepository.findAllByOrderByCodeAsc().stream()
                .filter(item -> item.getCode().equals(locationCode))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/products").with(asAdmin()).with(csrf())
                        .param("code", productCode)
                        .param("name", "Mapped product " + suffix)
                        .param("categoryId", category.getId().toString())
                        .param("unitId", unit.getId().toString())
                        .param("taxCategoryCode", "A")
                        .param("buyingPrice", "100")
                        .param("sellingPrice", "150")
                        .param("reorderLevel", "5")
                        .param("active", "true")
                        .param("locationId", location.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        Product product = productRepository.findByCodeIgnoreCase(productCode).orElseThrow();
        assertEquals(location.getId(), product.getLocation().getId());

        mockMvc.perform(get("/products").param("locationId", location.getId().toString()).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(product.getName())))
                .andExpect(content().string(containsString(locationCode)));

        mockMvc.perform(get("/stock").param("locationId", location.getId().toString()).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(product.getName())))
                .andExpect(content().string(containsString(locationCode)));

        mockMvc.perform(get("/stock/locations").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(locationCode)))
                .andExpect(content().string(containsString("Aisle 2")));
    }

    @Test
    void duplicateStorageLocationCodeIsRejected() throws Exception {
        String code = "DRAWER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        mockMvc.perform(post("/stock/locations").with(asAdmin()).with(csrf())
                        .param("code", code)
                        .param("name", "Drawer test")
                        .param("type", "DRAWER")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/stock/locations").with(asAdmin()).with(csrf())
                        .param("code", code.toLowerCase())
                        .param("name", "Drawer duplicate")
                        .param("type", "DRAWER")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("stock/location-form"))
                .andExpect(content().string(containsString("already exists")));
    }

    private Customer savedLookupCustomer() {
        String tail = randomSixDigits();
        return savedLookupCustomer("0789" + tail, "9" + tail + "00");
    }

    private Customer savedLookupCustomer(String phone, String tin) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Customer customer = new Customer();
        customer.setName("Lookup Customer " + suffix);
        customer.setPhone(phone);
        customer.setTin(tin);
        customer.setEmail("lookup-" + suffix + "@example.rw");
        customer.setAddress("KG 1 Ave, Kigali");
        customer.setCustomerType(CustomerType.COMPANY);
        customer.setActive(true);
        return customerRepository.save(customer);
    }

    private String randomSixDigits() {
        return String.format("%06d", (UUID.randomUUID().getLeastSignificantBits() & Long.MAX_VALUE) % 1_000_000);
    }

    private Sale completedSale() {
        Product product = productRepository.findAll().stream()
                .filter(item -> item.getStockQuantity().signum() > 0)
                .findFirst().orElseThrow();

        CartRequest cart = new CartRequest();
        CartRequest.Item item = new CartRequest.Item();
        item.setProductId(product.getId());
        item.setQuantity(BigDecimal.ONE);
        item.setDiscount(BigDecimal.ZERO);
        cart.setItems(List.of(item));

        CartRequest.Payment payment = new CartRequest.Payment();
        payment.setMethod(PaymentMethod.CASH);
        payment.setAmount(product.getSellingPrice());
        cart.setPayments(List.of(payment));

        return saleService.completeSale(cart);
    }

    private RequestPostProcessor asAdmin() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        permissionRepository.findAll()
                .forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.getCode())));
        return user("admin").authorities(authorities);
    }

    static Stream<String> adminPages() {
        return Stream.of(
                "/dashboard",
                "/pos",
                "/sales",
                "/products",
                "/products/new",
                "/products/categories",
                "/products/units",
                "/customers",
                "/customers/new",
                "/suppliers",
                "/suppliers/new",
                "/purchases",
                "/purchases/new",
                "/imports",
                "/imports/new",
                "/stock",
                "/stock/locations",
                "/stock/locations/new",
                "/stock/movements",
                "/stock/periods",
                "/expenses",
                "/expenses/new",
                "/expenses/categories",
                "/reports",
                "/reports/sales",
                "/reports/stock",
                "/reports/profit",
                "/reports/purchases",
                "/reports/expenses",
                "/reports/cash",
                "/users",
                "/users/new",
                "/roles",
                "/settings/shop",
                "/settings/email",
                "/settings/tax",
                "/profile",
                "/audit",
                "/email-logs");
    }
}
