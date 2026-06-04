package com.example.booking.subscription;

import com.example.booking.shared.identity.SubscriptionId;
import com.example.booking.shared.identity.TenantId;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Package-private repository for the subscription aggregate. */
interface SubscriptionRepository extends CrudRepository<Subscription, SubscriptionId> {

    @Query("SELECT * FROM subscription WHERE tenant_id = :tenantId AND status <> 'CANCELLED'")
    Optional<Subscription> findCurrentForTenant(@Param("tenantId") TenantId tenantId);
}
