package io.github.jonasfortes12.context.extraction;

import io.github.jonasfortes12.core.model.ExternalReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IssueReferenceExtractorTest {

    private final IssueReferenceExtractor extractor = new IssueReferenceExtractor();

    @Test
    void extractsJiraKey() {
        assertEquals(
                List.of(new ExternalReference("DEBT-42", "comment")),
                extractor.extract("TODO: fix DEBT-42"));
    }

    @Test
    void extractsTrelloUrlAndStripsTrailingPunctuation() {
        assertEquals(
                List.of(new ExternalReference(
                        "https://trello.com/c/AbC123/card-name", "comment")),
                extractor.extract("See <[HTTPS://WWW.TRELLO.COM/C/AbC123/card-name]>."));
    }

    @Test
    void preservesFirstAppearanceAcrossReferenceTypes() {
        assertEquals(
                List.of(
                        new ExternalReference("OPS-12", "comment"),
                        new ExternalReference("https://trello.com/c/Card123", "comment"),
                        new ExternalReference("BUILD-7", "comment")),
                extractor.extract(
                        "OPS-12 blocks https://trello.com/c/Card123, then BUILD-7 completes it"));
    }

    @Test
    void removesDuplicateReferencesWithoutChangingOrder() {
        assertEquals(
                List.of(
                        new ExternalReference("OPS-12", "comment"),
                        new ExternalReference("https://trello.com/c/Card123/card", "comment")),
                extractor.extract(
                        "OPS-12 and https://www.trello.com/c/Card123/card; OPS-12 and "
                                + "HTTPS://TRELLO.COM/c/Card123/card"));
    }

    @Test
    void retainsUppercaseJiraStylePrefixesForProviderFiltering() {
        assertEquals(
                List.of(
                        new ExternalReference("HTTP-404", "comment"),
                        new ExternalReference("ERROR-500", "comment"),
                        new ExternalReference("RELEASE-2", "comment"),
                        new ExternalReference("CVE-2024", "comment"),
                        new ExternalReference("TODO-1", "comment"),
                        new ExternalReference("FIXME-2", "comment"),
                        new ExternalReference("VERSION-1", "comment"),
                        new ExternalReference("VALID-123", "comment")),
                extractor.extract("ordinary abc-123, release-2, v1.2.3, "
                        + "HTTP-404 ERROR-500 RELEASE-2 CVE-2024 TODO-1 FIXME-2 VERSION-1 VALID-123"));
    }

    @Test
    void preservesCaseSensitiveTrelloCardPathWhileCanonicalizingSchemeAndHost() {
        assertEquals(
                List.of(new ExternalReference(
                        "http://trello.com/c/AbC123/Card-Name", "comment")),
                extractor.extract("HTTP://TreLLo.CoM/c/AbC123/Card-Name"));
    }

    @Test
    void doesNotExtractTrelloUrlEmbeddedInWord() {
        assertEquals(
                List.of(),
                extractor.extract("nothttps://trello.com/c/Card123"));
    }

    @Test
    void acceptsCustomRulesAndPreservesFirstAppearanceOrdering() {
        String githubReference = "github:acme/project#42";
        ReferenceRule githubRule = comment -> List.of(
                new ReferenceMatch(githubReference, comment.indexOf(githubReference)));
        IssueReferenceExtractor extendedExtractor = new IssueReferenceExtractor(List.of(githubRule));

        assertEquals(
                List.of(
                        new ExternalReference("github:acme/project#42", "comment"),
                        new ExternalReference("OPS-12", "comment"),
                        new ExternalReference("https://trello.com/c/Card123", "comment")),
                extendedExtractor.extract(
                        "github:acme/project#42 then OPS-12 and https://trello.com/c/Card123"));
    }

    @Test
    void leavesNonTrelloHttpReferencesUnchangedApartFromTrailingPunctuation() {
        ReferenceRule githubRule = comment -> List.of(
                new ReferenceMatch("https://github.com/acme/project/issues/42,", 0),
                new ReferenceMatch("https://x.", 1));
        IssueReferenceExtractor extendedExtractor = new IssueReferenceExtractor(List.of(githubRule));

        assertEquals(
                List.of(
                        new ExternalReference("https://github.com/acme/project/issues/42", "comment"),
                        new ExternalReference("https://x", "comment")),
                extendedExtractor.extract("custom references"));
    }
}
