package io.github.jonasfortes12.extractor;

import io.github.jonasfortes12.core.model.ExtractionOptions;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.result.ExtractionResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AstCommentExtractorTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void extractsCommentedMethodsWithStableRepositoryRelativeCandidates() throws IOException, URISyntaxException {
        Path root = copyFixtureProject(false);
        RepositoryWorkspace workspace = new RepositoryWorkspace(root, "https://example.test/repository", "main");
        AstCommentExtractor extractor = new AstCommentExtractor();

        ExtractionResult first = extractor.extract(workspace, new ExtractionOptions("run-1"));
        ExtractionResult second = extractor.extract(workspace, new ExtractionOptions("run-1"));

        assertEquals(3, first.candidates().size());
        assertEquals(first.candidates(), second.candidates());
        assertTrue(first.errors().isEmpty());
        assertTrue(first.candidates().stream()
                .allMatch(candidate -> !Path.of(candidate.filePath()).isAbsolute()));
        assertTrue(first.candidates().stream()
                .allMatch(candidate -> candidate.filePath().equals("src/ProjectFixture.java")));
        assertTrue(first.candidates().stream()
                .allMatch(candidate -> candidate.filePath().indexOf('\\') < 0));
        assertTrue(first.candidates().stream()
                .anyMatch(candidate -> candidate.methodName().equals("documented")));
        assertEquals(2, first.candidates().stream()
                .filter(candidate -> candidate.methodName().equals("overloaded"))
                .count());
        assertEquals(Set.of(
                        "run-1:src/ProjectFixture.java:6:5:documented:ProjectFixture:documented()",
                        "run-1:src/ProjectFixture.java:15:50:overloaded:ProjectFixture.Nested:overloaded(int)",
                        "run-1:src/ProjectFixture.java:15:120:overloaded:ProjectFixture.Nested:overloaded(String)"),
                first.candidates().stream().map(SatdCandidate::candidateId).collect(Collectors.toSet()));

        for (SatdCandidate candidate : first.candidates()) {
            int sourceColumn = sourceColumn(candidate);
            assertTrue(sourceColumn > 0);
            assertTrue(candidate.candidateId().startsWith(
                    "run-1:" + candidate.filePath() + ":" + candidate.lineNumber() + ":"
                            + sourceColumn + ":"));
            assertTrue(candidate.candidateId().contains(":" + candidate.methodName() + ":"));
            assertEquals("https://example.test/repository", candidate.sourceProvenance().repositoryUrl());
            assertEquals("main", candidate.sourceProvenance().revision());
            assertEquals(candidate.filePath(), candidate.sourceProvenance().relativeFilePath());
            assertFalse(candidate.comment().isBlank());
            assertFalse(candidate.methodSourceCode().isBlank());
        }
    }

    @Test
    void malformedJavaFileProducesRecoverableErrorAndKeepsValidCandidates() throws IOException, URISyntaxException {
        Path root = copyFixtureProject(true);
        ExtractionResult result = new AstCommentExtractor().extract(
                new RepositoryWorkspace(root, "repo", "main"),
                new ExtractionOptions("run-1"));

        assertEquals(3, result.candidates().size());
        assertTrue(result.errors().stream().anyMatch(error ->
                error.code().equals("JAVA_PARSE_FAILED")
                        && error.recoverable()
                        && error.stage().equals("extraction")
                        && error.candidateId() == null));
    }

    @Test
    void ignoresJavaFilesThatAreSymbolicLinksOutsideTheWorkspace() throws IOException, URISyntaxException {
        Path root = copyFixtureProject(false);
        Path externalFile = temporaryDirectory.resolve("outside/Outside.java");
        Files.createDirectories(externalFile.getParent());
        Files.writeString(externalFile, """
                package outside;
                public class Outside {
                    // TODO: must not be extracted.
                    public void outsideMethod() {}
                }
                """);

        try {
            Files.createSymbolicLink(root.resolve("src/Outside.java"), externalFile);
        } catch (UnsupportedOperationException | SecurityException exception) {
            Assumptions.abort("symbolic links are not supported");
        } catch (IOException exception) {
            Assumptions.abort("symbolic links cannot be created: " + exception.getMessage());
        }

        ExtractionResult result = new AstCommentExtractor().extract(
                new RepositoryWorkspace(root, "repo", "main"),
                new ExtractionOptions("run-1"));

        assertEquals(3, result.candidates().size());
        assertTrue(result.candidates().stream()
                .noneMatch(candidate -> candidate.filePath().contains("Outside.java")));
    }

    @Test
    void invalidCommentProducesRecoverableErrorAndValidCandidatesContinue() throws IOException, URISyntaxException {
        Path root = copyFixtureProject(false);
        Path sourceDirectory = root.resolve("src");
        Files.writeString(sourceDirectory.resolve("BlankComment.java"), """
                package fixture;
                public class BlankComment {
                    /** */
                    public void blankComment() {}
                }
                """);

        ExtractionResult result = new AstCommentExtractor().extract(
                new RepositoryWorkspace(root, "repo", "main"),
                new ExtractionOptions("run-1"));

        assertEquals(3, result.candidates().size());
        assertTrue(result.errors().stream().anyMatch(error ->
                error.code().equals("CANDIDATE_INVALID")
                        && error.recoverable()
                        && error.candidateId() != null
                        && error.message().contains("src/BlankComment.java")));
    }

    @Test
    void stackedLineCommentsAreMergedIntoASingleCandidateComment() throws IOException, URISyntaxException {
        Path root = copyFixtureProject(false);
        Path sourceDirectory = root.resolve("src");
        Files.writeString(sourceDirectory.resolve("StackedComment.java"), """
                package fixture;
                public class StackedComment {
                    // DEBT2TEST-3: concurrent access isn't exercised here because the pool
                    // lacks synchronization; a true concurrency test would be flaky by design.
                    public void stacked() {}
                }
                """);

        ExtractionResult result = new AstCommentExtractor().extract(
                new RepositoryWorkspace(root, "repo", "main"),
                new ExtractionOptions("run-1"));

        SatdCandidate candidate = result.candidates().stream()
                .filter(item -> item.methodName().equals("stacked"))
                .findFirst()
                .orElseThrow();
        assertEquals(
                "DEBT2TEST-3: concurrent access isn't exercised here because the pool\n"
                        + "lacks synchronization; a true concurrency test would be flaky by design.",
                candidate.comment());
    }

    @Test
    void stackedLineCommentsDoNotAbsorbAPrecedingUnrelatedTrailingComment() throws IOException, URISyntaxException {
        Path root = copyFixtureProject(false);
        Path sourceDirectory = root.resolve("src");
        Files.writeString(sourceDirectory.resolve("TrailingComment.java"), """
                package fixture;
                public class TrailingComment {
                    int x = 5; // trailing note about x, unrelated to the method below
                    // TODO: only this line belongs to the method comment.
                    public void afterTrailingComment() {}
                }
                """);

        ExtractionResult result = new AstCommentExtractor().extract(
                new RepositoryWorkspace(root, "repo", "main"),
                new ExtractionOptions("run-1"));

        SatdCandidate candidate = result.candidates().stream()
                .filter(item -> item.methodName().equals("afterTrailingComment"))
                .findFirst()
                .orElseThrow();
        assertEquals("TODO: only this line belongs to the method comment.", candidate.comment());
    }

    @Test
    void candidateIdsIncludeDeclaringTypeForSameLineMethods() throws IOException, URISyntaxException {
        Path root = copyFixtureProject(false);
        copyFixture("SameLineTypes.java", root.resolve("src/SameLineTypes.java"));

        List<SatdCandidate> sameMethods = new AstCommentExtractor().extract(
                        new RepositoryWorkspace(root, "repo", "main"),
                        new ExtractionOptions("run-1"))
                .candidates().stream()
                .filter(candidate -> candidate.methodName().equals("same"))
                .toList();

        assertEquals(2, sameMethods.size());
        assertEquals(sameMethods.get(0).lineNumber(), sameMethods.get(1).lineNumber());
        assertNotEquals(sameMethods.get(0).candidateId(), sameMethods.get(1).candidateId());
        assertTrue(sameMethods.stream().anyMatch(candidate -> candidate.candidateId().contains(":SameLineTypes.First:")));
        assertTrue(sameMethods.stream().anyMatch(candidate -> candidate.candidateId().contains(":SameLineTypes.Second:")));
    }

    @Test
    void candidateIdsIncludeSourceColumnForSameLineAnonymousMethods() throws IOException, URISyntaxException {
        Path root = copyFixtureProject(false);
        copyFixture("AnonymousSameLineTypes.java", root.resolve("src/AnonymousSameLineTypes.java"));

        List<SatdCandidate> sameMethods = new AstCommentExtractor().extract(
                        new RepositoryWorkspace(root, "repo", "main"),
                        new ExtractionOptions("run-1"))
                .candidates().stream()
                .filter(candidate -> candidate.methodName().equals("same"))
                .toList();

        assertEquals(2, sameMethods.size());
        assertEquals(sameMethods.get(0).lineNumber(), sameMethods.get(1).lineNumber());
        assertNotEquals(sameMethods.get(0).candidateId(), sameMethods.get(1).candidateId());
        assertNotEquals(sourceColumn(sameMethods.get(0)), sourceColumn(sameMethods.get(1)));
    }

    @Test
    void directExtractorSanitizesRepositoryUrlBeforeCreatingProvenance() throws IOException, URISyntaxException {
        Path root = copyFixtureProject(false);
        List<String> rawUrls = List.of(
                "https://user:pass@host/repo.git?token=secret#fragment",
                "https:repo.git?token=secret#fragment",
                "https:user:pass@host/repo.git?token=secret#fragment",
                "https:user:pa@ss@host/repo.git?token=x#fragment",
                "https:repo.git?token=QUERY_SECRET@query_leak#fragment",
                "https:repo.git?token=fragment_secret#fragment@fragment_leak");

        for (String rawUrl : rawUrls) {
            ExtractionResult result = new AstCommentExtractor().extract(
                    new RepositoryWorkspace(root, rawUrl, "main"),
                    new ExtractionOptions("run-1"));

            assertEquals(3, result.candidates().size());
            for (SatdCandidate candidate : result.candidates()) {
                String provenanceUrl = candidate.sourceProvenance().repositoryUrl();
                assertFalse(provenanceUrl.contains("user"));
                assertFalse(provenanceUrl.contains("pass"));
                assertFalse(provenanceUrl.contains("token"));
                assertFalse(provenanceUrl.contains("fragment"));
                assertFalse(provenanceUrl.contains("pa@ss"));
                assertFalse(provenanceUrl.contains("ss@host"));
                assertFalse(provenanceUrl.contains("@"));
                assertFalse(provenanceUrl.contains("QUERY_SECRET"));
                assertFalse(provenanceUrl.contains("query_leak"));
                assertFalse(provenanceUrl.contains("fragment_secret"));
                assertFalse(provenanceUrl.contains("fragment_leak"));
            }
        }
    }

    private Path copyFixtureProject(boolean includeMalformed) throws IOException, URISyntaxException {
        Path root = temporaryDirectory.resolve("project");
        Path sourceDirectory = root.resolve("src");
        Files.createDirectories(sourceDirectory);
        copyFixture("ProjectFixture.java", sourceDirectory.resolve("ProjectFixture.java"));
        Files.writeString(root.resolve("notes.txt"), "not a Java source file");
        if (includeMalformed) {
            copyFixture("Malformed.java", sourceDirectory.resolve("Malformed.java"));
        }
        return root;
    }

    private void copyFixture(String name, Path destination) throws IOException, URISyntaxException {
        Path source = Path.of(Objects.requireNonNull(
                getClass().getResource("/fixtures/" + name)).toURI());
        Files.copy(source, destination);
    }

    private int sourceColumn(SatdCandidate candidate) {
        return Integer.parseInt(candidate.candidateId().split(":")[3]);
    }
}
