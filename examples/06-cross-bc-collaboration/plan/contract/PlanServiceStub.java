package com.example.booking.plan.contract;

import com.example.booking.shared.identity.PlanId;
import com.example.booking.shared.money.Money;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * In-memory {@link PlanService} shipped in the contract package so the
 * <em>subscription</em> context's tests can wire a plan stub instead of
 * booting the plan module. This is the cross-context half of the no-Mockito
 * convention: each context publishes the stub others need.
 */
public final class PlanServiceStub implements PlanService {

    private final Map<PlanId, PlanDto> byId = new LinkedHashMap<>();

    /** Seed a plan for a test's arrange step; returns it for convenience. */
    public PlanDto seed(PlanDto plan) {
        byId.put(plan.id(), plan);
        return plan;
    }

    /** Convenience: seed a simple active public plan and return its id. */
    public PlanId seedActive(String name, Money price) {
        PlanDto plan = new PlanDto(PlanId.newId(), name, price, true, PlanVisibility.PUBLIC);
        byId.put(plan.id(), plan);
        return plan.id();
    }

    @Override
    public PlanDto definePlan(String name, Money price, PlanVisibility visibility) {
        PlanDto plan = new PlanDto(PlanId.newId(), name, price, true, visibility);
        byId.put(plan.id(), plan);
        return plan;
    }

    @Override
    public Optional<PlanDto> findById(PlanId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Page<PlanDto> findAll(Pageable pageable) {
        List<PlanDto> all = new ArrayList<>(byId.values());
        int from = (int) Math.min(all.size(), pageable.getOffset());
        int to = (int) Math.min(all.size(), from + pageable.getPageSize());
        return new PageImpl<>(all.subList(from, to), pageable, all.size());
    }
}
