package com.example.booking.bus;

import com.example.booking.shared.outbox.OutboxPublisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter that implements the {@link OutboxPublisher} port (defined in
 * example 02) over a {@link MessageBusClient}. The {@code OutboxDispatcher}
 * depends on the port; this class is the only thing that knows a message bus
 * exists.
 *
 * <p>It does two adapter jobs: translate each {@code PublishRequest} into the
 * bus's message shape, and translate the bus's per-entry outcomes back into
 * the port's {@code (successful, failed)} result so the dispatcher can mark
 * each outbox row precisely. A different transport = a different class in
 * this package; nothing upstream changes.
 */
public final class MessageBusPublisher implements OutboxPublisher {

    private final MessageBusClient bus;
    private final String sourcePrefix;

    public MessageBusPublisher(MessageBusClient bus, String sourcePrefix) {
        this.bus = bus;
        this.sourcePrefix = sourcePrefix; // e.g. "booking."
    }

    @Override
    public PublishResult publish(Iterable<PublishRequest> batch) {
        List<PublishRequest> requests = new ArrayList<>();
        batch.forEach(requests::add);
        if (requests.isEmpty()) {
            return new PublishResult(Set.of(), Map.of());
        }

        List<MessageBusClient.BusMessage> messages = requests.stream()
            .map(r -> new MessageBusClient.BusMessage(
                sourcePrefix + r.aggregateType().toLowerCase(),
                r.eventType(),
                r.payload(),
                r.occurredAt()))
            .toList();

        List<MessageBusClient.Outcome> outcomes = bus.publish(messages);

        Set<UUID> successful = new java.util.HashSet<>();
        Map<UUID, String> failed = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            UUID id = requests.get(i).eventId();
            MessageBusClient.Outcome outcome = i < outcomes.size()
                ? outcomes.get(i)
                : new MessageBusClient.Outcome(false, "no outcome returned for entry " + i);
            if (outcome.ok()) {
                successful.add(id);
            } else {
                failed.put(id, outcome.errorOrNull() == null ? "unknown bus error" : outcome.errorOrNull());
            }
        }
        return new PublishResult(successful, failed);
    }
}
