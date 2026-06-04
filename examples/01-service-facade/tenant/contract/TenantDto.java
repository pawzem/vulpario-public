package com.example.booking.tenant.contract;

import com.example.booking.shared.identity.TenantId;

import java.util.Objects;

/**
 * The read-side projection of a {@code Tenant} returned by
 * {@link TenantService}. A DTO, not the aggregate: callers get an immutable
 * view of the public fields and have no way to reach the aggregate's command
 * methods or internal state.
 *
 * <p>The compact constructor revalidates on the way out — a DTO can never
 * represent a tenant that the aggregate's own invariants would reject.
 */
public record TenantDto(
    TenantId id,
    String name,
    String slug,
    String adminEmail,
    TenantStatus status
) {

    public TenantDto {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(slug, "slug");
        if (!slug.matches("^[a-z0-9-]{3,64}$")) {
            throw new IllegalArgumentException("slug must match [a-z0-9-]{3,64}: " + slug);
        }
        Objects.requireNonNull(adminEmail, "adminEmail");
        Objects.requireNonNull(status, "status");
    }
}
