package com.example.booking.tenant;

import com.example.booking.shared.identity.TenantId;
import com.example.booking.shared.time.Clock;
import com.example.booking.tenant.contract.TenantDto;
import com.example.booking.tenant.contract.TenantService;
import com.example.booking.tenant.contract.TenantSlugAlreadyTakenException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production {@link TenantService}. <strong>Package-private</strong> and
 * wired by Spring — nothing outside this package references the class, only
 * the interface.
 *
 * <p>This is where the {@code @Transactional} boundary lives. A command
 * method drives the aggregate's factory/command, then {@code save()}s; the
 * repository flushes the aggregate row and the outbox row in this same
 * transaction (example 02). The service holds the transaction; the aggregate
 * holds the invariants; the repository holds the SQL. Each does one job.
 *
 * <p>Note the constructor: dependencies are a repository it owns and a
 * {@link Clock} port. A cross-context dependency would be another BC's
 * <em>service</em> here — never its repository or aggregate (example 06).
 */
class TenantServiceImpl implements TenantService {

    private final TenantRepository tenants;
    private final Clock clock;

    TenantServiceImpl(TenantRepository tenants, Clock clock) {
        this.tenants = Objects.requireNonNull(tenants);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public TenantDto registerTenant(String name, String slug, String adminEmail) {
        tenants.findBySlug(slug).ifPresent(existing -> {
            throw new TenantSlugAlreadyTakenException(slug);
        });
        Tenant tenant = Tenant.register(TenantId.newId(), name, slug, adminEmail, clock.instant());
        tenants.save(tenant);
        return tenant.toDto();
    }

    @Override
    @Transactional
    public TenantDto rename(TenantId id, String newName) {
        Tenant tenant = load(id);
        tenant.rename(newName, clock.instant());
        return tenants.save(tenant).toDto();
    }

    @Override
    @Transactional
    public TenantDto suspend(TenantId id) {
        Tenant tenant = load(id);
        tenant.suspend();
        return tenants.save(tenant).toDto();
    }

    @Override
    public Optional<TenantDto> findById(TenantId id) {
        return tenants.findById(id).map(Tenant::toDto);
    }

    @Override
    public Optional<TenantDto> findBySlug(String slug) {
        return tenants.findBySlug(slug).map(Tenant::toDto);
    }

    @Override
    public Page<TenantDto> findAll(Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable");
        List<TenantDto> page = tenants.findAllPaged(pageable).stream()
            .map(Tenant::toDto)
            .toList();
        return new PageImpl<>(page, pageable, tenants.count());
    }

    private Tenant load(TenantId id) {
        return tenants.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("tenant not found: " + id));
    }
}
