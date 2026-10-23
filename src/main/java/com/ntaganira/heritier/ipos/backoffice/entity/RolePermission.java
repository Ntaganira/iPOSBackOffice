package com.ntaganira.heritier.ipos.backoffice.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "role_permissions")
public class RolePermission {

    @EmbeddedId
    private RolePermissionId id;

    public RolePermission(String roleCode, String permissionCode) {
        this.id = new RolePermissionId(roleCode, permissionCode);
    }
}
