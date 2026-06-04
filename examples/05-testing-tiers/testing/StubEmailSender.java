package com.example.booking.testing.email;

import com.example.booking.shared.email.EmailSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * In-memory {@link EmailSender} that captures every message instead of
 * sending it. The replacement for a mocked transport, per the project's
 * no-Mockito convention: a real implementation of the port, shared by every
 * test in any context that needs to observe outbound mail.
 *
 * <p>It can also arm a one-shot failure ({@link #failNext}) so retry paths are
 * exercised with real behavior, not a mock's {@code thenThrow}.
 */
public final class StubEmailSender implements EmailSender {

    private final List<EmailMessage> sent = new ArrayList<>();
    private RuntimeException nextFailure;

    /** Arm the next call to fail — for testing retry / suppression paths. */
    public void failNext(RuntimeException failure) {
        this.nextFailure = Objects.requireNonNull(failure);
    }

    @Override
    public void send(EmailMessage message) {
        if (nextFailure != null) {
            RuntimeException toThrow = nextFailure;
            nextFailure = null;
            throw toThrow;
        }
        sent.add(message);
    }

    public List<EmailMessage> sent() {
        return List.copyOf(sent);
    }

    public EmailMessage last() {
        if (sent.isEmpty()) {
            throw new IllegalStateException("no emails have been sent yet");
        }
        return sent.get(sent.size() - 1);
    }
}
