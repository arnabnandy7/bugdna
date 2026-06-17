package io.github.bugdna.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class JavaSourceScanner {

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
        ScanState state = new ScanState();

        for (int index = 0; index < lines.size(); index++) {
            scanLine(lines, index, issues, state);
        }

        return issues;
    }

    private void scanLine(
            List<String> lines,
            int index,
            List<BuildScanIssue> issues,
            ScanState state
    ) {
        String code = stripLineComment(lines.get(index));
        String trimmed = code.trim();
        int lineNumber = index + 1;

        detectMethodStart(state, trimmed, lineNumber);
        detectGenericExceptionUsage(issues, lineNumber, trimmed);
        detectEmptyCatchBlock(issues, lines, index);
        detectUnhandledExceptionInMethod(issues, lineNumber, trimmed, state);
        updateMethodState(state, code);
        updateExceptionHandlingState(state, code, trimmed);
    }

    private void detectMethodStart(ScanState state, String trimmed, int lineNumber) {
        if (state.method == null) {
            state.method = detectMethod(trimmed, lineNumber);
        }
    }

    private void detectUnhandledExceptionInMethod(
            List<BuildScanIssue> issues,
            int lineNumber,
            String trimmed,
            ScanState state
    ) {
        if (state.method != null) {
            detectUnhandledException(
                    issues,
                    lineNumber,
                    trimmed,
                    state.method,
                    state.isInsideExceptionHandling()
            );
        }
    }

    private void updateMethodState(ScanState state, String code) {
        if (state.method == null) {
            return;
        }
        if (code.indexOf('{') >= 0) {
            state.method.sawOpeningBrace = true;
        }
        state.method.depth += count(code, '{') - count(code, '}');
        if (state.method.depth <= 0 && state.method.sawOpeningBrace) {
            state.method = null;
        }
    }

    private void updateExceptionHandlingState(ScanState state, String code, String trimmed) {
        state.blockDepth += count(code, '{') - count(code, '}');
        if (isExceptionHandlingBlock(trimmed) && code.indexOf('{') >= 0) {
            state.exceptionHandlingDepth = Math.max(
                    state.exceptionHandlingDepth,
                    state.blockDepth
            );
        }
        if (state.exceptionHandlingDepth > 0
                && state.blockDepth < state.exceptionHandlingDepth) {
            state.exceptionHandlingDepth = 0;
        }
    }

    private MethodContext detectMethod(String trimmed, int line) {
        if (trimmed.startsWith("if ") || trimmed.startsWith("for ") || trimmed.startsWith("while ")
                || trimmed.startsWith("switch ") || trimmed.startsWith("catch ")) {
            return null;
        }
        if (containsJavaTypeDeclaration(trimmed) || !looksLikeMethodDeclaration(trimmed)) {
            return null;
        }
        MethodContext context = new MethodContext();
        context.line = line;
        context.declaresThrows = containsWord(trimmed, "throws");
        context.depth = 0;
        context.sawOpeningBrace = false;
        return context;
    }

    private void detectGenericExceptionUsage(List<BuildScanIssue> issues, int line, String trimmed) {
        List<String> catchDeclarations = catchDeclarations(trimmed);
        for (String declaration : catchDeclarations) {
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
        if (declaresGenericException(trimmed) || createsGenericException(trimmed)) {
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
        if (!hasCatchClause(trimmed)) {
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
        return containsExceptionType(declaration);
    }

    private List<String> catchDeclarations(String value) {
        List<String> declarations = new ArrayList<>();
        int searchFrom = 0;
        int catchIndex = catchIndex(value, searchFrom);
        while (catchIndex >= 0) {
            int openParenthesis = nextNonWhitespace(value, catchIndex + "catch".length());
            int closeParenthesis = matchingParenthesis(value, openParenthesis);
            if (closeParenthesis >= 0) {
                declarations.add(value.substring(openParenthesis + 1, closeParenthesis));
                searchFrom = closeParenthesis + 1;
            } else {
                searchFrom = catchIndex + "catch".length();
            }
            catchIndex = catchIndex(value, searchFrom);
        }
        return declarations;
    }

    private boolean hasCatchClause(String value) {
        return catchIndex(value, 0) >= 0;
    }

    private int catchIndex(String value, int start) {
        int index = value.indexOf("catch", start);
        while (index >= 0) {
            if (isWordAt(value, index, "catch")) {
                int next = nextNonWhitespace(value, index + "catch".length());
                if (next < value.length() && value.charAt(next) == '(') {
                    return index;
                }
            }
            index = value.indexOf("catch", index + "catch".length());
        }
        return -1;
    }

    private int nextNonWhitespace(String value, int start) {
        int cursor = start;
        while (cursor < value.length() && Character.isWhitespace(value.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private int matchingParenthesis(String value, int openParenthesis) {
        if (openParenthesis < 0
                || openParenthesis >= value.length()
                || value.charAt(openParenthesis) != '(') {
            return -1;
        }
        int depth = 0;
        for (int index = openParenthesis; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private boolean declaresGenericException(String trimmed) {
        int throwsIndex = trimmed.indexOf("throws");
        if (throwsIndex < 0 || !isWordAt(trimmed, throwsIndex, "throws")) {
            return false;
        }
        int end = firstIndexOf(trimmed, throwsIndex, ';', '{');
        String declaration = end >= 0
                ? trimmed.substring(throwsIndex + "throws".length(), end)
                : trimmed.substring(throwsIndex + "throws".length());
        return containsExceptionType(declaration);
    }

    private boolean createsGenericException(String trimmed) {
        return containsWord(trimmed, "new")
                && (containsTypeConstruction(trimmed, "Exception")
                || containsTypeConstruction(trimmed, "Throwable")
                || containsTypeConstruction(trimmed, "RuntimeException"));
    }

    private boolean containsTypeConstruction(String value, String type) {
        int index = value.indexOf(type);
        while (index >= 0) {
            int cursor = index + type.length();
            while (cursor < value.length() && Character.isWhitespace(value.charAt(cursor))) {
                cursor++;
            }
            if (isWordAt(value, index, type) && cursor < value.length() && value.charAt(cursor) == '(') {
                return true;
            }
            index = value.indexOf(type, index + type.length());
        }
        return false;
    }

    private boolean containsExceptionType(String value) {
        return containsWord(value, "Exception")
                || containsWord(value, "Throwable")
                || containsWord(value, "RuntimeException");
    }

    private boolean containsJavaTypeDeclaration(String value) {
        return containsWord(value, "class")
                || containsWord(value, "interface")
                || containsWord(value, "enum")
                || containsWord(value, "record");
    }

    private boolean looksLikeMethodDeclaration(String value) {
        int openParenthesis = value.indexOf('(');
        int closeParenthesis = value.indexOf(')', openParenthesis + 1);
        if (openParenthesis <= 0 || closeParenthesis < 0 || value.indexOf(';') >= 0) {
            return false;
        }

        String beforeParameters = value.substring(0, openParenthesis).trim();
        int methodNameStart = lastIdentifierStart(beforeParameters);
        if (methodNameStart < 0) {
            return false;
        }

        String returnAndModifiers = beforeParameters.substring(0, methodNameStart).trim();
        return returnAndModifiers.length() > 0;
    }

    private int lastIdentifierStart(String value) {
        int cursor = value.length() - 1;
        while (cursor >= 0 && Character.isWhitespace(value.charAt(cursor))) {
            cursor--;
        }
        if (cursor < 0 || !Character.isJavaIdentifierPart(value.charAt(cursor))) {
            return -1;
        }
        while (cursor >= 0 && Character.isJavaIdentifierPart(value.charAt(cursor))) {
            cursor--;
        }
        return cursor + 1;
    }

    private boolean containsWord(String value, String word) {
        int index = value.indexOf(word);
        while (index >= 0) {
            if (isWordAt(value, index, word)) {
                return true;
            }
            index = value.indexOf(word, index + word.length());
        }
        return false;
    }

    private boolean isWordAt(String value, int index, String word) {
        if (index < 0 || index + word.length() > value.length()) {
            return false;
        }
        if (!value.regionMatches(index, word, 0, word.length())) {
            return false;
        }
        boolean startsCleanly = index == 0 || !Character.isJavaIdentifierPart(value.charAt(index - 1));
        int end = index + word.length();
        boolean endsCleanly = end == value.length() || !Character.isJavaIdentifierPart(value.charAt(end));
        return startsCleanly && endsCleanly;
    }

    private int firstIndexOf(String value, int start, char first, char second) {
        int firstIndex = value.indexOf(first, start);
        int secondIndex = value.indexOf(second, start);
        if (firstIndex < 0) {
            return secondIndex;
        }
        if (secondIndex < 0) {
            return firstIndex;
        }
        return Math.min(firstIndex, secondIndex);
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

    private static final class ScanState {
        private MethodContext method;
        private int blockDepth;
        private int exceptionHandlingDepth;

        private boolean isInsideExceptionHandling() {
            return exceptionHandlingDepth > 0 && blockDepth >= exceptionHandlingDepth;
        }
    }
}
