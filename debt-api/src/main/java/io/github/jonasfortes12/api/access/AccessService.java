package io.github.jonasfortes12.api.access;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import io.github.jonasfortes12.api.access.mail.AccessCodeSender;
import io.github.jonasfortes12.persistence.access.AccessCode;
import io.github.jonasfortes12.persistence.access.AccessCodeStore;
import io.github.jonasfortes12.persistence.access.NewAccessCode;
import io.github.jonasfortes12.persistence.access.Participant;

/**
 * Access-code policy: who gets a code, what a code currently grants, and spending a code on exactly
 * one run. Storage is delegated to {@link AccessCodeStore}; every rule lives here.
 *
 * <p>Logs never contain a code, its hash, or an unmasked email.
 */
@Service
public class AccessService {

    private static final Logger LOG = LoggerFactory.getLogger(AccessService.class);

    private final AccessCodeStore store;
    private final AccessCodeSender sender;
    private final AccessProperties properties;
    private final Clock clock;
    private final TransactionOperations transactions;
    private final IpRateLimiter ipRateLimiter;
    private final SecureRandom random = new SecureRandom();

    public AccessService(AccessCodeStore store, AccessCodeSender sender, AccessProperties properties, Clock clock,
            TransactionOperations transactions) {
        this.store = store;
        this.sender = sender;
        this.properties = properties;
        this.clock = clock;
        this.transactions = transactions;
        this.ipRateLimiter = new IpRateLimiter(clock, properties.ipLimitPerHour());
    }

    /**
     * Uses Spring Boot's transaction template; without a transaction manager (only in the database-free
     * tests, since the API does not start without a database) each store call runs on its own.
     */
    @Autowired
    public AccessService(AccessCodeStore store, AccessCodeSender sender, AccessProperties properties, Clock clock,
            ObjectProvider<TransactionOperations> transactions) {
        this(store, sender, properties, clock, transactions.getIfAvailable(TransactionOperations::withoutTransaction));
    }

    /**
     * Records the participant's consent and emails a new code, or an explanation when the run quota
     * is used up. Inside the cooldown nothing is sent, and the caller cannot tell the difference: the
     * HTTP answer is the same in every case, so it never reveals whether an email is known.
     *
     * @throws TermsNotCurrentException when {@code acceptedTermsVersion} is not the current version
     * @throws TooManyRequestsException when {@code ip} is over its hourly limit
     */
    public void requestCode(String name, String email, String acceptedTermsVersion, String ip) {
        requireCurrentTerms(acceptedTermsVersion);
        if (!ipRateLimiter.tryAcquire(ip)) {
            throw new TooManyRequestsException();
        }
        Runnable mail = transactions.execute(status -> decideMail(name.trim(), EmailAddresses.normalize(email), ip));
        mail.run();
    }

    /**
     * Runs in one transaction that holds the participant's row lock, so concurrent requests for one
     * email are decided one after another. Returns the email to send once the transaction has
     * committed, so no SMTP call happens while the lock is held.
     */
    private Runnable decideMail(String name, String normalizedEmail, String ip) {
        Instant now = clock.instant();
        Participant participant = store.upsertParticipant(normalizedEmail, name, now);
        if (isInCooldown(participant, now)) {
            LOG.info("access code request for {} ignored: within cooldown", EmailAddresses.mask(normalizedEmail));
            return () -> { };
        }
        store.markMailed(participant.id(), now);
        int quota = runQuotaOf(participant);
        long runsStarted = store.countUsedCodes(participant.id());
        if (runsStarted >= quota) {
            LOG.info("access code request for {} refused: run quota {} reached",
                    EmailAddresses.mask(normalizedEmail), quota);
            return () -> sender.sendQuotaReached(normalizedEmail, quota);
        }
        return issueCode(participant, ip, now, (int) (quota - runsStarted));
    }

    /** @return the code's current grant, or empty for a missing or unknown code */
    public Optional<AuthenticatedCode> authenticate(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return Optional.empty();
        }
        return store.findByHash(AccessCodes.hash(AccessCodes.normalize(rawCode))).map(this::toAuthenticated);
    }

    /**
     * Spends the code on the run that {@code submitRun} creates. The code is marked used before the
     * run is submitted, atomically, so two parallel requests cannot both start a run; if submitting
     * fails the code is released again, since no run was created. If the run was created but cannot
     * be linked to the code, nobody could ever read or cancel it: it is handed to {@code abandonRun}
     * (which cancels it) and the code is released.
     *
     * @return the new run's execution id
     * @throws AccessCodeUsedException when the code was already used, is no longer valid, or its
     *                                 participant has reached the run quota
     */
    public String startRun(String rawCode, Supplier<String> submitRun, Consumer<String> abandonRun) {
        String codeHash = AccessCodes.hash(AccessCodes.normalize(rawCode));
        Instant now = clock.instant();
        if (!store.markUsed(codeHash, now, now.plus(properties.resultsTtl()), properties.runQuota())) {
            throw new AccessCodeUsedException();
        }
        String executionId = submitOrRelease(codeHash, submitRun);
        attachOrAbandon(codeHash, executionId, abandonRun);
        return executionId;
    }

    private void requireCurrentTerms(String acceptedTermsVersion) {
        if (!properties.termsVersion().equals(acceptedTermsVersion)) {
            throw new TermsNotCurrentException(acceptedTermsVersion, properties.termsVersion());
        }
    }

    private boolean isInCooldown(Participant participant, Instant now) {
        return participant.lastMailedAt() != null
                && now.isBefore(participant.lastMailedAt().plus(properties.requestCooldown()));
    }

    private int runQuotaOf(Participant participant) {
        return participant.runQuotaOverride() != null ? participant.runQuotaOverride() : properties.runQuota();
    }

    private Runnable issueCode(Participant participant, String ip, Instant now, int runsRemaining) {
        String code = AccessCodes.generate(random);
        Instant expiresAt = now.plus(properties.codeTtl());
        store.issueCode(participant.id(), new NewAccessCode(
                AccessCodes.hash(code), properties.termsVersion(), now, ip, now, expiresAt), now);
        return () -> sender.sendCode(participant.email(), code, expiresAt, runsRemaining,
                properties.termsVersion());
    }

    private String submitOrRelease(String codeHash, Supplier<String> submitRun) {
        try {
            return submitRun.get();
        } catch (RuntimeException failure) {
            releaseQuietly(codeHash, failure);
            throw failure;
        }
    }

    private void attachOrAbandon(String codeHash, String executionId, Consumer<String> abandonRun) {
        try {
            store.attachRun(codeHash, UUID.fromString(executionId));
        } catch (RuntimeException failure) {
            LOG.error("run {} could not be linked to its access code; cancelling it", executionId);
            abandonRun.accept(executionId);
            releaseQuietly(codeHash, failure);
            throw failure;
        }
    }

    /** Best effort: a failed release must not hide the failure that caused it. */
    private void releaseQuietly(String codeHash, RuntimeException cause) {
        try {
            store.releaseUse(codeHash);
        } catch (RuntimeException releaseFailure) {
            cause.addSuppressed(releaseFailure);
        }
    }

    private AuthenticatedCode toAuthenticated(AccessCode code) {
        Instant now = clock.instant();
        if (code.usedAt() != null) {
            AccessCodeState state = now.isBefore(code.resultsAccessUntil()) ? AccessCodeState.USED : AccessCodeState.EXPIRED;
            return new AuthenticatedCode(code.codeHash(), state, code.resultsAccessUntil(), code.executionId());
        }
        boolean valid = code.revokedAt() == null && now.isBefore(code.expiresAt());
        return new AuthenticatedCode(code.codeHash(), valid ? AccessCodeState.UNUSED : AccessCodeState.EXPIRED,
                code.expiresAt(), null);
    }
}
