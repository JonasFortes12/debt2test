package io.github.jonasfortes12.tester;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TestGeneratorService {

    public static class SatdItem {
        private String filePath;
        private String methodName;
        private int lineNumber;
        private String comment;
        private String methodSourceCode;
        private boolean isSatd;
        private String debtType;
        private String generatedTestCode;
        private String status;

        public String getFilePath() { return filePath; }
        public String getMethodName() { return methodName; }
        public int getLineNumber() { return lineNumber; }
        public String getComment() { return comment; }
        public String getMethodSourceCode() { return methodSourceCode; }
        public boolean isSatd() { return isSatd; }
        public String getDebtType() { return debtType; }
        public String getGeneratedTestCode() { return generatedTestCode; }
        public void setGeneratedTestCode(String generatedTestCode) { this.generatedTestCode = generatedTestCode; }
        public void setStatus(String status) { this.status = status; }
    }

    private final LlmConfig config;
    private final LlmProvider provider;

    public TestGeneratorService(LlmConfig config) {
        this.config = config;
        if (config.getProvider().equalsIgnoreCase("anthropic")) {
            this.provider = new AnthropicProvider(config);
        } else if (config.getProvider().equalsIgnoreCase("gemini")) {
            this.provider = new GeminiProvider(config);
        } else {
            this.provider = new OpenAiProvider(config);
        }
    }

    public void processReport(File reportFile, File outputDir) {
        System.out.println("[INFO] Reading debt report from: " + reportFile.getAbsolutePath());
        List<SatdItem> items = new ArrayList<>();
        try (FileReader reader = new FileReader(reportFile)) {
            Type listType = new TypeToken<List<SatdItem>>(){}.getType();
            items = new Gson().fromJson(reader, listType);
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to read debt report: " + e.getMessage());
            return;
        }

        System.out.println("[INFO] Loaded " + items.size() + " SATD items for test generation.");
        List<SatdItem> results = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            SatdItem item = items.get(i);
            System.out.printf("[INFO] [%d/%d] Generating test for %s -> %s()%n", i+1, items.size(), new File(item.getFilePath()).getName(), item.getMethodName());
            
            if (config.getApiKey().isEmpty()) {
                System.out.println("[WARN] DEBT_TESTER_API_KEY is not set. Using mock test generator.");
                item.setGeneratedTestCode("// Mock Test generated because API key is missing\n@Test\nvoid test" + item.getMethodName() + "Debt() {\n    // TODO: Pay off " + item.getDebtType() + "\n}");
                item.setStatus("MOCK_SUCCESS");
            } else {
                try {
                    String testCode = provider.generateTest(item.getComment(), item.getDebtType(), item.getMethodSourceCode());
                    item.setGeneratedTestCode(testCode);
                    item.setStatus("SUCCESS");
                } catch (Exception e) {
                    System.err.println("[ERROR] LLM generation failed: " + e.getMessage());
                    item.setGeneratedTestCode("// ERROR: Failed to generate test - " + e.getMessage());
                    item.setStatus("ERROR");
                }
            }
            results.add(item);
        }

        exportReports(results, outputDir);
    }

    private void exportReports(List<SatdItem> results, File outputDir) {
        try {
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // JSON Report
            File jsonFile = new File(outputDir, "debt-test-report.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(jsonFile)) {
                gson.toJson(results, writer);
            }
            System.out.println("[SUCCESS] Consolidated JSON report saved to: " + jsonFile.getAbsolutePath());

            // Markdown Report
            File mdFile = new File(outputDir, "debt-test-report.md");
            try (FileWriter writer = new FileWriter(mdFile)) {
                writer.write("# Technical Debt Test Generation Report\n\n");
                writer.write("Generated test cases to pay off self-admitted technical debt (SATD).\n\n");
                for (SatdItem item : results) {
                    writer.write("## " + new File(item.getFilePath()).getName() + " -> " + item.getMethodName() + "()\n\n");
                    writer.write("- **Debt Type:** `" + item.getDebtType() + "`\n");
                    writer.write("- **Line Number:** `" + item.getLineNumber() + "`\n");
                    writer.write("- **Status:** `" + item.status + "`\n");
                    writer.write("- **Comment:** `" + item.getComment().replace("\n", " ") + "`\n\n");
                    writer.write("```java\n" + item.getMethodSourceCode() + "\n```\n\n");
                    writer.write("### Generated Test Case\n\n");
                    writer.write("```java\n" + item.getGeneratedTestCode() + "\n```\n\n");
                    writer.write("---\n\n");
                }
            }
            System.out.println("[SUCCESS] Consolidated Markdown report saved to: " + mdFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to export reports: " + e.getMessage());
        }
    }
}
