package io.github.jonasfortes12.extractor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.jonasfortes12.core.model.ExtractionOptions;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.port.SatdExtractor;
import io.github.jonasfortes12.core.result.ExtractionResult;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public final class AstCommentExtractor implements SatdExtractor {

    @Override
    public ExtractionResult extract(RepositoryWorkspace workspace, ExtractionOptions options) {
        List<SatdCandidate> candidates = new ArrayList<>();
        List<PipelineError> errors = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(workspace.rootDirectory())) {
            paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .forEach(path -> parseJavaFile(path, workspace, options, candidates, errors));
        } catch (IOException | UncheckedIOException exception) {
            errors.add(new PipelineError(
                    "extraction",
                    "JAVA_TRAVERSAL_FAILED",
                    "Java source traversal failed",
                    null,
                    true));
        }

        return new ExtractionResult(candidates, errors);
    }

    private void parseJavaFile(
            Path path,
            RepositoryWorkspace workspace,
            ExtractionOptions options,
            List<SatdCandidate> candidates,
            List<PipelineError> errors) {
        String relativePath = workspace.rootDirectory()
                .relativize(path)
                .toString()
                .replace(File.separatorChar, '/');

        CompilationUnit compilationUnit;
        try {
            compilationUnit = StaticJavaParser.parse(path);
        } catch (IOException | UncheckedIOException | ParseProblemException exception) {
            errors.add(new PipelineError(
                    "extraction",
                    "JAVA_PARSE_FAILED",
                    "Java source file could not be parsed: " + relativePath,
                    null,
                    true));
            return;
        }

        compilationUnit.findAll(MethodDeclaration.class).forEach(method -> {
            if (method.getComment().isEmpty()) {
                return;
            }

            int line = method.getBegin().map(position -> position.line).orElse(0);
            int column = method.getBegin().map(position -> position.column).orElse(0);
            String methodName = method.getNameAsString();
            String methodSignature = method.getSignature().asString();
            String declaringTypeName = declaringTypeName(method);
            String candidateId = options.runId() + ":" + relativePath + ":" + line + ":" + column + ":" + methodName
                    + ":" + declaringTypeName + ":" + methodSignature;
            try {
                candidates.add(new SatdCandidate(
                        candidateId,
                        relativePath,
                        methodName,
                        line,
                        method.getComment().orElseThrow().getContent().trim(),
                        method.toString(),
                        new SourceProvenance(
                                RepositoryUrlSanitizer.sanitize(workspace.repositoryUrl()),
                                workspace.revision(),
                                relativePath)));
            } catch (IllegalArgumentException exception) {
                errors.add(new PipelineError(
                        "extraction",
                        "CANDIDATE_INVALID",
                        "Java candidate is invalid: " + relativePath + ":" + line + ":" + methodName,
                        candidateId,
                        true));
            }
        });
    }

    private String declaringTypeName(MethodDeclaration method) {
        List<String> typeNames = new ArrayList<>();
        Node current = method;
        while (current != null) {
            if (current instanceof TypeDeclaration<?> type) {
                typeNames.add(type.getNameAsString());
            }
            current = current.getParentNode().orElse(null);
        }
        if (typeNames.isEmpty()) {
            return "<unknown>";
        }
        Collections.reverse(typeNames);
        return String.join(".", typeNames);
    }
}
