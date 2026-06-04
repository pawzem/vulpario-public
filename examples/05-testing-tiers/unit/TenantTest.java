package com.example.booking.tenant;

import com.example.booking.shared.identity.TenantId;
import com.example.booking.tenant.contract.TenantRegistered;
import com.example.booking.tenant.contract.TenantRenamed;
import com.example.booking.tenant.contract.TenantStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <strong>Tier 1 — unit.</strong> One class under test ({@code Tenant}), no
 * Spring, no database, no doubles even — the aggregate is pure. These run in
 * milliseconds and pin the domain invariants: valid construction, the events
 * a command buffers, and the rules it rejects.
 */
class TenantTest {

    private static final Instant NOW = Instant.parse("2026-04-20T10:00:00Z");

    @Test
    void register_creates_tenant_in_registered_state_at_version_zero() {
        TenantId id = TenantId.newId();
        Tenant tenant = Tenant.register(id, "Acme", "acme-corp", "admin@acme.example", NOW);

        assertEquals(id, tenant.id());
        assertEquals("Acme", tenant.name());
        assertEquals("acme-corp", tenant.slug());
        assertEquals(TenantStatus.REGISTERED, tenant.status());
        assertEquals(0L, tenant.version());
    }

    @Test
    void register_buffers_a_tenant_registered_event_with_all_fields() {
        TenantId id = TenantId.newId();
        Tenant tenant = Tenant.register(id, "Acme", "acme-corp", "admin@acme.example", NOW);

        var events = tenant.pendingEvents();
        assertEquals(1, events.size());
        TenantRegistered event = (TenantRegistered) events.get(0);
        assertEquals(id, event.tenantId());
        assertEquals("acme-corp", event.slug());
        assertEquals(NOW, event.registeredAt());
    }

    @Test
    void rename_changes_the_name_and_buffers_a_rename_event() {
        Tenant tenant = Tenant.register(TenantId.newId(), "Acme", "acme-corp", "a@b.co", NOW);
        tenant.rename("Acme Europe", NOW.plusSeconds(60));

        assertEquals("Acme Europe", tenant.name());
        assertTrue(tenant.pendingEvents().stream().anyMatch(e -> e instanceof TenantRenamed));
    }

    @Test
    void register_rejects_blank_name_bad_slug_and_bad_email() {
        assertThrows(IllegalArgumentException.class,
            () -> Tenant.register(TenantId.newId(), "   ", "acme-corp", "a@b.co", NOW));
        assertThrows(IllegalArgumentException.class,
            () -> Tenant.register(TenantId.newId(), "A", "ACME", "a@b.co", NOW));
        assertThrows(IllegalArgumentException.class,
            () -> Tenant.register(TenantId.newId(), "A", "acme-corp", "not-an-email", NOW));
    }

    @Test
    void suspend_is_idempotent_and_emits_no_event() {
        Tenant tenant = Tenant.register(TenantId.newId(), "A", "acme-corp", "a@b.co", NOW);
        int eventsAfterRegister = tenant.pendingEvents().size();

        tenant.suspend();
        tenant.suspend();

        assertEquals(TenantStatus.SUSPENDED, tenant.status());
        assertEquals(eventsAfterRegister, tenant.pendingEvents().size(), "suspend emits nothing");
    }

    @Test
    void rehydrate_restores_state_and_emits_nothing() {
        Tenant tenant = Tenant.rehydrate(
            TenantId.newId(), "A", "acme-corp", TenantStatus.ACTIVE, "admin@acme.example", 7L);

        assertEquals(7L, tenant.version());
        assertTrue(tenant.pendingEvents().isEmpty(),
            "rehydration must not emit — that event was already written when the row was first persisted");
    }
}
