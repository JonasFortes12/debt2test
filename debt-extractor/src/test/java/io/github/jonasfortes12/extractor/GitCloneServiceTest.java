package io.github.jonasfortes12.extractor;

import io.github.jonasfortes12.core.error.PipelineException;
import io.github.jonasfortes12.core.model.RepositoryRequest;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.port.RepositoryWorkspaceProvider;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitCloneServiceTest {

    private static final String TEMP_DIRECTORY_PREFIX = "debt2test-repo-";

    @TempDir
    Path temporaryDirectory;

    @Test
    void preparesBranchTagAndCommitRevisionsAndReleasesWorkspaces() throws Exception {
        LocalRepository repository = createLocalRepository();
        GitCloneService service = new GitCloneService(temporaryDirectory);

        assertRevisionContents(service, repository, "feature", "first revision\n");
        assertRevisionContents(service, repository, "v1", "first revision\n");
        assertRevisionContents(service, repository, repository.secondCommit(), "second revision\n");

        RepositoryWorkspace workspace = service.prepare(
                new RepositoryRequest(repository.path().toUri().toString(), repository.secondCommit()));
        try {
            assertTrue(Files.isDirectory(workspace.rootDirectory()));
            assertTrue(Files.exists(workspace.rootDirectory().resolve("README.md")));
            assertEquals(repository.path().toUri().toString(), workspace.repositoryUrl());
            assertEquals(repository.secondCommit(), workspace.revision());
            assertNotEquals(repository.path().toAbsolutePath(), workspace.rootDirectory().toAbsolutePath());
        } finally {
            RepositoryWorkspaceProvider provider = service;
            provider.release(workspace);
        }
        assertFalse(Files.exists(workspace.rootDirectory()));
    }

    @Test
    void recordsTheResolvedCommitForBranchAndDefaultRevisions() throws Exception {
        LocalRepository repository = createLocalRepository();
        GitCloneService service = new GitCloneService(temporaryDirectory);

        RepositoryWorkspace branchWorkspace = service.prepare(
                new RepositoryRequest(repository.path().toUri().toString(), "feature"));
        try {
            assertEquals(repository.firstCommit(), branchWorkspace.revision());
        } finally {
            service.release(branchWorkspace);
        }

        RepositoryWorkspace defaultWorkspace = service.prepare(
                new RepositoryRequest(repository.path().toUri().toString(), null));
        try {
            assertEquals(repository.secondCommit(), defaultWorkspace.revision());
        } finally {
            service.release(defaultWorkspace);
        }
    }

    @Test
    void invalidRepositoryProducesSanitizedNonRecoverableFailure() throws IOException {
        Path cloneParent = temporaryDirectory.resolve("clones");
        Files.createDirectories(cloneParent);
        GitCloneService service = new GitCloneService(cloneParent);
        Set<Path> before = cloneDirectories(cloneParent);
        String invalidRepository = "file:///definitely-missing-repository?token=secret";

        PipelineException exception = assertThrows(PipelineException.class, () ->
                service.prepare(new RepositoryRequest(invalidRepository, "main")));

        assertEquals("WORKSPACE_PREPARATION_FAILED", exception.error().code());
        assertFalse(exception.error().recoverable());
        assertEquals("repository workspace could not be prepared", exception.getMessage());
        assertFalse(exception.getMessage().contains("secret"));
        assertEquals(before, cloneDirectories(cloneParent));
    }

    @Test
    void sanitizesUserInfoQueryAndFragmentBeforeReportMetadata() {
        String rawUrl = "https://user:pass@host/repo.git?token=secret#fragment";
        String sanitizedUrl = RepositoryUrlSanitizer.sanitize(rawUrl);
        RepositoryWorkspace workspace = new RepositoryWorkspace(Path.of("workspace"), sanitizedUrl, null);
        SourceProvenance provenance = new SourceProvenance(sanitizedUrl, null, "src/Example.java");

        assertEquals("https://host/repo.git", sanitizedUrl);
        for (String value : new String[]{sanitizedUrl, workspace.repositoryUrl(), provenance.repositoryUrl(),
                workspace.toString(), provenance.toString()}) {
            assertFalse(value.contains("user"));
            assertFalse(value.contains("pass"));
            assertFalse(value.contains("token"));
            assertFalse(value.contains("fragment"));
        }
    }

    @Test
    void stripsQueryAndFragmentFromOpaqueFallback() {
        String rawUrl = "https:repo.git?token=secret#fragment";

        String sanitizedUrl = RepositoryUrlSanitizer.sanitize(rawUrl);

        assertEquals("https:repo.git", sanitizedUrl);
        assertFalse(sanitizedUrl.contains("token"));
        assertFalse(sanitizedUrl.contains("fragment"));
    }

    @Test
    void stripsCredentialsFromOpaqueFallback() {
        String rawUrl = "https:user:pass@host/repo.git?token=secret#fragment";

        String sanitizedUrl = RepositoryUrlSanitizer.sanitize(rawUrl);

        assertEquals("host/repo.git", sanitizedUrl);
        assertFalse(sanitizedUrl.contains("user"));
        assertFalse(sanitizedUrl.contains("pass"));
        assertFalse(sanitizedUrl.contains("token"));
        assertFalse(sanitizedUrl.contains("fragment"));
    }

    @Test
    void stripsCredentialFragmentsWhenPasswordContainsAt() {
        String rawUrl = "https:user:pa@ss@host/repo.git?token=x#fragment";
        String sanitizedUrl = RepositoryUrlSanitizer.sanitize(rawUrl);
        RepositoryWorkspace workspace = new RepositoryWorkspace(Path.of("workspace"), sanitizedUrl, null);
        SourceProvenance provenance = new SourceProvenance(sanitizedUrl, null, "src/Example.java");

        assertEquals("host/repo.git", sanitizedUrl);
        for (String value : new String[]{sanitizedUrl, workspace.repositoryUrl(), provenance.repositoryUrl(),
                workspace.toString(), provenance.toString()}) {
            assertFalse(value.contains("user"));
            assertFalse(value.contains("pa@ss"));
            assertFalse(value.contains("ss@host"));
            assertFalse(value.contains("token"));
            assertFalse(value.contains("fragment"));
            assertFalse(value.contains("@"));
        }
    }

    @Test
    void stripsAtCharactersFromQueryAndFragmentBeforeCredentialDetection() {
        for (String rawUrl : List.of(
                "https:repo.git?token=QUERY_SECRET@query_leak#fragment",
                "https:repo.git?token=fragment_secret#fragment@fragment_leak")) {
            String sanitizedUrl = RepositoryUrlSanitizer.sanitize(rawUrl);
            RepositoryWorkspace workspace = new RepositoryWorkspace(Path.of("workspace"), sanitizedUrl, null);
            SourceProvenance provenance = new SourceProvenance(sanitizedUrl, null, "src/Example.java");

            assertEquals("https:repo.git", sanitizedUrl);
            for (String value : new String[]{sanitizedUrl, workspace.repositoryUrl(), provenance.repositoryUrl(),
                    workspace.toString(), provenance.toString()}) {
                assertFalse(value.contains("QUERY_SECRET"));
                assertFalse(value.contains("query_leak"));
                assertFalse(value.contains("fragment_secret"));
                assertFalse(value.contains("fragment_leak"));
            }
        }
    }

    @Test
    void mapsCleanupFailuresToSanitizedPipelineExceptions() throws Exception {
        Assumptions.assumeTrue(Files.getFileAttributeView(
                temporaryDirectory, java.nio.file.attribute.PosixFileAttributeView.class) != null);
        Assumptions.assumeFalse("root".equals(System.getProperty("user.name")));

        LocalRepository repository = createLocalRepository();
        GitCloneService service = new GitCloneService(temporaryDirectory);
        RepositoryWorkspace workspace = service.prepare(
                new RepositoryRequest(repository.path().toUri().toString(), repository.secondCommit()));
        Path workspaceRoot = workspace.rootDirectory();
        Set<PosixFilePermission> readOnly = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(workspaceRoot, readOnly);

        try {
            PipelineException exception = assertThrows(PipelineException.class, () ->
                    service.release(workspace));

            assertEquals("WORKSPACE_CLEANUP_FAILED", exception.error().code());
            assertFalse(exception.error().recoverable());
            assertEquals("repository workspace could not be cleaned up", exception.getMessage());
        } finally {
            Files.setPosixFilePermissions(workspaceRoot,
                    EnumSet.allOf(PosixFilePermission.class));
            service.release(workspace);
        }
    }

    @Test
    void releaseDoesNotDeleteUnownedWorkspacePaths() throws IOException {
        Path project = temporaryDirectory.resolve("project");
        Files.createDirectories(project);
        Path marker = project.resolve("keep.txt");
        Files.writeString(marker, "keep");

        GitCloneService service = new GitCloneService(temporaryDirectory);
        service.release(new RepositoryWorkspace(project, "repo", "main"));
        service.release(new RepositoryWorkspace(temporaryDirectory, "repo", "main"));

        assertTrue(Files.isDirectory(project));
        assertTrue(Files.exists(marker));
        assertTrue(Files.isDirectory(temporaryDirectory));
    }

    @Test
    void failedPartialWorkspaceRemainsOwnedAndRetryable() throws Exception {
        Assumptions.assumeTrue(Files.getFileAttributeView(
                temporaryDirectory, java.nio.file.attribute.PosixFileAttributeView.class) != null);
        Assumptions.assumeFalse("root".equals(System.getProperty("user.name")));

        Path cloneParent = temporaryDirectory.resolve("failed-clones");
        Files.createDirectories(cloneParent);
        AtomicReference<Path> failedRoot = new AtomicReference<>();
        Set<PosixFilePermission> readOnly = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_EXECUTE);
        GitCloneService service = new GitCloneService(cloneParent, (url, directory) -> {
            failedRoot.set(directory);
            Path nestedDirectory = directory.resolve("nested");
            Files.createDirectories(nestedDirectory);
            Files.writeString(nestedDirectory.resolve("partial.txt"), "partial clone");
            Files.setPosixFilePermissions(nestedDirectory, readOnly);
            throw new IOException("injected clone failure");
        });

        PipelineException exception = assertThrows(PipelineException.class, () ->
                service.prepare(new RepositoryRequest("file:///invalid", "main")));

        Path partialWorkspace = failedRoot.get();
        assertNotNull(partialWorkspace);
        assertTrue(Files.exists(partialWorkspace));
        assertEquals("WORKSPACE_PREPARATION_FAILED", exception.error().code());
        assertEquals("repository workspace could not be prepared", exception.getMessage());
        try {
            Files.setPosixFilePermissions(partialWorkspace.resolve("nested"),
                    EnumSet.allOf(PosixFilePermission.class));
            Files.setPosixFilePermissions(partialWorkspace,
                    EnumSet.allOf(PosixFilePermission.class));
            service.release(new RepositoryWorkspace(partialWorkspace, "repo", "main"));
            assertFalse(Files.exists(partialWorkspace));
        } finally {
            if (Files.exists(partialWorkspace)) {
                Files.setPosixFilePermissions(partialWorkspace.resolve("nested"),
                        EnumSet.allOf(PosixFilePermission.class));
                Files.setPosixFilePermissions(partialWorkspace,
                        EnumSet.allOf(PosixFilePermission.class));
                service.release(new RepositoryWorkspace(partialWorkspace, "repo", "main"));
            }
        }
    }

    private void assertRevisionContents(
            GitCloneService service,
            LocalRepository repository,
            String revision,
            String expectedContents) throws Exception {
        RepositoryWorkspace workspace = service.prepare(
                new RepositoryRequest(repository.path().toUri().toString(), revision));
        try {
            assertEquals(expectedContents, Files.readString(workspace.rootDirectory().resolve("README.md")));
        } finally {
            service.release(workspace);
        }
    }

    private LocalRepository createLocalRepository() throws Exception {
        Path source = temporaryDirectory.resolve("source-" + System.nanoTime());
        String firstCommit;
        String secondCommit;
        try (Git git = Git.init().setDirectory(source.toFile()).call()) {
            Files.writeString(source.resolve("README.md"), "first revision\n");
            git.add().addFilepattern("README.md").call();
            firstCommit = git.commit().setMessage("first commit").setAuthor("Test User", "test@example.test")
                    .setCommitter("Test User", "test@example.test").call().getName();
            git.branchCreate().setName("feature").setStartPoint(firstCommit).call();
            git.tag().setName("v1")
                    .setObjectId(git.getRepository().parseCommit(ObjectId.fromString(firstCommit))).call();
            Files.writeString(source.resolve("README.md"), "second revision\n");
            git.add().addFilepattern("README.md").call();
            secondCommit = git.commit().setMessage("second commit").setAuthor("Test User", "test@example.test")
                    .setCommitter("Test User", "test@example.test").call().getName();
        }
        return new LocalRepository(source, firstCommit, secondCommit);
    }

    private Set<Path> cloneDirectories(Path parent) throws IOException {
        try (var paths = Files.list(parent)) {
            return paths.filter(path -> path.getFileName().toString().startsWith(TEMP_DIRECTORY_PREFIX))
                    .map(Path::toAbsolutePath)
                    .collect(Collectors.toSet());
        }
    }

    private void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new DeleteFailure(exception);
                }
            });
        } catch (DeleteFailure failure) {
            throw failure.exception;
        }
    }

    private record LocalRepository(Path path, String firstCommit, String secondCommit) {
    }

    private static final class DeleteFailure extends RuntimeException {
        private final IOException exception;

        private DeleteFailure(IOException exception) {
            this.exception = exception;
        }
    }
}
