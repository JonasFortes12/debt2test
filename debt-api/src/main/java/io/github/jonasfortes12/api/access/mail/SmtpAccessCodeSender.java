package io.github.jonasfortes12.api.access.mail;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import io.github.jonasfortes12.api.access.EmailAddresses;

/**
 * Sends plain-text emails over SMTP. A delivery failure is logged (email masked) and swallowed: the
 * code request has already been answered 202, which by design never depends on delivery.
 */
public class SmtpAccessCodeSender implements AccessCodeSender {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpAccessCodeSender.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String frontendUrl;

    public SmtpAccessCodeSender(JavaMailSender mailSender, String from, String frontendUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void sendCode(String email, String code, Instant expiresAt, int runsRemaining,
            String termsVersion) {
        send(email, MailTemplates.codeMessage(code, expiresAt, runsRemaining, termsVersion, frontendUrl));
    }

    @Override
    public void sendQuotaReached(String email, int runQuota) {
        send(email, MailTemplates.quotaMessage(runQuota));
    }

    private void send(String email, MailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(email);
        mail.setSubject(message.subject());
        mail.setText(message.body());
        try {
            mailSender.send(mail);
        } catch (MailException failure) {
            LOG.error("could not send '{}' to {}: {}", message.subject(), EmailAddresses.mask(email),
                    failure.getClass().getSimpleName());
        }
    }
}
