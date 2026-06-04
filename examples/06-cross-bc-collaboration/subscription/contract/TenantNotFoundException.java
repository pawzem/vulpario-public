package com.example.booking.subscription.contract;

import com.example.booking.shared.identity.TenantId;

/** Thrown when subscribing a tenant that doesn't exist (per the tenant facade). */
public class TenantNotFoundException extends IllegalStateException {

    public TenantNotFoundException(TenantId tenantId) {
        super("tenant does not exist: " + tenantId);
    }
}
