package io.github.jonasfortes12.api.access;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Access-code policy, bound from {@code debt.access.*} (env {@code DEBT_ACCESS_*}).
 *
 * @param runQuota        runs a participant may start, unless an operator set an override
 * @param codeTtl         how long an unused code stays valid
 * @param resultsTtl      how long a used code keeps access to its run
 * @param requestCooldown minimum time between two issued codes for one email
 * @param ipLimitPerHour  code requests accepted per client IP per hour
 * @param termsVersion    the terms version a request must accept
 * @param frontendUrl     optional; when set, emails link to {@code <frontendUrl>/?code=<code>}
 */
@ConfigurationProperties("debt.access")
public record AccessProperties(
        int runQuota,
        Duration codeTtl,
        Duration resultsTtl,
        Duration requestCooldown,
        int ipLimitPerHour,
        String termsVersion,
        String frontendUrl) {
}
