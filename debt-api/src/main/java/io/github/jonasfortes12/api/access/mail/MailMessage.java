package io.github.jonasfortes12.api.access.mail;

/** A plain-text email, before addressing. */
public record MailMessage(String subject, String body) {
}
