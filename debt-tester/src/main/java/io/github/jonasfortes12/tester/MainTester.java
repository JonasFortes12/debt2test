package io.github.jonasfortes12.tester;

import java.io.File;

public class MainTester {
    public static void main(String[] args) {
        System.out.println("=== STARTING DEBT TESTER MODULE ===");
        LlmConfig config = new LlmConfig();
        System.out.println("[INFO] LLM Provider: " + config.getProvider());
        System.out.println("[INFO] LLM Model: " + config.getModel());

        File reportFile = new File("output/debt-report.json");
        if (!reportFile.exists()) {
            System.err.println("[ERROR] debt-report.json not found at output/. Run debt-classifier first.");
            return;
        }

        File outputDir = new File("output");
        TestGeneratorService service = new TestGeneratorService(config);
        service.processReport(reportFile, outputDir);
        System.out.println("=== DEBT TESTER COMPLETE ===");
    }
}
