package com.example.booking.subscription;

import com.example.booking.shared.identity.PlanId;
import com.example.booking.shared.identity.SubscriptionId;
import com.example.booking.shared.identity.TenantId;
import com.example.booking.subscription.contract.SubscriptionActivated;
import com.example.booking.subscription.contract.SubscriptionDto;
import com.example.booking.subscription.contract.SubscriptionStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Package-private subscription aggregate. Kept minimal here — the aggregate
 * mechanics are covered in example 01; this example is about how the
 * <em>service</em> collaborates across contexts, not about persistence.
 */
@Table("subscription")
final class Subscription implements Persistable<SubscriptionId> {

    @Id
    private final SubscriptionId id;
    private final TenantId tenantId;
    private final PlanId planId;
    private SubscriptionStatus status;
    @Version
    private long version;
    @Transient
    private final List<Object> pendingEvents = new ArrayList<>();

    Subscription(SubscriptionId id, TenantId tenantId, PlanId planId, SubscriptionStatus status, long version) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.planId = Objects.requireNonNull(planId);
        this.status = Objects.requireNonNull(status);
        this.version = version;
    }

    static Subscription activate(SubscriptionId id, TenantId tenantId, PlanId planId, Instant at) {
        Subscription s = new Subscription(id, tenantId, planId, SubscriptionStatus.ACTIVE, 0L);
        s.pendingEvents.add(new SubscriptionActivated(id, tenantId, planId, at));
        return s;
    }

    @Override public SubscriptionId getId() { return id; }
    @Override public boolean isNew() { return version == 0L; }

    @DomainEvents
    Collection<Object> domainEvents() {
        return List.copyOf(pendingEvents);
    }

    @AfterDomainEventPublication
    void clearDomainEvents() {
        pendingEvents.clear();
    }

    SubscriptionDto toDto() {
        return new SubscriptionDto(id, tenantId, planId, status);
    }
}
