package com.example.booking.shared.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed id for a plan. Shared-kernel value object, alongside {@code TenantId}
 * (example 01) and {@code SubscriptionId}. Living in the kernel — not in the
 * plan context's contract — is what lets a {@code SubscriptionActivated} event
 * carry a {@code PlanId} without dragging the plan module into every consumer's
 * dependency graph. That placement is how the module graph stays acyclic.
 */
public record PlanId(UUID value) {

    public PlanId {
        Objects.requireNonNull(value, "PlanId value must not be null");
    }

    public static PlanId of(UUID value) {
        return new PlanId(value);
    }

    public static PlanId newId() {
        return new PlanId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
