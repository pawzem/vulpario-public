package com.example.booking.tenant.contract;

/**
 * Thrown by {@link TenantService#registerTenant} when the requested slug is
 * already in use. A typed domain exception in the contract package — the web
 * layer maps it to a {@code 409 Conflict} Problem+JSON response, and other
 * modules can {@code catch} it without depending on tenant internals.
 */
public class TenantSlugAlreadyTakenException extends IllegalStateException {

    public TenantSlugAlreadyTakenException(String slug) {
        super("Tenant slug '" + slug + "' is already taken");
    }
}
