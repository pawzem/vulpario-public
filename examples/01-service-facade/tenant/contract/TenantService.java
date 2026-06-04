package com.example.booking.tenant.contract;

import com.example.booking.shared.identity.TenantId;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * The tenant bounded context's single public facade. All controllers, other
 * modules, and tests go through this interface — the {@code contract}
 * package is the only surface other code is allowed to import from
 * {@code com.example.booking.tenant}.
 *
 * <p><strong>The service is also the read model.</strong> Notice that the
 * queries ({@link #findById}, {@link #findBySlug}, {@link #findAll},
 * {@link #exists}) live right here on the facade. There is no separate
 * {@code TenantReadModel} interface or {@code TenantView} projection type —
 * the service interface <em>is</em> the read side. One surface to learn, one
 * surface to stub.
 *
 * <p>Implementations: the production impl is {@code TenantServiceImpl}
 * (package-private, wired by Spring); for tests in other modules, use
 * {@link TenantServiceStub} (in-memory, also in this package).
 */
public interface TenantService {

    /**
     * Register a new tenant. Emits a {@code TenantRegistered} integration
     * event to the outbox in the same transaction as the insert.
     *
     * @throws TenantSlugAlreadyTakenException if {@code slug} is in use.
     */
    TenantDto registerTenant(String name, String slug, String adminEmail);

    /**
     * Rename a tenant. Emits {@code TenantRenamed}. A command method, not a
     * setter — the aggregate decides whether the change is valid and what
     * event it produces.
     */
    TenantDto rename(TenantId id, String newName);

    /**
     * Suspend a tenant (e.g. non-payment). Idempotent — suspending an
     * already-suspended tenant is a no-op. A status flip with no integration
     * event, to show that not every command emits one.
     */
    TenantDto suspend(TenantId id);

    /** Look up a tenant by id. Empty if none exists. */
    Optional<TenantDto> findById(TenantId id);

    /**
     * Look up a tenant by its URL slug. Used by unauthenticated public
     * endpoints that resolve a tenant before a user signs in.
     */
    Optional<TenantDto> findBySlug(String slug);

    /**
     * Paged listing of every tenant. Returns a {@link Page} and accepts a
     * {@link Pageable} — never a raw {@code List}. That rule is enforced for
     * every list endpoint by a build-failing fitness test (see example 04).
     */
    Page<TenantDto> findAll(Pageable pageable);

    /** Cheap existence check — equivalent to {@code findById(id).isPresent()}. */
    default boolean exists(TenantId id) {
        return findById(id).isPresent();
    }
}
