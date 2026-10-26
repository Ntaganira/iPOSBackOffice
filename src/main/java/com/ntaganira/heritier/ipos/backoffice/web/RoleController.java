package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : RoleController.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Web controller for role.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Permission;
import com.ntaganira.heritier.ipos.backoffice.entity.Role;
import com.ntaganira.heritier.ipos.backoffice.entity.RolePermission;
import com.ntaganira.heritier.ipos.backoffice.repository.PermissionRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.RolePermissionRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.RoleRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@PreAuthorize("hasAuthority('USER_VIEW')")
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleController(RoleRepository roleRepository,
                          PermissionRepository permissionRepository,
                          RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @GetMapping("/roles")
    public String roles(Model model) {
        List<Role> roles = roleRepository.findAllByOrderByCodeAsc();
        Map<String, List<String>> rolePermissions = new LinkedHashMap<>();
        for (Role role : roles) {
            rolePermissions.put(role.getCode(), rolePermissionRepository.findByIdRoleCode(role.getCode())
                    .stream().map(permission -> permission.getId().getPermissionCode()).toList());
        }
        Map<String, List<Permission>> permissionsByCategory = permissionRepository
                .findAllByOrderByCategoryAscSortOrderAsc().stream()
                .collect(Collectors.groupingBy(Permission::getCategory, LinkedHashMap::new, Collectors.toList()));
        model.addAttribute("roles", roles);
        model.addAttribute("rolePermissions", rolePermissions);
        model.addAttribute("permissionsByCategory", permissionsByCategory);
        return "users/roles";
    }
}
