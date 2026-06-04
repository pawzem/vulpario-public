package com.example.booking.tenant.contract;

/**
 * Wire record returned by the tenant controller. Note the {@code from}
 * factory: the controller's whole job is {@code TenantResponse.from(dto)} —
 * translate the service DTO to the HTTP shape and nothing else. No business
 * logic, no second service call, no cross-context enrichment.
 */
public record TenantResponse(
    String id,
    String name,
    String slug,
    String adminEmail,
    String status
) {

    public static TenantResponse from(TenantDto dto) {
        return new TenantResponse(
            dto.id().toString(),
            dto.name(),
            dto.slug(),
            dto.adminEmail(),
            dto.status().name()
        );
    }
}
