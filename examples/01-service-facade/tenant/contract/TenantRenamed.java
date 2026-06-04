package com.example.booking.tenant.contract;

import com.example.booking.shared.identity.TenantId;

import java.time.Instant;
import java.util.Objects;

/** Integration event: a tenant's display name changed. */
public record TenantRenamed(
    TenantId tenantId,
    String newName,
    Instant renamedAt
) {

    public static final String EVENT_TYPE = "tenant.TenantRenamed.v1";

    public TenantRenamed {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(newName, "newName");
        Objects.requireNonNull(renamedAt, "renamedAt");
    }
}
