package com.ntaganira.heritier.ipos.backoffice.repository;

import com.ntaganira.heritier.ipos.backoffice.entity.RolePermission;
import com.ntaganira.heritier.ipos.backoffice.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByIdRoleCode(String roleCode);

    @Modifying
    @Query("delete from RolePermission rp where rp.id.roleCode = :roleCode")
    void deleteByRoleCode(@Param("roleCode") String roleCode);
}
