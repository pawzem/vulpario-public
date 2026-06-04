package com.example.booking.plan.contract;

public enum PlanVisibility {

    /** Offered to tenants in the self-service picker. */
    PUBLIC,

    /** Admin-assigned only (free tiers, partner pricing) — never self-selectable. */
    INTERNAL
}
