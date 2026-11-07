package com.devpulse.common.security;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_ORG_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static UUID getCurrentOrgId() {
        return CURRENT_ORG_ID.get();
    }

    public static void setCurrentOrgId(UUID orgId) {
        CURRENT_ORG_ID.set(orgId);
    }

    public static void clear() {
        CURRENT_ORG_ID.remove();
    }
}

