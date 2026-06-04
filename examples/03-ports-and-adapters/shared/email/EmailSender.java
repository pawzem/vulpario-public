package com.example.booking.shared.email;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provider-agnostic email <strong>port</strong>. Domain code calls
 * {@link #send(EmailMessage)} and never learns which transport is behind it —
 * a local SMTP server in dev, a cloud email API in production, a logging
 * no-op when nothing is configured. The port speaks domain language
 * (recipient, subject, attachments); no vendor type ever crosses it.
 */
public interface EmailSender {

    void send(EmailMessage message);

    record EmailMessage(
        String to,
        String from,
        String subject,
        String body,
        /** Extra transport headers — a correlation id is the common one. */
        Map<String, String> headers,
        /** File attachments (e.g. a calendar {@code .ics}). Empty by default. */
        List<Attachment> attachments
    ) {

        public EmailMessage {
            to = requireEmail(to, "to");
            from = requireEmail(from, "from");
            Objects.requireNonNull(subject, "subject");
            if (subject.isBlank()) {
                throw new IllegalArgumentException("subject must not be blank");
            }
            Objects.requireNonNull(body, "body");
            headers = headers == null ? Map.of() : Map.copyOf(headers);
            attachments = attachments == null ? List.of() : List.copyOf(attachments);
        }

        public EmailMessage(String to, String from, String subject, String body) {
            this(to, from, subject, body, Map.of(), List.of());
        }

        private static String requireEmail(String value, String field) {
            Objects.requireNonNull(value, field);
            if (!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new IllegalArgumentException(field + " is not a valid address: " + value);
            }
            return value;
        }
    }

    record Attachment(String filename, String contentType, byte[] content) {
        public Attachment {
            Objects.requireNonNull(filename, "filename");
            if (filename.isBlank()) {
                throw new IllegalArgumentException("filename must not be blank");
            }
            Objects.requireNonNull(contentType, "contentType");
            Objects.requireNonNull(content, "content");
        }
    }
}
