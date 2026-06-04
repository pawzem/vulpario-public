package com.example.booking.shared.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <strong>safe fallback adapter</strong>: logs the intended message and
 * drops it. It's bean-present on <em>every</em> boot, so the port is
 * exercised at context load even when no real transport is wired, and local
 * dev never crashes for want of a mail credential. The INFO line leaves a
 * visible trail so an engineer notices that nothing is actually being sent.
 *
 * <p>A real adapter (see {@code SmtpEmailSender}) registers as
 * {@code @Primary} and takes over when its property gate is enabled.
 */
public final class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info("LoggingEmailSender (no real transport wired) — from={} to={} subject={}",
            message.from(), message.to(), message.subject());
    }
}
