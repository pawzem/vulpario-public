package com.example.booking.tenant.contract;

import com.example.booking.shared.identity.TenantId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * In-memory {@link TenantService} for use by <em>other</em> modules' tests.
 * It ships in the contract package precisely so a sibling context (say,
 * subscription) can wire a {@code TenantServiceStub} instead of standing up
 * the whole tenant module.
 *
 * <p>This is the project's no-Mockito convention in action: a real, readable
 * implementation the compiler keeps in sync with the interface — not a
 * mock whose stubbed methods silently rot when the contract changes.
 */
public final class TenantServiceStub implements TenantService {

    private final Map<TenantId, TenantDto> byId = new LinkedHashMap<>();

    /** Seed a tenant for a test's arrange step. Returns it for convenience. */
    public TenantDto seed(TenantDto tenant) {
        byId.put(tenant.id(), tenant);
        return tenant;
    }

    @Override
    public TenantDto registerTenant(String name, String slug, String adminEmail) {
        boolean slugTaken = byId.values().stream().anyMatch(t -> t.slug().equals(slug));
        if (slugTaken) {
            throw new TenantSlugAlreadyTakenException(slug);
        }
        TenantDto dto = new TenantDto(TenantId.newId(), name, slug, adminEmail, TenantStatus.REGISTERED);
        byId.put(dto.id(), dto);
        return dto;
    }

    @Override
    public TenantDto rename(TenantId id, String newName) {
        TenantDto current = require(id);
        TenantDto renamed = new TenantDto(
            current.id(), newName, current.slug(), current.adminEmail(), current.status());
        byId.put(id, renamed);
        return renamed;
    }

    @Override
    public TenantDto suspend(TenantId id) {
        TenantDto current = require(id);
        TenantDto suspended = new TenantDto(
            current.id(), current.name(), current.slug(), current.adminEmail(), TenantStatus.SUSPENDED);
        byId.put(id, suspended);
        return suspended;
    }

    @Override
    public Optional<TenantDto> findById(TenantId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<TenantDto> findBySlug(String slug) {
        return byId.values().stream().filter(t -> t.slug().equals(slug)).findFirst();
    }

    @Override
    public Page<TenantDto> findAll(Pageable pageable) {
        List<TenantDto> all = new ArrayList<>(byId.values());
        int from = (int) Math.min(all.size(), pageable.getOffset());
        int to = (int) Math.min(all.size(), from + pageable.getPageSize());
        return new PageImpl<>(all.subList(from, to), pageable, all.size());
    }

    private TenantDto require(TenantId id) {
        TenantDto current = byId.get(id);
        if (current == null) {
            throw new IllegalArgumentException("tenant not found: " + id);
        }
        return current;
    }
}
