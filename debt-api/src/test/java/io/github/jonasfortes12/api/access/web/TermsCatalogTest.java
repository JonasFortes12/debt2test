package io.github.jonasfortes12.api.access.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TermsCatalogTest {

    @Test
    void loadsTheConfiguredVersionFromTheClasspath() {
        TermsCatalog catalog = TermsCatalog.load("v1");

        assertEquals("v1", catalog.version());
        assertTrue(catalog.text().contains("LLM"), "v1 discloses that code snippets reach the LLM provider");
    }

    @Test
    void missingTermsFileFailsFast() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> TermsCatalog.load("v999"));
        assertEquals("terms file terms/terms-v999.md is missing for DEBT_ACCESS_TERMS_VERSION=v999",
                failure.getMessage());
    }
}
