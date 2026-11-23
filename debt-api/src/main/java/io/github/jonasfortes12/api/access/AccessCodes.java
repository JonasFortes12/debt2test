package io.github.jonasfortes12.api.access;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.random.RandomGenerator;

/**
 * Access-code format: {@code D2T-} plus 7 groups of 4 Crockford base32 characters (140 random
 * bits), so guessing is not a realistic attack and no lockout is needed. Only the SHA-256 of the
 * normalized code is ever stored.
 */
public final class AccessCodes {

    private static final String PREFIX = "D2T";
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int GROUPS = 7;
    private static final int GROUP_LENGTH = 4;

    private AccessCodes() {
    }

    public static String generate(RandomGenerator random) {
        StringBuilder code = new StringBuilder(PREFIX);
        for (int group = 0; group < GROUPS; group++) {
            code.append('-');
            for (int i = 0; i < GROUP_LENGTH; i++) {
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
        }
        return code.toString();
    }

    /** Accepts a code as pasted: surrounding whitespace and lowercase are tolerated. */
    public static String normalize(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    public static String hash(String normalizedCode) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalizedCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", impossible);
        }
    }
}
