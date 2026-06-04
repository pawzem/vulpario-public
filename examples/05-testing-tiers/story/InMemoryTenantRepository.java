package com.example.booking.tenant;

import com.example.booking.shared.identity.TenantId;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hand-written in-memory {@link TenantRepository} for story tests. By
 * convention it lives in the <em>same package</em> as the production code but
 * under {@code src/test}, so only the tenant context's own tests can see it —
 * a sibling context never reaches for another BC's internal test double.
 *
 * <p>It implements only the methods the service actually calls and lets the
 * rest of {@code CrudRepository} throw. That's deliberate: a story-test double
 * documents exactly the slice of the repository the use case depends on.
 */
final class InMemoryTenantRepository implements TenantRepository {

    private final Map<TenantId, Tenant> store = new LinkedHashMap<>();

    @Override
    public <S extends Tenant> S save(S tenant) {
        store.put(tenant.id(), tenant);
        return tenant;
    }

    @Override
    public Optional<Tenant> findById(TenantId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return store.values().stream().filter(t -> slug.equals(t.slug())).findFirst();
    }

    @Override
    public List<Tenant> findAllPaged(Pageable pageable) {
        List<Tenant> all = new ArrayList<>(store.values());
        int from = (int) Math.min(all.size(), pageable.getOffset());
        int to = (int) Math.min(all.size(), from + pageable.getPageSize());
        return all.subList(from, to);
    }

    @Override
    public long count() {
        return store.size();
    }

    // ---- Remainder of CrudRepository: unused by the use cases under test. ----
    @Override public <S extends Tenant> Iterable<S> saveAll(Iterable<S> entities) { throw notUsed(); }
    @Override public boolean existsById(TenantId id) { return store.containsKey(id); }
    @Override public Iterable<Tenant> findAll() { return List.copyOf(store.values()); }
    @Override public Iterable<Tenant> findAllById(Iterable<TenantId> ids) { throw notUsed(); }
    @Override public void deleteById(TenantId id) { store.remove(id); }
    @Override public void delete(Tenant entity) { store.remove(entity.id()); }
    @Override public void deleteAllById(Iterable<? extends TenantId> ids) { throw notUsed(); }
    @Override public void deleteAll(Iterable<? extends Tenant> entities) { throw notUsed(); }
    @Override public void deleteAll() { store.clear(); }

    private static UnsupportedOperationException notUsed() {
        return new UnsupportedOperationException("not needed by the use cases under test");
    }
}
