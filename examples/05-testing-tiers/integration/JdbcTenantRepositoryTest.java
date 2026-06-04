package com.example.booking.tenant;

import com.example.booking.shared.identity.TenantId;
import com.example.booking.tenant.contract.TenantStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <strong>Tier 3 — integration.</strong> The real {@code JdbcTenantRepository}
 * against a real PostgreSQL in a Testcontainer. This is where you verify the
 * things only the database can tell you: the SQL is right, the
 * {@link TenantId} ↔ {@code uuid} mapping round-trips, and the {@code @Version}
 * optimistic lock actually fires on a stale write.
 *
 * <p>{@code JdbcTestSupport} (not shown) is a tiny {@code @SpringBootConfiguration}
 * that enables Spring Data JDBC and registers the id converters. Liquibase
 * applies the schema to the container at startup. Story tests outnumber these
 * — they're slower — but every JDBC repository has at least one.
 */
@SpringBootTest(classes = JdbcTestSupport.class)
@Testcontainers
class JdbcTenantRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    TenantRepository tenants;

    @Test
    void save_then_find_by_slug_round_trips_every_field() {
        Tenant saved = tenants.save(
            Tenant.register(TenantId.newId(), "Acme", "acme-corp", "admin@acme.example", Instant.now()));

        Tenant found = tenants.findBySlug("acme-corp").orElseThrow();

        assertEquals(saved.id(), found.id());
        assertEquals("Acme", found.name());
        assertEquals(TenantStatus.REGISTERED, found.status());
        assertTrue(found.version() >= 1, "first insert advances the optimistic-lock version");
    }

    @Test
    void a_stale_write_trips_the_optimistic_lock() {
        Tenant persisted = tenants.save(
            Tenant.register(TenantId.newId(), "Acme", "acme-corp", "admin@acme.example", Instant.now()));

        // Two callers load the same row...
        Tenant loadedA = tenants.findById(persisted.id()).orElseThrow();
        Tenant loadedB = tenants.findById(persisted.id()).orElseThrow();

        // ...A commits first and wins.
        loadedA.rename("Acme One", Instant.now());
        tenants.save(loadedA);

        // ...B now holds a stale version — its save must be rejected, not
        // silently overwrite A's change.
        loadedB.rename("Acme Two", Instant.now());
        assertThrows(OptimisticLockingFailureException.class, () -> tenants.save(loadedB));
    }
}
