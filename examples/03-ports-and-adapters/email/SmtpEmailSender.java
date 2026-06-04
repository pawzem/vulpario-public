package com.example.booking.email;

import com.example.booking.shared.email.EmailSender;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * A real {@link EmailSender} adapter over SMTP. It lives <em>outside</em> the
 * shared kernel, in an adapter package, and activates only behind a property
 * gate ({@code booking.email.transport=smtp}). When active it registers as
 * {@code @Primary}, overriding the {@code LoggingEmailSender} fallback.
 *
 * <p>Note the boundary: this class knows about {@link JavaMailSender},
 * MIME, and charsets — the SDK details. The domain code that called
 * {@code emailSender.send(...)} knows none of it. Swapping SMTP for a cloud
 * email API is a new adapter and a property flip; not one line of domain code
 * changes. That is the whole return on the port/adapter split.
 */
@Component
@Primary
@ConditionalOnProperty(name = "booking.email.transport", havingValue = "smtp")
final class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mail;
    private final String defaultFrom;

    SmtpEmailSender(JavaMailSender mail,
                    @org.springframework.beans.factory.annotation.Value("${booking.email.from:no-reply@bookly.example}")
                    String defaultFrom) {
        this.mail = mail;
        this.defaultFrom = defaultFrom;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mime = mail.createMimeMessage();
            boolean multipart = !message.attachments().isEmpty();
            MimeMessageHelper helper =
                new MimeMessageHelper(mime, multipart, StandardCharsets.UTF_8.name());

            helper.setTo(message.to());
            helper.setFrom(message.from() == null ? defaultFrom : message.from());
            helper.setSubject(message.subject());
            helper.setText(message.body(), false);
            message.headers().forEach((k, v) -> setHeaderQuietly(mime, k, v));
            for (EmailSender.Attachment a : message.attachments()) {
                helper.addAttachment(a.filename(),
                    new org.springframework.core.io.ByteArrayResource(a.content()), a.contentType());
            }
            mail.send(mime);
        } catch (Exception e) {
            // Let it propagate: an email failure inside a transactional flow
            // should unwind that flow, not be swallowed. The reminder pipeline
            // sends outside the transaction precisely so a send failure can be
            // retried without rolling back the booking.
            throw new EmailDeliveryException("SMTP send failed for " + message.to(), e);
        }
    }

    private static void setHeaderQuietly(MimeMessage mime, String name, String value) {
        try {
            mime.setHeader(name, value);
        } catch (Exception ignored) {
            // a malformed custom header must not abort the send
        }
    }

    static final class EmailDeliveryException extends RuntimeException {
        EmailDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
