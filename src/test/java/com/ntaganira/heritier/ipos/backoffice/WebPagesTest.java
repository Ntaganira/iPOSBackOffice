package com.ntaganira.heritier.ipos.backoffice;

import com.ntaganira.heritier.ipos.backoffice.repository.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
