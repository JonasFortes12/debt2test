package io.github.jonasfortes12.context.extraction;

import io.github.jonasfortes12.core.model.ExternalReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IssueReferenceExtractor {

    private static final ReferenceRule DEFAULT_RULE = IssueReferenceExtractor::extractDefaultReferences;
    private static final String JIRA_REFERENCE = "\\b[A-Z][A-Z0-9]{1,9}-[0-9]+\\b";
    private static final String TRELLO_REFERENCE =
            "(?i:(?<![A-Za-z0-9_])https?://(?:www\\.)?trello\\.com/c/[A-Za-z0-9]+"
                    + "(?:/[^\\s()\\[\\]{}<>\"']+)?)";
    private static final Pattern REFERENCE_PATTERN = Pattern.compile(
            "(?:" + JIRA_REFERENCE + ")|(?:" + TRELLO_REFERENCE + ")");
    private static final Pattern TRELLO_URL_PATTERN = Pattern.compile(TRELLO_REFERENCE);
    private static final String URL_TRAILING_PUNCTUATION = ".,;:!?()[]{}<>\\\"'";
    private final List<ReferenceRule> rules;

    public IssueReferenceExtractor() {
        this(List.of());
    }

    public IssueReferenceExtractor(List<ReferenceRule> additionalRules) {
        Objects.requireNonNull(additionalRules, "additionalRules must not be null");
        List<ReferenceRule> orderedRules = new ArrayList<>(additionalRules.size() + 1);
        orderedRules.add(DEFAULT_RULE);
        orderedRules.addAll(additionalRules);
        this.rules = List.copyOf(orderedRules);
    }

    /** Extracts syntax-only references; provider filtering remains provider-specific. */
    public List<ExternalReference> extract(String comment) {
        Objects.requireNonNull(comment, "comment must not be null");

        Map<String, ReferenceOccurrence> firstOccurrences = new LinkedHashMap<>();
        int sequence = 0;
        for (ReferenceRule rule : rules) {
            List<ReferenceMatch> ruleMatches = Objects.requireNonNull(
                    rule.extract(comment), "reference rule result must not be null");
            for (ReferenceMatch match : ruleMatches) {
                Objects.requireNonNull(match, "reference rule matches must not contain null");
                String value = normalizeReference(match.value());
                ReferenceOccurrence occurrence = new ReferenceOccurrence(value, match.offset(), sequence++);
                ReferenceOccurrence previous = firstOccurrences.get(value);
                if (previous == null || occurrence.offset() < previous.offset()) {
                    firstOccurrences.put(value, occurrence);
                }
            }
        }
        List<ReferenceOccurrence> occurrences = new ArrayList<>(firstOccurrences.values());
        occurrences.sort(Comparator.comparingInt(ReferenceOccurrence::offset)
                .thenComparingInt(ReferenceOccurrence::sequence));
        List<ExternalReference> references = new ArrayList<>(occurrences.size());
        for (ReferenceOccurrence occurrence : occurrences) {
            references.add(new ExternalReference(occurrence.value(), "comment"));
        }
        return List.copyOf(references);
    }

    private static List<ReferenceMatch> extractDefaultReferences(String comment) {
        List<ReferenceMatch> matches = new ArrayList<>();
        Matcher matcher = REFERENCE_PATTERN.matcher(comment);
        while (matcher.find()) {
            matches.add(new ReferenceMatch(matcher.group(), matcher.start()));
        }
        return List.copyOf(matches);
    }

    private static String normalizeReference(String value) {
        if (!isHttpUrl(value)) {
            return value;
        }
        String trimmed = stripTrailingUrlPunctuation(value);
        return isTrelloUrl(trimmed) ? normalizeTrelloUrl(trimmed) : trimmed;
    }

    private static boolean isHttpUrl(String value) {
        return value.regionMatches(true, 0, "http://", 0, 7)
                || value.regionMatches(true, 0, "https://", 0, 8);
    }

    private static boolean isTrelloUrl(String value) {
        return TRELLO_URL_PATTERN.matcher(value).matches();
    }

    private static String stripTrailingUrlPunctuation(String value) {
        int end = value.length();
        while (end > 0 && URL_TRAILING_PUNCTUATION.indexOf(value.charAt(end - 1)) >= 0) {
            end--;
        }
        return value.substring(0, end);
    }

    private static String normalizeTrelloUrl(String value) {
        int schemeEnd = value.indexOf("://");
        int pathStart = value.indexOf('/', schemeEnd + 3);
        String path = value.substring(pathStart);
        int routeEnd = path.indexOf('/', 1);
        String scheme = value.substring(0, schemeEnd).toLowerCase(Locale.ROOT);
        return scheme + "://trello.com/c" + path.substring(routeEnd);
    }

    private record ReferenceOccurrence(String value, int offset, int sequence) {
    }
}
