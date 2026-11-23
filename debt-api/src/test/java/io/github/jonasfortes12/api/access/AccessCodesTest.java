package io.github.jonasfortes12.api.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

class AccessCodesTest {

    @Test
    void generatedCodeMatchesFormat() {
        String code = AccessCodes.generate(new SecureRandom());
        assertTrue(code.matches("^D2T(-[0-9A-HJKMNP-TV-Z]{4}){7}$"), code);
    }

    @Test
    void normalizeTrimsAndUppercases() {
        assertEquals("D2T-ABCD", AccessCodes.normalize("  d2t-abcd  "));
    }

    @Test
    void hashIsLowercaseSha256Hex() {
        String hash = AccessCodes.hash("D2T-ABCD");
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", AccessCodes.hash(""));
    }

    @Test
    void twoGeneratedCodesDiffer() {
        SecureRandom random = new SecureRandom();
        assertNotEquals(AccessCodes.generate(random), AccessCodes.generate(random));
    }
}
