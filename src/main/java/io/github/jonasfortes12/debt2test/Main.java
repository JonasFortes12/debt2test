package io.github.jonasfortes12.debt2test;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

public class Main {
    public static void main(String[] args) {
        // String containing a simulated Java code with an SATD comment
        String simulatedCode = """
                public class Calculator {
                   // TODO: implement handling for division by zero
                   public int divide(int a, int b) {
                       return a / b;
                   }
                }
                """;

        try {
            // 1. Try to compile the string into a CompilationUnit (AST Root)
            CompilationUnit cu = StaticJavaParser.parse(simulatedCode);

            System.out.println("[SUCCESS] JavaParser read the syntactic structure successfully!");


            // 2. Search for the method inside the AST and retrieve the associated comment
            MethodDeclaration method = cu.findFirst(MethodDeclaration.class).get();

            if (method.getComment().isPresent()) {
                String comment = method.getComment().get().getContent();
                System.out.println("Method found: " + method.getNameAsString());
                System.out.println("Captured SATD comment: " + comment.trim());
            }

        } catch (Exception e) {
            System.err.println("[FAILURE] JavaParser encountered an error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}