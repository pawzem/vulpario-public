package com.example.booking.plan.contract;

import com.example.booking.shared.identity.PlanId;
import com.example.booking.shared.money.Money;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * The plan context's facade — the only surface the subscription context (or
 * anyone else) is allowed to touch. The subscription context depends on
 * {@code "plan :: contract"} and calls these methods; it never sees the
 * {@code Plan} aggregate or the plan repository.
 */
public interface PlanService {

    /** Define a new active plan; fails if another active plan shares the name. */
    PlanDto definePlan(String name, Money price, PlanVisibility visibility);

    /** Look up a plan by id. Empty if none exists. */
    Optional<PlanDto> findById(PlanId id);

    /** Paged catalog listing. */
    Page<PlanDto> findAll(Pageable pageable);

    /**
     * Cheap "exists and is active" check. The subscription context uses this
     * to validate a plan id before creating a subscription — through the
     * facade, not by querying the plan tables.
     */
    default boolean existsAndActive(PlanId id) {
        return findById(id).map(PlanDto::active).orElse(false);
    }
}
