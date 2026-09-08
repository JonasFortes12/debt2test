package io.github.jonasfortes12.extractor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;
import io.github.jonasfortes12.core.model.ExtractionOptions;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.port.SatdExtractor;
import io.github.jonasfortes12.core.result.ExtractionResult;
import io.github.jonasfortes12.core.util.UrlSanitizer;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        Set<Comment> attachedComments = attachedComments(compilationUnit);
        Map<Integer, LineComment> lineCommentsByLine = lineCommentsByLine(compilationUnit);

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
                        methodComment(method, attachedComments, lineCommentsByLine),
                        method.toString(),
                        new SourceProvenance(
                                UrlSanitizer.sanitize(workspace.repositoryUrl()),
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

    /**
     * Resolves a method's full SATD comment. JavaParser attaches at most one {@link Comment}
     * per node, so when a comment is a stack of contiguous {@code //} lines, only the line
     * closest to the method ends up as {@link MethodDeclaration#getComment()} and the earlier
     * lines are silently unattached. This reassembles the full block by walking upward through
     * unclaimed, line-contiguous {@link LineComment}s until it hits a line that has no comment
     * or one already claimed by another node (e.g. a trailing comment on a preceding field).
     */
    private String methodComment(
            MethodDeclaration method, Set<Comment> attachedComments, Map<Integer, LineComment> lineCommentsByLine) {
        Comment comment = method.getComment().orElseThrow();
        if (!comment.isLineComment()) {
            return comment.getContent().trim();
        }

        Deque<String> block = new ArrayDeque<>();
        block.addFirst(comment.getContent().trim());
        int line = comment.getBegin().map(position -> position.line).orElseThrow() - 1;
        LineComment precedingLine = lineCommentsByLine.get(line);
        while (precedingLine != null && !attachedComments.contains(precedingLine)) {
            block.addFirst(precedingLine.getContent().trim());
            line--;
            precedingLine = lineCommentsByLine.get(line);
        }
        return String.join("\n", block);
    }

    private Set<Comment> attachedComments(CompilationUnit compilationUnit) {
        Set<Comment> attached = Collections.newSetFromMap(new IdentityHashMap<>());
        compilationUnit.findAll(Node.class).forEach(node -> node.getComment().ifPresent(attached::add));
        return attached;
    }

    private Map<Integer, LineComment> lineCommentsByLine(CompilationUnit compilationUnit) {
        Map<Integer, LineComment> byLine = new HashMap<>();
        compilationUnit.getAllContainedComments().stream()
                .filter(Comment::isLineComment)
                .map(Comment::asLineComment)
                .forEach(lineComment -> lineComment.getBegin().ifPresent(position -> byLine.put(position.line, lineComment)));
        return byLine;
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
