package com.ntaganira.heritier.ipos.backoffice.repository;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.repository
 * - File      : DocumentSequenceRepository.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Data repository for document sequence.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.DocumentSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DocumentSequence d where d.name = :name")
    Optional<DocumentSequence> lockByName(@Param("name") String name);
}
