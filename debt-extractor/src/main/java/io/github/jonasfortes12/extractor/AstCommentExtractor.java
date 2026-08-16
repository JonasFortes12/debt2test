package io.github.jonasfortes12.extractor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class AstCommentExtractor {

    public static class SatdCandidate {
        private String filePath;
        private String methodName;
        private int lineNumber;
        private String commentContent;
        private String methodSourceCode;

        public SatdCandidate(String filePath, String methodName, int lineNumber, String commentContent,
                String methodSourceCode) {
            this.filePath = filePath;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
            this.commentContent = commentContent;
            this.methodSourceCode = methodSourceCode;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getCommentContent() {
            return commentContent;
        }

        public String getMethodSourceCode() {
            return methodSourceCode;
        }
    }

    public List<SatdCandidate> extractFromProject(File projectRoot) {
        List<SatdCandidate> candidates = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(projectRoot.toPath())) {
            paths.filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> parseJavaFile(path.toFile(), candidates));
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to traverse project files: " + e.getMessage());
        }
        return candidates;
    }

    private void parseJavaFile(File file, List<SatdCandidate> candidates) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);
            cu.findAll(MethodDeclaration.class).forEach(method -> {
                if (method.getComment().isPresent()) {
                    String commentText = method.getComment().get().getContent();
                    int line = method.getBegin().isPresent() ? method.getBegin().get().line : 0;
                    String methodCode = method.toString();

                    candidates.add(new SatdCandidate(
                            file.getAbsolutePath(),
                            method.getNameAsString(),
                            line,
                            commentText.trim(),
                            methodCode));
                }
            });
        } catch (Exception e) {
            System.out
                    .println("[ERROR] Failed to analyze file: " + file.getAbsolutePath() + " - " + e.getMessage());
        }
    }
}