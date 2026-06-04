package com.example.booking.tenant;

import com.example.booking.shared.identity.TenantId;
import com.example.booking.shared.outbox.OutboxEvent;
import com.example.booking.shared.outbox.OutboxWriter;
import com.example.booking.tenant.contract.TenantRegistered;
import com.example.booking.tenant.contract.TenantRenamed;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Bridges the {@code Tenant} aggregate's domain events to two destinations,
 * in one place:
 *
 * <ol>
 *   <li><strong>The outbox</strong> — a durable row for cross-service
 *       delivery over the bus.</li>
 *   <li><strong>The in-process application bus</strong> — re-publishing the
 *       raw contract event so sibling contexts' policies can react with
 *       {@code @TransactionalEventListener(AFTER_COMMIT)} <em>now</em>,
 *       without waiting for a bus round-trip.</li>
 * </ol>
 *
 * <p>This is how a saga runs in-process today and over the bus tomorrow with
 * no change to the policy code: the policy listens to the contract event
 * either way. Because this listener runs while the aggregate's transaction is
 * still open (Spring Data JDBC publishes {@code @DomainEvents} during
 * {@code save()}), the outbox INSERT is atomic with the state change.
 *
 * <p>{@code CloudEventFactory} (injected) is a thin Jackson wrapper that
 * renders the event into a CloudEvents-shaped JSON payload — elided here.
 */
class TenantOutboxListener {

    private final OutboxWriter outbox;
    private final CloudEventFactory cloudEvents;
    private final ApplicationEventPublisher inProcessBus;

    TenantOutboxListener(OutboxWriter outbox, CloudEventFactory cloudEvents, ApplicationEventPublisher inProcessBus) {
        this.outbox = Objects.requireNonNull(outbox);
        this.cloudEvents = Objects.requireNonNull(cloudEvents);
        this.inProcessBus = Objects.requireNonNull(inProcessBus);
    }

    @EventListener
    void onTenantEvent(TenantOutboxEnvelope envelope) {
        switch (envelope.event()) {
            case TenantRegistered e -> {
                write(envelope, e.tenantId(), TenantRegistered.EVENT_TYPE, e.registeredAt(), e);
                inProcessBus.publishEvent(e);
            }
            case TenantRenamed e -> {
                write(envelope, e.tenantId(), TenantRenamed.EVENT_TYPE, e.renamedAt(), e);
                inProcessBus.publishEvent(e);
            }
            default -> { /* events without a downstream consumer are ignored */ }
        }
    }

    private void write(TenantOutboxEnvelope envelope, TenantId tenantId, String eventType, Instant occurredAt, Object data) {
        UUID eventId = UUID.randomUUID();
        String payload = cloudEvents.envelope(eventId, eventType, occurredAt, data);
        outbox.append(new OutboxEvent(
            eventId,
            envelope.aggregateType(),
            envelope.aggregateId(),
            envelope.aggregateVersion(),
            eventType,
            (short) 1,
            payload,
            Map.of(),
            tenantId,
            occurredAt));
    }

    /** A thin port that renders a domain event into CloudEvents-shaped JSON. Implementation elided. */
    interface CloudEventFactory {
        String envelope(UUID eventId, String eventType, Instant occurredAt, Object data);
    }
}
