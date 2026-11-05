package com.ntaganira.heritier.ipos.backoffice.entity;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.entity
 * - File      : DocumentSequence.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : JPA entity representing document sequence.
 * </pre>
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "document_sequences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_sequences_tenant_name",
                columnNames = {"tenant_id", "name"}
        )
)
public class DocumentSequence extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false)
    private long value;
}
