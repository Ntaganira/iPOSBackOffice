package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : TenantService.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Tenant provisioning, listing and activation management.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.AdminUserView;
import com.ntaganira.heritier.ipos.backoffice.dto.TenantForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Tenant;
import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.TenantRepository;
import com.ntaganira.heritier.ipos.backoffice.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository userRepository;
    private final TenantAdminBootstrap tenantAdminBootstrap;
    private final TenantStatusService tenantStatusService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    public TenantService(TenantRepository tenantRepository,
                         AppUserRepository userRepository,
                         TenantAdminBootstrap tenantAdminBootstrap,
                         TenantStatusService tenantStatusService,
                         AuditService auditService,
                         JdbcTemplate jdbcTemplate) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.tenantAdminBootstrap = tenantAdminBootstrap;
        this.tenantStatusService = tenantStatusService;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Page<Tenant> search(String q, Pageable pageable) {
        String query = StringUtils.hasText(q) ? "%" + q.trim().toLowerCase(Locale.ROOT) + "%" : "";
        return tenantRepository.search(query, pageable);
    }

    public List<AdminUserView> users(long tenantId) {
        return jdbcTemplate.query("""
                        select id, full_name, username, email, role
                        from app_users
                        where tenant_id = ? and enabled = true
                        order by full_name asc
                        """,
                (rs, rowNum) -> new AdminUserView(rs.getLong("id"), rs.getString("full_name"),
                        rs.getString("username"), rs.getString("email"), rs.getString("role")),
                tenantId);
    }

    public Tenant get(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tenant not found."));
    }

    public List<Tenant> activeTenants() {
        return tenantRepository.findByEnabledTrueOrderByNameAsc();
    }

    public Tenant create(TenantForm form) {
        validate(form);
        String code = form.getCode().trim().toLowerCase(Locale.ROOT);
        String email = form.getAdminEmail().trim().toLowerCase(Locale.ROOT);
        if (tenantRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("This tenant code is already in use.");
        }
        if (userRepository.countGlobalByEmail(email) > 0) {
            throw new BusinessException("This administrator email is already used by another user.");
        }

        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(form.getName().trim());
        tenant.setEmail(form.getEmail().trim().toLowerCase(Locale.ROOT));
        tenant.setPhone(StringUtils.hasText(form.getPhone()) ? form.getPhone().trim() : null);
        tenant.setEnabled(true);
        Tenant saved = tenantRepository.save(tenant);

        try {
            TenantContext.runAs(saved.getId(), () -> tenantAdminBootstrap.provision(form));
        } catch (RuntimeException ex) {
            saved.setEnabled(false);
            tenantRepository.save(saved);
            tenantStatusService.evict(saved.getId());
            throw new BusinessException("Tenant provisioning failed: " + rootMessage(ex));
        }

        tenantStatusService.evict(saved.getId());
        TenantContext.runAsSystem(() -> auditService.created("Tenant", saved.getId(), saved.getName(),
                Map.of("code", saved.getCode(), "name", saved.getName(),
                        "adminEmail", email, "enabled", saved.isEnabled())));
        return saved;
    }

    public void toggle(Long id) {
        if (id == null || id == TenantContext.SYSTEM_TENANT_ID) {
            throw new BusinessException("The platform tenant cannot be disabled.");
        }
        Tenant tenant = get(id);
        boolean before = tenant.isEnabled();
        tenant.setEnabled(!before);
        tenantRepository.save(tenant);
        tenantStatusService.evict(id);
        TenantContext.runAsSystem(() -> auditService.updated("Tenant", tenant.getId(), tenant.getName(),
                Map.of("enabled", before), Map.of("enabled", tenant.isEnabled())));
    }

    private void validate(TenantForm form) {
        if (!form.getAdminPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("The temporary passwords do not match.");
        }
        if (!StringUtils.hasText(form.getAdminPassword()) || form.getAdminPassword().length() < 8) {
            throw new BusinessException("Temporary password must be at least 8 characters long.");
        }
        boolean letter = form.getAdminPassword().chars().anyMatch(Character::isLetter);
        boolean digit = form.getAdminPassword().chars().anyMatch(Character::isDigit);
        if (!letter || !digit) {
            throw new BusinessException("Use at least one letter and one number in the temporary password.");
        }
    }

    private String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? ex.getMessage() : current.getMessage();
    }
}
