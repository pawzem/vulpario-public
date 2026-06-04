package com.example.booking.tenant.contract;

import com.example.booking.shared.identity.TenantId;

import java.time.Instant;
import java.util.Objects;

/**
 * Integration event: a tenant was created. Published to the outbox by the
 * aggregate's command method (see example 02 for how a buffered domain event
 * becomes a durable outbox row).
 *
 * <p>The event type carries a version suffix ({@code .v1}). Consumers and
 * the bus key on this string; bumping the schema means a new suffix plus an
 * upcaster, never a silent shape change.
 */
public record TenantRegistered(
    TenantId tenantId,
    String name,
    String slug,
    String adminEmail,
    Instant registeredAt
) {

    public static final String EVENT_TYPE = "tenant.TenantRegistered.v1";

    public TenantRegistered {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(slug, "slug");
        Objects.requireNonNull(adminEmail, "adminEmail");
        Objects.requireNonNull(registeredAt, "registeredAt");
    }
}
