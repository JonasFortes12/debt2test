package io.github.jonasfortes12.api.access;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.github.jonasfortes12.api.access.mail.AccessCodeSender;

final class RecordingAccessCodeSender implements AccessCodeSender {

    record SentCode(String email, String code, Instant expiresAt, int runsRemaining,
            String termsVersion) {
    }

    record SentQuota(String email, int runQuota) {
    }

    final List<SentCode> codes = new ArrayList<>();
    final List<SentQuota> quotas = new ArrayList<>();

    @Override
    public void sendCode(String email, String code, Instant expiresAt, int runsRemaining,
            String termsVersion) {
        codes.add(new SentCode(email, code, expiresAt, runsRemaining, termsVersion));
    }

    @Override
    public void sendQuotaReached(String email, int runQuota) {
        quotas.add(new SentQuota(email, runQuota));
    }

    String lastCode() {
        return codes.getLast().code();
    }
}
