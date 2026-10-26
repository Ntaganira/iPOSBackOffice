package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : RoleService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Application service for role.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Permission;
import com.ntaganira.heritier.ipos.backoffice.entity.Role;
import com.ntaganira.heritier.ipos.backoffice.entity.RolePermission;
import com.ntaganira.heritier.ipos.backoffice.enums.UserRole;
import com.ntaganira.heritier.ipos.backoffice.repository.PermissionRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.RolePermissionRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditService auditService;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       RolePermissionRepository rolePermissionRepository,
                       AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Role> roles() {
        return roleRepository.findAllByOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<Permission> permissions() {
        return permissionRepository.findAllByOrderByCategoryAscSortOrderAsc();
    }

    @Transactional(readOnly = true)
    public Map<String, List<Permission>> permissionsByCategory() {
        Map<String, List<Permission>> grouped = new LinkedHashMap<>();
        for (Permission permission : permissions()) {
            grouped.computeIfAbsent(permission.getCategory(), key -> new java.util.ArrayList<>()).add(permission);
        }
        return grouped;
    }

    @Transactional(readOnly = true)
    public Set<String> permissionCodes(UserRole role) {
        if (role == null) {
            return Set.of();
        }
        if (role == UserRole.ADMIN) {
            Set<String> all = new LinkedHashSet<>();
            for (Permission permission : permissionRepository.findAll()) {
                all.add(permission.getCode());
            }
            return all;
        }
        Set<String> codes = new LinkedHashSet<>();
        for (RolePermission rolePermission : rolePermissionRepository.findByIdRoleCode(role.name())) {
            codes.add(rolePermission.getId().getPermissionCode());
        }
        return codes;
    }

    @Transactional(readOnly = true)
    public Set<String> permissionCodes(String roleCode) {
        if (roleCode == null) {
            return Set.of();
        }
        try {
            return permissionCodes(UserRole.valueOf(roleCode));
        } catch (IllegalArgumentException ex) {
            return Set.of();
        }
    }

    @Transactional
    public void updateRolePermissions(String roleCode, Collection<String> permissionCodes) {
        UserRole role;
        try {
            role = UserRole.valueOf(roleCode);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Unknown role.");
        }
        if (role == UserRole.ADMIN) {
            throw new BusinessException("Administrator permissions are fixed and cannot be edited.");
        }
        Set<String> before = permissionCodes(role);
        rolePermissionRepository.deleteByRoleCode(roleCode);
        Set<String> wanted = permissionCodes == null ? Set.of() : new LinkedHashSet<>(permissionCodes);
        for (String code : wanted) {
            if (permissionRepository.existsById(code)) {
                rolePermissionRepository.save(new RolePermission(roleCode, code));
            }
        }
        auditService.record("UPDATE", "Role", roleCode, roleCode,
                Map.of("permissions", before), Map.of("permissions", wanted));
    }

    public String roleLabel(String code) {
        return roleRepository.findById(code).map(Role::getName).orElse(code);
    }
}
