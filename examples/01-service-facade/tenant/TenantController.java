package com.example.booking.tenant;

import com.example.booking.shared.identity.TenantId;
import com.example.booking.tenant.contract.RegisterTenantRequest;
import com.example.booking.tenant.contract.TenantResponse;
import com.example.booking.tenant.contract.TenantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * <strong>Controllers are transport only.</strong> Each method binds the
 * request, calls a <em>single</em> service method, maps the DTO to a wire
 * record via {@code TenantResponse.from(...)}, and returns. No business
 * logic, no second service call, no cross-context enrichment. Typed domain
 * exceptions propagate to a per-context {@code @RestControllerAdvice} (not
 * shown) that renders Problem+JSON.
 *
 * <p>Package-private. It's registered explicitly by a small
 * {@code @Configuration} in the package rather than discovered by a global
 * component scan — keeping the class invisible outside the module.
 */
@RestController
@RequestMapping("/api/v1/tenants")
class TenantController {

    private final TenantService tenants;

    TenantController(TenantService tenants) {
        this.tenants = tenants;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TenantResponse register(@RequestBody RegisterTenantRequest request) {
        return TenantResponse.from(
            tenants.registerTenant(request.name(), request.slug(), request.adminEmail()));
    }

    @GetMapping("/{id}")
    ResponseEntity<TenantResponse> get(@PathVariable UUID id) {
        return tenants.findById(TenantId.of(id))
            .map(TenantResponse::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Returns Page<T> and accepts Pageable — the only shape the pagination
    // fitness test (example 04) accepts for a list endpoint.
    @GetMapping
    Page<TenantResponse> list(Pageable pageable) {
        return tenants.findAll(pageable).map(TenantResponse::from);
    }
}
