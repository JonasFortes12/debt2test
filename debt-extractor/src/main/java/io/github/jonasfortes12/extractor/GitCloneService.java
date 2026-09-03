package io.github.jonasfortes12.extractor;

import io.github.jonasfortes12.core.error.PipelineException;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.RepositoryRequest;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.port.RepositoryWorkspaceProvider;
import io.github.jonasfortes12.core.util.UrlSanitizer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GitCloneService implements RepositoryWorkspaceProvider {

    private static final String TEMP_DIRECTORY_PREFIX = "debt2test-repo-";

    private final Path temporaryDirectory;
    private final Set<Path> managedWorkspaces = ConcurrentHashMap.newKeySet();
    private final CloneOperation cloneOperation;

    public GitCloneService() {
        this(Path.of(System.getProperty("java.io.tmpdir")), GitCloneService::cloneWithJGit);
    }

    GitCloneService(Path temporaryDirectory) {
        this(temporaryDirectory, GitCloneService::cloneWithJGit);
    }

    GitCloneService(Path temporaryDirectory, CloneOperation cloneOperation) {
        this.temporaryDirectory = Objects.requireNonNull(temporaryDirectory, "temporaryDirectory")
                .toAbsolutePath()
                .normalize();
        this.cloneOperation = Objects.requireNonNull(cloneOperation, "cloneOperation");
    }

    @Override
    public RepositoryWorkspace prepare(RepositoryRequest request) {
        Path directory = null;
        try {
            directory = Files.createTempDirectory(temporaryDirectory, TEMP_DIRECTORY_PREFIX);
            managedWorkspaces.add(normalize(directory));
            Files.delete(directory);

            String rawRepositoryUrl = request.repositoryUrl();
            String resolvedRevision;
            try (Git git = cloneOperation.clone(rawRepositoryUrl, directory)) {
                if (request.revision() != null && !request.revision().isBlank()) {
                    checkoutRevision(git, request.revision());
                }
                resolvedRevision = resolvedRevision(git, request.revision());
            }

            RepositoryWorkspace workspace = new RepositoryWorkspace(
                    directory,
                    UrlSanitizer.sanitize(rawRepositoryUrl),
                    resolvedRevision);
            managedWorkspaces.add(normalize(directory));
            return workspace;
        } catch (Exception preparationFailure) {
            try {
                if (directory != null) {
                    releaseOwned(directory);
                }
            } catch (IOException | RuntimeException cleanupFailure) {
                cleanupFailure.addSuppressed(preparationFailure);
                throw workspaceException(
                        "WORKSPACE_PREPARATION_FAILED",
                        "repository workspace could not be prepared",
                        cleanupFailure);
            }
            throw workspaceException(
                    "WORKSPACE_PREPARATION_FAILED",
                    "repository workspace could not be prepared",
                    preparationFailure);
        }
    }

    @Override
    public void release(RepositoryWorkspace workspace) {
        Objects.requireNonNull(workspace, "workspace");
        try {
            releaseOwned(workspace.rootDirectory());
        } catch (IOException | RuntimeException cleanupFailure) {
            throw workspaceException(
                    "WORKSPACE_CLEANUP_FAILED",
                    "repository workspace could not be cleaned up",
                    cleanupFailure);
        }
    }

    private void checkoutRevision(Git git, String revision) throws Exception {
        ObjectId commit = resolveRevision(git, revision);
        if (commit == null) {
            throw new IOException("requested revision is unavailable");
        }
        git.checkout()
                .setName(commit.name())
                .setForced(true)
                .call();
    }

    private ObjectId resolveRevision(Git git, String revision) throws IOException {
        String[] revisionCandidates = {
                revision + "^{commit}",
                "refs/heads/" + revision + "^{commit}",
                "refs/tags/" + revision + "^{commit}",
                "refs/remotes/origin/" + revision + "^{commit}",
                "origin/" + revision + "^{commit}"
        };
        for (String candidate : revisionCandidates) {
            ObjectId resolved = git.getRepository().resolve(candidate);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private String resolvedRevision(Git git, String requestedRevision) throws IOException {
        ObjectId head = git.getRepository().resolve("HEAD");
        return head == null ? requestedRevision : head.name();
    }

    private void deleteRecursively(Path directory) throws IOException {
        if (directory == null) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        }
    }

    private PipelineException workspaceException(String code, String message, Throwable cause) {
        return new PipelineException(new PipelineError(
                "repository",
                code,
                message,
                null,
                false), cause);
    }

    private static Git cloneWithJGit(String repositoryUrl, Path directory) throws Exception {
        return Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(directory.toFile())
                .call();
    }

    private void releaseOwned(Path directory) throws IOException {
        Path normalizedDirectory = normalize(directory);
        if (!isDirectChild(normalizedDirectory) || !managedWorkspaces.remove(normalizedDirectory)) {
            return;
        }
        try {
            deleteRecursively(normalizedDirectory);
        } catch (IOException | RuntimeException cleanupFailure) {
            managedWorkspaces.add(normalizedDirectory);
            throw cleanupFailure;
        }
    }

    private boolean isDirectChild(Path directory) {
        return directory.getParent() != null
                && directory.getParent().equals(temporaryDirectory)
                && directory.getFileName() != null
                && directory.getFileName().toString().startsWith(TEMP_DIRECTORY_PREFIX);
    }

    private Path normalize(Path directory) {
        return directory.toAbsolutePath().normalize();
    }

    @FunctionalInterface
    interface CloneOperation {
        Git clone(String repositoryUrl, Path directory) throws Exception;
    }
}
