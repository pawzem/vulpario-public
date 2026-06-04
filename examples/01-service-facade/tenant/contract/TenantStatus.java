package com.example.booking.tenant.contract;

public enum TenantStatus {

    /** Registered, but the admin invite hasn't been accepted yet. */
    REGISTERED,

    /** Onboarded and paying — bookings are allowed. */
    ACTIVE,

    /** Flagged (non-payment, compliance, …). Bookings are refused. */
    SUSPENDED
}
