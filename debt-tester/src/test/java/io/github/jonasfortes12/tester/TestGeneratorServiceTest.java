package io.github.jonasfortes12.tester;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestGeneratorServiceTest {

    @Test
    public void testMockGenerationPipeline() throws Exception {
        File tempDir = new File("output-test");
        tempDir.mkdirs();
        File reportJson = new File(tempDir, "debt-report.json");

        String sampleReport = "[{" +
                "\"filePath\": \"src/Test.java\"," +
                "\"methodName\": \"foo\"," +
                "\"lineNumber\": 10," +
                "\"comment\": \"TODO: fix this\"," +
                "\"methodSourceCode\": \"void foo() {}\"," +
                "\"isSatd\": true," +
                "\"debtType\": \"TEST_DEBT\"" +
                "}]";

        try (FileWriter writer = new FileWriter(reportJson)) {
            writer.write(sampleReport);
        }

        LlmConfig config = new LlmConfig(); // No API key -> triggers mock mode
        TestGeneratorService service = new TestGeneratorService(config);
        service.processReport(reportJson, tempDir);

        File outputJson = new File(tempDir, "debt-test-report.json");
        File outputMd = new File(tempDir, "debt-test-report.md");

        assertTrue(outputJson.exists());
        assertTrue(outputMd.exists());

        // Cleanup
        outputJson.delete();
        outputMd.delete();
        reportJson.delete();
        tempDir.delete();
    }
}
