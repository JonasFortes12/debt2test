package io.github.jonasfortes12.orchestrator.util;

public final class OrchestrationUtils {

    private OrchestrationUtils() {
    }

    public static String valueOrDefault(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    public static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    public static String argumentOrDefault(String[] args, int index, String defaultValue) {
        return (args != null && index < args.length && args[index] != null && !args[index].isBlank())
                ? args[index]
                : defaultValue;
    }

    public static String toHexString(byte[] digest) {
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }

    public static void logPipeline(String message) {
        System.out.println("[pipeline] " + message);
    }
}
