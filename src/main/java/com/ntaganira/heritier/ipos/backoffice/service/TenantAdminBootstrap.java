package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : TenantAdminBootstrap.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Creates the first administrator and settings for a new tenant.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.TenantForm;
import com.ntaganira.heritier.ipos.backoffice.entity.AppUser;
import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import com.ntaganira.heritier.ipos.backoffice.enums.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class TenantAdminBootstrap {

    private final EntityManagerFactory entityManagerFactory;
    private final PasswordEncoder passwordEncoder;

    public TenantAdminBootstrap(EntityManagerFactory entityManagerFactory, PasswordEncoder passwordEncoder) {
        this.entityManagerFactory = entityManagerFactory;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Uses a dedicated Hibernate session so the web request's open-in-view session
     * cannot leak the platform tenant into the new tenant's rows.
     */
    public void provision(TenantForm form) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();

            ShopSetting settings = new ShopSetting();
            settings.setShopName(form.getName().trim());
            settings.setEmail(form.getEmail().trim().toLowerCase(Locale.ROOT));
            settings.setPhone(form.getPhone());
            entityManager.persist(settings);

            AppUser user = new AppUser();
            user.setUsername(form.getAdminUsername().trim().toLowerCase(Locale.ROOT));
            user.setPasswordHash(passwordEncoder.encode(form.getAdminPassword()));
            user.setFullName(form.getAdminFullName().trim());
            user.setEmail(form.getAdminEmail().trim().toLowerCase(Locale.ROOT));
            user.setRole(UserRole.ADMIN);
            user.setEnabled(true);
            user.setPasswordChangeRequired(true);
            entityManager.persist(user);

            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw ex;
        } finally {
            entityManager.close();
        }
    }
}
