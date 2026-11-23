package io.github.jonasfortes12.api.access.mail;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development sender, active when no SMTP host is configured: prints each message, code included,
 * to the log instead of emailing it. The only place a code is ever logged.
 */
public class LoggingAccessCodeSender implements AccessCodeSender {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingAccessCodeSender.class);

    private final String frontendUrl;

    public LoggingAccessCodeSender(String frontendUrl) {
        this.frontendUrl = frontendUrl;
        LOG.warn("dev mail sender active: access codes are printed to the log, not emailed");
    }

    @Override
    public void sendCode(String email, String code, Instant expiresAt, int runsRemaining,
            String termsVersion) {
        log(MailTemplates.codeMessage(code, expiresAt, runsRemaining, termsVersion, frontendUrl));
    }

    @Override
    public void sendQuotaReached(String email, int runQuota) {
        log(MailTemplates.quotaMessage(runQuota));
    }

    private static void log(MailMessage message) {
        LOG.warn("[dev mail] {}\n{}", message.subject(), message.body());
    }
}
