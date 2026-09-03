package io.github.jonasfortes12.orchestrator.util;

public final class OrchestrationUtils {

    private OrchestrationUtils() {
        // Utility class, no instances
    }

    /**
     * Return the provided value if it's not blank, otherwise return the default.
     *
     * @param value the value to check
     * @param defaultValue the fallback if value is blank
     * @return value or defaultValue
     */
    public static String valueOrDefault(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /**
     * Return the provided value, or empty string if null.
     *
     * @param value the value to check
     * @return value or empty string
     */
    public static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Extract argument at index from args, with fallback to default.
     * Returns default if index is out of bounds or value is blank.
     *
     * @param args the command-line arguments array (nullable)
     * @param index the position to read from
     * @param defaultValue the fallback
     * @return argument at index or defaultValue
     */
    public static String argumentOrDefault(String[] args, int index, String defaultValue) {
        return (args != null && index < args.length && args[index] != null && !args[index].isBlank())
                ? args[index]
                : defaultValue;
    }

    /**
     * Compute the hex digest of the given bytes.
     *
     * @param digest the message digest bytes
     * @return hex-encoded string (e.g., "a1b2c3...")
     */
    public static String toHexString(byte[] digest) {
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }

    /**
     * Print a pipeline progress message to standard output.
     *
     * @param message the message to log
     */
    public static void logPipeline(String message) {
        System.out.println("[pipeline] " + message);
    }
}
