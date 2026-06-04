package com.example.booking.plan.contract;

import com.example.booking.shared.identity.PlanId;
import com.example.booking.shared.money.Money;

import java.util.Objects;

/**
 * Read-side view of a plan, returned by {@link PlanService}. Carries a
 * shared-kernel {@link Money} so the subscription context can read a plan's
 * price without knowing anything about how the plan context stores it.
 */
public record PlanDto(
    PlanId id,
    String name,
    Money price,
    boolean active,
    PlanVisibility visibility
) {
    public PlanDto {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(visibility, "visibility");
    }
}
