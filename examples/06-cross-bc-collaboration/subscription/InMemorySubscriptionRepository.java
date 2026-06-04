package com.example.booking.subscription;

import com.example.booking.shared.identity.SubscriptionId;
import com.example.booking.shared.identity.TenantId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory {@link SubscriptionRepository} for the subscription context's own story tests. */
final class InMemorySubscriptionRepository implements SubscriptionRepository {

    private final Map<SubscriptionId, Subscription> store = new LinkedHashMap<>();

    @Override
    public <S extends Subscription> S save(S subscription) {
        store.put(subscription.getId(), subscription);
        return subscription;
    }

    @Override
    public Optional<Subscription> findCurrentForTenant(TenantId tenantId) {
        return store.values().stream()
            .filter(s -> s.toDto().tenantId().equals(tenantId))
            .filter(s -> s.toDto().status() != com.example.booking.subscription.contract.SubscriptionStatus.CANCELLED)
            .findFirst();
    }

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
        return Optional.ofNullable(store.get(id));
    }

    // ---- Remainder of CrudRepository: unused by these tests. ----
    @Override public <S extends Subscription> Iterable<S> saveAll(Iterable<S> entities) { throw notUsed(); }
    @Override public boolean existsById(SubscriptionId id) { return store.containsKey(id); }
    @Override public Iterable<Subscription> findAll() { return List.copyOf(store.values()); }
    @Override public Iterable<Subscription> findAllById(Iterable<SubscriptionId> ids) { throw notUsed(); }
    @Override public long count() { return store.size(); }
    @Override public void deleteById(SubscriptionId id) { store.remove(id); }
    @Override public void delete(Subscription entity) { store.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends SubscriptionId> ids) { throw notUsed(); }
    @Override public void deleteAll(Iterable<? extends Subscription> entities) { throw notUsed(); }
    @Override public void deleteAll() { store.clear(); }

    private static UnsupportedOperationException notUsed() {
        return new UnsupportedOperationException("not needed by the use cases under test");
    }
}
