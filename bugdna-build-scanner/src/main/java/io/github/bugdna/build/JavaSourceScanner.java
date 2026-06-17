package io.github.bugdna.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaSourceScanner {

    private static final Pattern CATCH_PATTERN = Pattern.compile("\\bcatch\\s*\\(([^)]*)\\)");
    private static final Pattern THROWS_PATTERN = Pattern.compile("\\bthrows\\b");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "^(?!.*\\b(class|interface|enum|record)\\b)"
                    + "(?:public|protected|private|static|final|synchronized|abstract|native|strictfp|\\s)*"
                    + "[\\w<>\\[\\], ?]+\\s+\\w+\\s*\\([^;]*\\)\\s*(throws\\s+[\\w\\s.,<>?]+)?\\{?"
    );
    private static final List<String> CHECKED_EXCEPTION_CALLS = Arrays.asList(
            "Files.read", "Files.write", "Files.copy", "Files.move", "Files.delete",
            "Files.newInputStream", "Files.newOutputStream", "new FileInputStream",
            "new FileOutputStream", "new FileReader", "new FileWriter", "Thread.sleep",
            "Class.forName", ".wait(", ".join(", ".openStream(", ".openConnection("
    );

    private final Path file;

    JavaSourceScanner(Path file) {
        this.file = file;
    }

    List<BuildScanIssue> scan() throws IOException {
        List<String> rawLines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> lines = stripBlockComments(rawLines);
        List<BuildScanIssue> issues = new ArrayList<>();
        MethodContext method = null;
        int blockDepth = 0;
        int exceptionHandlingDepth = 0;

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String code = stripLineComment(line);
            String trimmed = code.trim();
            boolean insideExceptionHandling = exceptionHandlingDepth > 0
                    && blockDepth >= exceptionHandlingDepth;

            if (method == null) {
                MethodContext detected = detectMethod(trimmed, index + 1);
                if (detected != null) {
                    method = detected;
                }
            }

            detectGenericExceptionUsage(issues, index + 1, trimmed);
            detectEmptyCatchBlock(issues, lines, index);
            if (method != null) {
                detectUnhandledException(issues, index + 1, trimmed, method, insideExceptionHandling);
            }

            if (method != null) {
                if (code.indexOf('{') >= 0) {
                    method.sawOpeningBrace = true;
                }
                method.depth += count(code, '{') - count(code, '}');
                if (method.depth <= 0 && method.sawOpeningBrace) {
                    method = null;
                }
            }

            blockDepth += count(code, '{') - count(code, '}');
            if (isExceptionHandlingBlock(trimmed) && code.indexOf('{') >= 0) {
                exceptionHandlingDepth = Math.max(exceptionHandlingDepth, blockDepth);
            }
            if (exceptionHandlingDepth > 0 && blockDepth < exceptionHandlingDepth) {
                exceptionHandlingDepth = 0;
            }
        }

        return issues;
    }

    private MethodContext detectMethod(String trimmed, int line) {
        if (trimmed.startsWith("if ") || trimmed.startsWith("for ") || trimmed.startsWith("while ")
                || trimmed.startsWith("switch ") || trimmed.startsWith("catch ")) {
            return null;
        }
        Matcher matcher = METHOD_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return null;
        }
        MethodContext context = new MethodContext();
        context.line = line;
        context.declaresThrows = THROWS_PATTERN.matcher(trimmed).find();
        context.depth = 0;
        context.sawOpeningBrace = false;
        return context;
    }

    private void detectGenericExceptionUsage(List<BuildScanIssue> issues, int line, String trimmed) {
        Matcher matcher = CATCH_PATTERN.matcher(trimmed);
        while (matcher.find()) {
            String declaration = matcher.group(1);
            if (usesGenericException(declaration)) {
                issues.add(issue(
                        BuildScanRule.GENERIC_EXCEPTION_USAGE,
                        BuildScanSeverity.WARNING,
                        line,
                        "Avoid catching generic Exception, Throwable, or RuntimeException.",
                        trimmed
                ));
            }
        }
        if (trimmed.matches(".*\\bthrows\\s+([^;{]*\\b)?(Exception|Throwable|RuntimeException)\\b.*")
                || trimmed.matches(".*\\bnew\\s+(Exception|Throwable|RuntimeException)\\s*\\(.*")) {
            issues.add(issue(
                    BuildScanRule.GENERIC_EXCEPTION_USAGE,
                    BuildScanSeverity.WARNING,
                    line,
                    "Use a specific exception type instead of generic Exception, Throwable, or RuntimeException.",
                    trimmed
            ));
        }
    }

    private void detectEmptyCatchBlock(List<BuildScanIssue> issues, List<String> lines, int index) {
        String trimmed = stripLineComment(lines.get(index)).trim();
        if (!CATCH_PATTERN.matcher(trimmed).find()) {
            return;
        }

        int openLine = index;
        int openColumn = trimmed.indexOf('{');
        while (openColumn < 0 && openLine + 1 < lines.size()) {
            openLine++;
            trimmed = stripLineComment(lines.get(openLine)).trim();
            openColumn = trimmed.indexOf('{');
        }
        if (openColumn < 0) {
            return;
        }

        StringBuilder body = new StringBuilder();
        int depth = 0;
        boolean insideBody = false;
        for (int i = openLine; i < lines.size(); i++) {
            String code = stripLineComment(lines.get(i));
            int start = i == openLine ? code.indexOf('{') : 0;
            for (int j = Math.max(start, 0); j < code.length(); j++) {
                char character = code.charAt(j);
                if (character == '{') {
                    depth++;
                    insideBody = true;
                    continue;
                }
                if (character == '}') {
                    depth--;
                    if (depth == 0) {
                        if (body.toString().trim().isEmpty()) {
                            issues.add(issue(
                                    BuildScanRule.EMPTY_CATCH_BLOCK,
                                    BuildScanSeverity.WARNING,
                                    index + 1,
                                    "Catch block is empty; log, rethrow, or document intentional suppression.",
                                    stripLineComment(lines.get(index)).trim()
                            ));
                        }
                        return;
                    }
                } else if (insideBody && depth > 0) {
                    body.append(character);
                }
            }
        }
    }

    private void detectUnhandledException(
            List<BuildScanIssue> issues,
            int line,
            String trimmed,
            MethodContext method,
            boolean insideExceptionHandling
    ) {
        if (method.declaresThrows || insideExceptionHandling || trimmed.startsWith("//")) {
            return;
        }
        for (String call : CHECKED_EXCEPTION_CALLS) {
            if (trimmed.contains(call)) {
                issues.add(issue(
                        BuildScanRule.UNHANDLED_EXCEPTION,
                        BuildScanSeverity.WARNING,
                        line,
                        "Checked-exception API appears outside try/catch or a method with throws.",
                        trimmed
                ));
                return;
            }
        }
    }

    private boolean usesGenericException(String declaration) {
        return declaration.matches(".*\\b(Exception|Throwable|RuntimeException)\\b.*");
    }

    private boolean isExceptionHandlingBlock(String trimmed) {
        return trimmed.startsWith("try")
                || trimmed.contains(" try ")
                || trimmed.startsWith("catch")
                || trimmed.contains(" catch ");
    }

    private BuildScanIssue issue(
            BuildScanRule rule,
            BuildScanSeverity severity,
            int line,
            String message,
            String snippet
    ) {
        return new BuildScanIssue(rule, severity, file, line, message, snippet);
    }

    private static List<String> stripBlockComments(List<String> lines) {
        List<String> stripped = new ArrayList<>(lines.size());
        boolean inBlock = false;
        for (String line : lines) {
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                if (inBlock) {
                    if (i + 1 < line.length() && line.charAt(i) == '*' && line.charAt(i + 1) == '/') {
                        inBlock = false;
                        i++;
                    }
                    continue;
                }
                if (i + 1 < line.length() && line.charAt(i) == '/' && line.charAt(i + 1) == '*') {
                    inBlock = true;
                    i++;
                    continue;
                }
                output.append(line.charAt(i));
            }
            stripped.add(output.toString());
        }
        return stripped;
    }

    private static String stripLineComment(String line) {
        int comment = line.indexOf("//");
        return comment >= 0 ? line.substring(0, comment) : line;
    }

    private static int count(String value, char expected) {
        int total = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == expected) {
                total++;
            }
        }
        return total;
    }

    private static final class MethodContext {
        private int line;
        private int depth;
        private boolean declaresThrows;
        private boolean sawOpeningBrace;
    }
}
