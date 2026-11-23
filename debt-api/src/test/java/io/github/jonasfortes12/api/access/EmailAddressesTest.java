package io.github.jonasfortes12.api.access;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmailAddressesTest {

    @Test
    void normalizeTrimsAndLowercases() {
        assertEquals("jonas@gmail.com", EmailAddresses.normalize("  Jonas@Gmail.COM "));
    }

    @Test
    void maskKeepsFirstCharAndDomain() {
        assertEquals("j***@gmail.com", EmailAddresses.mask("jonas@gmail.com"));
    }

    @Test
    void maskOfMalformedAddressRevealsNothing() {
        assertEquals("***", EmailAddresses.mask("not-an-email"));
    }
}
