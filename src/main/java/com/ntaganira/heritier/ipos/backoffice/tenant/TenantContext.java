package com.ntaganira.heritier.ipos.backoffice.tenant;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.tenant
 * - File      : TenantContext.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Holds the tenant of the current request or background task.
 * </pre>
 */

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public final class TenantContext {

    public static final long SYSTEM_TENANT_ID = 0L;
    public static final long DEFAULT_TENANT_ID = 1L;

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static long require() {
        Long tenantId = CURRENT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant is bound to the current thread.");
        }
        return tenantId;
    }

    public static void set(Long tenantId) {
        if (tenantId == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(tenantId);
        }
    }

    public static void setSystem() {
        set(SYSTEM_TENANT_ID);
    }

    public static boolean isSystem() {
        return Long.valueOf(SYSTEM_TENANT_ID).equals(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static <T> T runAs(long tenantId, Supplier<T> action) {
        Long previous = CURRENT.get();
        set(tenantId);
        try {
            return action.get();
        } finally {
            set(previous);
        }
    }

    public static void runAs(long tenantId, Runnable action) {
        runAs(tenantId, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T runAsSystem(Supplier<T> action) {
        return runAs(SYSTEM_TENANT_ID, action);
    }

    public static void runAsSystem(Runnable action) {
        runAs(SYSTEM_TENANT_ID, action);
    }

    public static <T> Callable<T> wrap(long tenantId, Callable<T> callable) {
        return () -> runAs(tenantId, () -> {
            try {
                return callable.call();
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });
    }
}
