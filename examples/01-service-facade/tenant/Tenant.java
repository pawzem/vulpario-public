package com.example.booking.tenant;

import com.example.booking.shared.identity.TenantId;
import com.example.booking.tenant.contract.TenantDto;
import com.example.booking.tenant.contract.TenantRegistered;
import com.example.booking.tenant.contract.TenantRenamed;
import com.example.booking.tenant.contract.TenantStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The tenant aggregate. <strong>Package-private</strong> — no code outside
 * {@code com.example.booking.tenant} can name this type. Consumers see only
 * {@link TenantDto} through the service facade.
 *
 * <p>It's a <em>rich</em> aggregate: state changes go through
 * intention-revealing command methods ({@link #register}, {@link #rename},
 * {@link #suspend}) that enforce invariants and buffer domain events. There
 * are deliberately <strong>no JavaBean setters</strong> — a build-failing
 * fitness test (example 04) rejects any {@code public void setX(arg)} on a
 * class annotated with {@link Table}.
 *
 * <p>Persistence is Spring Data JDBC. Spring publishes whatever
 * {@link #domainEvents()} returns after a successful {@code save()}; a
 * listener turns those into durable outbox rows (example 02). Implementing
 * {@link Persistable} lets the aggregate own its "is this an insert?"
 * decision via the optimistic-lock {@code version}.
 */
@Table("tenant")
final class Tenant implements Persistable<TenantId> {

    private static final Pattern SLUG = Pattern.compile("^[a-z0-9-]{3,64}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Id
    @Column("id")
    private final TenantId id;

    @Column("name")
    private String name;

    @Column("slug")
    private final String slug;

    @Column("status")
    private TenantStatus status;

    @Column("admin_email")
    private String adminEmail;

    @Version
    private long version;

    @Transient
    private final List<Object> pendingEvents = new ArrayList<>();

    Tenant(TenantId id, String name, String slug, TenantStatus status, String adminEmail, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireNonBlank(name, "name");
        this.slug = slug;
        this.status = Objects.requireNonNull(status, "status");
        this.adminEmail = adminEmail;
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative: " + version);
        }
        this.version = version;
    }

    /** Rehydration helper for the repository — restores stored state, emits nothing. */
    static Tenant rehydrate(
        TenantId id, String name, String slug, TenantStatus status, String adminEmail, long version
    ) {
        return new Tenant(id, name, slug, status, adminEmail, version);
    }

    /** Factory: a brand-new tenant in {@code REGISTERED} state at version 0, buffering its birth event. */
    static Tenant register(TenantId id, String name, String slug, String adminEmail, Instant registeredAt) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(registeredAt, "registeredAt");
        String cleanName = requireNonBlank(name, "name");
        requireSlug(slug);
        requireEmail(adminEmail);

        Tenant tenant = new Tenant(id, cleanName, slug, TenantStatus.REGISTERED, adminEmail, 0L);
        tenant.pendingEvents.add(new TenantRegistered(id, cleanName, slug, adminEmail, registeredAt));
        return tenant;
    }

    void rename(String newName, Instant at) {
        this.name = requireNonBlank(newName, "name");
        pendingEvents.add(new TenantRenamed(id, this.name, at));
    }

    /** Idempotent: suspending an already-suspended tenant is a silent no-op, and emits no event. */
    void suspend() {
        this.status = TenantStatus.SUSPENDED;
    }

    void activate() {
        this.status = TenantStatus.ACTIVE;
    }

    @Override
    public TenantId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return version == 0L;
    }

    @DomainEvents
    Collection<Object> domainEvents() {
        return List.copyOf(pendingEvents);
    }

    @AfterDomainEventPublication
    void clearDomainEvents() {
        pendingEvents.clear();
    }

    /** For tests — peek at buffered events without consuming them. */
    List<Object> pendingEvents() {
        return List.copyOf(pendingEvents);
    }

    TenantId id() { return id; }
    String name() { return name; }
    String slug() { return slug; }
    TenantStatus status() { return status; }
    String adminEmail() { return adminEmail; }
    long version() { return version; }

    TenantDto toDto() {
        return new TenantDto(id, name, slug, adminEmail, status);
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null) {
            throw new NullPointerException(field);
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }

    private static void requireSlug(String slug) {
        Objects.requireNonNull(slug, "slug");
        if (!SLUG.matcher(slug).matches()) {
            throw new IllegalArgumentException("slug must match ^[a-z0-9-]{3,64}$: " + slug);
        }
    }

    private static void requireEmail(String email) {
        Objects.requireNonNull(email, "adminEmail");
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("adminEmail is not a valid address: " + email);
        }
    }
}
