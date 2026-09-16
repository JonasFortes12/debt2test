package io.github.jonasfortes12.core.port;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.jonasfortes12.core.model.ClassificationOptions;
import io.github.jonasfortes12.core.model.ContextRequest;
import io.github.jonasfortes12.core.model.ExtractionOptions;
import io.github.jonasfortes12.core.model.PipelineRequest;
import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.model.RepositoryRequest;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.model.TestGenerationOptions;

class PipelineRunStoreTest {

    private static PipelineRequest request() {
        return new PipelineRequest(
                "run-1",
                new RepositoryRequest("https://example.com/repo.git", "main"),
                new ExtractionOptions("run-1"),
                new ClassificationOptions(true),
                new ContextRequest(true),
                new TestGenerationOptions("junit5", "v1"),
                new ReportOptions(Path.of("output")));
    }

    @Test
    void noOpStoreAcceptsEveryCallbackWithoutThrowing() {
        PipelineRunStore store = PipelineRunStore.NO_OP;
        PipelineResult result = new PipelineResult(
                "run-1", RunStatus.COMPLETED, null, List.of(), List.of(), null);

        assertDoesNotThrow(() -> {
            store.runStarted("exec-1", request());
            store.runIdResolved("exec-1", "run-1");
            store.workspaceReady("exec-1",
                    new RepositoryWorkspace(Path.of("/tmp/ws"), "https://example.com/repo.git", "abc123"));
            store.candidatesExtracted("exec-1", List.of());
            store.candidatesClassified("exec-1", List.of());
            store.contextEnriched("exec-1", List.of());
            store.testsGenerated("exec-1", List.of());
            store.runFinished("exec-1", result);
        });
    }

    @Test
    void partialImplementationOverridesOnlyWhatItNeeds() {
        StringBuilder observed = new StringBuilder();
        PipelineRunStore store = new PipelineRunStore() {
            @Override
            public void runStarted(String executionId, PipelineRequest request) {
                observed.append("started:").append(executionId);
            }
        };

        store.runStarted("exec-2", request());
        store.runFinished("exec-2", new PipelineResult(
                "run-1", RunStatus.COMPLETED, null, List.of(), List.of(), null));

        assertEquals("started:exec-2", observed.toString());
    }
}
