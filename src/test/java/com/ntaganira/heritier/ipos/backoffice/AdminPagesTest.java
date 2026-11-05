package com.ntaganira.heritier.ipos.backoffice;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice
 * - File      : AdminPagesTest.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : MockMvc integration tests for platform administration pages.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.AppUser;
import com.ntaganira.heritier.ipos.backoffice.entity.Tenant;
import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.TenantRepository;
import com.ntaganira.heritier.ipos.backoffice.security.IposUserPrincipal;
import com.ntaganira.heritier.ipos.backoffice.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminPagesTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin",
            "/admin/tenants",
            "/admin/tenants/new",
            "/admin/audit",
            "/admin/tax",
            "/admin/profile"
    })
    void adminPagesRender(String path) throws Exception {
        mockMvc.perform(get(path).with(asSuperAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardShowsPlatformNavigation() throws Exception {
        mockMvc.perform(get("/admin").with(asSuperAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Platform")))
                .andExpect(content().string(containsString("All tenants")))
                .andExpect(content().string(containsString("Global audit")));
    }

    @Test
    void createsTenantAndItsFirstAdministrator() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toLowerCase();
        String code = "acme-" + suffix;
        String adminEmail = "owner-" + suffix + "@example.com";

        mockMvc.perform(post("/admin/tenants").with(asSuperAdmin()).with(csrf())
                        .param("code", code)
                        .param("name", "Acme " + suffix)
                        .param("email", "shop-" + suffix + "@example.com")
                        .param("phone", "+250700000000")
                        .param("adminFullName", "Acme Owner")
                        .param("adminUsername", "owner" + suffix)
                        .param("adminEmail", adminEmail)
                        .param("adminPassword", "Temporary123")
                        .param("confirmPassword", "Temporary123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/tenants/*"));

        Tenant tenant = tenantRepository.findByCodeIgnoreCase(code).orElseThrow();
        assertNotNull(tenant.getId());
        assertEquals(true, tenant.isEnabled());

        AppUser owner = TenantContext.runAs(tenant.getId(),
                () -> userRepository.findByUsername("owner" + suffix).orElseThrow());
        assertEquals(tenant.getId(), owner.getTenantId());
        assertEquals(adminEmail, owner.getEmail());
        assertEquals("ADMIN", owner.getRole().name());
        assertEquals(true, owner.isPasswordChangeRequired());

        mockMvc.perform(get("/admin/tenants/" + tenant.getId()).with(asSuperAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(adminEmail)));
    }

    @Test
    void tenantUserCannotOpenPlatformPages() throws Exception {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mockMvc.perform(get("/admin").with(user("admin").authorities(authorities)))
                .andExpect(status().is3xxRedirection());
    }

    private RequestPostProcessor asSuperAdmin() {
        AppUser user = userRepository.findGlobalByEmail("superadmin@ipos.local").orElseThrow();
        IposUserPrincipal principal = new IposUserPrincipal(
                user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash(),
                user.getFullName(), user.getRole(), user.getTenantId(), "SYSTEM",
                user.isEnabled(), user.isLocked(), false,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        return user(principal);
    }
}
