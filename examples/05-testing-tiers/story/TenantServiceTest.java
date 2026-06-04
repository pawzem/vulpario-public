package com.example.booking.tenant;

import com.example.booking.shared.identity.TenantId;
import com.example.booking.tenant.contract.TenantDto;
import com.example.booking.tenant.contract.TenantSlugAlreadyTakenException;
import com.example.booking.tenant.contract.TenantStatus;
import com.example.booking.testing.time.FakeClock;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <strong>Tier 2 — story.</strong> The <em>real</em> {@code TenantServiceImpl}
 * wired with in-memory/fake collaborators ({@code InMemoryTenantRepository},
 * {@code FakeClock}). It exercises the use case end-to-end inside the context
 * — the transaction boundary, the slug-collision rule, the aggregate driving —
 * but skips the database, so it stays fast.
 *
 * <p>No mocks: the doubles are real objects whose behavior the test sets up by
 * calling real methods. When the {@code TenantService} interface changes, the
 * compiler points at every double that needs updating.
 */
class TenantServiceTest {

    private final FakeClock clock = FakeClock.startingAt(Instant.parse("2026-04-20T10:00:00Z"));
    private final InMemoryTenantRepository repository = new InMemoryTenantRepository();
    private final TenantServiceImpl service = new TenantServiceImpl(repository, clock);

    @Test
    void register_persists_the_tenant_and_returns_a_dto() {
        TenantDto dto = service.registerTenant("Acme", "acme-corp", "admin@acme.example");

        assertEquals("acme-corp", dto.slug());
        assertEquals(TenantStatus.REGISTERED, dto.status());
        assertTrue(repository.findById(dto.id()).isPresent(), "tenant should be persisted");
    }

    @Test
    void register_rejects_a_duplicate_slug() {
        service.registerTenant("Acme", "acme-corp", "admin@acme.example");

        assertThrows(TenantSlugAlreadyTakenException.class,
            () -> service.registerTenant("Acme Two", "acme-corp", "other@acme.example"));
    }

    @Test
    void rename_updates_the_persisted_name() {
        TenantDto created = service.registerTenant("Acme", "acme-corp", "admin@acme.example");

        TenantDto renamed = service.rename(created.id(), "Acme Europe");

        assertEquals("Acme Europe", renamed.name());
        assertEquals("Acme Europe", service.findById(created.id()).orElseThrow().name());
    }

    @Test
    void find_all_returns_a_bounded_page() {
        for (int i = 0; i < 5; i++) {
            service.registerTenant("Tenant " + i, "tenant-" + i, "admin" + i + "@x.co");
        }

        var page = service.findAll(PageRequest.of(0, 2));

        assertEquals(2, page.getContent().size());
        assertEquals(5, page.getTotalElements());
    }

    @Test
    void rename_unknown_tenant_is_rejected() {
        assertThrows(IllegalArgumentException.class,
            () -> service.rename(TenantId.newId(), "Nope"));
    }
}
