package com.example.booking.tenant.contract;

/**
 * Wire record for {@code POST /api/v1/tenants}. Request/response records live
 * in the contract package next to the DTOs; the controller binds the body to
 * this and calls a single service method. Validation annotations are elided
 * here for brevity — the aggregate is the real validation authority.
 */
public record RegisterTenantRequest(String name, String slug, String adminEmail) {
}
