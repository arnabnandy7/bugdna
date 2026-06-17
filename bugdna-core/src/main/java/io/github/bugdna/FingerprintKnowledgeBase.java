package io.github.bugdna;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class FingerprintKnowledgeBase {

    private FingerprintKnowledgeBase() {
    }

    static Map<String, FingerprintKnowledge> read(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input must not be null");

        KnowledgeBaseParser parser = new KnowledgeBaseParser();
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                parser.accept(line, lineNumber);
            }
        }

        return parser.entries();
    }

    private static final class KnowledgeBaseParser {

        private final Map<String, FingerprintKnowledge> entries = new LinkedHashMap<>();
        private String currentId;
        private Map<String, String> currentFields;

        private void accept(String line, int lineNumber) {
            if (isBlankOrComment(line)) {
                return;
            }

            if (startsWithWhitespace(line)) {
                acceptField(line, lineNumber);
            } else {
                acceptEntry(line, lineNumber);
            }
        }

        private void acceptEntry(String line, int lineNumber) {
            saveCurrentEntry();
            currentId = parseEntryId(line, lineNumber);
            currentFields = new LinkedHashMap<>();
        }

        private void acceptField(String line, int lineNumber) {
            if (currentId == null) {
                throw invalid(lineNumber, "field must belong to a fingerprint ID");
            }
            parseField(line, currentFields, lineNumber);
        }

        private Map<String, FingerprintKnowledge> entries() {
            saveCurrentEntry();
            return Collections.unmodifiableMap(entries);
        }

        private void saveCurrentEntry() {
            if (currentId != null) {
                entries.put(currentId, new FingerprintKnowledge(currentId, currentFields));
            }
        }

        private static boolean isBlankOrComment(String line) {
            String trimmed = line.trim();
            return trimmed.isEmpty() || trimmed.startsWith("#");
        }

        private static boolean startsWithWhitespace(String line) {
            return !line.isEmpty() && Character.isWhitespace(line.charAt(0));
        }

        private static String parseEntryId(String line, int lineNumber) {
            String trimmed = stripComment(line).trim();
            if (!trimmed.endsWith(":")) {
                throw invalid(lineNumber, "fingerprint ID must end with ':'");
            }

            String id = trimmed.substring(0, trimmed.length() - 1).trim();
            if (id.isEmpty()) {
                throw invalid(lineNumber, "fingerprint ID must not be blank");
            }
            return id;
        }

        private static void parseField(
                String line,
                Map<String, String> fields,
                int lineNumber
        ) {
            String trimmed = stripComment(line).trim();
            int separator = trimmed.indexOf(':');
            if (separator <= 0) {
                throw invalid(lineNumber, "field must use 'name: value'");
            }

            String name = trimmed.substring(0, separator).trim();
            String value = trimmed.substring(separator + 1).trim();
            fields.put(name, unquote(value));
        }

        private static String stripComment(String line) {
            boolean singleQuoted = false;
            boolean doubleQuoted = false;

            for (int i = 0; i < line.length(); i++) {
                char current = line.charAt(i);
                if (current == '\'' && !doubleQuoted) {
                    singleQuoted = !singleQuoted;
                } else if (current == '"' && !singleQuoted) {
                    doubleQuoted = !doubleQuoted;
                } else if (current == '#' && !singleQuoted && !doubleQuoted) {
                    return line.substring(0, i);
                }
            }

            return line;
        }

        private static String unquote(String value) {
            if (value.length() < 2) {
                return value;
            }

            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
            return value;
        }

        private static IllegalArgumentException invalid(int lineNumber, String message) {
            return new IllegalArgumentException(
                    "Invalid BugDNA knowledge base at line " + lineNumber + ": " + message
            );
        }
    }
}
